package com.giantnovadevs.mysamoney.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giantnovadevs.mysamoney.billing.BillingManager
import com.giantnovadevs.mysamoney.config.ProUserGate
import com.giantnovadevs.mysamoney.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

class ProViewModel(app: Application) : AndroidViewModel(app) {

    private val preferencesManager = PreferencesManager(app)
    private val billingManager = BillingManager(app)

    // Single source of truth for Pro status across the app.
    // Uses ProUserGate, which also supports one-place test override.
    // Eagerly so DataStore is read the moment the ViewModel is created,
    // not deferred until the first UI subscriber. Avoids a "false" flash
    // on launch for returning Pro users.
    val isProUser = combine(
        preferencesManager.isProUser,
        billingManager.isProUser
    ) { savedStatus, billingStatus ->
        ProUserGate.resolve(savedStatus, billingStatus)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Expose the product details (like "₹199") to the UI
    val proProductPrice = billingManager.proProductDetails.map {
        it?.oneTimePurchaseOfferDetails?.formattedPrice ?: "Loading..."
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "...")

    init {
        // When a purchase is completed, save it to our preferences
        billingManager.onPurchaseCompleted = {
            viewModelScope.launch {
                preferencesManager.saveProStatus(true)
            }
        }
    }

    /**
     * Called by the "Upgrade to Pro" button.
     */
    fun launchPurchase(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }

    val freeScansRemaining = preferencesManager.freeScansRemaining
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    // A function to call when a scan is used
    fun useFreeScan() {
        viewModelScope.launch(Dispatchers.IO) { // Run on IO thread
            preferencesManager.decrementFreeScans()
        }
    }
}