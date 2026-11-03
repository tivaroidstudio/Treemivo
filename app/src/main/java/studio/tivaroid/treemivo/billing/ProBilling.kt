package studio.tivaroid.treemivo.billing

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.*

/**
 * One-time Google Play purchase. Never cache an arbitrary "isPro" preference.
 * A production backend should verify purchase tokens via the Play Developer API.
 */
class ProBilling(app: Application) : PurchasesUpdatedListener {
    companion object { const val PRODUCT_ID = "treemivo_pro_lifetime" }

    private val client = BillingClient.newBuilder(app)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    var isPro by mutableStateOf(false)
        private set
    var price by mutableStateOf<String?>(null)
        private set
    var connected by mutableStateOf(false)
        private set
    var status by mutableStateOf<String?>(null)
        private set
    private var productDetails: ProductDetails? = null

    init {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() { connected = false }
            override fun onBillingSetupFinished(result: BillingResult) {
                connected = result.responseCode == BillingClient.BillingResponseCode.OK
                if (connected) {
                    restore()
                    queryProduct()
                } else status = result.debugMessage
            }
        })
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        ) { result, data ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = data.productDetailsList.firstOrNull { it.productId == PRODUCT_ID }
                price = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    fun restore() {
        if (!client.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                status = result.debugMessage
                return@queryPurchasesAsync
            }
            val valid = purchases.filter {
                PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            isPro = valid.isNotEmpty()
            valid.filterNot { it.isAcknowledged }.forEach { purchase ->
                client.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken).build()
                ) { ack ->
                    if (ack.responseCode != BillingClient.BillingResponseCode.OK)
                        status = ack.debugMessage
                }
            }
        }
    }

    fun purchase(activity: Activity) {
        val details = productDetails
        if (!client.isReady || details == null) {
            status = "Product unavailable. First create it in Google Play Console."
            return
        }
        val item = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details).build()
        val result = client.launchBillingFlow(
            activity, BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(item)).build()
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK)
            status = result.debugMessage
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) restore()
        else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED)
            status = result.debugMessage
    }

    fun close() { client.endConnection() }
}
