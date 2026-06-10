package prasad.vennam.moneypilot.ui.ai

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import prasad.vennam.moneypilot.ui.dashboard.SyncState
import prasad.vennam.moneypilot.ui.dashboard.components.DashboardTopBar
import prasad.vennam.moneypilot.data.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    userData: UserPreferences.UserData?,
    syncState: SyncState?,
    onProfileClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val insights = remember { getMockInsights() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Smart Insights", 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AiHeaderSection()
            }
            
            items(insights) { insight ->
                InsightCard(insight)
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun AiHeaderSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        border = AssistChipDefaults.assistChipBorder(enabled = true)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    "AI Analysis Ready",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Personalized financial advice based on your recent activity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InsightCard(insight: SmartInsight) {
    val statusColor = when (insight.status) {
        InsightStatus.POSITIVE -> Color(0xFF4CAF50)
        InsightStatus.NEGATIVE -> MaterialTheme.colorScheme.error
        InsightStatus.WARNING -> Color(0xFFFF9800)
        InsightStatus.NEUTRAL -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = insight.icon ?: Icons.Rounded.Info,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (insight.percentageChange != null) {
                    TrendIndicator(insight.percentageChange, insight.status)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = insight.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = statusColor
            )
            
            if (insight.recommendation != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(alpha = 0.1f)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = insight.recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun TrendIndicator(percentage: Float, status: InsightStatus) {
    val isPositive = percentage > 0
    val color = when (status) {
        InsightStatus.POSITIVE -> Color(0xFF4CAF50)
        InsightStatus.NEGATIVE -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPositive) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${if (isPositive) "+" else ""}${percentage.toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

fun getMockInsights(): List<SmartInsight> {
    return listOf(
        SmartInsight(
            id = "1",
            title = "Food Spending",
            description = "Your spending on groceries and dining out has increased compared to last month.",
            type = InsightType.SPENDING,
            status = InsightStatus.NEGATIVE,
            value = "+₹4,200",
            percentageChange = 18f,
            recommendation = "Consider cooking at home more often this week.",
            icon = Icons.Rounded.Restaurant
        ),
        SmartInsight(
            id = "2",
            title = "Fuel Budget",
            description = "You've exceeded your monthly fuel budget early this month.",
            type = InsightType.BUDGET,
            status = InsightStatus.WARNING,
            value = "₹1,200 Over",
            percentageChange = 12f,
            recommendation = "Try carpooling or using public transport for short trips.",
            icon = Icons.Rounded.LocalGasStation
        ),
        SmartInsight(
            id = "3",
            title = "Savings Growth",
            description = "Excellent work! Your savings rate has improved significantly.",
            type = InsightType.SAVINGS,
            status = InsightStatus.POSITIVE,
            value = "+₹15,000",
            percentageChange = 12f,
            recommendation = "You might want to move some of this to a high-yield investment.",
            icon = Icons.Rounded.Savings
        ),
        SmartInsight(
            id = "4",
            title = "Subscription Alert",
            description = "We found 2 recurring subscriptions you haven't used in 30 days.",
            type = InsightType.SPENDING,
            status = InsightStatus.NEUTRAL,
            value = "Save ₹899/mo",
            recommendation = "Review and cancel unused services to optimize your budget.",
            icon = Icons.Rounded.Subscriptions
        )
    )
}
