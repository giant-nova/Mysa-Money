package com.giantnovadevs.mysamoney.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min

private const val PRO_PRODUCT_ID = "mysamoney_pro"
private const val RECONNECT_DELAY_BASE_MS = 1_000L
private const val RECONNECT_DELAY_MAX_MS = 32_000L

class BillingManager(private val context: Context) {

    private val TAG = "BillingManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var reconnectDelayMs = RECONNECT_DELAY_BASE_MS

    private val _proProductDetails = MutableStateFlow<ProductDetails?>(null)
    val proProductDetails = _proProductDetails.asStateFlow()

    private val _isProUser = MutableStateFlow(false)
    val isProUser = _isProUser.asStateFlow()

    // Set by EntitlementRepository before connect() is called — guaranteed no race.
    var onPurchaseCompleted: () -> Unit = {}
    var onPurchaseRevoked: () -> Unit = {}

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.products.contains(PRO_PRODUCT_ID)) {
                        _isProUser.value = true
                        acknowledgePurchase(purchase)
                        onPurchaseCompleted()
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User cancelled purchase flow.")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Re-query to sync — purchase exists but the flow wasn't completed
                queryPurchases()
            }
            else -> {
                Log.e(TAG, "Purchase update failed: ${billingResult.debugMessage}")
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    /**
     * Called by EntitlementRepository after callbacks are wired.
     * Separating connect() from init ensures callbacks are always set
     * before the first queryPurchases() result arrives.
     */
    fun connect() {
        connectToBillingService()
    }

    private fun connectToBillingService() {
        if (billingClient.isReady) return
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Billing connected.")
                    reconnectDelayMs = RECONNECT_DELAY_BASE_MS // reset backoff on success
                    queryProductDetails()
                    queryPurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected. Retrying in ${reconnectDelayMs}ms.")
                scope.launch {
                    delay(reconnectDelayMs)
                    reconnectDelayMs = min(reconnectDelayMs * 2, RECONNECT_DELAY_MAX_MS)
                    connectToBillingService()
                }
            }
        })
    }

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRO_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && details.isNotEmpty()) {
                _proProductDetails.value = details[0]
                Log.i(TAG, "Product price: ${details[0].oneTimePurchaseOfferDetails?.formattedPrice}")
            } else {
                Log.e(TAG, "Product detail query failed: ${result.debugMessage}")
            }
        }
    }

    internal fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            Log.i(TAG, "queryPurchases: code=${result.responseCode} count=${purchases.size}")
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                var foundPro = false
                for (purchase in purchases) {
                    Log.i(TAG, "  purchase: ${purchase.products} state=${purchase.purchaseState}")
                    if (!purchase.products.contains(PRO_PRODUCT_ID)) continue

                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            foundPro = true
                            _isProUser.value = true
                            acknowledgePurchase(purchase)
                            onPurchaseCompleted() // persist to DataStore
                        }
                        Purchase.PurchaseState.PENDING -> {
                            // Payment initiated but not completed (UPI, bank transfer, etc.).
                            // Don't grant Pro yet, but also don't revoke existing DataStore status —
                            // the payment is in-flight and will resolve to PURCHASED or cancelled.
                            foundPro = true
                            Log.i(TAG, "Pro purchase PENDING — preserving existing status.")
                        }
                        else -> Unit
                    }
                }
                if (!foundPro) {
                    // Billing confirmed OK with no Pro purchase at all —
                    // safe to revoke stale DataStore status (e.g. leftover beta tester cache).
                    // NOTE: only fires if the test purchase was manually refunded in Play Console.
                    _isProUser.value = false
                    onPurchaseRevoked()
                }
            }
            // On billing error / offline: keep DataStore cache as fallback (handled by EntitlementRepository combine())
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val details = _proProductDetails.value ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            ))
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.i(TAG, "Purchase acknowledged.")
            } else {
                Log.e(TAG, "Acknowledge failed: ${result.debugMessage} — will retry on next launch.")
            }
        }
    }
}
