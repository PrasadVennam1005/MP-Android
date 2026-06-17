package prasad.vennam.moneypilot.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * KPI card height reference:
 *   padding(16) + icon(20) + spacer(8) + labelSmall(~16) + titleLarge(~28) + padding(16) ≈ 104dp
 *
 * We use an inline adaptive banner so the ad fills the device width exactly
 * and we constrain it to the KPI card height (104dp) so the layout is consistent.
 */
private val KPI_CARD_HEIGHT = 104.dp

@Composable
fun AdBannerView(
    isPremium: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isPremium) return

    val context = LocalContext.current
    // Screen width in dp for the adaptive ad size calculation
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    // Create the adaptive AdSize once — it matches the device screen width
    // and chooses the closest supported height (typically 50–90dp).
    // We then constrain the composable height to KPI_CARD_HEIGHT so it
    // visually aligns with the KPI row above/below it.
    val adSize = remember(screenWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }

    AndroidView(
        modifier = modifier
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
            // If orientation changes, reload with new adaptive size
            val newSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                context,
                screenWidthDp,
            )
            if (adView.adSize != newSize) {
                adView.setAdSize(newSize)
                adView.loadAd(AdRequest.Builder().build())
            }
        },
    )
}
