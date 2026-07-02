package prasad.vennam.moneypilot.ui.login

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.rememberAsyncImagePainter
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.data.UserPreferences
import prasad.vennam.moneypilot.ui.viewmodel.MainViewModel
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.TrackScreen
import prasad.vennam.moneypilot.util.QuotesManager
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AuthScreen(
    mainViewModel: MainViewModel,
    analyticsHelper: AnalyticsHelper,
    skipSplash: Boolean = false,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onAuthSuccess: () -> Unit,
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.AUTH)
    val context = LocalContext.current
    val quote = remember(context) { QuotesManager.getQuoteOfTheDay(context) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val credentialManager = remember { CredentialManager.create(context) }

    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    var isSplash by rememberSaveable { mutableStateOf(!skipSplash) }
    var loginError by remember { mutableStateOf<String?>(null) }

    val loginErrorNoAccounts = stringResource(R.string.login_error_no_accounts)
    val loginErrorCancelled = stringResource(R.string.login_error_cancelled)
    val loginErrorFailed = stringResource(R.string.login_error_failed)
    val loginErrorGeneric = stringResource(R.string.login_error_generic)

    val transitionProgress by animateFloatAsState(
        targetValue = if (isSplash) 0f else 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "SplashToLoginTransition",
    )

    val initialScale = remember { Animatable(if (skipSplash || !isSplash) 1f else 0f) }
    val initialAlpha = remember { Animatable(if (skipSplash || !isSplash) 1f else 0f) }

    // Infinite transitions for dynamic UI feel
    val infiniteTransition = rememberInfiniteTransition(label = "AuthAnimations")

    // Animated background gradient coordinates
    val bgAnimProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BgAnimation"
    )

    // Animated floating bokehs
    val bokehTranslationY1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bokeh1"
    )
    val bokehTranslationY2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -60f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Bokeh2"
    )

    // Logo breathing animation (only active during splash phase)
    val logoPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoPulse"
    )

    // Radar wave animations (only active during splash phase)
    val radarScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarScale1"
    )
    val radarAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAlpha1"
    )

    val radarScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarScale2"
    )
    val radarAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAlpha2"
    )

    val logoRotation = remember { Animatable(if (skipSplash || !isSplash) 0f else -180f) }

    // Staggered layout animations for entering login screen
    val loginAlpha1 by animateFloatAsState(
        targetValue = if (!isSplash) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 100, easing = EaseInOutSine),
        label = "loginAlpha1"
    )
    val loginOffsetY1 by animateFloatAsState(
        targetValue = if (!isSplash) 0f else 40f,
        animationSpec = tween(durationMillis = 800, delayMillis = 100, easing = EaseInOutSine),
        label = "loginOffsetY1"
    )

    val loginAlpha2 by animateFloatAsState(
        targetValue = if (!isSplash) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 300, easing = EaseInOutSine),
        label = "loginAlpha2"
    )
    val loginOffsetY2 by animateFloatAsState(
        targetValue = if (!isSplash) 0f else 60f,
        animationSpec = tween(durationMillis = 800, delayMillis = 300, easing = EaseInOutSine),
        label = "loginOffsetY2"
    )

    val loginAlpha3 by animateFloatAsState(
        targetValue = if (!isSplash) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 500, easing = EaseInOutSine),
        label = "loginAlpha3"
    )
    val loginOffsetY3 by animateFloatAsState(
        targetValue = if (!isSplash) 0f else 80f,
        animationSpec = tween(durationMillis = 800, delayMillis = 500, easing = EaseInOutSine),
        label = "loginOffsetY3"
    )

    val logoBias by animateFloatAsState(
        targetValue = if (isSplash) 0f else -0.75f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "LogoBiasAnimation"
    )

    LaunchedEffect(Unit) {
        if (!skipSplash && isSplash) {
            launch {
                initialScale.animateTo(
                    1f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                )
            }
            launch {
                logoRotation.animateTo(
                    0f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                )
            }
            launch {
                initialAlpha.animateTo(1f, animationSpec = tween(1000))
            }

            delay(2500.milliseconds)

            // Read the current StateFlow value directly to avoid stale Compose snapshot
            if (mainViewModel.isLoggedIn.value) {
                onAuthSuccess()
            } else {
                isSplash = false
            }
        } else {
            if (mainViewModel.isLoggedIn.value) onAuthSuccess()
        }
    }

    // Logo size shrinks during transition
    val logoSurfaceSize = lerp(120.dp, 80.dp, transitionProgress)
    val logoIconSize = lerp(64.dp, 40.dp, transitionProgress)
    val logoSpacer = lerp(32.dp, 24.dp, transitionProgress)

    // Combine breathing scale with initial entry scale
    val combinedLogoScale = if (isSplash) {
        initialScale.value * logoPulseScale
    } else {
        initialScale.value
    }

    // Dynamic animated flowing gradient background
    val gradientStart = Offset(x = 0f, y = -500f + (bgAnimProgress * 300f))
    val gradientEnd = Offset(x = 1000f, y = 1500f - (bgAnimProgress * 300f))
    
    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f - (0.12f * transitionProgress)),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f - (0.07f * transitionProgress)),
            MaterialTheme.colorScheme.background,
        ),
        start = gradientStart,
        end = gradientEnd
    )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(brush = backgroundBrush),
        contentAlignment = Alignment.Center,
    ) {
        // Floating Ambient Bokeh Glows (Bokeh 1)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = bokehTranslationY1 * 2f
                    translationX = bokehTranslationY1 * 0.5f
                }
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .offset(x = (-60).dp, y = 40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        shape = CircleShape
                    )
                    .blur(60.dp)
            )
        }

        // Bokeh 2
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = bokehTranslationY2 * 1.5f
                    translationX = -bokehTranslationY2 * 0.8f
                }
        ) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 80.dp, y = (-80).dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
                    .blur(70.dp)
            )
        }

        Column(
            modifier =
                Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .align(BiasAlignment(horizontalBias = 0f, verticalBias = logoBias))
                    .animateContentSize(animationSpec = tween(800, easing = FastOutSlowInEasing)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Shared Logo with Radar Waves
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(logoSurfaceSize * 1.6f)
            ) {
                if (isSplash) {
                    // Radar Ring 1
                    Box(
                        modifier = Modifier
                            .size(logoSurfaceSize)
                            .scale(radarScale1)
                            .alpha(radarAlpha1 * initialAlpha.value)
                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), CircleShape)
                    )
                    // Radar Ring 2
                    Box(
                        modifier = Modifier
                            .size(logoSurfaceSize)
                            .scale(radarScale2)
                            .alpha(radarAlpha2 * initialAlpha.value)
                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), CircleShape)
                    )
                }

                Surface(
                    modifier =
                        Modifier
                            .size(logoSurfaceSize)
                            .scale(combinedLogoScale)
                            .rotate(logoRotation.value)
                            .alpha(initialAlpha.value),
                    shape = CircleShape,
                    color = Color.Transparent,
                    tonalElevation = lerp(8.dp, 4.dp, transitionProgress),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = rememberAsyncImagePainter(model = R.mipmap.ic_launcher_round),
                            contentDescription = "MoneyPilot Logo",
                            modifier = Modifier.size(logoSurfaceSize),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(logoSpacer))

            // Box wrapping the dynamic content
            Box(contentAlignment = Alignment.TopCenter) {
                // SPLASH CONTENT
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSplash,
                    enter = fadeIn(),
                    exit = fadeOut(tween(400)) + slideOutVertically(tween(800, easing = FastOutSlowInEasing)) { it / 2 },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.moneypilot),
                            style =
                                MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                ),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.alpha(initialAlpha.value),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.track_save_grow),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 4.sp,
                                    fontSize = 18.sp,
                                ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.alpha(initialAlpha.value),
                        )
                    }
                }

                // LOGIN CONTENT
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isSplash,
                    enter = fadeIn(tween(400, delayMillis = 400)) + slideInVertically(tween(800, easing = FastOutSlowInEasing)) { it / 4 },
                    exit = fadeOut(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Group 1: Welcome Header
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = loginAlpha1
                                    translationY = loginOffsetY1.dp.toPx()
                                }
                        ) {
                            Text(
                                text = stringResource(R.string.welcome_to_moneypilot),
                                style =
                                    MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                    ),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.your_journey_to_financial_freedom),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )

                            // Dynamic Motivational Quote
                            quote?.let { q ->
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "\"${q.quote}\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                    ),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "— ${q.author}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Group 2: Glassmorphic Card containing Buttons
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = loginAlpha2
                                    translationY = loginOffsetY2.dp.toPx()
                                },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Google Sign In Button
                                if (isLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 3.dp
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isLoading = true
                                                try {
                                                    val googleIdOption =
                                                        GetGoogleIdOption
                                                            .Builder()
                                                            .setFilterByAuthorizedAccounts(false)
                                                            .setServerClientId(prasad.vennam.moneypilot.BuildConfig.GOOGLE_CLIENT_ID)
                                                            .setAutoSelectEnabled(true)
                                                            .build()

                                                    val request =
                                                        GetCredentialRequest
                                                            .Builder()
                                                            .addCredentialOption(googleIdOption)
                                                            .build()

                                                    val result =
                                                        credentialManager.getCredential(
                                                            request = request,
                                                            context = context,
                                                        )

                                                    val credential = result.credential
                                                    val googleIdTokenCredential =
                                                        try {
                                                            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                                                GoogleIdTokenCredential.createFrom(credential.data)
                                                            } else {
                                                                null
                                                            }
                                                        } catch (e: Exception) {
                                                            null
                                                        }

                                                    if (googleIdTokenCredential != null) {
                                                        analyticsHelper.logEvent(
                                                            AnalyticsConstants.Event.LOGIN,
                                                            mapOf(AnalyticsConstants.Param.METHOD to "google"),
                                                        )
                                                        mainViewModel.saveUserData(
                                                            UserPreferences.UserData(
                                                                name = googleIdTokenCredential.displayName ?: "User",
                                                                email = googleIdTokenCredential.id,
                                                                photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                                                            ),
                                                        ) {
                                                            onAuthSuccess()
                                                        }
                                                    }
                                                } catch (e: NoCredentialException) {
                                                    Log.e("AuthScreen", "Login failed: No credentials available", e)
                                                    loginError = loginErrorNoAccounts
                                                } catch (e: GetCredentialCancellationException) {
                                                    Log.d("AuthScreen", "Login cancelled by user")
                                                    loginError = loginErrorCancelled
                                                } catch (e: GetCredentialException) {
                                                    Log.e("AuthScreen", "Login failed: ${e.message}")
                                                    loginError = loginErrorFailed
                                                } catch (e: Exception) {
                                                    Log.e("AuthScreen", "Error: ${e.message}")
                                                    loginError = loginErrorGeneric
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        },
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                        shape = MaterialTheme.shapes.large,
                                        colors =
                                            ButtonDefaults.buttonColors(
                                                containerColor = Color.White,
                                                contentColor = Color(0xFF1F1F1F),
                                            ),
                                        border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_google_logo),
                                                contentDescription = "Google Logo",
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(22.dp),
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                stringResource(R.string.continue_with_google),
                                                style =
                                                    MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF1F1F1F),
                                                    ),
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedButton(
                                        onClick = {
                                            analyticsHelper.logEvent(
                                                AnalyticsConstants.Event.LOGIN,
                                                mapOf(AnalyticsConstants.Param.METHOD to "guest"),
                                            )
                                            scope.launch {
                                                mainViewModel.saveUserData(
                                                    UserPreferences.UserData(
                                                        name = "Guest Pilot",
                                                        email = "guest@moneypilot.app",
                                                        photoUrl = null,
                                                    ),
                                                ) {
                                                    onAuthSuccess()
                                                }
                                            }
                                        },
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                        shape = MaterialTheme.shapes.large,
                                        border =
                                            ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                                brush =
                                                    Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                            MaterialTheme.colorScheme.primary,
                                                        ),
                                                    ),
                                            ),
                                    ) {
                                        Text(
                                            stringResource(R.string.continue_as_guest),
                                            style =
                                                MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                ),
                                        )
                                    }
                                }

                                // Login error message
                                loginError?.let { error ->
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = error,
                                        style =
                                            MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.error,
                                                textAlign = TextAlign.Center,
                                            ),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Group 3: Privacy Policy Text
                        val annotatedText =
                            buildAnnotatedString {
                                append(stringResource(R.string.agree_to_policy_prefix))
                                withLink(
                                    LinkAnnotation.Clickable(
                                        tag = "terms",
                                        styles =
                                            TextLinkStyles(
                                                style =
                                                    SpanStyle(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        textDecoration = TextDecoration.Underline,
                                                    ),
                                            ),
                                    ) {
                                        onNavigateToTerms()
                                    },
                                ) {
                                    append(stringResource(R.string.terms_of_service))
                                }
                                append(stringResource(R.string.agree_to_policy_and))
                                withLink(
                                    LinkAnnotation.Clickable(
                                        tag = "privacy",
                                        styles =
                                            TextLinkStyles(
                                                style =
                                                    SpanStyle(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        textDecoration = TextDecoration.Underline,
                                                    ),
                                            ),
                                    ) {
                                        onNavigateToPrivacy()
                                    },
                                ) {
                                    append(stringResource(R.string.privacy_policy))
                                }
                                append(".")
                            }

                        Text(
                            text = annotatedText,
                            style =
                                MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                ),
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .graphicsLayer {
                                    alpha = loginAlpha3
                                    translationY = loginOffsetY3.dp.toPx()
                                },
                        )
                    }
                }
            }
        }
    }
}
