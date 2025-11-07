package com.giantnovadevs.mysamoney.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// The product ID you just created in the Play Console
private const val PRO_PRODUCT_ID = "mysamoney_pro"

class BillingManager(private val context: Context) {

    private val TAG = "BillingManager"

    // This holds the "Pro" product details (like its price)
    private val _proProductDetails = MutableStateFlow<ProductDetails?>(null)
    val proProductDetails = _proProductDetails.asStateFlow()

    // This holds whether the user has *already* purchased Pro
    private val _isProUser = MutableStateFlow(false)
    val isProUser = _isProUser.asStateFlow()

    // Callback for when a purchase is completed
    var onPurchaseCompleted: () -> Unit = {}

    /**
     * This listener must be defined *before* the billingClient.
     */
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.products.contains(PRO_PRODUCT_ID)) {
                    _isProUser.value = true
                    acknowledgePurchase(purchase)
                    onPurchaseCompleted() // Trigger the callback
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "User canceled the purchase flow.")
        } else {
            Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    // 1. Initialize the Billing Client
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        connectToBillingService()
    }

    private fun connectToBillingService() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Billing service connected.")
                    // 2. Query for products and check past purchases
                    queryProductDetails()
                    queryPurchases()
                } else {
                    Log.e(TAG, "Billing service connection failed: ${billingResult.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected. Retrying...")
                connectToBillingService() // Retry connection
            }
        })
    }

    /**
     * Queries the Play Store for the "mysamoney_pro" product details.
     */
    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                _proProductDetails.value = productDetailsList[0]
                Log.i(TAG, "Pro product details found: ${productDetailsList[0].oneTimePurchaseOfferDetails?.formattedPrice}")
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Checks for any active purchases the user has already made.
     * This is how we know if they are a Pro user when they open the app.
     */
    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.products.contains(PRO_PRODUCT_ID) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        _isProUser.value = true
                        acknowledgePurchase(purchase) // Acknowledge the purchase
                    }
                }
            }
        }
    }

    /**
     * Launches the Google Play purchase screen for the "Pro" product.
     */
    fun launchPurchaseFlow(activity: Activity) {
        val productDetails = _proProductDetails.value ?: return // Don't launch if product not loaded

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Confirms to Google that the user has received their product.
     * If you don't do this, the purchase will be refunded in 3 days.
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgeParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Purchase acknowledged.")
                } else {
                    Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
                }
            }
        }
    }
}