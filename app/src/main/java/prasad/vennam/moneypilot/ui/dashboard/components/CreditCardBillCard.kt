package prasad.vennam.moneypilot.ui.dashboard.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.ui.viewmodel.CreditCardBillState
import java.util.Locale

@Composable
fun CreditCardBillCard(
    billState: CreditCardBillState,
    onPayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!billState.hasBill || billState.isPaid) return

    val infiniteTransition = rememberInfiniteTransition(label = "cc_promo_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cc_glow_alpha"
    )

    val formattedAmount = String.format(Locale.getDefault(), "%,.2f", billState.billAmount)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E272C),
                        Color(0xFF2D383F),
                        Color(0xFF38464E)
                    )
                )
            )
            .clickable { onPayClick() },
        contentAlignment = Alignment.Center
    ) {
        // Metallic reflection glow
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF90A4AE).copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(70.dp)
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Credit card icon bag
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CreditCard,
                    contentDescription = null,
                    tint = Color(0xFFFFB300), // Premium gold icon
                    modifier = Modifier.size(28.dp)
                )
            }

            // Description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Credit Card Bill Due",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Pending ₹$formattedAmount for ${billState.billMonthName}. Tap to clear statement.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.75f)
                    ),
                    maxLines = 2
                )
            }

            // Pay action button
            Button(
                onClick = onPayClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB300),
                    contentColor = Color(0xFF1E272C)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Pay Bill",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
