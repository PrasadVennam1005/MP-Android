package prasad.vennam.moneypilot.feature.ai.presentation

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.window.core.layout.WindowWidthSizeClass
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ads.AdConfig
import prasad.vennam.moneypilot.data.entity.TransactionType
import prasad.vennam.moneypilot.feature.ai.model.AiAction
import prasad.vennam.moneypilot.feature.ai.model.Author
import prasad.vennam.moneypilot.feature.ai.model.ChatMessage
import prasad.vennam.moneypilot.feature.ai.model.LlmState
import prasad.vennam.moneypilot.ui.ai.InsightsContent
import prasad.vennam.moneypilot.ui.ai.generateSmartInsights
import prasad.vennam.moneypilot.ui.components.BannerPlaceholder
import prasad.vennam.moneypilot.ui.components.BaseBottomSheet
import prasad.vennam.moneypilot.ui.viewmodel.AnalyticsViewModel
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.LocalCurrencyCode
import prasad.vennam.moneypilot.util.TrackScreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onBackClick: () -> Unit,
    analyticsHelper: AnalyticsHelper,
    viewModel: AiViewModel = hiltViewModel(),
    analyticsViewModel: AnalyticsViewModel = hiltViewModel(),
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.AI_CHAT)
    val uiState by viewModel.uiState.collectAsState()
    val messages = uiState.messages
    val aiState = uiState.llmState
    val downloadProgress = uiState.downloadProgress
    val pendingAction = uiState.pendingAction

    val analyticsUiState by analyticsViewModel.uiState.collectAsState()
    val aiRecState by analyticsViewModel.aiRecommendation.collectAsState()
    val currencyCode = LocalCurrencyCode.current
    val isConsentGranted by viewModel.isUserConsentGranted.collectAsState()
    var showConsentDialog by remember { mutableStateOf(false) }

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
    val gridState = rememberLazyGridState()

    val insights =
        remember(analyticsUiState, currencyCode) {
            if (analyticsUiState.isLoading) emptyList() else generateSmartInsights(analyticsUiState, currencyCode)
        }

    LaunchedEffect(analyticsUiState.isLoading, currencyCode) {
        if (!analyticsUiState.isLoading) {
            analyticsViewModel.generateAiRecommendation(currencyCode)
        }
    }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    var showSuggestionsBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.actionFeedback.collect { feedback ->
            snackbarHostState.showSnackbar(feedback)
        }
    }

    val isKeyboardOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    LaunchedEffect(isKeyboardOpen, messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                                        ),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                stringResource(R.string.moneypilot_copilot),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            StatusIndicator(aiState)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        analyticsHelper.logEvent(AnalyticsConstants.Event.AI_CHAT_SUGGESTIONS_ICON_CLICKED)
                        showSuggestionsBottomSheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = stringResource(R.string.suggestions),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        ),
            ) {
                Column {
                    if (aiState is LlmState.Error) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = aiState.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    if (pendingAction != null) {
                        ActionConfirmationCard(
                            action = pendingAction,
                            onConfirm = { editedAction ->
                                analyticsHelper.logEvent(
                                    AnalyticsConstants.Event.AI_CHAT_ACTION_CONFIRMED,
                                    mapOf(AnalyticsConstants.Param.TYPE to editedAction::class.java.simpleName),
                                )
                                viewModel.confirmAction(editedAction)
                            },
                            onDismiss = {
                                analyticsHelper.logEvent(AnalyticsConstants.Event.AI_CHAT_ACTION_DISMISSED)
                                viewModel.dismissAction()
                            },
                        )
                    }

                    Row(
                        modifier =
                            Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                            placeholder = {
                                Text(
                                    if (aiState is LlmState.RateLimited) {
                                        stringResource(R.string.ai_rate_limited_title)
                                    } else if (pendingAction != null) {
                                        stringResource(R.string.review_action_placeholder)
                                    } else {
                                        stringResource(R.string.ask_ai_placeholder)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            },
                            shape = RoundedCornerShape(28.dp),
                            singleLine = true,
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                ),
                            enabled = aiState is LlmState.Ready || aiState is LlmState.Generating || aiState is LlmState.ActionConfirm,
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Custom Gradient Send Button
                        // Fix #4: Disallow sending when a confirmation card is pending
                        // Fix #1: Content description reflects whether we're sending or confirming
                        val buttonEnabled =
                            inputText.isNotBlank() &&
                                    pendingAction == null &&
                                    (aiState is LlmState.Ready || aiState is LlmState.Generating || aiState is LlmState.ActionConfirm)

                        val sendContentDesc = if (pendingAction != null)
                            stringResource(R.string.confirm_action)
                        else
                            stringResource(R.string.send_message)

                        Box(
                            modifier =
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (buttonEnabled) {
                                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                                        } else {
                                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)))
                                        },
                                    )
                                    .clickable(
                                        enabled = buttonEnabled,
                                        onClick = {
                                            // Fix #8: require consent before sending to cloud, but skip if local model is ready
                                            val localModelAvailable = uiState.isLocalModelAvailable
                                            val apiKeyConfigured = prasad.vennam.moneypilot.BuildConfig.GEMINI_API_KEY
                                                .trim().removeSurrounding("\"").isNotBlank()
                                            val needsConsent = !localModelAvailable && apiKeyConfigured && !isConsentGranted
                                            if (needsConsent) {
                                                showConsentDialog = true
                                            } else {
                                                viewModel.sendMessage(inputText)
                                                inputText = ""
                                            }
                                        },
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Send,
                                contentDescription = sendContentDesc,
                                tint = if (buttonEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (showSuggestionsBottomSheet) {
            SampleQueriesBottomSheet(
                analyticsHelper = analyticsHelper,
                onDismissRequest = { showSuggestionsBottomSheet = false },
                onSuggestionClick = { text ->
                    inputText = text
                    showSuggestionsBottomSheet = false
                    try {
                        focusRequester.requestFocus()
                    } catch (e: Exception) {
                        // Focus request might fail if node is not attached yet
                    }
                },
            )
        }

        // Fix #8: Cloud AI consent dialog
        if (showConsentDialog) {
            AlertDialog(
                onDismissRequest = { showConsentDialog = false },
                title = { Text(stringResource(R.string.ai_consent_title), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.ai_consent_desc), style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.grantConsent()
                        showConsentDialog = false
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }) {
                        Text(stringResource(R.string.ai_consent_agree), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConsentDialog = false }) {
                        Text(stringResource(R.string.ai_consent_decline))
                    }
                },
            )
        }

        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            if (isExpanded) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1.2f)) {
                        InsightsContent(
                            uiState = analyticsUiState,
                            aiRecState = aiRecState,
                            insights = insights,
                            currencyCode = currencyCode,
                            isExpanded = false,
                            gridState = gridState,
                            analyticsHelper = analyticsHelper,
                            onNavigateToAiChat = {},
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Box(modifier = Modifier.weight(1f)) {
                        AiChatBody(
                            aiState = aiState,
                            downloadProgress = downloadProgress,
                            messages = messages,
                            listState = listState,
                            analyticsHelper = analyticsHelper,
                            onSuggestionClick = { text ->
                                inputText = text
                                try {
                                    focusRequester.requestFocus()
                                } catch (ignored: Exception) {
                                }
                            },
                            viewModel = viewModel
                        )
                    }
                }
            } else {
                AiChatBody(
                    aiState = aiState,
                    downloadProgress = downloadProgress,
                    messages = messages,
                    listState = listState,
                    analyticsHelper = analyticsHelper,
                    onSuggestionClick = { text ->
                        inputText = text
                        try {
                            focusRequester.requestFocus()
                        } catch (ignored: Exception) {
                        }
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}


@Composable
private fun DownloadModelCard(
    title: String,
    desc: String,
    analyticsHelper: AnalyticsHelper,
    viewModel: AiViewModel,
    modifier: Modifier = Modifier,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.CloudDownload,
                    null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                desc,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                            ),
                        )
                        .clickable {
                            analyticsHelper.logEvent(AnalyticsConstants.Event.AI_CHAT_MODEL_DOWNLOAD_CLICKED)
                            viewModel.downloadModel()
                        },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.download_model_btn),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun AiChatBody(
    aiState: LlmState,
    downloadProgress: Float,
    messages: List<ChatMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    analyticsHelper: AnalyticsHelper,
    onSuggestionClick: (String) -> Unit,
    viewModel: AiViewModel,
) {
    if (aiState is LlmState.Idle) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            DownloadModelCard(
                title = stringResource(R.string.ai_download_required_title),
                desc = stringResource(R.string.ai_download_required_desc),
                analyticsHelper = analyticsHelper,
                viewModel = viewModel,
            )
        }
    } else if (aiState is LlmState.RateLimited && messages.isEmpty()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            DownloadModelCard(
                title = stringResource(R.string.ai_rate_limited_title),
                desc = stringResource(R.string.ai_rate_limited_desc),
                analyticsHelper = analyticsHelper,
                viewModel = viewModel,
            )
        }
    } else if (aiState is LlmState.Downloading) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val progressPercent = (downloadProgress * 100).toInt()
                    CircularProgressIndicator(
                        progress = { downloadProgress },
                        strokeWidth = 6.dp,
                        modifier = Modifier.size(72.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.downloading_model_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.downloading_progress_desc, progressPercent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    )
                }
            }
        }
    } else if (aiState is LlmState.Initializing) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.initializing_llm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        if (messages.isEmpty()) {
            WelcomeScreen(
                analyticsHelper = analyticsHelper,
                onSuggestionClick = onSuggestionClick,
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
            ) {
                items(messages, key = { it.id }) { message ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter =
                            fadeIn(animationSpec = tween(400)) +
                                    slideInVertically(
                                        initialOffsetY = { it / 3 },
                                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                                    ),
                    ) {
                        ChatBubble(message)
                    }
                }

                if (aiState is LlmState.RateLimited) {
                    item(key = "rate_limit_download_card") {
                        DownloadModelCard(
                            title = stringResource(R.string.ai_rate_limited_title),
                            desc = stringResource(R.string.ai_rate_limited_desc),
                            analyticsHelper = analyticsHelper,
                            viewModel = viewModel,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(state: LlmState) {
    val text =
        when (state) {
            LlmState.Idle -> stringResource(R.string.status_offline)
            LlmState.Initializing -> stringResource(R.string.status_initializing)
            is LlmState.Ready -> stringResource(R.string.status_ready)
            is LlmState.Error -> stringResource(R.string.status_error)
            is LlmState.Generating -> stringResource(R.string.status_processing)
            LlmState.Downloading -> stringResource(R.string.status_downloading)
            is LlmState.ActionConfirm -> stringResource(R.string.status_pending_confirm)
            LlmState.RateLimited -> stringResource(R.string.limit_exceeded)
        }
    val color =
        when (state) {
            LlmState.Idle -> Color.Gray
            LlmState.Initializing -> Color(0xFFFFA500)
            is LlmState.Ready -> Color(0xFF4CAF50)
            is LlmState.Error -> MaterialTheme.colorScheme.error
            is LlmState.Generating -> MaterialTheme.colorScheme.primary
            LlmState.Downloading -> MaterialTheme.colorScheme.primary
            is LlmState.ActionConfirm -> Color(0xFFFFA500)
            LlmState.RateLimited -> MaterialTheme.colorScheme.error
        }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse_alpha",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        color.copy(
                            alpha = if (state is LlmState.Generating || state is LlmState.Initializing || state is LlmState.Downloading) alpha else 1f,
                        ),
                    ),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.author == Author.USER

    val textColor =
        if (isUser) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val bubbleShape =
        if (isUser) {
            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 4.dp,
            )
        } else {
            RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 4.dp,
                bottomEnd = 20.dp,
            )
        }

    val bubbleModifier =
        Modifier
            .widthIn(max = 320.dp)
            .clip(bubbleShape)
            .then(
                if (isUser) {
                    Modifier.background(
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                ),
                        ),
                    )
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(
                            width = 1.dp,
                            brush =
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                    ),
                                ),
                            shape = bubbleShape,
                        )
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                            ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = bubbleModifier,
        ) {
            // Fix #2: use the isTyping flag instead of checking for a "..." sentinel string
            if (message.isTyping) {
                TypingDotsIndicator(tint = textColor)
            } else {
                Text(
                    text = parseMarkdownToAnnotatedString(message.content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * Parses basic Markdown constructs (bold, italic, inline code, bullets) into AnnotatedString.
 */
fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    val processedText = text
        .lines()
        .map { line ->
            val trimmed = line.trimStart()
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                "    • " + trimmed.substring(2)
            } else if (trimmed.startsWith("• ")) {
                "    " + trimmed
            } else {
                line
            }
        }
        .joinToString("\n")

    return buildAnnotatedString {
        var cursor = 0
        while (cursor < processedText.length) {
            val boldIdx = processedText.indexOf("**", cursor)
            val italicIdx = processedText.indexOf("*", cursor)
            val codeIdx = processedText.indexOf("`", cursor)

            // Find the closest tag
            val nextTagIdx = listOf(boldIdx, italicIdx, codeIdx)
                .filter { it >= 0 }
                .minOrNull() ?: -1

            if (nextTagIdx == -1) {
                // No more tags, append remainder of string
                append(processedText.substring(cursor))
                break
            }

            // Append text up to next tag
            if (nextTagIdx > cursor) {
                append(processedText.substring(cursor, nextTagIdx))
                cursor = nextTagIdx
            }

            when (cursor) {
                boldIdx -> {
                    val endBold = processedText.indexOf("**", cursor + 2)
                    if (endBold != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(processedText.substring(cursor + 2, endBold))
                        pop()
                        cursor = endBold + 2
                    } else {
                        // Unmatched tag, treat as plain text
                        append("**")
                        cursor += 2
                    }
                }
                italicIdx -> {
                    val endItalic = processedText.indexOf("*", cursor + 1)
                    if (endItalic != -1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(processedText.substring(cursor + 1, endItalic))
                        pop()
                        cursor = endItalic + 1
                    } else {
                        append("*")
                        cursor += 1
                    }
                }
                codeIdx -> {
                    val endCode = processedText.indexOf("`", cursor + 1)
                    if (endCode != -1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFF4081) // Monospace styled inline code
                            )
                        )
                        append(processedText.substring(cursor + 1, endCode))
                        pop()
                        cursor = endCode + 1
                    } else {
                        append("`")
                        cursor += 1
                    }
                }
                else -> {
                    append(processedText[cursor].toString())
                    cursor += 1
                }
            }
        }
    }
}

@Composable
fun TypingDotsIndicator(tint: Color) {
    val transition = rememberInfiniteTransition(label = "typing_dots")

    // Fix #10: animate scale (not alpha) so dots are always fully visible and clearly bounce
    val dot1Scale by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "dot1",
    )
    val dot2Scale by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(500, delayMillis = 160, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "dot2",
    )
    val dot3Scale by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(500, delayMillis = 320, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "dot3",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .graphicsLayer { scaleX = dot1Scale; scaleY = dot1Scale }
                .clip(CircleShape)
                .background(tint)
        )
        Box(
            Modifier
                .size(8.dp)
                .graphicsLayer { scaleX = dot2Scale; scaleY = dot2Scale }
                .clip(CircleShape)
                .background(tint)
        )
        Box(
            Modifier
                .size(8.dp)
                .graphicsLayer { scaleX = dot3Scale; scaleY = dot3Scale }
                .clip(CircleShape)
                .background(tint)
        )
    }
}

@Composable
fun WelcomeScreen(
    analyticsHelper: AnalyticsHelper,
    onSuggestionClick: (String) -> Unit,
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var adLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val adLoader =
                AdLoader
                    .Builder(context, AdConfig.nativeAdUnitId)
                    .forNativeAd { ad ->
                        nativeAd = ad
                        adLoading = false
                    }.withAdListener(
                        object : com.google.android.gms.ads.AdListener() {
                            override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                                adLoading = false
                            }
                        },
                    ).build()
            adLoader.loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            adLoading = false
        }
    }

    DisposableEffect(nativeAd) {
        onDispose {
            nativeAd?.destroy()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // AI pulsing gradient icon
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_star")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "scale",
        )
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                        ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.meet_moneypilot_copilot),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.copilot_welcome_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (adLoading) {
            BannerPlaceholder(modifier = Modifier.padding(vertical = 12.dp))
        } else if (nativeAd != null) {
            AdmobNativeAd(nativeAd = nativeAd!!, modifier = Modifier.padding(vertical = 12.dp))
        }
    }
}

@Composable
fun ActionConfirmationCard(
    action: AiAction,
    onConfirm: (AiAction) -> Unit,
    onDismiss: () -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    var currentAction by remember(action) { mutableStateOf(action) }
    // Fix #7: track category validation error
    var categoryError by remember { mutableStateOf(false) }

    // local field states for AddTransaction
    var amountState by remember(action) {
        mutableStateOf(if (action is AiAction.AddTransaction) action.amount.toString() else "")
    }
    var typeState by remember(action) {
        mutableStateOf(if (action is AiAction.AddTransaction) action.type else TransactionType.EXPENSE)
    }
    var categoryState by remember(action) {
        mutableStateOf(if (action is AiAction.AddTransaction) action.categoryName else "")
    }
    var noteState by remember(action) {
        mutableStateOf(if (action is AiAction.AddTransaction) action.note else "")
    }
    var dateOffsetState by remember(action) {
        mutableStateOf(if (action is AiAction.AddTransaction) action.dateOffset.toString() else "0")
    }

    // local field states for AddInvestment
    var investmentNameState by remember(action) {
        mutableStateOf(if (action is AiAction.AddInvestment) action.name else "")
    }
    var investmentTypeState by remember(action) {
        mutableStateOf(if (action is AiAction.AddInvestment) action.type else "")
    }
    var investedAmountState by remember(action) {
        mutableStateOf(if (action is AiAction.AddInvestment) action.investedAmount.toString() else "")
    }
    var currentValueState by remember(action) {
        mutableStateOf(if (action is AiAction.AddInvestment) action.currentValue.toString() else "")
    }

    // local field states for AddLoan
    var loanNameState by remember(action) {
        mutableStateOf(if (action is AiAction.AddLoan) action.name else "")
    }
    var loanTotalAmountState by remember(action) {
        mutableStateOf(if (action is AiAction.AddLoan) action.totalAmount.toString() else "")
    }
    var loanEmiAmountState by remember(action) {
        mutableStateOf(if (action is AiAction.AddLoan) action.emiAmount.toString() else "")
    }
    var loanInterestRateState by remember(action) {
        mutableStateOf(if (action is AiAction.AddLoan) action.interestRate.toString() else "")
    }
    var loanTenureMonthsState by remember(action) {
        mutableStateOf(if (action is AiAction.AddLoan) action.tenureMonths.toString() else "")
    }
    var loanNextEmiDaysState by remember(action) {
        mutableStateOf(if (action is AiAction.AddLoan) action.nextEmiDays.toString() else "30")
    }

    fun saveChanges() {
        when (action) {
            is AiAction.AddTransaction -> {
                // Fix #7: validate category before saving
                if (categoryState.isBlank()) {
                    categoryError = true
                    return
                }
                categoryError = false
                val amount = amountState.toLongOrNull() ?: action.amount
                val dateOffset = dateOffsetState.toIntOrNull() ?: action.dateOffset
                currentAction =
                    AiAction.AddTransaction(
                        amount = amount,
                        type = typeState,
                        categoryName = categoryState,
                        note = noteState,
                        dateOffset = dateOffset,
                    )
            }

            is AiAction.AddInvestment -> {
                val invested = investedAmountState.toLongOrNull() ?: action.investedAmount
                val current = currentValueState.toLongOrNull() ?: action.currentValue
                currentAction =
                    AiAction.AddInvestment(
                        name = investmentNameState,
                        type = investmentTypeState,
                        investedAmount = invested,
                        currentValue = current,
                    )
            }

            is AiAction.AddLoan -> {
                val total = loanTotalAmountState.toLongOrNull() ?: action.totalAmount
                val emi = loanEmiAmountState.toLongOrNull() ?: action.emiAmount
                val interest = loanInterestRateState.toDoubleOrNull() ?: action.interestRate
                val tenure = loanTenureMonthsState.toIntOrNull() ?: action.tenureMonths
                val nextEmi = loanNextEmiDaysState.toIntOrNull() ?: action.nextEmiDays
                currentAction =
                    AiAction.AddLoan(
                        name = loanNameState,
                        totalAmount = total,
                        emiAmount = emi,
                        interestRate = interest,
                        tenureMonths = tenure,
                        nextEmiDays = nextEmi,
                    )
            }
        }
        isEditing = false
    }

    fun cancelChanges() {
        when (val current = currentAction) {
            is AiAction.AddTransaction -> {
                amountState = current.amount.toString()
                typeState = current.type
                categoryState = current.categoryName
                noteState = current.note
                dateOffsetState = current.dateOffset.toString()
            }

            is AiAction.AddInvestment -> {
                investmentNameState = current.name
                investmentTypeState = current.type
                investedAmountState = current.investedAmount.toString()
                currentValueState = current.currentValue.toString()
            }

            is AiAction.AddLoan -> {
                loanNameState = current.name
                loanTotalAmountState = current.totalAmount.toString()
                loanEmiAmountState = current.emiAmount.toString()
                loanInterestRateState = current.interestRate.toString()
                loanTenureMonthsState = current.tenureMonths.toString()
                loanNextEmiDaysState = current.nextEmiDays.toString()
            }
        }
        isEditing = false
    }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = RoundedCornerShape(16.dp),
        border =
            BorderStroke(
                width = 1.dp,
                brush =
                    Brush.linearGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                            ),
                    ),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        val (icon, titleRes, color) =
            when (currentAction) {
                is AiAction.AddTransaction ->
                    Triple(
                        Icons.Rounded.AccountBalanceWallet,
                        if ((currentAction as AiAction.AddTransaction).type.name == "EXPENSE") R.string.log_expense else R.string.log_income,
                        MaterialTheme.colorScheme.primary,
                    )

                is AiAction.AddInvestment ->
                    Triple(
                        Icons.AutoMirrored.Rounded.TrendingUp,
                        R.string.log_investment,
                        Color(0xFF4CAF50),
                    )

                is AiAction.AddLoan ->
                    Triple(
                        Icons.Rounded.AccountBalance,
                        R.string.log_loan,
                        Color(0xFFFFA500),
                    )
            }

        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                if (!isEditing) {
                    IconButton(onClick = { isEditing = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.edit_details),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEditing) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (action) {
                        is AiAction.AddTransaction -> {
                            OutlinedTextField(
                                value = amountState,
                                onValueChange = { amountState = it },
                                label = { Text(stringResource(R.string.amount)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    ),
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                FilterChip(
                                    selected = typeState == TransactionType.EXPENSE,
                                    onClick = { typeState = TransactionType.EXPENSE },
                                    label = { Text(stringResource(R.string.expense)) },
                                    modifier = Modifier.weight(1f),
                                )
                                FilterChip(
                                    selected = typeState == TransactionType.INCOME,
                                    onClick = { typeState = TransactionType.INCOME },
                                    label = { Text(stringResource(R.string.income)) },
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            OutlinedTextField(
                                value = categoryState,
                                onValueChange = {
                                    categoryState = it
                                    if (it.isNotBlank()) categoryError = false // Fix #7: clear error on input
                                },
                                label = { Text(stringResource(R.string.category)) },
                                // Fix #6: placeholder hint when field is empty
                                placeholder = { Text(stringResource(R.string.hint_category), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
                                isError = categoryError,
                                supportingText = if (categoryError) {
                                    { Text(stringResource(R.string.category_required), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                                } else null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            OutlinedTextField(
                                value = noteState,
                                onValueChange = { noteState = it },
                                label = { Text(stringResource(R.string.detail_note)) },
                                // Fix #6: placeholder hint when field is empty
                                placeholder = { Text(stringResource(R.string.hint_note), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            OutlinedTextField(
                                value = dateOffsetState,
                                onValueChange = { dateOffsetState = it },
                                label = { Text(stringResource(R.string.date_offset_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    ),
                            )
                        }

                        is AiAction.AddInvestment -> {
                            OutlinedTextField(
                                value = investmentNameState,
                                onValueChange = { investmentNameState = it },
                                label = { Text(stringResource(R.string.detail_asset_name)) },
                                // Fix #6: placeholder hint
                                placeholder = { Text(stringResource(R.string.hint_investment_name), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            OutlinedTextField(
                                value = investmentTypeState,
                                onValueChange = { investmentTypeState = it },
                                label = { Text(stringResource(R.string.detail_asset_type)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            OutlinedTextField(
                                value = investedAmountState,
                                onValueChange = { investedAmountState = it },
                                label = { Text(stringResource(R.string.detail_invested)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    ),
                            )

                            OutlinedTextField(
                                value = currentValueState,
                                onValueChange = { currentValueState = it },
                                label = { Text(stringResource(R.string.detail_current_value)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    ),
                            )
                        }

                        is AiAction.AddLoan -> {
                            OutlinedTextField(
                                value = loanNameState,
                                onValueChange = { loanNameState = it },
                                label = { Text(stringResource(R.string.detail_loan_name)) },
                                // Fix #6: placeholder hint
                                placeholder = { Text(stringResource(R.string.hint_loan_name), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            OutlinedTextField(
                                value = loanTotalAmountState,
                                onValueChange = { loanTotalAmountState = it },
                                label = { Text(stringResource(R.string.detail_principal)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    ),
                            )

                            OutlinedTextField(
                                value = loanEmiAmountState,
                                onValueChange = { loanEmiAmountState = it },
                                label = { Text(stringResource(R.string.detail_emi_amount)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    ),
                            )

                            OutlinedTextField(
                                value = loanInterestRateState,
                                onValueChange = { loanInterestRateState = it },
                                label = { Text(stringResource(R.string.interest_rate_pa_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                                    ),
                            )

                            OutlinedTextField(
                                value = loanTenureMonthsState,
                                onValueChange = { loanTenureMonthsState = it },
                                label = { Text(stringResource(R.string.tenure_months)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    ),
                            )

                            OutlinedTextField(
                                value = loanNextEmiDaysState,
                                onValueChange = { loanNextEmiDaysState = it },
                                label = { Text(stringResource(R.string.first_emi_due_days)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    ),
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (val current = currentAction) {
                        is AiAction.AddTransaction -> {
                            DetailRow(stringResource(R.string.amount), "₹${current.amount}")
                            DetailRow(stringResource(R.string.category), current.categoryName)
                            DetailRow(stringResource(R.string.detail_note), current.note.ifBlank { "N/A" })
                            // Fix #5: positive dateOffset = future date; negative = past
                            val dateLabel =
                                when {
                                    current.dateOffset == 0 -> stringResource(R.string.today)
                                    current.dateOffset == -1 -> stringResource(R.string.yesterday)
                                    current.dateOffset < -1 -> stringResource(R.string.days_ago, kotlin.math.abs(current.dateOffset))
                                    else -> stringResource(R.string.in_days, current.dateOffset) // future
                                }
                            DetailRow(stringResource(R.string.detail_date), dateLabel)
                        }

                        is AiAction.AddInvestment -> {
                            DetailRow(stringResource(R.string.detail_asset_name), current.name)
                            DetailRow(stringResource(R.string.detail_asset_type), current.type)
                            DetailRow(stringResource(R.string.detail_invested), "₹${current.investedAmount}")
                            DetailRow(stringResource(R.string.detail_current_value), "₹${current.currentValue}")
                        }

                        is AiAction.AddLoan -> {
                            DetailRow(stringResource(R.string.detail_loan_name), current.name)
                            DetailRow(stringResource(R.string.detail_principal), "₹${current.totalAmount}")
                            DetailRow(stringResource(R.string.detail_emi_amount), "₹${current.emiAmount}")
                            if (current.interestRate > 0) {
                                DetailRow(stringResource(R.string.interest), "${current.interestRate}% p.a.")
                            }
                            if (current.tenureMonths > 0) {
                                DetailRow(stringResource(R.string.tenure), "${current.tenureMonths} months")
                            }
                            DetailRow(stringResource(R.string.detail_first_emi_due), stringResource(R.string.in_days, current.nextEmiDays))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isEditing) {
                    OutlinedButton(
                        onClick = { cancelChanges() },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(44.dp),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.cancel), fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                                    ),
                                )
                                .clickable { saveChanges() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.save), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(44.dp),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.dismiss), fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                                    ),
                                )
                                .clickable { onConfirm(currentAction) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.confirm), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun Int.dpToPx(context: android.content.Context): Int = (this * context.resources.displayMetrics.density).toInt()

@Composable
fun AdmobNativeAd(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Resolve theme colors
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.toArgb()

    AndroidView(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        factory = { ctx ->
            NativeAdView(ctx).apply {
                val rootLayout =
                    FrameLayout(ctx).apply {
                        layoutParams =
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                    }

                // Main card container
                val cardContainer =
                    LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(
                            16.dpToPx(ctx),
                            20.dpToPx(ctx), // overlapping badge clearance
                            16.dpToPx(ctx),
                            16.dpToPx(ctx),
                        )

                        // Linear Gradient matching primary/tertiary colors of the app theme
                        val gradient =
                            GradientDrawable(
                                GradientDrawable.Orientation.LEFT_RIGHT,
                                intArrayOf(primaryColor, tertiaryColor),
                            ).apply {
                                cornerRadius = 20.dpToPx(ctx).toFloat()
                            }
                        background = gradient

                        layoutParams =
                            FrameLayout
                                .LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                ).apply {
                                    topMargin = 8.dpToPx(ctx)
                                }
                    }

                // Icon (ImageView)
                val iconView =
                    ImageView(ctx).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                44.dpToPx(ctx),
                                44.dpToPx(ctx),
                            )
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        val iconBg =
                            GradientDrawable().apply {
                                setColor(0x26FFFFFF) // 15% opacity white
                                cornerRadius = 12.dpToPx(ctx).toFloat()
                            }
                        background = iconBg
                        setPadding(4.dpToPx(ctx), 4.dpToPx(ctx), 4.dpToPx(ctx), 4.dpToPx(ctx))
                    }
                cardContainer.addView(iconView)

                // Text Container (Headline & Body)
                val textContainer =
                    LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams =
                            LinearLayout
                                .LayoutParams(
                                    0,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    1f,
                                ).apply {
                                    leftMargin = 12.dpToPx(ctx)
                                    rightMargin = 12.dpToPx(ctx)
                                }
                    }

                val headlineView =
                    TextView(ctx).apply {
                        textSize = 14f
                        setTextColor(android.graphics.Color.WHITE)
                        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                textContainer.addView(headlineView)

                val bodyView =
                    TextView(ctx).apply {
                        textSize = 11f
                        setTextColor(0xC7FFFFFF.toInt()) // 78% opacity white
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        setPadding(0, 2.dpToPx(ctx), 0, 0)
                    }
                textContainer.addView(bodyView)

                cardContainer.addView(textContainer)

                // Call to Action View
                val ctaView =
                    TextView(ctx).apply {
                        textSize = 12f
                        setTextColor("#1A237E".toColorInt())
                        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(14.dpToPx(ctx), 8.dpToPx(ctx), 14.dpToPx(ctx), 8.dpToPx(ctx))

                        val ctaBg =
                            GradientDrawable().apply {
                                setColor("#FFD54F".toColorInt()) // Gold color to match LearnFinance CTA
                                cornerRadius = 10.dpToPx(ctx).toFloat()
                            }
                        background = ctaBg
                    }
                cardContainer.addView(ctaView)

                rootLayout.addView(cardContainer)

                // SPONSORED badge
                val adBadge =
                    TextView(ctx).apply {
                        text = ctx.getString(R.string.ad_sponsored)
                        textSize = 9f
                        setTextColor(android.graphics.Color.WHITE)
                        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(6.dpToPx(ctx), 2.dpToPx(ctx), 6.dpToPx(ctx), 2.dpToPx(ctx))

                        val badgeBg =
                            GradientDrawable().apply {
                                setColor(tertiaryColor)
                                cornerRadius = 4.dpToPx(ctx).toFloat()
                            }
                        background = badgeBg

                        layoutParams =
                            FrameLayout
                                .LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                ).apply {
                                    gravity = Gravity.TOP or Gravity.START
                                    leftMargin = 16.dpToPx(ctx)
                                }
                    }
                rootLayout.addView(adBadge)

                addView(rootLayout)

                // Register asset views
                this.headlineView = headlineView
                this.bodyView = bodyView
                this.iconView = iconView
                this.callToActionView = ctaView

                // Bind Native Ad assets
                headlineView.text = nativeAd.headline
                bodyView.text = nativeAd.body

                if (nativeAd.icon != null) {
                    iconView.setImageDrawable(nativeAd.icon?.drawable)
                    iconView.visibility = android.view.View.VISIBLE
                } else {
                    iconView.visibility = android.view.View.GONE
                }

                if (nativeAd.callToAction != null) {
                    ctaView.text = nativeAd.callToAction
                    ctaView.visibility = android.view.View.VISIBLE
                } else {
                    ctaView.visibility = android.view.View.GONE
                }

                setNativeAd(nativeAd)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleQueriesBottomSheet(
    analyticsHelper: AnalyticsHelper,
    onDismissRequest: () -> Unit,
    onSuggestionClick: (String) -> Unit,
) {
    val categories = listOf(
        stringResource(R.string.transactions),
        stringResource(R.string.category_wealth),
        stringResource(R.string.loans)
    )
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    BaseBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                    ),
                                ),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.explore_suggestions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.explore_suggestions_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }

            // Category tag selection row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
            ) {
                categories.forEachIndexed { index, name ->
                    val isSelected = index == selectedCategoryIndex
                    val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    val chipTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(chipBg)
                                .clickable { selectedCategoryIndex = index }
                                .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            color = chipTextColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val currentSuggestions =
                when (selectedCategoryIndex) {
                    0 ->
                        listOf(
                            Pair(stringResource(R.string.suggestion_food_expense), Icons.Rounded.Fastfood),
                            Pair(stringResource(R.string.suggestion_fuel_expense), Icons.Rounded.LocalGasStation),
                            Pair(stringResource(R.string.suggestion_salary_income), Icons.Rounded.Payments),
                            Pair(stringResource(R.string.suggestion_shopping_expense), Icons.Rounded.ShoppingBag),
                        )

                    1 ->
                        listOf(
                            Pair(stringResource(R.string.suggestion_fd_investment), Icons.Rounded.Savings),
                            Pair(stringResource(R.string.suggestion_stock_investment), Icons.AutoMirrored.Rounded.TrendingUp),
                            Pair(stringResource(R.string.suggestion_mf_investment), Icons.Rounded.Analytics),
                            Pair(stringResource(R.string.suggestion_gold_investment), Icons.Rounded.MonetizationOn),
                        )

                    else ->
                        listOf(
                            Pair(stringResource(R.string.suggestion_home_loan), Icons.Rounded.Home),
                            Pair(stringResource(R.string.suggestion_personal_loan), Icons.Rounded.Person),
                            Pair(stringResource(R.string.suggestion_car_loan), Icons.Rounded.DirectionsCar),
                            Pair(stringResource(R.string.suggestion_loans_summary), Icons.Rounded.Summarize),
                        )
                }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
            ) {
                currentSuggestions.forEach { (text, icon) ->
                    var clicked by remember { mutableStateOf(false) }
                    val scaleFactor by animateFloatAsState(
                        targetValue = if (clicked) 0.97f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                        label = "suggestion_click",
                    )
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scaleFactor
                                    scaleY = scaleFactor
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    clicked = true
                                    analyticsHelper.logEvent(
                                        AnalyticsConstants.Event.AI_CHAT_BOTTOMSHEET_SUGGESTION_CLICKED,
                                        mapOf(AnalyticsConstants.Param.TEXT to text),
                                    )
                                    onSuggestionClick(text)
                                    // Fix #3: removed `clicked = false` here — setting it back in the
                                    // same lambda frame prevents any recomposition showing the pressed state.
                                    // The state naturally resets when the composable leaves composition.
                                },
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
