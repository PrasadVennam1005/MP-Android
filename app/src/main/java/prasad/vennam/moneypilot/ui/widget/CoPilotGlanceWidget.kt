package prasad.vennam.moneypilot.ui.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.appwidget.AppWidgetManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import prasad.vennam.moneypilot.R

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
    val bgColor = ColorProvider(Color(0xFF0F101A))
    val primaryBlue = ColorProvider(Color(0xFF2563EB))
    val secondaryBtnColor = ColorProvider(Color(0xFF1E293B))
    val white = ColorProvider(Color(0xFFFFFFFF))
    val grey = ColorProvider(Color(0x88FFFFFF))
    val lightGrey = ColorProvider(Color(0xFFE2E8F0))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "MoneyPilot CoPilot",
                style = TextStyle(
                    color = white,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = "Refresh",
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
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ColorProvider(Color(0x1AFFFFFF)))
        ) {}
        Spacer(modifier = GlanceModifier.height(8.dp))

        // Monthly Spend Status
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "SPENT THIS MONTH",
                style = TextStyle(
                    color = grey,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = spentText,
                style = TextStyle(
                    color = white,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.padding(top = 2.dp)
            )

            // Recent Transaction
            Text(
                text = "RECENT TRANSACTION",
                style = TextStyle(
                    color = grey,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = GlanceModifier.padding(top = 8.dp)
            )
            Text(
                text = recentText,
                style = TextStyle(
                    color = lightGrey,
                    fontSize = 12.sp
                ),
                modifier = GlanceModifier.padding(top = 2.dp),
                maxLines = 1
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ColorProvider(Color(0x1AFFFFFF)))
        ) {}
        Spacer(modifier = GlanceModifier.height(8.dp))

        // Action Buttons Row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Add Transaction Button
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(32.dp)
                    .background(primaryBlue)
                    .clickable(
                        actionStartActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://moneypilot.app/add-transaction")).apply {
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
                    contentDescription = "Add Tx",
                    modifier = GlanceModifier.size(14.dp)
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
                    .height(32.dp)
                    .background(secondaryBtnColor)
                    .clickable(
                        actionStartActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://moneypilot.app/cosplit")).apply {
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
                    modifier = GlanceModifier.size(14.dp)
                )
                Text(
                    text = "CoSplit",
                    style = TextStyle(
                        color = white,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.padding(start = 4.dp)
                )
            }
        }
    }
}
