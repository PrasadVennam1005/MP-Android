package prasad.vennam.moneypilot.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Smoke tests for [AnalyticsConstants].
 *
 * Validates:
 * - All constant values are non-blank
 * - No duplicate values within each category
 * - Snake_case convention for events and params
 * - PascalCase convention for screen names
 * - Key screen/event/param constants resolve to expected values
 */
class AnalyticsConstantsSmokeTest {

    // ── Screen constants ────────────────────────────────────────────────

    private val allScreens = listOf(
        AnalyticsConstants.Screen.DASHBOARD,
        AnalyticsConstants.Screen.HISTORY,
        AnalyticsConstants.Screen.LOANS,
        AnalyticsConstants.Screen.INVESTMENTS,
        AnalyticsConstants.Screen.REPORTS,
        AnalyticsConstants.Screen.AUTH,
        AnalyticsConstants.Screen.SETTINGS,
        AnalyticsConstants.Screen.AI_CHAT,
        AnalyticsConstants.Screen.SUBSCRIPTIONS,
        AnalyticsConstants.Screen.SAVING_GOALS,
        AnalyticsConstants.Screen.NOTIFICATIONS,
        AnalyticsConstants.Screen.ANALYTICS_TAB,
        AnalyticsConstants.Screen.CURRENCY_CONVERTER,
        AnalyticsConstants.Screen.RECEIPT_SCANNER,
        AnalyticsConstants.Screen.PREMIUM,
        AnalyticsConstants.Screen.EMERGENCY_FUND,
        AnalyticsConstants.Screen.MANAGE_CATEGORIES,
        AnalyticsConstants.Screen.ADD_TRANSACTION,
        AnalyticsConstants.Screen.EDIT_TRANSACTION,
        AnalyticsConstants.Screen.FINANCIAL_SANDBOX,
        AnalyticsConstants.Screen.FAQ,
        AnalyticsConstants.Screen.FINANCIAL_NEWS,
        AnalyticsConstants.Screen.INSIGHTS,
        AnalyticsConstants.Screen.EMI_CALCULATOR,
        AnalyticsConstants.Screen.LEARN_FINANCE,
        AnalyticsConstants.Screen.ARTICLE_DETAIL,
    )

    @Test
    fun `screen constants are not blank`() {
        allScreens.forEach { screen ->
            assertTrue("Screen constant must not be blank", screen.isNotBlank())
        }
    }

    @Test
    fun `screen constants have no duplicates`() {
        val duplicates = allScreens.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue("Duplicate screen constants found: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `screen constants do not contain underscores or spaces`() {
        allScreens.forEach { screen ->
            assertFalse("Screen '$screen' should use PascalCase (no underscores)", screen.contains("_"))
            assertFalse("Screen '$screen' should not contain spaces", screen.contains(" "))
        }
    }

    @Test
    fun `key screen constants resolve to expected values`() {
        assertEquals("Dashboard", AnalyticsConstants.Screen.DASHBOARD)
        assertEquals("Settings", AnalyticsConstants.Screen.SETTINGS)
        assertEquals("AiChat", AnalyticsConstants.Screen.AI_CHAT)
        assertEquals("Auth", AnalyticsConstants.Screen.AUTH)
        assertEquals("ArticleDetail", AnalyticsConstants.Screen.ARTICLE_DETAIL)
    }

    // ── Event constants ─────────────────────────────────────────────────

    private val allEvents = listOf(
        AnalyticsConstants.Event.SCREEN_ENGAGEMENT_TIME,
        AnalyticsConstants.Event.INVESTMENTS_REFRESH_CLICKED,
        AnalyticsConstants.Event.INVESTMENTS_TAB_SWITCHED,
        AnalyticsConstants.Event.LOGIN,
        AnalyticsConstants.Event.CURRENCY_REFRESH_CLICKED,
        AnalyticsConstants.Event.CURRENCY_MODE_SWITCHED,
        AnalyticsConstants.Event.CURRENCY_SWAPPED,
        AnalyticsConstants.Event.AI_CHAT_SUGGESTIONS_ICON_CLICKED,
        AnalyticsConstants.Event.AI_CHAT_ACTION_CONFIRMED,
        AnalyticsConstants.Event.AI_CHAT_ACTION_DISMISSED,
        AnalyticsConstants.Event.AI_CHAT_MODEL_DOWNLOAD_CLICKED,
        AnalyticsConstants.Event.AI_CHAT_WELCOME_SUGGESTION_CLICKED,
        AnalyticsConstants.Event.AI_CHAT_BOTTOMSHEET_SUGGESTION_CLICKED,
        AnalyticsConstants.Event.REPORTS_TAB_SWITCHED,
        AnalyticsConstants.Event.INSIGHTS_AI_RECOMMENDATION_CLICKED,
        AnalyticsConstants.Event.NEWS_TAB_SWITCHED,
        AnalyticsConstants.Event.NEWS_CATEGORY_CLICKED,
        AnalyticsConstants.Event.NEWS_BOOKMARK_REMOVED,
        AnalyticsConstants.Event.NEWS_BOOKMARK_ADDED,
        AnalyticsConstants.Event.NEWS_SHARED,
        AnalyticsConstants.Event.TRANSACTION_ADDED,
        AnalyticsConstants.Event.TRANSACTION_SAVED,
        AnalyticsConstants.Event.HISTORY_TAB_SWITCHED,
        AnalyticsConstants.Event.HISTORY_FILTERS_RESET,
        AnalyticsConstants.Event.HISTORY_FILTERS_APPLIED,
        AnalyticsConstants.Event.PROFILE_CLICKED,
        AnalyticsConstants.Event.NOTIFICATION_ICON_CLICKED,
        AnalyticsConstants.Event.AI_CHAT_TOPBAR_CLICKED,
        AnalyticsConstants.Event.QUICK_ACTION_CLICKED,
        AnalyticsConstants.Event.INSIGHTS_CARD_CLICKED,
        AnalyticsConstants.Event.LEARN_FINANCE_PROMO_CLICKED,
        AnalyticsConstants.Event.EMERGENCY_FUND_CARD_CLICKED,
        AnalyticsConstants.Event.SAVING_GOALS_CARD_CLICKED,
        AnalyticsConstants.Event.BUDGET_SEE_ALL_CLICKED,
        AnalyticsConstants.Event.HISTORY_SEE_ALL_CLICKED,
        AnalyticsConstants.Event.FLOATING_AI_BOT_CLICKED,
        AnalyticsConstants.Event.LOAN_EMI_CALCULATOR_CLICKED,
        AnalyticsConstants.Event.SETTINGS_BIOMETRIC_TOGGLED,
        AnalyticsConstants.Event.SETTINGS_NOTIFICATION_TRACKING_TOGGLED,
        AnalyticsConstants.Event.SETTINGS_SMS_TRACKING_TOGGLED,
        AnalyticsConstants.Event.FAQ_ASK_QUESTION_CLICKED,
        AnalyticsConstants.Event.FAQ_ITEM_EXPANDED,
        AnalyticsConstants.Event.SAVING_GOAL_DELETED,
        AnalyticsConstants.Event.SAVING_GOAL_DEPOSIT,
        AnalyticsConstants.Event.SAVING_GOAL_COMPLETED,
        AnalyticsConstants.Event.SAVING_GOAL_WITHDRAWAL,
        AnalyticsConstants.Event.SUBSCRIPTION_DELETED,
        AnalyticsConstants.Event.EMERGENCY_FUND_DEPOSIT,
        AnalyticsConstants.Event.EMERGENCY_FUND_WITHDRAW,
        AnalyticsConstants.Event.SCANNER_FLASH_TOGGLED,
        AnalyticsConstants.Event.SCANNER_GALLERY_OPENED,
        AnalyticsConstants.Event.SCANNER_SHUTTER_CLICKED,
        AnalyticsConstants.Event.SCANNER_EXPENSE_SAVED,
        AnalyticsConstants.Event.CATEGORY_SAVED,
        AnalyticsConstants.Event.CATEGORY_DELETED,
        AnalyticsConstants.Event.PURCHASE_ATTEMPTED,
        AnalyticsConstants.Event.NOTIFICATIONS_FILTER_CLICKED,
        AnalyticsConstants.Event.NOTIFICATIONS_CLEARED_ALL,
        AnalyticsConstants.Event.CURRENCY_CHANGED,
        AnalyticsConstants.Event.SUBSCRIPTION_ADDED,
        AnalyticsConstants.Event.SUBSCRIPTION_UPDATED,
        AnalyticsConstants.Event.SAVING_GOAL_ADDED,
        AnalyticsConstants.Event.SAVING_GOAL_UPDATED,
        AnalyticsConstants.Event.SCANNER_GALLERY_UPLOAD,
        AnalyticsConstants.Event.SCANNER_PICTURE_CAPTURED,
        AnalyticsConstants.Event.BUDGET_WARNING_VIEWED,
    )

    @Test
    fun `event constants are not blank`() {
        allEvents.forEach { event ->
            assertTrue("Event constant must not be blank", event.isNotBlank())
        }
    }

    @Test
    fun `event constants have no duplicates`() {
        val duplicates = allEvents.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue("Duplicate event constants found: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `event constants use snake_case`() {
        allEvents.forEach { event ->
            assertTrue(
                "Event '$event' should be snake_case (lowercase + underscores)",
                event.matches(Regex("^[a-z][a-z0-9_]*$"))
            )
        }
    }

    @Test
    fun `key event constants resolve to expected values`() {
        assertEquals("login", AnalyticsConstants.Event.LOGIN)
        assertEquals("screen_engagement_time", AnalyticsConstants.Event.SCREEN_ENGAGEMENT_TIME)
        assertEquals("quick_action_clicked", AnalyticsConstants.Event.QUICK_ACTION_CLICKED)
        assertEquals("budget_warning_viewed", AnalyticsConstants.Event.BUDGET_WARNING_VIEWED)
        assertEquals("settings_biometric_toggled", AnalyticsConstants.Event.SETTINGS_BIOMETRIC_TOGGLED)
    }

    // ── Param constants ─────────────────────────────────────────────────

    private val allParams = listOf(
        AnalyticsConstants.Param.SCREEN_NAME,
        AnalyticsConstants.Param.ENGAGEMENT_TIME_SEC,
        AnalyticsConstants.Param.TAB,
        AnalyticsConstants.Param.METHOD,
        AnalyticsConstants.Param.MODE,
        AnalyticsConstants.Param.TYPE,
        AnalyticsConstants.Param.TEXT,
        AnalyticsConstants.Param.CATEGORY,
        AnalyticsConstants.Param.URL,
        AnalyticsConstants.Param.ACTION,
        AnalyticsConstants.Param.FROM,
        AnalyticsConstants.Param.TO,
        AnalyticsConstants.Param.GOAL_NAME,
        AnalyticsConstants.Param.GOAL_TARGET,
        AnalyticsConstants.Param.GOAL_SAVED,
        AnalyticsConstants.Param.GOAL_DEADLINE,
        AnalyticsConstants.Param.SUBSCRIPTION_NAME,
        AnalyticsConstants.Param.SUBSCRIPTION_AMOUNT,
        AnalyticsConstants.Param.SUBSCRIPTION_CYCLE,
        AnalyticsConstants.Param.SUBSCRIPTION_MODE,
        AnalyticsConstants.Param.SUBSCRIPTION_NOTIFY,
        AnalyticsConstants.Param.PERCENT,
        AnalyticsConstants.Param.PAYMENT_MODE,
        AnalyticsConstants.Param.IS_EDIT,
        AnalyticsConstants.Param.AMOUNT,
        AnalyticsConstants.Param.SUCCESS,
        AnalyticsConstants.Param.MERCHANT_FOUND,
        AnalyticsConstants.Param.PARSED_BY_AI,
        AnalyticsConstants.Param.IS_ON,
        AnalyticsConstants.Param.NAME,
        AnalyticsConstants.Param.COLOR,
        AnalyticsConstants.Param.ICON,
        AnalyticsConstants.Param.IS_EXPENSE,
        AnalyticsConstants.Param.PRODUCT_ID,
        AnalyticsConstants.Param.ENABLED,
        AnalyticsConstants.Param.QUESTION,
        AnalyticsConstants.Param.DEPOSIT_AMOUNT,
        AnalyticsConstants.Param.WITHDRAWAL_AMOUNT,
        AnalyticsConstants.Param.CATEGORY_FILTERED,
        AnalyticsConstants.Param.PAYMENT_MODE_FILTERED,
    )

    @Test
    fun `param constants are not blank`() {
        allParams.forEach { param ->
            assertTrue("Param constant must not be blank", param.isNotBlank())
        }
    }

    @Test
    fun `param constants have no duplicates`() {
        val duplicates = allParams.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue("Duplicate param constants found: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `param constants use snake_case`() {
        allParams.forEach { param ->
            assertTrue(
                "Param '$param' should be snake_case (lowercase + underscores)",
                param.matches(Regex("^[a-z][a-z0-9_]*$"))
            )
        }
    }

    @Test
    fun `key param constants resolve to expected values`() {
        assertEquals("screen_name", AnalyticsConstants.Param.SCREEN_NAME)
        assertEquals("method", AnalyticsConstants.Param.METHOD)
        assertEquals("action", AnalyticsConstants.Param.ACTION)
        assertEquals("enabled", AnalyticsConstants.Param.ENABLED)
        assertEquals("category", AnalyticsConstants.Param.CATEGORY)
    }

    // ── Cross-category uniqueness ───────────────────────────────────────

    @Test
    fun `event and param values do not collide`() {
        val overlap = allEvents.toSet().intersect(allParams.toSet())
        assertTrue(
            "Event and Param values should not collide, but found: $overlap",
            overlap.isEmpty()
        )
    }

    // ── Constants count guard ───────────────────────────────────────────

    @Test
    fun `screen constants count is 26`() {
        assertEquals("Expected 26 screen constants", 26, allScreens.size)
    }

    @Test
    fun `event constants count is 66`() {
        assertEquals("Expected 66 event constants", 66, allEvents.size)
    }

    @Test
    fun `param constants count is 40`() {
        assertEquals("Expected 40 param constants", 40, allParams.size)
    }
}
