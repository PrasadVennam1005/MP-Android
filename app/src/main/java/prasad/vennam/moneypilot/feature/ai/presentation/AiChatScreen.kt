package prasad.vennam.moneypilot.feature.ai.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.AdLoader
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import android.view.Gravity
import android.view.ViewGroup
import android.graphics.drawable.GradientDrawable
import prasad.vennam.moneypilot.ui.components.BannerPlaceholder
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.feature.ai.model.AiAction
import prasad.vennam.moneypilot.feature.ai.model.Author
import prasad.vennam.moneypilot.feature.ai.model.ChatMessage
import prasad.vennam.moneypilot.feature.ai.model.LlmState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onBackClick: () -> Unit,
    viewModel: AiViewModel = hiltViewModel(),
) {
    val messages by viewModel.messages.collectAsState()
    val aiState by viewModel.aiState.collectAsState(LlmState.Idle)
    val downloadProgress by viewModel.downloadProgress.collectAsState(0f)
    val pendingAction by viewModel.pendingAction.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.actionFeedback.collect { feedback ->
            snackbarHostState.showSnackbar(feedback)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
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
                                    text = (aiState as LlmState.Error).message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    if (pendingAction != null) {
                        ActionConfirmationCard(
                            action = pendingAction!!,
                            onConfirm = { viewModel.confirmAction(pendingAction!!) },
                            onDismiss = { viewModel.dismissAction() },
                        )
                    }

                    Row(
                        modifier =
                            Modifier
                                .padding(16.dp)
                                .navigationBarsPadding()
                                .imePadding()
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
                                    if (pendingAction != null) {
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
                        val buttonEnabled = inputText.isNotBlank() &&
                            (aiState is LlmState.Ready || aiState is LlmState.Generating || aiState is LlmState.ActionConfirm)
                        
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (buttonEnabled) {
                                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                                    } else {
                                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)))
                                    }
                                )
                                .clickable(
                                    enabled = buttonEnabled,
                                    onClick = {
                                        viewModel.sendMessage(inputText)
                                        inputText = ""
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Send,
                                contentDescription = stringResource(R.string.search),
                                tint = if (buttonEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            if (aiState is LlmState.Idle) {
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
                                stringResource(R.string.ai_download_required_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.ai_download_required_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                        )
                                    )
                                    .clickable { viewModel.downloadModel() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.download_model_btn),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
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
                        onSuggestionClick = { text ->
                            inputText = text
                            try {
                                focusRequester.requestFocus()
                            } catch (e: Exception) {
                                // Focus request might fail if node is not attached yet
                            }
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(messages, key = { it.id }) { message ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                visible = true
                            }
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                                    initialOffsetY = { it / 3 },
                                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                                )
                            ) {
                                ChatBubble(message)
                            }
                        }
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
            LlmState.Ready -> stringResource(R.string.status_ready)
            is LlmState.Error -> stringResource(R.string.status_error)
            is LlmState.Generating -> stringResource(R.string.status_processing)
            LlmState.Downloading -> stringResource(R.string.status_downloading)
            is LlmState.ActionConfirm -> stringResource(R.string.status_pending_confirm)
        }
    val color =
        when (state) {
            LlmState.Idle -> Color.Gray
            LlmState.Initializing -> Color(0xFFFFA500)
            LlmState.Ready -> Color(0xFF4CAF50)
            is LlmState.Error -> MaterialTheme.colorScheme.error
            is LlmState.Generating -> MaterialTheme.colorScheme.primary
            LlmState.Downloading -> MaterialTheme.colorScheme.primary
            is LlmState.ActionConfirm -> Color(0xFFFFA500)
        }
        
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
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
                            alpha = if (state is LlmState.Generating || state is LlmState.Initializing || state is LlmState.Downloading) alpha else 1f
                        )
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
            .widthIn(max = 280.dp)
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
                            brush = Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                )
                            ),
                            shape = bubbleShape,
                        )
                },
            ).padding(horizontal = 16.dp, vertical = 12.dp)

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
            if (message.content == "...") {
                TypingDotsIndicator(tint = textColor)
            } else {
                Text(
                    text = message.content,
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

@Composable
fun TypingDotsIndicator(tint: Color) {
    val transition = rememberInfiniteTransition(label = "typing_dots")

    val dot1Scale by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "dot1",
    )
    val dot2Scale by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(600, delayMillis = 200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "dot2",
    )
    val dot3Scale by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(600, delayMillis = 400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "dot3",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
    ) {
        val dotModifier = Modifier.size(6.dp).clip(CircleShape)
        Box(dotModifier.background(tint.copy(alpha = dot1Scale)))
        Box(dotModifier.background(tint.copy(alpha = dot2Scale)))
        Box(dotModifier.background(tint.copy(alpha = dot3Scale)))
    }
}

@Composable
fun WelcomeScreen(onSuggestionClick: (String) -> Unit) {
    val categories = listOf("Transactions", "Wealth", "Loans")
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    // Load AdMob Native Ad
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var adLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val adLoader = AdLoader.Builder(context, "ca-app-pub-3940256099942544/2247696110") // Test Native ID
                .forNativeAd { ad ->
                    nativeAd = ad
                    adLoading = false
                }
                .withAdListener(object : com.google.android.gms.ads.AdListener() {
                    override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                        adLoading = false
                    }
                })
                .build()
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
        modifier = Modifier
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
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        Box(
            modifier = Modifier
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
            textAlign = TextAlign.Center
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

        // Category Tag Selection Row
        Text(
            text = "QUICK ACTIONS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 8.dp, bottom = 8.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            categories.forEachIndexed { index, name ->
                val isSelected = index == selectedCategoryIndex
                val chipBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                val chipTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(chipBg)
                        .clickable { selectedCategoryIndex = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        color = chipTextColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        if (adLoading) {
            BannerPlaceholder(modifier = Modifier.padding(vertical = 12.dp))
        } else if (nativeAd != null) {
            AdmobNativeAd(nativeAd = nativeAd!!, modifier = Modifier.padding(vertical = 12.dp))
        }

        val currentSuggestions = when (selectedCategoryIndex) {
            0 -> listOf(
                Pair("Add food expense of 500 from Swiggy", Icons.Rounded.Fastfood),
                Pair("Spent 1200 on petrol today", Icons.Rounded.LocalGasStation),
                Pair("Received salary of 75000", Icons.Rounded.Payments),
                Pair("Log shopping expense of 3500 on Amazon", Icons.Rounded.ShoppingBag)
            )
            1 -> listOf(
                Pair("Invested 25000 in FD today", Icons.Rounded.Savings),
                Pair("Bought HDFC stock for 15k", Icons.AutoMirrored.Rounded.TrendingUp),
                Pair("Invested 10000 in Mutual Fund", Icons.Rounded.Analytics),
                Pair("Bought Gold for 5000 today", Icons.Rounded.MonetizationOn)
            )
            else -> listOf(
                Pair("Add SBI Home Loan of 30L, EMI 45000", Icons.Rounded.Home),
                Pair("Add personal loan of 50k", Icons.Rounded.Person),
                Pair("Add car loan of 12L, EMI 22000", Icons.Rounded.DirectionsCar),
                Pair("Show active loans summary", Icons.Rounded.Summarize)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            currentSuggestions.forEach { (text, icon) ->
                var clicked by remember { mutableStateOf(false) }
                val scaleFactor by animateFloatAsState(
                    targetValue = if (clicked) 0.97f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                    label = "suggestion_click"
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scaleFactor
                            scaleY = scaleFactor
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            clicked = true
                            onSuggestionClick(text)
                            clicked = false
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ActionConfirmationCard(
    action: AiAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
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
            when (action) {
                is AiAction.AddTransaction ->
                    Triple(
                        Icons.Rounded.AccountBalanceWallet,
                        if (action.type.name == "EXPENSE") R.string.log_expense else R.string.log_income,
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
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                when (action) {
                    is AiAction.AddTransaction -> {
                        DetailRow(stringResource(R.string.amount), "₹${action.amount}")
                        DetailRow(stringResource(R.string.category), action.categoryName)
                        DetailRow(stringResource(R.string.detail_note), action.note.ifBlank { "N/A" })
                        val dateLabel =
                            when (action.dateOffset) {
                                0 -> "Today"
                                -1 -> "Yesterday"
                                else -> "${kotlin.math.abs(action.dateOffset)} days ago"
                            }
                        DetailRow(stringResource(R.string.detail_date), dateLabel)
                    }
                    is AiAction.AddInvestment -> {
                        DetailRow(stringResource(R.string.detail_asset_name), action.name)
                        DetailRow(stringResource(R.string.detail_asset_type), action.type)
                        DetailRow(stringResource(R.string.detail_invested), "₹${action.investedAmount}")
                        DetailRow(stringResource(R.string.detail_current_value), "₹${action.currentValue}")
                    }
                    is AiAction.AddLoan -> {
                        DetailRow(stringResource(R.string.detail_loan_name), action.name)
                        DetailRow(stringResource(R.string.detail_principal), "₹${action.totalAmount}")
                        DetailRow(stringResource(R.string.detail_emi_amount), "₹${action.emiAmount}")
                        if (action.interestRate > 0) {
                            DetailRow("Interest", "${action.interestRate}% p.a.")
                        }
                        if (action.tenureMonths > 0) {
                            DetailRow("Tenure", "${action.tenureMonths} months")
                        }
                        DetailRow(stringResource(R.string.detail_first_emi_due), "In ${action.nextEmiDays} days")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
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
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                            )
                        )
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center
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

private fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

@Composable
fun AdmobNativeAd(nativeAd: NativeAd, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // Resolve theme colors
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.toArgb()

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        factory = { ctx ->
            NativeAdView(ctx).apply {
                val rootLayout = FrameLayout(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }

                // Main card container
                val cardContainer = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(
                        16.dpToPx(ctx),
                        20.dpToPx(ctx), // overlapping badge clearance
                        16.dpToPx(ctx),
                        16.dpToPx(ctx)
                    )
                    
                    // Linear Gradient matching primary/tertiary colors of the app theme
                    val gradient = GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(primaryColor, tertiaryColor)
                    ).apply {
                        cornerRadius = 20.dpToPx(ctx).toFloat()
                    }
                    background = gradient
                    
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 8.dpToPx(ctx)
                    }
                }

                // Icon (ImageView)
                val iconView = ImageView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        44.dpToPx(ctx),
                        44.dpToPx(ctx)
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    val iconBg = GradientDrawable().apply {
                        setColor(0x26FFFFFF.toInt()) // 15% opacity white
                        cornerRadius = 12.dpToPx(ctx).toFloat()
                    }
                    background = iconBg
                    setPadding(4.dpToPx(ctx), 4.dpToPx(ctx), 4.dpToPx(ctx), 4.dpToPx(ctx))
                }
                cardContainer.addView(iconView)

                // Text Container (Headline & Body)
                val textContainer = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        leftMargin = 12.dpToPx(ctx)
                        rightMargin = 12.dpToPx(ctx)
                    }
                }

                val headlineView = TextView(ctx).apply {
                    textSize = 14f
                    setTextColor(android.graphics.Color.WHITE)
                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                textContainer.addView(headlineView)

                val bodyView = TextView(ctx).apply {
                    textSize = 11f
                    setTextColor(0xC7FFFFFF.toInt()) // 78% opacity white
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setPadding(0, 2.dpToPx(ctx), 0, 0)
                }
                textContainer.addView(bodyView)

                cardContainer.addView(textContainer)

                // Call to Action View
                val ctaView = TextView(ctx).apply {
                    textSize = 12f
                    setTextColor(android.graphics.Color.parseColor("#1A237E"))
                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setPadding(14.dpToPx(ctx), 8.dpToPx(ctx), 14.dpToPx(ctx), 8.dpToPx(ctx))
                    
                    val ctaBg = GradientDrawable().apply {
                        setColor(android.graphics.Color.parseColor("#FFD54F")) // Gold color to match LearnFinance CTA
                        cornerRadius = 10.dpToPx(ctx).toFloat()
                    }
                    background = ctaBg
                }
                cardContainer.addView(ctaView)

                rootLayout.addView(cardContainer)

                // SPONSORED badge
                val adBadge = TextView(ctx).apply {
                    text = "SPONSORED"
                    textSize = 9f
                    setTextColor(android.graphics.Color.WHITE)
                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setPadding(6.dpToPx(ctx), 2.dpToPx(ctx), 6.dpToPx(ctx), 2.dpToPx(ctx))
                    
                    val badgeBg = GradientDrawable().apply {
                        setColor(tertiaryColor)
                        cornerRadius = 4.dpToPx(ctx).toFloat()
                    }
                    background = badgeBg
                    
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
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
        }
    )
}
