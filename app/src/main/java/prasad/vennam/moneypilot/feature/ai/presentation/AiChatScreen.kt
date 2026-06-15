package prasad.vennam.moneypilot.feature.ai.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
                color = MaterialTheme.colorScheme.surface,
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            },
                            shape = RoundedCornerShape(28.dp),
                            singleLine = true,
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                ),
                            enabled = aiState is LlmState.Ready || aiState is LlmState.Generating || aiState is LlmState.ActionConfirm,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            enabled =
                                inputText.isNotBlank() &&
                                    (aiState is LlmState.Ready || aiState is LlmState.Generating || aiState is LlmState.ActionConfirm),
                            colors =
                                IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = stringResource(R.string.search))
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
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Download,
                                    null,
                                    modifier = Modifier.size(32.dp),
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
                            Button(
                                onClick = { viewModel.downloadModel() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.download_model_btn), fontWeight = FontWeight.Bold)
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
                        },
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(messages) { message ->
                            ChatBubble(message)
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color),
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
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

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
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
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
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
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
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.meet_moneypilot_copilot),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.copilot_welcome_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.tap_quick_action_start),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SuggestionCard(
                category = stringResource(R.string.history),
                icon = Icons.Rounded.AccountBalanceWallet,
                iconColor = MaterialTheme.colorScheme.primary,
                suggestions =
                    listOf(
                        "Add food expense of 500 from Swiggy",
                        "Received freelancing payment of 12000",
                    ),
                onSuggestionClick = onSuggestionClick,
            )

            SuggestionCard(
                category = stringResource(R.string.wealth_assets),
                icon = Icons.Rounded.TrendingUp,
                iconColor = Color(0xFF4CAF50),
                suggestions =
                    listOf(
                        "Invested 25000 in FD today",
                        "Bought HDFC stock for 15k",
                    ),
                onSuggestionClick = onSuggestionClick,
            )

            SuggestionCard(
                category = stringResource(R.string.borrowings_loans),
                icon = Icons.Rounded.AccountBalance,
                iconColor = Color(0xFFFFA500),
                suggestions =
                    listOf(
                        "Add SBI Home Loan of 30L, EMI 45000",
                        "Add personal loan of 50k",
                    ),
                onSuggestionClick = onSuggestionClick,
            )
        }
    }
}

@Composable
fun SuggestionCard(
    category: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                suggestions.forEach { suggestion ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                .clickable { onSuggestionClick(suggestion) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
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
                        Icons.Rounded.TrendingUp,
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
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
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
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.dismiss), fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.confirm), fontWeight = FontWeight.SemiBold)
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
