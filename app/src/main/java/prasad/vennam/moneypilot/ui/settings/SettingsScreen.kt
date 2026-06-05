package prasad.vennam.moneypilot.ui.settings

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import coil.compose.AsyncImage
import prasad.vennam.moneypilot.ui.viewmodel.MainViewModel
import prasad.vennam.moneypilot.ui.viewmodel.TransactionViewModel
import prasad.vennam.moneypilot.ui.viewmodel.BudgetViewModel
import prasad.vennam.moneypilot.ui.viewmodel.InvestmentViewModel
import prasad.vennam.moneypilot.ui.viewmodel.RestoreState
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.ExportHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlin.collections.find

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    transactionViewModel: TransactionViewModel,
    budgetViewModel: BudgetViewModel,
    investmentViewModel: InvestmentViewModel,
    mainViewModel: MainViewModel,
    analyticsHelper: AnalyticsHelper,
    onLogout: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val userData by mainViewModel.userData.collectAsState()
    val isSynced by mainViewModel.isSynced.collectAsState()
    val spreadsheetId by mainViewModel.spreadsheetId.collectAsState()
    val currentCurrencyCode by mainViewModel.currency.collectAsState()
    val transactions by transactionViewModel.allTransactions.collectAsState()
    val categories by transactionViewModel.allCategories.collectAsState()
    val exchangeRates by mainViewModel.exchangeRates.collectAsState()
    val currentGoal by mainViewModel.financialGoal.collectAsState()
    val currentTarget by mainViewModel.monthlySavingsTarget.collectAsState()

    val scope = rememberCoroutineScope()
    val isGuest = remember(userData) { userData?.email == "guest@moneypilot.app" }
    val context = LocalContext.current
    var isSyncing by remember { mutableStateOf(false) }

    var showLogoutWarning by remember { mutableStateOf(false) }
    var showExportFormatDialog by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    val currencySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showLoginRequiredDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    val currentThemeMode by mainViewModel.themeMode.collectAsState()
    val themeSubtitle = when (currentThemeMode) {
        1 -> "Light"
        2 -> "Dark"
        else -> "System Default"
    }
    var pendingFeatureName by remember { mutableStateOf("") }
    val credentialManager = remember { CredentialManager.create(context) }
    val restoreState by mainViewModel.restoreState.collectAsState()

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            mainViewModel.checkAndPerformRestore(context)
        }
    }

    LaunchedEffect(restoreState) {
        when (val state = restoreState) {
            is RestoreState.Success -> {
                Toast.makeText(context, context.run { getString(prasad.vennam.moneypilot.R.string.backup_successfully_restored) }, Toast.LENGTH_LONG).show()
                mainViewModel.resetRestoreCheck()
            }
            is RestoreState.NeedAuthorization -> {
                authLauncher.launch(state.intent)
            }
            is RestoreState.Error -> {
                Toast.makeText(context, context.run { getString(prasad.vennam.moneypilot.R.string.restore_failed, state.message) }, Toast.LENGTH_LONG).show()
                mainViewModel.resetRestoreCheck()
            }
            else -> {}
        }
    }

    val checkGuestAction = { featureName: String, onAuthorized: () -> Unit ->
        if (isGuest) {
            pendingFeatureName = featureName
            showLoginRequiredDialog = true
        } else {
            onAuthorized()
        }
    }

    val triggerGoogleLogin = {
        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(prasad.vennam.moneypilot.BuildConfig.GOOGLE_CLIENT_ID)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                val googleIdTokenCredential = try {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        GoogleIdTokenCredential.createFrom(credential.data)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }

                if (googleIdTokenCredential != null) {
                    analyticsHelper.logEvent("login", mapOf("method" to "google"))
                    mainViewModel.saveUserData(
                        UserPreferences.UserData(
                            name = googleIdTokenCredential.displayName ?: "User",
                            email = googleIdTokenCredential.id,
                            photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                        )
                    ) {
                        mainViewModel.checkAndPerformRestore(context)
                        showLoginRequiredDialog = false
                    }
                }
            } catch (e: GetCredentialException) {
                Log.e("SettingsScreen", "Login failed: ${e.message}")
                Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("SettingsScreen", "Error: ${e.message}")
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        ExportHelper.exportToCsv(
                            transactions = transactions,
                            categories = categories,
                            outputStream = outputStream,
                            currencyCode = currentCurrencyCode
                        )
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, "Data exported successfully as CSV!", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        ExportHelper.exportToPdf(
                            transactions = transactions,
                            categories = categories,
                            outputStream = outputStream,
                            currencyCode = currentCurrencyCode
                        )
                        scope.launch(Dispatchers.Main) {
                            Toast.makeText(context, "Data exported successfully as PDF!", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    scope.launch(Dispatchers.Main) {
                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val currencyOptions = listOf(
        CurrencyInfo("INR", "Indian Rupee", "₹", "🇮🇳"),
        CurrencyInfo("USD", "US Dollar", "$", "🇺🇸"),
        CurrencyInfo("EUR", "Euro", "€", "🇪🇺"),
        CurrencyInfo("GBP", "British Pound", "£", "🇬🇧"),
        CurrencyInfo("JPY", "Japanese Yen", "¥", "🇯🇵"),
        CurrencyInfo("AUD", "Australian Dollar", "A$", "🇦🇺"),
        CurrencyInfo("CAD", "Canadian Dollar", "C$", "🇨🇦"),
        CurrencyInfo("CHF", "Swiss Franc", "Fr", "🇨🇭"),
        CurrencyInfo("CNY", "Chinese Yuan", "¥", "🇨🇳"),
        CurrencyInfo("SGD", "Singapore Dollar", "S$", "🇸🇬"),
        CurrencyInfo("AED", "UAE Dirham", "د.إ", "🇦🇪"),
        CurrencyInfo("SAR", "Saudi Riyal", "ر.س", "🇸🇦"),
        CurrencyInfo("ZAR", "South African Rand", "R", "🇿🇦"),
        CurrencyInfo("BRL", "Brazilian Real", "R$", "🇧🇷"),
        CurrencyInfo("MXN", "Mexican Peso", "$", "🇲🇽"),
        CurrencyInfo("KRW", "South Korean Won", "₩", "🇰🇷")
    )
    
    val currentCurrency = currencyOptions.find { it.code == currentCurrencyCode } ?: currencyOptions[0]

    val rateAgainstUSD = exchangeRates[currentCurrencyCode] ?: 1.0
    val liveRateText = if (currentCurrencyCode == "USD") {
        "Base Currency"
    } else {
        "1 USD = ${currentCurrency.symbol}${String.format(java.util.Locale.US, "%.2f", rateAgainstUSD)}"
    }

    if (showCurrencySheet) {
        CurrencySelectionBottomSheet(
            selectedCurrencyCode = currentCurrency.code,
            onCurrencySelect = { 
                analyticsHelper.logEvent("currency_changed", mapOf(
                    "from" to currentCurrencyCode,
                    "to" to it
                ))
                mainViewModel.setCurrency(it)
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
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
                    SettingsItem(
                        icon = Icons.Rounded.CurrencyExchange,
                        title = "Currency",
                        subtitle = "${currentCurrency.name} (${currentCurrency.symbol}) • $liveRateText",
                        isLocked = isGuest,
                        onClick = {
                            checkGuestAction("Currency Change") {
                                showCurrencySheet = true
                            }
                        }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.Flag,
                        title = "Financial Goal",
                        subtitle = "$currentGoal (Target: $currentTarget% savings)",
                        onClick = {
                            mainViewModel.resetOnboarding()
                        }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.Category,
                        title = "Manage Categories",
                        subtitle = "Add or edit transaction categories",
                        isLocked = isGuest,
                        onClick = {
                            checkGuestAction("Manage Categories") {
                                onNavigateToCategories()
                            }
                        }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.Palette,
                        title = "Theme",
                        subtitle = themeSubtitle,
                        onClick = { showThemeDialog = true }
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
                        isLocked = isGuest,
                        onClick = {
                            checkGuestAction("Notifications") {
                                onNavigateToNotifications()
                            }
                        }
                    )

                    SettingsItem(
                        icon = Icons.Rounded.FileDownload,
                        title = "Export Data",
                        subtitle = "Download as CSV or PDF",
                        isLocked = isGuest,
                        onClick = {
                            checkGuestAction("Export Data") {
                                showExportFormatDialog = true
                            }
                        }
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
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Logout", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }

    if (showLoginRequiredDialog) {
        LoginRequiredDialog(
            featureName = pendingFeatureName,
            onDismiss = { showLoginRequiredDialog = false },
            onLoginClick = { triggerGoogleLogin() }
        )
    }

    if (showExportFormatDialog) {
        AlertDialog(
            onDismissRequest = { showExportFormatDialog = false },
            title = { Text("Export Data", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            text = { Text("Select the format to export your transaction report.") },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = {
                            showExportFormatDialog = false
                            csvExportLauncher.launch("moneypilot_transactions_${System.currentTimeMillis()}.csv")
                        }
                    ) {
                        Text("CSV", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            showExportFormatDialog = false
                            pdfExportLauncher.launch("moneypilot_report_${System.currentTimeMillis()}.pdf")
                        }
                    ) {
                        Text("PDF", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showExportFormatDialog = false }) {
                        Text("Cancel")
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val modes = listOf(
                        Triple(0, "System Default", Icons.Rounded.SettingsSuggest),
                        Triple(1, "Light Mode", Icons.Rounded.LightMode),
                        Triple(2, "Dark Mode", Icons.Rounded.DarkMode)
                    )
                    modes.forEach { (mode, name, icon) ->
                        val isSelected = currentThemeMode == mode
                        Surface(
                            onClick = {
                                mainViewModel.setThemeMode(mode)
                                showThemeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
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
    isLocked: Boolean = false,
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

            if (isLocked) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Sign-in required",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRequiredDialog(
    featureName: String,
    onDismiss: () -> Unit,
    onLoginClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Login with Google", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudSync,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        title = {
            Text(
                text = "Backup & Sync Required",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "To access $featureName, please sign in with Google.",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "All your current transactions and data will be automatically synced and secured in your personal Google Sheets backup.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    )
}

@Composable
fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
