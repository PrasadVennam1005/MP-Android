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
import prasad.vennam.moneypilot.BuildConfig
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.util.Security

class BillingManager(
    private val context: Context,
    private val userPreferences: UserPreferences,
    private val externalScope: CoroutineScope,
) : PurchasesUpdatedListener {
    private lateinit var billingClient: BillingClient

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private var retryCount = 0

    companion object {
        const val PRODUCT_LIFETIME = "premium_upgrade_lifetime_1499"
        const val PRODUCT_SUBSCRIPTION = "premium_subscription_monthly"
        const val PRODUCT_SUBSCRIPTION_ALT = "premium_subscription_monthly_49"
        private const val BASE64_PLAY_CONSOLE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAphouDOfSj+OcfrmXxHhc6C7lL9nruK7Pg4zxqc50vNkXesZFuDR7TV9EocFX51Ao1cnZlSkSulAHIPIqkx79b2LviXT7u10/IP5rcmxLw9qrZqhzCEf80XvNR9YWbmMbOGgZ1zIKUdexmu00cLESUpVKYDTxQbCCX1ev3pmYQQNmakgZBAuGRMK0tfW1IsOv6oSH2BJNOQokjI6wcY5EuAWZOfhM32yw3mkQbpzmn6fY6L+kNQ01ptqXCrZsfNeO+f9Rz7cbeQDLQyZpoDZeTPGxVtyIR3USaV2762g0ZeQRqNBZdUKGeSJLi8nhVawLADHUt1EEL8UI321UnJgzXwIDAQAB"
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
        android.util.Log.d("BillingManager", "connectToBillingService() called")
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    android.util.Log.d(
                        "BillingManager",
                        "onBillingSetupFinished responseCode: ${billingResult.responseCode}, debugMessage: ${billingResult.debugMessage}"
                    )
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        retryCount = 0
                        queryProductDetails()
                        queryPurchases()
                    } else {
                        android.util.Log.e(
                            "BillingManager",
                            "Billing setup failed: responseCode = ${billingResult.responseCode}, message = ${billingResult.debugMessage}"
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    android.util.Log.w("BillingManager", "onBillingServiceDisconnected() called, retryCount = $retryCount")
                    if (retryCount < 3) {
                        retryCount++
                        val delayMs = Math.pow(2.0, retryCount.toDouble()).toLong() * 1000
                        externalScope.launch {
                            kotlinx.coroutines.delay(delayMs)
                            connectToBillingService()
                        }
                    }
                }
            },
        )
    }

    private fun queryProductDetails() {
        android.util.Log.d("BillingManager", "queryProductDetails() called")
        val inAppProducts = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val subsProducts = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_SUBSCRIPTION)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_SUBSCRIPTION_ALT)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val tempProductsList = mutableListOf<ProductDetails>()

        // 1. Query One-time (INAPP) Products
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(inAppProducts)
            .build()
        android.util.Log.d("BillingManager", "Querying INAPP products: $PRODUCT_LIFETIME")
        billingClient.queryProductDetailsAsync(inAppParams) { billingResultInApp, queryResultInApp ->
            android.util.Log.d(
                "BillingManager",
                "INAPP query responseCode: ${billingResultInApp.responseCode}, debugMessage: ${billingResultInApp.debugMessage}"
            )
            if (billingResultInApp.responseCode == BillingClient.BillingResponseCode.OK) {
                android.util.Log.d("BillingManager", "INAPP products retrieved: ${queryResultInApp.productDetailsList.size}")
                queryResultInApp.productDetailsList.forEach {
                    android.util.Log.d("BillingManager", "INAPP product: ${it.productId}, name: ${it.name}")
                }
                tempProductsList.addAll(queryResultInApp.productDetailsList)
                _products.value = tempProductsList.toList()
            } else {
                android.util.Log.e("BillingManager", "Failed to query INAPP products: ${billingResultInApp.debugMessage}")
            }

            // 2. Query Subscription (SUBS) Products
            val subsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(subsProducts)
                .build()
            android.util.Log.d("BillingManager", "Querying SUBS products: $PRODUCT_SUBSCRIPTION")
            billingClient.queryProductDetailsAsync(subsParams) { billingResultSubs, queryResultSubs ->
                android.util.Log.d(
                    "BillingManager",
                    "SUBS query responseCode: ${billingResultSubs.responseCode}, debugMessage: ${billingResultSubs.debugMessage}"
                )
                if (billingResultSubs.responseCode == BillingClient.BillingResponseCode.OK) {
                    android.util.Log.d("BillingManager", "SUBS products retrieved: ${queryResultSubs.productDetailsList.size}")
                    queryResultSubs.productDetailsList.forEach {
                        android.util.Log.d("BillingManager", "SUBS product: ${it.productId}, name: ${it.name}")
                    }
                    val existingIds = tempProductsList.map { it.productId }.toSet()
                    tempProductsList.addAll(queryResultSubs.productDetailsList.filter { it.productId !in existingIds })
                    _products.value = tempProductsList.toList()
                } else {
                    android.util.Log.e("BillingManager", "Failed to query SUBS products: ${billingResultSubs.debugMessage}")
                }
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
                        if (BuildConfig.DEBUG || Security.verifyPurchase(BASE64_PLAY_CONSOLE_KEY, purchase.originalJson, purchase.signature)) {
                            isPremium = true
                            acknowledgePurchaseIfNeeded(purchase)
                        }
                    }
                }
            }

            if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                subsResult.purchasesList.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (BuildConfig.DEBUG || Security.verifyPurchase(BASE64_PLAY_CONSOLE_KEY, purchase.originalJson, purchase.signature)) {
                            isPremium = true
                            acknowledgePurchaseIfNeeded(purchase)
                        }
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
            if (BuildConfig.DEBUG || Security.verifyPurchase(BASE64_PLAY_CONSOLE_KEY, purchase.originalJson, purchase.signature)) {
                externalScope.launch(Dispatchers.IO) {
                    userPreferences.setPremium(true)
                    acknowledgePurchaseIfNeeded(purchase)
                }
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
