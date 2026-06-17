package prasad.vennam.moneypilot.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * KPI card height reference:
 *   padding(16) + icon(20) + spacer(8) + labelMedium(~16) + titleLarge(~28) + padding(16) ≈ 104dp
 */
private val KPI_CARD_HEIGHT = 100.dp

@Composable
fun AdBannerView(
    isPremium: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isPremium) return

    val context = LocalContext.current

    // BoxWithConstraints measures the *actual* available width after parent
    // padding/margins are applied — so the AdSize never overflows the container.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // maxWidth is the real pixel-accurate width of this slot in dp
        val availableWidthDp = maxWidth.value.toInt()

        val adSize = remember(availableWidthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, availableWidthDp)
        }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(KPI_CARD_HEIGHT),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adSize)
                    adUnitId = prasad.vennam.moneypilot.ads.AdConfig.bannerAdUnitId
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = { adView ->
                // Re-calculate and reload if available width changed (e.g. orientation)
                val newSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    context,
                    availableWidthDp,
                )
                if (adView.adSize != newSize) {
                    adView.setAdSize(newSize)
                    adView.loadAd(AdRequest.Builder().build())
                }
            },
        )
    }
}
