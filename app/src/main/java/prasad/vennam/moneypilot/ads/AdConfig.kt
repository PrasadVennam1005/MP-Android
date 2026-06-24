package prasad.vennam.moneypilot.ads

import prasad.vennam.moneypilot.BuildConfig

object AdConfig {
    // --- Banner Ads ---
    private const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val BANNER_PROD_ID = BuildConfig.BANNER_PROD_ID

    // --- Interstitial Ads ---
    private const val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val INTERSTITIAL_PROD_ID = BuildConfig.INTERSTITIAL_PROD_ID

    // --- Rewarded Ads ---
    private const val REWARDED_TEST_ID = "ca-app-pub-3940256099942544/5224354917"
    private const val REWARDED_PROD_ID = BuildConfig.REWARDED_PROD_ID

    // --- Native Ads ---
    private const val NATIVE_TEST_ID = "ca-app-pub-3940256099942544/2247696110"
    private const val NATIVE_PROD_ID = BuildConfig.NATIVE_PROD_ID

    val bannerAdUnitId: String
        get() = if (BuildConfig.DEBUG) BANNER_TEST_ID else BANNER_PROD_ID.ifEmpty { BANNER_TEST_ID }

    val interstitialAdUnitId: String
        get() = if (BuildConfig.DEBUG) INTERSTITIAL_TEST_ID else INTERSTITIAL_PROD_ID.ifEmpty { INTERSTITIAL_TEST_ID }

    val rewardedAdUnitId: String
        get() = if (BuildConfig.DEBUG) REWARDED_TEST_ID else REWARDED_PROD_ID.ifEmpty { REWARDED_TEST_ID }

    val nativeAdUnitId: String
        get() = if (BuildConfig.DEBUG) NATIVE_TEST_ID else NATIVE_PROD_ID.ifEmpty { NATIVE_TEST_ID }
}
