package prasad.vennam.moneypilot.ui.dashboard.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R
import java.util.*

data class PaymentApp(
    val name: String,
    val packageName: String,
    val iconRes: Int? = null // Using a placeholder for now
)

@Composable
fun PaymentAppsSection() {
    val context = LocalContext.current
    val country = Locale.getDefault().country
    
    val paymentApps = getPaymentAppsForCountry(country)
    
    if (paymentApps.isEmpty()) return

    Column {
        SectionHeader(title = "Payment Apps")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(paymentApps) { app ->
                PaymentAppItem(app) {
                    openApp(context, app.packageName)
                }
            }
        }
    }
}

@Composable
fun PaymentAppItem(app: PaymentApp, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(70.dp)
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                // In a real app, you'd use the actual app icon or a high-quality drawable
                Text(
                    text = app.name.take(1),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

fun getPaymentAppsForCountry(country: String): List<PaymentApp> {
    return when (country) {
        "IN" -> listOf(
            PaymentApp("GPay", "com.google.android.apps.nbu.paisa.user"),
            PaymentApp("PhonePe", "com.phonepe.app"),
            PaymentApp("Paytm", "net.one97.paytm"),
            PaymentApp("Amazon Pay", "in.amazon.mShop.android.shopping")
        )
        "US" -> listOf(
            PaymentApp("PayPal", "com.paypal.android.p2pmobile"),
            PaymentApp("Venmo", "com.venmo"),
            PaymentApp("Cash App", "com.squareup.cash")
        )
        "GB" -> listOf(
            PaymentApp("Revolut", "com.revolut.revolut"),
            PaymentApp("Monzo", "co.uk.getmondo"),
            PaymentApp("PayPal", "com.paypal.android.p2pmobile")
        )
        else -> listOf(
            PaymentApp("PayPal", "com.paypal.android.p2pmobile"),
            PaymentApp("GPay", "com.google.android.apps.walletnfcrel")
        )
    }
}

fun openApp(context: Context, packageName: String) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        context.startActivity(launchIntent)
    } else {
        // Fallback: Open in Play Store
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }
}
