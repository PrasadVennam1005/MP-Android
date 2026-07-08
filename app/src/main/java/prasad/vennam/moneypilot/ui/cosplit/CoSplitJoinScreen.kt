package prasad.vennam.moneypilot.ui.cosplit

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.feature.cosplit.ui.CoSplitViewModel
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.TrackScreen
import prasad.vennam.moneypilot.ui.components.AdBannerView

@Composable
fun CoSplitJoinScreen(
    viewModel: CoSplitViewModel,
    groupId: String,
    groupName: String,
    analyticsHelper: AnalyticsHelper,
    onNavigateToGroup: (String) -> Unit,
    onCancel: () -> Unit
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.CO_SPLIT_JOIN)
    val groups by viewModel.groups.collectAsState()
    val email by viewModel.userEmail.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isJoining by remember { mutableStateOf(false) }

    // If the user is already a member of this group, redirect them immediately to details
    LaunchedEffect(groups) {
        val alreadyMember = groups.any { it.id == groupId }
        if (alreadyMember) {
            onNavigateToGroup(groupId)
        }
    }

    val isPremium by viewModel.isPremium.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.GroupAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "You've been invited!",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = groupName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Join this group on MoneyPilot to collaborate on expenses, track balances, and settle bills in real-time.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || email == "guest@moneypilot.app") {
                            Toast.makeText(context, "Please log in first to join groups", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        isJoining = true
                        viewModel.joinGroup(groupId) { success ->
                            isJoining = false
                            if (success) {
                                Toast.makeText(context, "Successfully joined group!", Toast.LENGTH_SHORT).show()
                                onNavigateToGroup(groupId)
                            } else {
                                Toast.makeText(context, "Failed to join group", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isJoining
                ) {
                    if (isJoining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Join Group", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        }
        AdBannerView(
            isPremium = isPremium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
