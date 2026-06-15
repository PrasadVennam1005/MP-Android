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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

data class PaymentApp(
    val name: String,
    val packageName: String,
)

@Composable
fun PaymentAppsSection(currencyCode: String) {
    val context = LocalContext.current

    val paymentApps =
        remember(currencyCode) {
            getPaymentAppsForCurrency(currencyCode).filter { app ->
                isAppInstalled(context, app.packageName)
            }
        }

    if (paymentApps.isEmpty()) return

    Column {
        SectionHeader(title = "Launch Payment Apps")
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
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
fun PaymentAppItem(
    app: PaymentApp,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val appIcon =
        remember(app.packageName) {
            try {
                context.packageManager.getApplicationIcon(app.packageName)
            } catch (e: Exception) {
                null
            }
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .width(72.dp)
                .clickable { onClick() },
    ) {
        Surface(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(CircleShape),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = CircleShape,
            tonalElevation = 1.dp,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (appIcon != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = appIcon),
                        contentDescription = "${app.name} icon",
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                    )
                } else {
                    Text(
                        text = app.name.take(1),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun getPaymentAppsForCurrency(currency: String): List<PaymentApp> =
    when (currency.uppercase().trim()) {
        "INR" ->
            listOf(
                PaymentApp("GPay", "com.google.android.apps.nbu.paisa.user"),
                PaymentApp("PhonePe", "com.phonepe.app"),
                PaymentApp("Paytm", "net.one97.paytm"),
                PaymentApp("BHIM", "in.org.npci.upiapp"),
                PaymentApp("Amazon Pay", "in.amazon.mShop.android.shopping"),
                PaymentApp("MobiKwik", "com.mobikwik_new"),
            )
        "USD" ->
            listOf(
                PaymentApp("PayPal", "com.paypal.android.p2pmobile"),
                PaymentApp("Venmo", "com.venmo"),
                PaymentApp("Cash App", "com.squareup.cash"),
                PaymentApp("Zelle", "com.zellepay.zelle"),
                PaymentApp("Google Wallet", "com.google.android.apps.walletnfcrel"),
            )
        "GBP" ->
            listOf(
                PaymentApp("Revolut", "com.revolut.revolut"),
                PaymentApp("Monzo", "co.uk.getmondo"),
                PaymentApp("PayPal", "com.paypal.android.p2pmobile"),
                PaymentApp("Barclays", "mobi.barclays.android.barclaysmobilebanking"),
                PaymentApp("Lloyds Bank", "com.lloydsbank.mobilebanking"),
            )
        "EUR" ->
            listOf(
                PaymentApp("Revolut", "com.revolut.revolut"),
                PaymentApp("PayPal", "com.paypal.android.p2pmobile"),
                PaymentApp("Google Wallet", "com.google.android.apps.walletnfcrel"),
                PaymentApp("N26", "de.number26.android"),
                PaymentApp("Lydia", "com.lydia"),
            )
        "CAD" ->
            listOf(
                PaymentApp("RBC Mobile", "com.rbc.mobile.android"),
                PaymentApp("TD Canada", "com.td"),
                PaymentApp("CIBC Mobile", "com.cibc.mobile.banking"),
                PaymentApp("Scotiabank", "com.scotiabank.mobile"),
                PaymentApp("BMO Mobile", "com.bmo.mobile"),
                PaymentApp("Tangerine", "com.tangerine.android"),
                PaymentApp("Wealthsimple", "com.wealthsimple.trade"),
                PaymentApp("PayPal", "com.paypal.android.p2pmobile"),
            )
        "AUD" ->
            listOf(
                PaymentApp("CommBank", "au.com.commbank.netbank"),
                PaymentApp("NAB Mobile", "au.com.nab.mobile"),
                PaymentApp("Westpac", "com.westpac.bank"),
                PaymentApp("ANZ Plus", "au.com.anz.plus"),
                PaymentApp("Beem", "au.com.beemit"),
                PaymentApp("PayPal", "com.paypal.android.p2pmobile"),
            )
        "SGD" ->
            listOf(
                PaymentApp("DBS PayLah!", "com.dbs.dbspaylah"),
                PaymentApp("Grab", "com.grabtaxi.passenger"),
                PaymentApp("OCBC Digital", "com.ocbc.mobile"),
                PaymentApp("UOB TMRW", "com.uob.mobile"),
                PaymentApp("NETSPay", "sg.nets.netspay"),
                PaymentApp("Singtel Dash", "com.SingTel.mWallet"),
            )
        "JPY" ->
            listOf(
                PaymentApp("PayPay", "jp.ne.paypay.android"),
                PaymentApp("Rakuten Pay", "jp.co.rakuten.pay"),
                PaymentApp("LINE Pay", "com.linecorp.linepay"),
            )
        "BRL" ->
            listOf(
                PaymentApp("Nubank", "com.nu"),
                PaymentApp("Banco do Brasil", "com.bb.android"),
                PaymentApp("Itaú", "com.itau"),
                PaymentApp("Bradesco", "com.bradesco"),
                PaymentApp("PicPay", "com.picpay"),
                PaymentApp("Mercado Pago", "com.mercadopago.merchant"),
            )
        "NZD" ->
            listOf(
                PaymentApp("ANZ goMoney", "nz.co.anz.gomoney"),
                PaymentApp("ASB Bank", "nz.co.asb.mobile"),
                PaymentApp("BNZ Bank", "nz.co.bnz.droidbanking"),
                PaymentApp("Westpac NZ", "nz.co.westpac"),
            )
        "MXN" ->
            listOf(
                PaymentApp("BBVA México", "com.bancomer.mbanking"),
                PaymentApp("Mercado Pago", "com.mercadopago.merchant"),
                PaymentApp("PayPal", "com.paypal.android.p2pmobile"),
            )
        "AED" ->
            listOf(
                PaymentApp("e& money", "com.etisalat.ewallet"),
                PaymentApp("Payit", "com.Isys.Payitv2"),
                PaymentApp("ENBD X", "com.emiratesnbd.EmiratesNBD"),
                PaymentApp("ADCB", "com.adcb.mobilebanking"),
                PaymentApp("Mashreq UAE", "com.mashreq.mobile"),
                PaymentApp("PayPal", "com.paypal.android.p2pmobile"),
                PaymentApp("Google Wallet", "com.google.android.apps.walletnfcrel"),
            )
        "SAR" ->
            listOf(
                PaymentApp("stc pay", "sa.com.stcpay"),
                PaymentApp("urpay", "com.neoleap.urpay"),
                PaymentApp("Al Rajhi Bank", "com.alrajhibank.alrajhimobile"),
                PaymentApp("SNB Mobile", "com.alahli.mobile"),
                PaymentApp("PayPal", "com.paypal.android.p2pmobile"),
            )
        else ->
            listOf(
                PaymentApp("PayPal", "com.paypal.android.p2pmobile"),
                PaymentApp("Google Wallet", "com.google.android.apps.walletnfcrel"),
            )
    }

fun isAppInstalled(
    context: Context,
    packageName: String,
): Boolean =
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                packageName,
                android.content.pm.PackageManager.PackageInfoFlags
                    .of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
        true
    } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
        false
    } catch (e: Exception) {
        false
    }

fun openApp(
    context: Context,
    packageName: String,
) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        context.startActivity(launchIntent)
    } else {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: Exception) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }
}
