package prasad.vennam.moneypilot.ui.dashboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R

enum class SyncState { SYNCED, SYNCING, PENDING_CONNECTION, FAILED }

@Composable
fun SyncStatusIndicator(state: SyncState) {
    val infiniteTransition = rememberInfiniteTransition(label = "syncPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "syncAlpha",
    )
    val (icon, color, label) =
        when (state) {
            SyncState.SYNCED -> Triple(Icons.Rounded.CloudDone, Color(0xFF4CAF50), stringResource(R.string.cloud_synced))
            SyncState.SYNCING ->
                Triple(
                    Icons.Rounded.CloudSync,
                    MaterialTheme.colorScheme.primary,
                    stringResource(R.string.syncing),
                )

            SyncState.PENDING_CONNECTION ->
                Triple(
                    Icons.Rounded.CloudOff,
                    MaterialTheme.colorScheme.outline,
                    stringResource(R.string.not_connected),
                )

            SyncState.FAILED ->
                Triple(
                    Icons.Rounded.Warning,
                    MaterialTheme.colorScheme.error,
                    stringResource(R.string.sync_failed),
                )
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .padding(end = 8.dp)
                .graphicsLayer { if (state == SyncState.SYNCING) this.alpha = alpha },
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
