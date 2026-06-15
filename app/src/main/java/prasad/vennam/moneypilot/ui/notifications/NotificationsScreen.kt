package prasad.vennam.moneypilot.ui.notifications

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.entity.Notification
import prasad.vennam.moneypilot.ui.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWeb: (url: String, title: String) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsState()
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Alerts", "Sync", "Budget", "System")

    val filteredNotifications =
        remember(notifications, selectedCategory) {
            if (selectedCategory == "All") {
                notifications
            } else {
                notifications.filter { it.category.equals(selectedCategory, ignoreCase = true) }
            }
        }

    var showClearAllConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notifications),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { showClearAllConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = stringResource(R.string.clear_all),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
        ) {
            // Horizontal Categories Scroll
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                edgePadding = 16.dp,
                divider = {},
                indicator = {},
                containerColor = Color.Transparent,
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    val count =
                        if (category ==
                            "All"
                        ) {
                            notifications.size
                        } else {
                            notifications.count { it.category.equals(category, ignoreCase = true) }
                        }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(category)
                                if (count > 0) {
                                    Badge(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    ) {
                                        Text(count.toString())
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        border = null,
                    )
                }
            }

            if (filteredNotifications.isEmpty()) {
                EmptyNotificationsState(selectedCategory)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = filteredNotifications,
                        key = { it.id },
                    ) { item ->
                        SwipeToDismissNotification(
                            item = item,
                            onDismiss = {
                                viewModel.deleteNotification(item.id)
                                Toast.makeText(context, context.getString(R.string.notification_deleted), Toast.LENGTH_SHORT).show()
                            },
                            onBookmark = {
                                if (!item.url.isNullOrBlank()) {
                                    viewModel.bookmarkNotificationUrl(item.title, item.url)
                                    Toast.makeText(context, context.getString(R.string.saved_offline), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.no_link_to_bookmark), Toast.LENGTH_SHORT).show()
                                }
                            },
                            onNavigateToWeb = onNavigateToWeb,
                        )
                    }
                }
            }
        }
    }

    if (showClearAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmation = false },
            title = { Text(stringResource(R.string.clear_all_notifications_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.clear_all_notifications_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearAllConfirmation = false
                        Toast.makeText(context, context.getString(R.string.all_notifications_cleared), Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.clear_all), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissNotification(
    item: Notification,
    onDismiss: () -> Unit,
    onBookmark: () -> Unit,
    onNavigateToWeb: (url: String, title: String) -> Unit,
) {
    var isRemoved by remember { mutableStateOf(false) }

    LaunchedEffect(isRemoved) {
        if (isRemoved) {
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = !isRemoved,
        exit = shrinkVertically(animationSpec = tween(500)) + fadeOut(animationSpec = tween(500)),
    ) {
        val dismissState =
            rememberSwipeToDismissBoxState(
                confirmValueChange = { dismissValue ->
                    when (dismissValue) {
                        SwipeToDismissBoxValue.EndToStart -> {
                            isRemoved = true
                            true
                        }
                        SwipeToDismissBoxValue.StartToEnd -> {
                            onBookmark()
                            false
                        }
                        else -> false
                    }
                },
            )

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val color =
                    when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.StartToEnd -> {
                            if (!item.url.isNullOrBlank()) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        }
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                        else -> Color.Transparent
                    }
                val alignment =
                    when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                        else -> Alignment.Center
                    }
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(color)
                            .padding(horizontal = 24.dp),
                    contentAlignment = alignment,
                ) {
                    if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                        when (dismissState.dismissDirection) {
                            SwipeToDismissBoxValue.StartToEnd -> {
                                Icon(
                                    imageVector = if (!item.url.isNullOrBlank()) Icons.Rounded.Bookmark else Icons.Rounded.Block,
                                    contentDescription = if (!item.url.isNullOrBlank()) "Bookmark" else "No Link",
                                    tint = if (!item.url.isNullOrBlank()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            SwipeToDismissBoxValue.EndToStart -> {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            else -> {}
                        }
                    }
                }
            },
            content = {
                NotificationItemCard(
                    notification = item,
                    onNavigateToWeb = onNavigateToWeb,
                )
            },
        )
    }
}

@Composable
fun NotificationItemCard(
    notification: Notification,
    onNavigateToWeb: (url: String, title: String) -> Unit,
) {
    val categoryDetails =
        remember(notification.category) {
            when (notification.category.lowercase(Locale.ROOT)) {
                "alerts" -> Triple(Icons.Rounded.Warning, Color(0xFFFF9800), "Alerts")
                "sync" -> Triple(Icons.Rounded.CloudDone, Color(0xFF2196F3), "Sync")
                "budget" -> Triple(Icons.AutoMirrored.Rounded.TrendingDown, Color(0xFF4CAF50), "Budget")
                else -> Triple(Icons.Rounded.Info, Color(0xFF9C27B0), "System")
            }
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (!notification.url.isNullOrBlank()) {
                        Modifier.clickable { onNavigateToWeb(notification.url, notification.title) }
                    } else {
                        Modifier
                    },
                ),
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = categoryDetails.second.copy(alpha = 0.15f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = categoryDetails.first,
                        contentDescription = categoryDetails.third,
                        tint = categoryDetails.second,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = formatTime(notification.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!notification.url.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onNavigateToWeb(notification.url, notification.title)
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                stringResource(R.string.read_article),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                            Icon(
                                imageVector = Icons.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotificationsState(category: String) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(100.dp)
                    .background(
                        brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                            ),
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsNone,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (category == "All") "All Caught Up! ✨" else "No $category Notifications",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "When alerts, budget targets, or backups trigger, you'll see them listed here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { time = date }

    return if (now.get(Calendar.DATE) == time.get(Calendar.DATE)) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
    } else if (now.get(Calendar.DATE) - time.get(Calendar.DATE) == 1) {
        "Yesterday"
    } else {
        SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
}
