package prasad.vennam.moneypilot.ui.faq

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.TrackScreen

// ─── Data model ───────────────────────────────────────────────────────────────

private data class FaqCategory(
    val title: String,
    val icon: ImageVector,
    val items: List<FaqItem>,
)

private data class FaqItem(
    val question: String,
    val answer: String,
)

// ─── FAQ data ────────────────────────────────────────────────────────────────

private val faqCategories =
    listOf(
        FaqCategory(
            title = "Getting Started",
            icon = Icons.Rounded.Rocket,
            items =
                listOf(
                    FaqItem(
                        "What is MoneyPilot?",
                        "MoneyPilot is your personal finance co-pilot. It lets you track expenses and income, set budgets, monitor investments, and get AI-powered insights — all synced securely to your private Google Sheet.",
                    ),
                    FaqItem(
                        "Do I need a Google account?",
                        "You can use MoneyPilot as a Guest with full offline access to core features. Signing in with your Google account unlocks cloud sync, so your data is backed up to your own Google Drive.",
                    ),
                    FaqItem(
                        "How do I add my first transaction?",
                        "Tap the ＋ button on the Dashboard or Expenses tab. Fill in the amount, category, note, and date — then tap Save. Your balance updates instantly.",
                    ),
                ),
        ),
        FaqCategory(
            title = "Sync & Cloud",
            icon = Icons.Rounded.CloudSync,
            items =
                listOf(
                    FaqItem(
                        "How does Google Sheets sync work?",
                        "When you sign in with Google, MoneyPilot creates a private spreadsheet called \"MoneyPilot Sync\" in your Google Drive. Your transactions are pushed to that sheet. You can view, filter, and even edit them right in Google Sheets.",
                    ),
                    FaqItem(
                        "Why hasn't sync started yet?",
                        "Sync runs automatically after sign-in, but can take a minute on first launch. You can also trigger it manually from Settings → Cloud Sync → Sync Now.",
                    ),
                    FaqItem(
                        "Is my data private?",
                        "Absolutely. Your data lives only on your device and in your own Google Drive. MoneyPilot never stores your financial data on any third-party server.",
                    ),
                ),
        ),
        FaqCategory(
            title = "Budgets & Goals",
            icon = Icons.Rounded.TrackChanges,
            items =
                listOf(
                    FaqItem(
                        "How do I create a budget?",
                        "Go to Settings → Financial Goal. Set your monthly savings target and income goal. MoneyPilot will track your progress on the Dashboard and Reports tabs.",
                    ),
                    FaqItem(
                        "Can I set budgets per category?",
                        "Yes! Open the Budget section in the Expenses tab and tap a category to set a monthly limit. You'll get a visual indicator when you're close to the limit.",
                    ),
                    FaqItem(
                        "What happens when I exceed a budget?",
                        "You'll see a red warning indicator on that category. If notifications are enabled, MoneyPilot will also send you a push notification.",
                    ),
                ),
        ),
        FaqCategory(
            title = "Investments",
            icon = Icons.AutoMirrored.Rounded.ShowChart,
            items =
                listOf(
                    FaqItem(
                        "What investments can I track?",
                        "MoneyPilot supports stocks, ETFs, mutual funds, crypto, gold, real estate, and custom assets. Enter your holdings and we'll fetch live prices to show your portfolio value.",
                    ),
                    FaqItem(
                        "How often are prices updated?",
                        "Prices refresh automatically when you open the Investments tab. You can also pull-to-refresh for the latest data.",
                    ),
                ),
        ),
        FaqCategory(
            title = "Account & Data",
            icon = Icons.Rounded.ManageAccounts,
            items =
                listOf(
                    FaqItem(
                        "How do I delete my account?",
                        "Go to Settings → scroll to the bottom → Delete Account. This removes all your local data. Your Google Drive spreadsheet is not deleted and stays in your Drive.",
                    ),
                    FaqItem(
                        "Can I export my data?",
                        "Yes! Settings → Export Data lets you download a CSV of all transactions, which you can open in any spreadsheet app.",
                    ),
                    FaqItem(
                        "How do I change the currency?",
                        "Go to Settings → Currency and select your preferred currency. All amounts will display in that currency using live exchange rates.",
                    ),
                ),
        ),
    )

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(
    analyticsHelper: AnalyticsHelper,
    onNavigateBack: () -> Unit
) {
    TrackScreen(analyticsHelper, "FAQ")
    val context = LocalContext.current
    var showContactSheet by remember { mutableStateOf(false) }

    // Track which FAQ item is expanded: category index -> item index
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Help & FAQ",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    analyticsHelper.logEvent("faq_ask_question_clicked")
                    showContactSheet = true
                },
                icon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                text = { Text("Ask a Question") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Hero header
            item {
                FaqHeroCard()
            }

            // Categories
            faqCategories.forEachIndexed { catIdx, category ->
                item(key = "cat_$catIdx") {
                    FaqCategorySection(
                        category = category,
                        expandedStates = expandedStates,
                        categoryKey = "cat_$catIdx",
                        analyticsHelper = analyticsHelper
                    )
                }
            }
        }
    }

    // Contact bottom sheet
    if (showContactSheet) {
        ContactBottomSheet(
            onDismiss = { showContactSheet = false },
            onSendEmail = { subject, body ->
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("support@moneypilotapp.com"))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                context.startActivity(Intent.createChooser(intent, "Send Email"))
                showContactSheet = false
            },
        )
    }
}

// ─── Hero card ───────────────────────────────────────────────────────────────

@Composable
private fun FaqHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    ),
                )
            ).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "How can we help?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Browse questions below or tap \"Ask a Question\" at the bottom to email our team.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Category section ────────────────────────────────────────────────────────

@Composable
private fun FaqCategorySection(
    category: FaqCategory,
    expandedStates: MutableMap<String, Boolean>,
    categoryKey: String,
    analyticsHelper: AnalyticsHelper,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Category header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                category.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // FAQ items
        category.items.forEachIndexed { itemIdx, item ->
            val key = "${categoryKey}_item_$itemIdx"
            val isExpanded = expandedStates[key] == true

            FaqItemCard(
                item = item,
                isExpanded = isExpanded,
                onToggle = {
                    if (!isExpanded) {
                        analyticsHelper.logEvent("faq_item_expanded", mapOf("question" to item.question))
                    }
                    expandedStates[key] = !isExpanded
                },
            )
        }
    }
}

// ─── FAQ Item card ───────────────────────────────────────────────────────────

@Composable
private fun FaqItemCard(
    item: FaqItem,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "chevron_rotation",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle),
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
            ) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.8.dp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = item.answer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                    )
                }
            }
        }
    }
}

// ─── Contact bottom sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactBottomSheet(
    onDismiss: () -> Unit,
    onSendEmail: (subject: String, body: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    val isSendEnabled = subject.isNotBlank() && body.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Ask Our Team",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "We'll reply within 24 hours",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Subject field
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                placeholder = { Text("e.g. Sync not working") },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Rounded.Subject,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            Spacer(Modifier.height(14.dp))

            // Body field
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Message") },
                placeholder = { Text("Describe your issue or question in detail…") },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Rounded.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                maxLines = 8,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            Spacer(Modifier.height(20.dp))

            // Send button
            Button(
                onClick = { onSendEmail(subject.trim(), body.trim()) },
                enabled = isSendEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Send Email",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Opens your email app with our address pre-filled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
