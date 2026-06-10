package prasad.vennam.moneypilot.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import prasad.vennam.moneypilot.R

@Composable
fun QuickActionSection(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddInvestment: () -> Unit,
    onAddLoan: () -> Unit,
    onScanReceipt: () -> Unit,
    isGuest: Boolean,
) {
    Column {
        SectionHeader(stringResource(R.string.quick_actions))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickActionButton(
                stringResource(R.string.expense),
                Icons.Rounded.RemoveCircleOutline,
                MaterialTheme.colorScheme.error,
                onAddExpense,
                Modifier.weight(1f),
            )
            QuickActionButton(
                stringResource(R.string.income),
                Icons.Rounded.AddCircleOutline,
                MaterialTheme.colorScheme.secondary,
                onAddIncome,
                Modifier.weight(1f),
            )
            QuickActionButton(
                stringResource(R.string.investment),
                Icons.Rounded.AccountBalanceWallet,
                MaterialTheme.colorScheme.primary,
                onAddInvestment,
                Modifier.weight(1f),
            )
            QuickActionButton(
                stringResource(R.string.loans),
                Icons.Rounded.AccountBalanceWallet,
                MaterialTheme.colorScheme.tertiary,
                onAddLoan,
                Modifier.weight(1f),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickActionButton(
                stringResource(R.string.scan),
                Icons.Rounded.Camera,
                MaterialTheme.colorScheme.outline,
                onScanReceipt,
                Modifier.weight(1f),
                isGuest,
            )
            Spacer(modifier = Modifier.weight(3f))
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
) {
    OutlinedCard(
        onClick = if (disabled) ({}) else onClick,
        modifier =
            modifier
                .height(90.dp)
                .graphicsLayer { alpha = if (disabled) 0.5f else 1f },
        shape = MaterialTheme.shapes.large,
        border =
            CardDefaults
                .outlinedCardBorder()
                .copy(brush = Brush.linearGradient(listOf(color.copy(alpha = 0.5f), color))),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    onActionClick: (() -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            if (onInfoClick != null) {
                IconButton(onClick = onInfoClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Info,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(stringResource(R.string.see_all), style = MaterialTheme.typography.labelLarge)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
