package prasad.vennam.moneypilot.ui.premium

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import androidx.compose.ui.tooling.preview.Preview
import prasad.vennam.moneypilot.ui.theme.MoneyPilotTheme
import prasad.vennam.moneypilot.R
import prasad.vennam.moneypilot.billing.BillingManager
import prasad.vennam.moneypilot.util.AnalyticsConstants
import prasad.vennam.moneypilot.util.AnalyticsHelper
import prasad.vennam.moneypilot.util.TrackScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBackClick: () -> Unit,
    analyticsHelper: AnalyticsHelper,
    viewModel: PremiumViewModel = hiltViewModel(),
) {
    TrackScreen(analyticsHelper, AnalyticsConstants.Screen.PREMIUM)
    val products by viewModel.products.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val context = LocalContext.current

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.premium_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Section with Gold/Amber Star
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFF59E0B), // Beautiful gold star
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.upgrade_to_premium),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.premium_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Features List Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val features = listOf(
                        "Ad-Free Experience",
                        "AI Co-Pilot Assistant",
                        "Unlimited Sync & Backups",
                        "Custom Tags & Labels",
                        "Advanced Analytics",
                        "Premium Widgets"
                    )

                    features.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp),
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = feature, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isPremium) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.you_are_premium),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(id = R.string.premium_thank_you),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                if (products.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                } else {
                    if (isExpanded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            products.forEach { product ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ProductCard(
                                        product = product,
                                        onPurchaseClick = {
                                            analyticsHelper.logEvent(
                                                AnalyticsConstants.Event.PURCHASE_ATTEMPTED,
                                                mapOf(AnalyticsConstants.Param.PRODUCT_ID to product.productId),
                                            )
                                            viewModel.launchBillingFlow(context as Activity, product)
                                        },
                                    )
                                }
                            }
                        }
                    } else {
                        products.forEach { product ->
                            ProductCard(
                                product = product,
                                onPurchaseClick = {
                                    analyticsHelper.logEvent(
                                        AnalyticsConstants.Event.PURCHASE_ATTEMPTED,
                                        mapOf(AnalyticsConstants.Param.PRODUCT_ID to product.productId),
                                    )
                                    viewModel.launchBillingFlow(context as Activity, product)
                                },
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ProductDetails,
    onPurchaseClick: () -> Unit,
) {
    val isLifetime = product.productId == BillingManager.PRODUCT_LIFETIME
    
    // Clean up fallback titles dynamically
    val displayName = when (product.productId) {
        BillingManager.PRODUCT_LIFETIME -> "MoneyPilot Premium (Lifetime)"
        BillingManager.PRODUCT_SUBSCRIPTION -> "MoneyPilot Premium (Monthly)"
        "premium_subscription_monthly_49" -> "MoneyPilot Premium (Basic Monthly)"
        else -> product.name.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    val displayDesc = when (product.productId) {
        BillingManager.PRODUCT_LIFETIME -> "Pay once, enjoy forever. Access all features and future updates."
        BillingManager.PRODUCT_SUBSCRIPTION -> "Unlock full access with a standard monthly subscription."
        "premium_subscription_monthly_49" -> "Unlock standard features with our basic monthly plan."
        else -> product.description
    }

    // Card Colors & Text Colors
    val cardBgColor = if (isLifetime) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val onCardTextColor = if (isLifetime) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val onCardDescColor = if (isLifetime) {
        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val cardBorder = if (isLifetime) {
        BorderStroke(
            width = 2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary
                )
            )
        )
    } else {
        BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(cardBorder, shape = RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLifetime) 0.dp else 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            // Placement of badge at the top
            if (isLifetime) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "BEST VALUE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = onCardTextColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = displayDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = onCardDescColor,
            )

            Spacer(modifier = Modifier.height(20.dp))

            val price = if (product.productType == BillingClient.ProductType.INAPP) {
                product.oneTimePurchaseOfferDetails?.formattedPrice ?: "N/A"
            } else {
                product.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.firstOrNull()
                    ?.formattedPrice ?: "N/A"
            }

            // Button label logic
            val buttonLabel = if (price.contains("Free", ignoreCase = true) || price == "₹0.00") {
                "Start Free Trial"
            } else {
                stringResource(id = R.string.buy_for_price, price)
            }

            // Button colors matching card context
            val buttonColors = if (isLifetime) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }

            Button(
                onClick = onPurchaseClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = buttonColors
            ) {
                Text(
                    text = buttonLabel,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PremiumActiveCardPreview() {
    MoneyPilotTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.you_are_premium),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(id = R.string.premium_thank_you),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
