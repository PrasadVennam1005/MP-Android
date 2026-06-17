package prasad.vennam.moneypilot.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager(
    private val context: Context,
) {
    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    private val adUnitId = AdConfig.interstitialAdUnitId
    var saveCounter = 0

    fun loadAd() {
        if (interstitialAd != null || isAdLoading) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("InterstitialAdManager", adError.toString())
                    interstitialAd = null
                    isAdLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d("InterstitialAdManager", "Ad was loaded.")
                    interstitialAd = ad
                    isAdLoading = false
                }
            },
        )
    }

    fun showAd(
        activity: Activity,
        onAdDismissed: () -> Unit,
    ) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("InterstitialAdManager", "Ad was dismissed.")
                        interstitialAd = null
                        onAdDismissed()
                        loadAd() // Preload the next ad
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d("InterstitialAdManager", "Ad failed to show.")
                        interstitialAd = null
                        onAdDismissed()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d("InterstitialAdManager", "Ad showed fullscreen content.")
                    }
                }
            interstitialAd?.show(activity)
        } else {
            Log.d("InterstitialAdManager", "The interstitial ad wasn't ready yet.")
            onAdDismissed()
            loadAd() // Preload for next time
        }
    }

    fun showAdIfNeeded(
        activity: Activity,
        isPremium: Boolean,
        onAdDismissed: () -> Unit,
    ) {
        if (isPremium) {
            onAdDismissed()
            return
        }
        saveCounter++
        if (saveCounter % 3 == 0) {
            showAd(activity, onAdDismissed)
        } else {
            onAdDismissed()
        }
    }
}
