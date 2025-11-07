package com.giantnovadevs.mysamoney.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giantnovadevs.mysamoney.billing.BillingManager
import com.giantnovadevs.mysamoney.data.PreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

class ProViewModel(app: Application) : AndroidViewModel(app) {

    private val preferencesManager = PreferencesManager(app)
    private val billingManager = BillingManager(app)

    // This is the "single source of truth" for Pro status.
    // It combines the saved preference (fast check) with the
    // real-time check from the BillingManager (slower, but authoritative).
    val isProUser = combine(
        preferencesManager.isProUser,
        billingManager.isProUser
    ) { savedStatus, billingStatus ->
        savedStatus || billingStatus // If either is true, they are Pro
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    fun consumeTestPurchase(onConsumed: () -> Unit) {
        billingManager.consumeTestPurchase {
            // After consuming, we also clear our saved preference
            viewModelScope.launch {
                preferencesManager.saveProStatus(false)
                onConsumed()
            }
        }
    }
}