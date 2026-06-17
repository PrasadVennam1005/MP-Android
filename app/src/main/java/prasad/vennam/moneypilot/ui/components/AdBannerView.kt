package prasad.vennam.moneypilot.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdBannerView(
    isPremium: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isPremium) {
        return // Do not render anything if premium
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = prasad.vennam.moneypilot.ads.AdConfig.bannerAdUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
