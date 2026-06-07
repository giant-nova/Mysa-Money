package com.giantnovadevs.mysamoney.billing

import android.app.Activity
import android.app.Application
import com.giantnovadevs.mysamoney.config.ProUserGate
import com.giantnovadevs.mysamoney.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single source of truth for Pro entitlement across the app.
 *
 * Owns the BillingManager and PreferencesManager, wires their signals together,
 * and persists billing confirmations to DataStore. All ViewModels read from this
 * singleton instead of maintaining their own entitlement flows, preventing
 * split-brain between UI components.
 *
 * Lifecycle: application-scoped singleton — survives ViewModel recreation.
 */
class EntitlementRepository private constructor(app: Application) {

    private val preferencesManager = PreferencesManager(app)
    private val billingManager = BillingManager(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Combined Pro status: true if EITHER DataStore cache OR live billing confirms Pro.
     * DataStore provides instant startup reads; billing provides authoritative revocation.
     * SharingStarted.Eagerly so the flow is collecting before any subscriber arrives —
     * isProUser.value is always the current truth, safe to read from any lifecycle method.
     */
    val isProUser: StateFlow<Boolean> = combine(
        preferencesManager.isProUser,
        billingManager.isProUser
    ) { saved, billing ->
        ProUserGate.resolve(saved, billing)
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val proProductPrice: Flow<String> = billingManager.proProductDetails.map {
        it?.oneTimePurchaseOfferDetails?.formattedPrice ?: "..."
    }

    init {
        // Callbacks must be wired before connect() — see BillingManager for why
        billingManager.onPurchaseCompleted = {
            scope.launch { preferencesManager.saveProStatus(true) }
        }
        billingManager.onPurchaseRevoked = {
            scope.launch { preferencesManager.saveProStatus(false) }
        }
        billingManager.connect()
    }

    fun launchPurchase(activity: Activity) = billingManager.launchPurchaseFlow(activity)

    companion object {
        @Volatile private var INSTANCE: EntitlementRepository? = null

        fun getInstance(app: Application): EntitlementRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: EntitlementRepository(app).also { INSTANCE = it }
            }
    }
}
