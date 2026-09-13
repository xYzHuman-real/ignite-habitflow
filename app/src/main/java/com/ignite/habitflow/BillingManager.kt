package com.ignite.habitflow

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams

/** Google Play Billing integration. Product IDs must be created in Play Console before launch. */
class BillingManager(
    context: Context,
    private val onPremiumChanged: (Boolean) -> Unit,
    private val onMessage: (String) -> Unit
) : PurchasesUpdatedListenerCompat {
    companion object {
        const val MONTHLY = "premium_monthly"
        const val YEARLY = "premium_yearly"
        const val LIFETIME = "premium_lifetime"
    }

    private val client = BillingClient.newBuilder(context)
        .setListener { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .forEach { acknowledgeIfNeeded(it) }
                onPremiumChanged(purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED })
            } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                onMessage("Purchase unavailable: ${result.debugMessage}")
            }
        }
        .enablePendingPurchases()
        .build()

    private var products: List<ProductDetails> = emptyList()

    fun connect() {
        if (client.isReady) { queryProducts(); queryExistingPurchases(); return }
        client.startConnection(object : BillingClientStateListenerCompat {
            override fun onSetupFinished(result: BillingResultCompat) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts(); queryExistingPurchases()
                } else onMessage("Google Play Billing is unavailable")
            }
            override fun onServiceDisconnected() { }
        })
    }

    fun buy(activity: Activity, productId: String): Boolean {
        val product = products.firstOrNull { it.productId == productId } ?: run {
            onMessage("Premium product is not available yet")
            return false
        }
        val offer = product.subscriptionOfferDetails?.firstOrNull()?.let {
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .setOfferToken(it.offerToken)
                .build()
        } ?: BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .build()
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(offer)).build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) onMessage(result.debugMessage)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun queryProducts() {
        val ids = listOf(MONTHLY, YEARLY, LIFETIME).map {
            QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(ProductType.SUBS).build()
        }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(ids).build()) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) products = details.productDetailsList
        }
    }

    private fun queryExistingPurchases() {
        client.queryPurchasesAsync(BillingClient.ProductType.SUBS) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { acknowledgeIfNeeded(it) }
                onPremiumChanged(purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED })
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            client.acknowledgePurchase(
                com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken).build()
            ) { }
        }
    }
}

/** Small compatibility aliases keep the UI-facing billing class easy to test. */
typealias BillingResultCompat = com.android.billingclient.api.BillingResult
typealias BillingClientStateListenerCompat = com.android.billingclient.api.BillingClientStateListener
typealias PurchasesUpdatedListenerCompat = Any
