package prasad.vennam.moneypilot.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay
import prasad.vennam.moneypilot.ads.AdConfig
import kotlin.time.Duration.Companion.milliseconds

private sealed interface AdState {
    data object Loading : AdState
    data object Loaded : AdState
    data object Failed : AdState
}

@Composable
fun AdBannerView(
    isPremium: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isPremium) return

    val context = LocalContext.current

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {

        val availableWidthDp = maxWidth.value.toInt()

        if (availableWidthDp <= 0) return@BoxWithConstraints

        var adState by remember(availableWidthDp) {
            mutableStateOf<AdState>(AdState.Loading)
        }

        var showPlaceholder by remember {
            mutableStateOf(true)
        }

        LaunchedEffect(Unit) {
            delay(3000.milliseconds)
            if (adState == AdState.Loading) {
                showPlaceholder = false
            }
        }

        val adSize = remember(availableWidthDp) {
            AdSize.getLargeAnchoredAdaptiveBannerAdSize(
                context,
                availableWidthDp
            )
        }

        val adView = remember(availableWidthDp) {
            AdView(context).apply {

                setAdSize(adSize)
                adUnitId = AdConfig.bannerAdUnitId

                adListener = object : AdListener() {

                    override fun onAdLoaded() {
                        adState = AdState.Loaded
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        adState = AdState.Failed
                    }
                }

                loadAd(
                    AdRequest.Builder().build()
                )
            }
        }

        DisposableEffect(adView) {
            onDispose {
                adView.destroy()
            }
        }

        when (adState) {

            AdState.Loaded -> {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { adView }
                )
            }

            AdState.Loading -> {
                if (showPlaceholder) {
                    BannerPlaceholder()
                }
            }

            AdState.Failed -> {
                // Hide completely
            }
        }
    }
}