package prasad.vennam.moneypilot.ads

import prasad.vennam.moneypilot.BuildConfig

object AdConfig {
    // --- Banner Ads ---
    private const val BANNER_TEST_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val BANNER_PROD_ID = BuildConfig.BANNER_PROD_ID

    // --- Interstitial Ads ---
    private const val INTERSTITIAL_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val INTERSTITIAL_PROD_ID = BuildConfig.INTERSTITIAL_PROD_ID

    val bannerAdUnitId: String
        get() = if (BuildConfig.DEBUG) BANNER_TEST_ID else BANNER_PROD_ID.ifEmpty { BANNER_TEST_ID }

    val interstitialAdUnitId: String
        get() = if (BuildConfig.DEBUG) INTERSTITIAL_TEST_ID else INTERSTITIAL_PROD_ID.ifEmpty { INTERSTITIAL_TEST_ID }
}
