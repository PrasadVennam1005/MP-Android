package prasad.vennam.moneypilot.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.dashboard.SyncStatusIndicator
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(
    userData: UserPreferences.UserData?,
    syncState: SyncState?,
    unreadCount: Int,
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit,
    showAiChat: Boolean = false,
    onAiChatClick: () -> Unit = {},
) {
    val greeting =
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> stringResource(R.string.good_morning)
            in 12..16 -> stringResource(R.string.good_afternoon)
            else -> stringResource(R.string.good_evening)
        }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = "$greeting,",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = userData?.name ?: stringResource(R.string.user),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            if (syncState != null) {
                SyncStatusIndicator(syncState)
            }
            AnimatedVisibility(
                visible = showAiChat,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                IconButton(onClick = onAiChatClick) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = stringResource(R.string.ai_assistant),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = onNotificationClick) {
                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ) {
                                Text(unreadCount.toString())
                            }
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = stringResource(R.string.notifications),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                if (userData?.photoUrl != null) {
                    AsyncImage(
                        model = userData.photoUrl,
                        contentDescription = stringResource(R.string.profile),
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = stringResource(R.string.profile),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
    )
}
