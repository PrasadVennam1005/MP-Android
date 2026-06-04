package prasad.vennam.moneypilot.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import prasad.vennam.moneypilot.ui.viewmodel.MainViewModel
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.ui.viewmodel.BudgetViewModel
import prasad.vennam.moneypilot.ui.viewmodel.InvestmentViewModel
import prasad.vennam.moneypilot.util.AnalyticsHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    transactionViewModel: TransactionViewModel,
    budgetViewModel: BudgetViewModel,
    investmentViewModel: InvestmentViewModel,
    mainViewModel: MainViewModel,
    analyticsHelper: AnalyticsHelper,
    onLogout: () -> Unit,
    onNavigateToCategories: () -> Unit
) {
    val userData by mainViewModel.userData.collectAsState()
    val currentCurrency by mainViewModel.currency.collectAsState()
    
    var showCurrencySheet by remember { mutableStateOf(false) }

    if (showCurrencySheet) {
        CurrencySelectionBottomSheet(
            selectedCurrencyCode = currentCurrency,
            onCurrencySelect = { 
                // 4. Settings Analytics: Track currency change
                analyticsHelper.logEvent("currency_changed", mapOf(
                    "from" to currentCurrency,
                    "to" to it
                ))
                mainViewModel.setCurrency(it) 
            },
            onDismiss = { showCurrencySheet = false }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Profile Section
            item {
                ProfileHeader(userData)
            }

            item { SectionDivider() }

            // General Settings
            item {
                SettingsGroup(title = "General") {
                    val currencyInfo = currencies.find { it.code == currentCurrency }
                    SettingsItem(
                        icon = Icons.Rounded.CurrencyExchange,
                        title = "Currency",
                        subtitle = "${currencyInfo?.flag ?: ""} ${currencyInfo?.name ?: currentCurrency}",
                        onClick = { showCurrencySheet = true }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.Category,
                        title = "Manage Categories",
                        subtitle = "Add or edit transaction categories",
                        onClick = onNavigateToCategories
                    )
                    SettingsItem(
                        icon = Icons.Rounded.Palette,
                        title = "Theme",
                        subtitle = "System Default",
                        onClick = { /* TODO */ }
                    )
                }
            }

            item { SectionDivider() }

            // App Settings
            item {
                SettingsGroup(title = "App") {
                    SettingsItem(
                        icon = Icons.Rounded.Notifications,
                        title = "Notifications",
                        subtitle = "Daily reminders & alerts",
                        onClick = { /* TODO */ }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.CloudSync,
                        title = "Google Sheets Sync",
                        subtitle = "Auto-export your data",
                        onClick = { /* TODO */ }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.FileDownload,
                        title = "Export Data",
                        subtitle = "Download as CSV or JSON",
                        onClick = { /* TODO */ }
                    )
                }
            }

            item { SectionDivider() }

            // Support & Info
            item {
                SettingsGroup(title = "Support") {
                    SettingsItem(
                        icon = Icons.Rounded.Info,
                        title = "About",
                        subtitle = "MoneyPilot v1.0.0",
                        onClick = { /* TODO */ }
                    )
                }
            }

            // Logout Button
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        mainViewModel.logout(onLogout)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = MaterialTheme.shapes.large,
                    elevation = null
                ) {
                    Icon(Icons.Rounded.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Logout", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    if (showCurrencySheet) {
        CurrencySelectionBottomSheet(
            selectedCurrencyCode = currentCurrency,
            onCurrencySelect = { mainViewModel.setCurrency(it) },
            onDismiss = { showCurrencySheet = false }
        )
    }
}

@Composable
fun ProfileHeader(userData: prasad.vennam.moneypilot.data.UserPreferences.UserData?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (userData?.photoUrl != null) {
            AsyncImage(
                model = userData.photoUrl,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column {
            Text(
                text = userData?.name ?: "User",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = userData?.email ?: "Sign in to sync your data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
