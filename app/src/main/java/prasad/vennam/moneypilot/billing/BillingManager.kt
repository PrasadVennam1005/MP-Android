package prasad.vennam.moneypilot.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import prasad.vennam.moneypilot.data.UserPreferences

class BillingManager(
    private val context: Context,
    private val userPreferences: UserPreferences,
    private val externalScope: CoroutineScope,
) : PurchasesUpdatedListener {
    private lateinit var billingClient: BillingClient

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    companion object {
        const val PRODUCT_LIFETIME = "premium_upgrade_lifetime"
        const val PRODUCT_SUBSCRIPTION = "premium_subscription_monthly"
    }

    init {
        initializeBillingClient()
    }

    private fun initializeBillingClient() {
        billingClient =
            BillingClient
                .newBuilder(context)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build()

        connectToBillingService()
    }

    private fun connectToBillingService() {
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        queryProductDetails()
                        queryPurchases()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                    // We could add backoff logic here.
                }
            },
        )
    }

    private fun queryProductDetails() {
        val productList =
            listOf(
                QueryProductDetailsParams.Product
                    .newBuilder()
                    .setProductId(PRODUCT_LIFETIME)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
                QueryProductDetailsParams.Product
                    .newBuilder()
                    .setProductId(PRODUCT_SUBSCRIPTION)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
            )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = queryResult.productDetailsList
            }
        }
    }

    private fun queryPurchases() {
        if (!billingClient.isReady) return

        externalScope.launch(Dispatchers.IO) {
            val inAppResult =
                billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                )
            val subsResult =
                billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                )

            var isPremium = false

            if (inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                inAppResult.purchasesList.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        isPremium = true
                        acknowledgePurchaseIfNeeded(purchase)
                    }
                }
            }

            if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                subsResult.purchasesList.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        isPremium = true
                        acknowledgePurchaseIfNeeded(purchase)
                    }
                }
            }

            userPreferences.setPremium(isPremium)
        }
    }

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
    ) {
        val productDetailsParamsList =
            listOf(
                BillingFlowParams.ProductDetailsParams
                    .newBuilder()
                    .setProductDetails(productDetails)
                    .apply {
                        // If it's a subscription, we need an offerToken
                        productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { token ->
                            setOfferToken(token)
                        }
                    }.build(),
            )

        val billingFlowParams =
            BillingFlowParams
                .newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // Handle an error caused by a user cancelling the purchase flow.
            }
            else -> {
                // Handle any other error codes.
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            externalScope.launch(Dispatchers.IO) {
                userPreferences.setPremium(true)
                acknowledgePurchaseIfNeeded(purchase)
            }
        }
    }

    private suspend fun acknowledgePurchaseIfNeeded(purchase: Purchase) {
        if (!purchase.isAcknowledged) {
            val acknowledgePurchaseParams =
                AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

            withContext(Dispatchers.IO) {
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Purchase acknowledged
                    }
                }
            }
        }
    }
}
