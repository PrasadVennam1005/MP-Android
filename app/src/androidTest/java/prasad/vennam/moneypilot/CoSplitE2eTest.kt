package prasad.vennam.moneypilot

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import prasad.vennam.moneypilot.data.UserPreferences
import java.io.File

import org.junit.Before
import org.junit.After
import prasad.vennam.moneypilot.feature.cosplit.data.repository.CoSplitRepositoryImpl

@RunWith(AndroidJUnit4::class)
class CoSplitE2eTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setUp() {
        CoSplitRepositoryImpl.delegate = FakeCoSplitRepository()
    }

    @After
    fun tearDown() {
        CoSplitRepositoryImpl.delegate = null
    }

    @Test
    fun testCoSplit_endToEndFlow() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Clear all persistent app states to start the test from a clean state
        // 1. Clear Datastore preferences
        val datastoreDir = targetContext.filesDir.parentFile?.let { parent ->
            File(parent, "datastore")
        }
        datastoreDir?.deleteRecursively()
        
        // 2. Clear Room Database files
        targetContext.deleteDatabase("money_pilot_database")
        
        // 3. Pre-populate Datastore to simulate a logged-in user (guests cannot use CoSplit)
        val userPrefs = UserPreferences(targetContext)
        runBlocking {
            userPrefs.saveUserData(
                UserPreferences.UserData(
                    name = "Test Pilot",
                    email = "testpilot@gmail.com",
                    upiId = "testpilot@upi"
                )
            )
            userPrefs.saveSpreadsheetId("dummy-spreadsheet-id")
            userPrefs.setSynced(true)
        }

        // Retrieve local string resources to be robust against changes
        val editMembersText = targetContext.getString(R.string.edit_members)
        val deleteGroupText = targetContext.getString(R.string.delete_group)
        val deleteText = targetContext.getString(R.string.delete)
        val cancelText = targetContext.getString(R.string.cancel)
        val emailLabel = targetContext.getString(R.string.email_address)
        val removeMemberDesc = targetContext.getString(R.string.remove_member)
        val addMemberDesc = targetContext.getString(R.string.add_member)
        val addText = targetContext.getString(R.string.add)

        // Launch MainActivity manually using ActivityScenario
        ActivityScenario.launch(MainActivity::class.java).use {
            // 1. Verify dashboard loads with logged-in user "Test Pilot"
            composeTestRule.waitUntil(timeoutMillis = 30000) {
                composeTestRule.onAllNodes(hasText("Test Pilot")).fetchSemanticsNodes().isNotEmpty()
            }

            // Wait for the scrollable container to be composed and visible
            composeTestRule.waitUntil(timeoutMillis = 30000) {
                composeTestRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
            }

            // Scroll the dashboard down to bring quick actions into view
            composeTestRule.onNode(hasScrollAction()).performTouchInput {
                swipeUp(durationMillis = 500)
            }

            // Wait for CoSplit quick action to load and become visible
            composeTestRule.waitUntil(timeoutMillis = 30000) {
                composeTestRule.onAllNodes(hasText("CoSplit")).fetchSemanticsNodes().isNotEmpty()
            }

            // 2. Navigate to CoSplit by clicking on the CoSplit quick action
            composeTestRule.onNode(hasText("CoSplit")).performClick()

            // 3. Verify CoSplit Groups screen is loaded
            composeTestRule.waitUntil(timeoutMillis = 30000) {
                composeTestRule.onAllNodes(hasText("CoSplit Groups")).fetchSemanticsNodes().isNotEmpty()
            }

            // 4. Click the "+" (Add Group) button
            composeTestRule.onNode(hasContentDescription("Add Group")).performClick()

            // 5. Fill out the "Create Group" dialog
            composeTestRule.onNode(hasText("Group Name")).performTextInput("Trip 2026")
            
            // Add member 1: friend@gmail.com
            composeTestRule.onNode(hasText(emailLabel)).performTextInput("friend@gmail.com")
            composeTestRule.onNode(hasContentDescription(addMemberDesc)).performClick()
            
            // Handle name prompt for friend@gmail.com
            composeTestRule.waitUntil(timeoutMillis = 30000) {
                composeTestRule.onAllNodes(hasText("Enter Name")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNode(hasText("Name")).performTextInput("Friend One")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            composeTestRule.onNode(hasText(addText)).performClick()

            // Click "Create" to save the group
            composeTestRule.onNode(hasText("Create")).performClick()

            // 6. Wait for the new group "Trip 2026" to appear on the list, and open it
            composeTestRule.waitUntil(timeoutMillis = 30000) {
                composeTestRule.onAllNodes(hasText("Trip 2026")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNode(hasText("Trip 2026") and !isEditable()).performClick()

            // 7. Verify Group Detail screen loads
            composeTestRule.waitUntil(timeoutMillis = 30000) {
                composeTestRule.onAllNodes(hasContentDescription("Add Expense")).fetchSemanticsNodes().isNotEmpty()
            }

            // 8. Click "Add Expense" to add a new expense
            composeTestRule.onNode(hasContentDescription("Add Expense")).performClick()

            // 9. Input details for "Dinner" costing 100
            composeTestRule.onNode(hasText("Description")).performTextInput("Dinner")
            composeTestRule.onNode(hasText("Total Amount (₹)")).performTextInput("100")
            
            // Close the keyboard to ensure the viewport has full height
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            
            // Scroll down to make sure Save Expense button is composed and visible
            composeTestRule.onNode(hasScrollAction()).performTouchInput {
                swipeUp(durationMillis = 500)
            }
            
            // Click "Save Expense"
            composeTestRule.onNode(hasText("Save Expense")).performClick()

            // 10. Wait for the group detail screen to update and verify the expense list contains "₹100.00" and "Dinner"
            composeTestRule.waitUntil(timeoutMillis = 30000) {
                composeTestRule.onAllNodes(hasText("₹100.00")).fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNode(hasText("Dinner")).assertExists()

            // 11. Switch to the "Balances" tab to verify balance calculation
            composeTestRule.onNode(hasText("Balances")).performClick()

            // Wait for the settlements route to calculate and display
            // Since Test Pilot paid 100 equally split with Friend One:
            // Test Pilot balance: +50.00, Friend One balance: -50.00.
            // Simplified settlement route: "Friend One pays Test Pilot ₹50.00".
            composeTestRule.waitUntil(timeoutMillis = 30000) {
                composeTestRule.onAllNodes(hasText("Friend One pays Test Pilot ₹50.00")).fetchSemanticsNodes().isNotEmpty()
            }

            // 12. Record a Settle Up Payment
            composeTestRule.onNode(hasText("Settle Up Payments")).performClick()
            
            // Wait for Settle Up Dialog to be visible
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                composeTestRule.onAllNodes(hasText("Record Settlement")).fetchSemanticsNodes().isNotEmpty()
            }
            
            // Input settlement amount - target the editable text field inside the dialog
            composeTestRule.onNode(hasSetTextAction() and hasAnyAncestor(hasText("Record Settlement"))).performTextInput("50")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            
            // Wait for Confirm button to be enabled (amount must be parseable as double)
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule.onAllNodes(hasText("Confirm") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            
            // Click Confirm
            composeTestRule.onNode(hasText("Confirm") and isEnabled()).performClick()

            // Wait for the dialog to dismiss
            composeTestRule.waitUntil(timeoutMillis = 10000) {
                composeTestRule.onAllNodes(hasText("Record Settlement")).fetchSemanticsNodes().isEmpty()
            }

            // Verify that balances have updated (should display all settled up)
            composeTestRule.waitUntil(timeoutMillis = 30000) {
                composeTestRule.onAllNodes(hasText("All settled up! No payment routes needed.")).fetchSemanticsNodes().isNotEmpty()
            }

            // 13. Clean up by deleting the group
            composeTestRule.onNode(hasContentDescription("More Options")).performClick()
            composeTestRule.onNode(hasText(deleteGroupText)).performClick()
            
            // Confirm deletion
            composeTestRule.onNode(hasText(deleteText)).performClick()

            // 14. Verify we are navigated back to the CoSplit Groups screen
            composeTestRule.waitUntil(timeoutMillis = 30000) {
                composeTestRule.onAllNodes(hasText("CoSplit Groups")).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }
}
