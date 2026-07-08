package prasad.vennam.moneypilot.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import prasad.vennam.moneypilot.data.dao.BudgetDao
import prasad.vennam.moneypilot.data.dao.CategoryDao
import prasad.vennam.moneypilot.data.dao.TransactionDao
import prasad.vennam.moneypilot.data.entity.TransactionType
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class CoPilotGlanceWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = CoPilotGlanceWidget()

    @Inject
    lateinit var transactionDao: TransactionDao

    @Inject
    lateinit var budgetDao: BudgetDao

    @Inject
    lateinit var categoryDao: CategoryDao

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgetData(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE || intent.action == Intent.ACTION_BOOT_COMPLETED) {
            updateWidgetData(context)
        }
    }

    private fun updateWidgetData(context: Context) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            try {
                // Securely query encrypted Room database via injected DAOs
                val transactions = transactionDao.getAllTransactionsSync()
                val budgets = budgetDao.getAllBudgetsSync()
                val categories = categoryDao.getAllCategoriesSync().associateBy { it.id }

                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonth = calendar.get(Calendar.MONTH)

                // Sum this month's expenses
                val monthlySpentMinor = transactions
                    .filter { tx ->
                        val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                        tx.type == TransactionType.EXPENSE &&
                                txCal.get(Calendar.YEAR) == currentYear &&
                                txCal.get(Calendar.MONTH) == currentMonth
                    }
                    .sumOf { it.amount }

                val monthlySpent = monthlySpentMinor / 100.0

                // Sum all Monthly budget caps
                val budgetLimitMinor = budgets
                    .filter { it.period == "Monthly" }
                    .sumOf { it.amount }
                val budgetLimit = budgetLimitMinor / 100.0

                val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }

                val spentText = "${currencyFormat.format(monthlySpent)} / Limit: ${currencyFormat.format(budgetLimit)}"

                val recentTx = transactions.firstOrNull()
                val recentTxText = if (recentTx != null) {
                    val catName = categories[recentTx.categoryId]?.name ?: "Expense"
                    val amountFormatted = currencyFormat.format(recentTx.amount / 100.0)
                    val noteText = if (recentTx.note.isNotBlank()) recentTx.note else catName
                    "$noteText - $amountFormatted"
                } else {
                    "No recent transactions"
                }

                // Update Glance state of active widgets
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(CoPilotGlanceWidget::class.java)

                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[CoPilotGlanceWidget.KEY_SPENT] = spentText
                        prefs[CoPilotGlanceWidget.KEY_RECENT] = recentTxText
                    }
                    glanceAppWidget.update(context, glanceId)
                }
            } catch (e: Exception) {
                android.util.Log.e("WidgetReceiver", "Failed to update widget data via Hilt context", e)
            }
        }
    }
}
