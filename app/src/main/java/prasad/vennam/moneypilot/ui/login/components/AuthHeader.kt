package prasad.vennam.moneypilot.ui.login.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.ui.theme.MoneyPilotTheme
import prasad.vennam.moneypilot.util.QuotesManager

@Composable
fun SplashBrandSection(
    alpha: Float,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.moneypilot),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.alpha(alpha),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.track_save_grow),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 4.sp,
                fontSize = 18.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.alpha(alpha),
        )
    }
}

@Composable
fun WelcomeHeaderSection(
    quote: QuotesManager.Quote?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.welcome_to_moneypilot),
            style = MaterialTheme.typography.headlineMedium.copy(
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

        quote?.let { q ->
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "\"${q.quote}\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
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
}

@Preview(showBackground = true)
@Composable
private fun WelcomeHeaderSectionPreview() {
    MoneyPilotTheme {
        WelcomeHeaderSection(
            quote = QuotesManager.Quote("Do not save what is left after spending, but spend what is left after saving.", "Warren Buffett")
        )
    }
}
