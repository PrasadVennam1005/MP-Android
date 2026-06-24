package prasad.vennam.moneypilot

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SmokeUiTest {

    init {
        // Clear all persistent app states to start the smoke test from a clean login/splash screen
        val targetContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        
        // 1. Clear Datastore preferences
        val datastoreDir = targetContext.filesDir.parentFile?.let { parent ->
            File(parent, "datastore")
        }
        datastoreDir?.deleteRecursively()
        
        // 2. Clear Room Database files
        targetContext.deleteDatabase("money_pilot_database")
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appSmokeTest_runGoldenPath() {
        // Retrieve localized resource strings to ensure the test is robust
        val continueAsGuestText = composeTestRule.activity.getString(R.string.continue_as_guest)
        val quickActionsText = composeTestRule.activity.getString(R.string.quick_actions)
        val backDescText = composeTestRule.activity.getString(R.string.back)
        val profileDescText = composeTestRule.activity.getString(R.string.profile)

        // 1. Wait for splash screen to finish and guest button to appear (timeout up to 10000ms)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasText(continueAsGuestText)).fetchSemanticsNodes().isNotEmpty()
        }

        // Click "Continue as Guest"
        composeTestRule.onNode(hasText(continueAsGuestText)).performClick()

        // 2. Wait for the Dashboard screen to finish loading its content.
        // We wait for "Quick Actions" because it is inside the LazyColumn content list, 
        // meaning the shimmer has finished and the scrollable list is composed.
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasText(quickActionsText)).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Assert guest name is displayed on dashboard greeting
        composeTestRule.onNode(hasText("Guest Pilot")).assertExists()

        // 3. Scroll the dashboard down to bring the savings goals card into view.
        // LazyColumn elements off-screen are not composed, so we swipe up on the scrollable container first.
        composeTestRule.onNode(hasScrollAction()).performTouchInput {
            swipeUp(durationMillis = 500)
        }

        // Click on the "Create Savings Goals" card
        composeTestRule.onNode(hasText("Create Savings Goals")).performClick()

        // 4. Verify "Savings Goals" screen is loaded
        composeTestRule.onNode(hasText("Savings Goals")).assertExists()

        // Click back button to return to Dashboard
        composeTestRule.onNode(hasContentDescription(backDescText)).performClick()

        // 5. Back on Dashboard, navigate to settings by clicking the Profile icon
        composeTestRule.onNode(hasContentDescription(profileDescText)).performClick()

        // 6. Verify Settings screen is loaded by checking for the "Recurring Subscriptions" option
        composeTestRule.onNode(hasText("Recurring Subscriptions")).assertExists()

        // Click "Recurring Subscriptions"
        composeTestRule.onNode(hasText("Recurring Subscriptions")).performClick()

        // 7. Verify "Subscriptions" screen is loaded
        composeTestRule.onNode(hasText("Subscriptions")).assertExists()

        // Click back button to return to Settings
        composeTestRule.onNode(hasContentDescription(backDescText)).performClick()

        // Assert Settings is displayed again
        composeTestRule.onNode(hasText("Recurring Subscriptions")).assertExists()

        // 8. Click back button on Settings to return to Dashboard
        composeTestRule.onNode(hasContentDescription(backDescText)).performClick()

        // Verify that the Dashboard is displayed again by checking for "Quick Actions"
        composeTestRule.onNode(hasText(quickActionsText)).assertExists()
    }
}
