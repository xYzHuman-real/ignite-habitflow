package com.ignite.habitflow

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/** Google Play Billing integration. Product IDs must be configured in Play Console before launch. */
class BillingManager(
    context: Context,
    private val onPremiumChanged: (Boolean) -> Unit,
    private val onMessage: (String) -> Unit
) : PurchasesUpdatedListener {
    companion object {
        const val MONTHLY = "premium_monthly"
        const val YEARLY = "premium_yearly"
        const val LIFETIME = "premium_lifetime"
    }

    private val client = BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build()
    private var subscriptions: List<ProductDetails> = emptyList()
    private var lifetime: ProductDetails? = null

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach(::acknowledgeIfNeeded)
            if (purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }) onPremiumChanged(true)
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            onMessage("Purchase unavailable: ${result.debugMessage}")
        }
    }

    fun connect() {
        if (client.isReady) { queryProducts(); queryExistingPurchases(); return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryExistingPurchases()
                } else onMessage("Google Play Billing is unavailable")
            }
            override fun onBillingServiceDisconnected() { }
        })
    }

    fun buy(activity: Activity, productId: String): Boolean {
        if (!client.isReady) {
            onMessage("Connecting to Google Play…")
            connect()
            return false
        }
        val product = if (productId == LIFETIME) lifetime else subscriptions.firstOrNull { it.productId == productId }
        if (product == null) {
            onMessage("Premium product is not available yet")
            return false
        }
        val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product)
        product.subscriptionOfferDetails?.firstOrNull()?.let { paramsBuilder.setOfferToken(it.offerToken) }
        val result = client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(paramsBuilder.build()))
                .build()
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) onMessage(result.debugMessage)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun queryProducts() {
        val subs = listOf(MONTHLY, YEARLY).map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(subs).build()) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) subscriptions = details
        }

        val oneTime = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(LIFETIME)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(oneTime)).build()) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) lifetime = details.firstOrNull()
        }
    }

    private fun queryExistingPurchases() {
        listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP).forEach { type ->
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach(::acknowledgeIfNeeded)
                    if (purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }) onPremiumChanged(true)
                }
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            ) { }
        }
    }
}
