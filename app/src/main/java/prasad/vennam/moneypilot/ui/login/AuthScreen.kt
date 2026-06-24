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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
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
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val credentialManager = remember { CredentialManager.create(context) }

    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    var isSplash by rememberSaveable { mutableStateOf(!skipSplash) }
    var loginError by remember { mutableStateOf<String?>(null) }

    val transitionProgress by animateFloatAsState(
        targetValue = if (isSplash) 0f else 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "SplashToLoginTransition",
    )

    val initialScale = remember { Animatable(if (skipSplash || !isSplash) 1f else 0f) }
    val initialAlpha = remember { Animatable(if (skipSplash || !isSplash) 1f else 0f) }

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

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f - (0.1f * transitionProgress)),
                                    MaterialTheme.colorScheme.background,
                                ),
                        ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .animateContentSize(animationSpec = tween(800, easing = FastOutSlowInEasing)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Shared Logo
            Surface(
                modifier =
                    Modifier
                        .size(logoSurfaceSize)
                        .scale(initialScale.value)
                        .alpha(initialAlpha.value),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = lerp(8.dp, 4.dp, transitionProgress),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.AirplanemodeActive,
                        contentDescription = null,
                        modifier = Modifier.size(logoIconSize),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

                        Spacer(modifier = Modifier.height(48.dp))

                        // Google Sign In Button
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                                                    mapOf(AnalyticsConstants.Param.METHOD to "google")
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
                                            loginError = context.getString(R.string.login_error_no_accounts)
                                        } catch (e: GetCredentialCancellationException) {
                                            Log.d("AuthScreen", "Login cancelled by user")
                                            loginError = context.getString(R.string.login_error_cancelled)
                                        } catch (e: GetCredentialException) {
                                            Log.e("AuthScreen", "Login failed: ${e.message}")
                                            loginError = context.getString(R.string.login_error_failed)
                                        } catch (e: Exception) {
                                            Log.e("AuthScreen", "Error: ${e.message}")
                                            loginError = context.getString(R.string.login_error_generic)
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
                                        mapOf(AnalyticsConstants.Param.METHOD to "guest")
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
                            Spacer(modifier = Modifier.height(12.dp))
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

                        Spacer(modifier = Modifier.height(24.dp))

                        // Privacy Policy Text
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
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                }
            }
        }
    }
}
