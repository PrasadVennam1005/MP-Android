package prasad.vennam.moneypilot.ui.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.appwidget.AppWidgetManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.compose.ui.graphics.Color
import prasad.vennam.moneypilot.R
import androidx.core.net.toUri

class CoPilotGlanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val KEY_SPENT = stringPreferencesKey("spent_text")
        val KEY_RECENT = stringPreferencesKey("recent_tx_text")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val spentText = prefs[KEY_SPENT] ?: "$0.00 / Limit: $0.00"
            val recentText = prefs[KEY_RECENT] ?: "No recent transactions"

            WidgetContent(context, spentText, recentText)
        }
    }
}

@Composable
private fun WidgetContent(context: Context, spentText: String, recentText: String) {
    // Premium theme color scheme matching the main app light/dark colors
    val bgColor = ColorProvider(
        day = Color(0xFFF8FAFC),       // BackgroundLight
        night = Color(0xFF0F172A)      // BackgroundDark
    )
    val cardBgColor = ColorProvider(
        day = Color(0xFFFFFFFF),       // SurfaceLight
        night = Color(0xFF1E293B)      // SurfaceDark
    )
    val borderCol = ColorProvider(
        day = Color(0xFFE2E8F0),       // Slate 200
        night = Color(0xFF334155)      // Slate 700
    )
    val textColor = ColorProvider(
        day = Color(0xFF0F172A),       // OnBackgroundLight
        night = Color(0xFFF8FAFC)      // OnBackgroundDark
    )
    val textSecondaryColor = ColorProvider(
        day = Color(0xFF64748B),       // Slate 500
        night = Color(0xFF94A3B8)      // Slate 400
    )
    val primaryBlue = ColorProvider(
        day = Color(0xFF2563EB),       // PrimaryLight
        night = Color(0xFF3B82F6)      // PrimaryDark / High contrast blue for dark mode
    )
    val white = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFFFFFFFF))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .padding(12.dp)
            .cornerRadius(20.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "CoPilot Stats",
                style = TextStyle(
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Refresh",
                colorFilter = ColorFilter.tint(textSecondaryColor),
                modifier = GlanceModifier
                    .size(20.dp)
                    .clickable {
                        val refreshIntent = Intent(context, CoPilotGlanceWidgetReceiver::class.java).apply {
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        }
                        context.sendBroadcast(refreshIntent)
                    }
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Card Container for Stats and transactions
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(cardBgColor)
                .padding(10.dp)
                .cornerRadius(12.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Spent progress
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = "SPENT THIS MONTH",
                    style = TextStyle(
                        color = textSecondaryColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = spentText,
                    style = TextStyle(
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.padding(top = 1.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(borderCol)
            ) {}
            Spacer(modifier = GlanceModifier.height(6.dp))

            // Recent activity
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = "RECENT TRANSACTION",
                    style = TextStyle(
                        color = textSecondaryColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = recentText,
                    style = TextStyle(
                        color = textColor,
                        fontSize = 12.sp
                    ),
                    modifier = GlanceModifier.padding(top = 1.dp),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        // Action Buttons Row matching app premium shape (12.dp radius)
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Add Transaction Button
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(34.dp)
                    .background(primaryBlue)
                    .cornerRadius(12.dp)
                    .clickable(
                        actionStartActivity(
                            Intent(Intent.ACTION_VIEW, "https://moneypilot.app/add-transaction".toUri()).apply {
                                `package` = context.packageName
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        )
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_add),
                    contentDescription = "Add",
                    colorFilter = ColorFilter.tint(white),
                    modifier = GlanceModifier.size(13.dp)
                )
                Text(
                    text = "Add Tx",
                    style = TextStyle(
                        color = white,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.padding(start = 4.dp)
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // CoSplit Button
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(34.dp)
                    .background(cardBgColor)
                    .cornerRadius(12.dp)
                    .clickable(
                        actionStartActivity(
                            Intent(Intent.ACTION_VIEW, "https://moneypilot.app/cosplit".toUri()).apply {
                                `package` = context.packageName
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        )
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_people),
                    contentDescription = "CoSplit",
                    colorFilter = ColorFilter.tint(textColor),
                    modifier = GlanceModifier.size(14.dp)
                )
                Text(
                    text = "CoSplit",
                    style = TextStyle(
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.padding(start = 4.dp)
                )
            }
        }
    }
}
