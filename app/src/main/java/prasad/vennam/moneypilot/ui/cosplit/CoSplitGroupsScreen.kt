package prasad.vennam.moneypilot.ui.cosplit

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prasad.vennam.moneypilot.feature.cosplit.data.model.CoSplitGroup
import prasad.vennam.moneypilot.feature.cosplit.ui.CoSplitViewModel
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.TrackScreen
import prasad.vennam.moneypilot.ui.components.AdBannerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoSplitGroupsScreen(
    viewModel: CoSplitViewModel,
    analyticsHelper: AnalyticsHelper,
    onNavigateBack: () -> Unit,
    onNavigateToGroup: (String) -> Unit
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.CO_SPLIT_GROUPS)
    val groups by viewModel.groups.collectAsState()
    val email by viewModel.userEmail.collectAsState()
    val isGuest = email.isBlank() || email == "guest@moneypilot.app"

    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.error.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CoSplit Groups", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isGuest) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add Group")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
            if (isGuest) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Feature Locked",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "CoSplit is not available for Guest accounts.",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Please login using a Google account to split expenses and sync with friends in real-time.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                if (groups.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "No Groups",
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No CoSplit groups yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Create a group to start splitting bills and tracking who owes what.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Group")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val totalOwed = groups.sumOf { group ->
                            group.balances[email] ?: 0.0
                        }
                        item {
                            TotalBalanceCard(totalOwed)
                        }

                        item {
                            Text(
                                "Your Groups",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(groups) { group ->
                            val userBalance = group.balances[email] ?: 0.0
                            GroupListItem(
                                group = group,
                                userBalance = userBalance,
                                onClick = {
                                    viewModel.selectGroup(group)
                                    onNavigateToGroup(group.id)
                                }
                            )
                        }
                    }
                }
            }
        }
        val isPremium by viewModel.isPremium.collectAsState()
        AdBannerView(
            isPremium = isPremium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

    if (showCreateDialog) {
        var isSaving by remember { mutableStateOf(false) }

        ManageGroupBottomSheet(
            isCreateMode = true,
            initialMembers = listOf(email),
            initialMemberNamesMap = emptyMap(),
            currentUserEmail = email,
            onFetchUserProfile = { userEmail -> viewModel.getUserProfile(userEmail) },
            onDismiss = { showCreateDialog = false },
            onSave = { name, members, memberNames ->
                isSaving = true
                viewModel.createGroup(name, members, memberNames) { success ->
                    isSaving = false
                    if (success) {
                        showCreateDialog = false
                    }
                }
            },
            isSaving = isSaving
        )
    }
}

@Composable
fun TotalBalanceCard(totalBalance: Double) {
    val cardColor = when {
        totalBalance > 0.01 -> Color(0xFFE8F5E9)
        totalBalance < -0.01 -> Color(0xFFFFEBEE)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val contentColor = when {
        totalBalance > 0.01 -> Color(0xFF2E7D32)
        totalBalance < -0.01 -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        totalBalance > 0.01 -> Icons.Rounded.TrendingUp
                        totalBalance < -0.01 -> Icons.Rounded.TrendingDown
                        else -> Icons.Rounded.DoneAll
                    },
                    contentDescription = null,
                    tint = contentColor
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = when {
                        totalBalance > 0.01 -> "You are owed"
                        totalBalance < -0.01 -> "You owe"
                        else -> "All settled up"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.copy(alpha = 0.7f)
                )
                Text(
                    text = "₹${String.format("%.2f", kotlin.math.abs(totalBalance))}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun GroupListItem(
    group: CoSplitGroup,
    userBalance: Double,
    onClick: () -> Unit
) {
    val balanceColor = when {
        userBalance > 0.01 -> Color(0xFF2E7D32)
        userBalance < -0.01 -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${group.members.size} members",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = when {
                        userBalance > 0.01 -> "you are owed"
                        userBalance < -0.01 -> "you owe"
                        else -> "settled up"
                    },
                    fontSize = 11.sp,
                    color = balanceColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹${String.format("%.2f", kotlin.math.abs(userBalance))}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = balanceColor
                )
            }
        }
    }
}
