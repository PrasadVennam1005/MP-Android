package prasad.vennam.moneypilot.ui.login.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ui.theme.MoneyPilotTheme

@Composable
fun AuthActionCard(
    isLoading: Boolean,
    loginError: String?,
    onGoogleSignInClick: () -> Unit,
    onGuestSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                    onClick = onGoogleSignInClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F1F1F),
                            ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onGuestSignInClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.primary,
                            ),
                        ),
                    ),
                ) {
                    Text(
                        stringResource(R.string.continue_as_guest),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }

            loginError?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun AuthTermsFooter(
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val annotatedText = buildAnnotatedString {
        append(stringResource(R.string.agree_to_policy_prefix))
        withLink(
            LinkAnnotation.Clickable(
                tag = "terms",
                styles = TextLinkStyles(
                    style = SpanStyle(
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
                styles = TextLinkStyles(
                    style = SpanStyle(
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
        style = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        ),
        modifier = modifier.padding(horizontal = 24.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun AuthActionCardPreview() {
    MoneyPilotTheme {
        AuthActionCard(
            isLoading = false,
            loginError = null,
            onGoogleSignInClick = {},
            onGuestSignInClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthActionCardLoadingPreview() {
    MoneyPilotTheme {
        AuthActionCard(
            isLoading = true,
            loginError = null,
            onGoogleSignInClick = {},
            onGuestSignInClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthTermsFooterPreview() {
    MoneyPilotTheme {
        AuthTermsFooter(
            onNavigateToTerms = {},
            onNavigateToPrivacy = {}
        )
    }
}
