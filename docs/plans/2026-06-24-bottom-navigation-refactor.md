# Bottom Navigation Visibility Refactor Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Hide the bottom navigation bar on all detail/sub-screens (e.g. Settings, Subscriptions, Savings Goals, and forms) to maximize screen real estate and prevent navigation confusion, while adding a consistent back button to the settings screen.

**Architecture:** Transition the main bottom navigation visibility check from a manual deny-list to a clean allow-list containing only top-level (root) destinations. Expose a back callback in the Settings screen and add a back button to its top app bar for screen-stack navigation.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Navigation3

---

### Task 1: Add Back Navigation to Settings Screen

**Files:**
- Modify: `app/src/main/java/prasad/vennam/moneypilot/ui/settings/SettingsScreen.kt` (Add onBackClick parameter and back navigation icon to top bar)
- Modify: `app/src/main/java/prasad/vennam/moneypilot/ui/navigation/NavGraph.kt` (Pass onBackClick = onBack in settings screen mapping)

**Step 1: Write SettingsScreen.kt change**
Modify `SettingsScreen` signature to add `onBackClick: () -> Unit` parameter, import `Icons.AutoMirrored.Rounded.ArrowBack`, and add the `navigationIcon` parameter to `CenterAlignedTopAppBar`.

**Step 2: Write NavGraph.kt change**
Modify the `Destination.Settings` destination mapping block in `NavGraph.kt` to pass `onBackClick = onBack`.

**Step 3: Verify Compilation**
Run: `./gradlew :app:compileDevDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: Transition Bottom Navigation to Allow-list Check

**Files:**
- Modify: `app/src/main/java/prasad/vennam/moneypilot/MainActivity.kt` (Refactor showNavigation boolean expression)

**Step 1: Write MainActivity.kt change**
Change the `showNavigation` variable to check if the current destination is one of the 5 top-level destinations using an allow-list check (`Destination.Dashboard`, `Destination.History`, `Destination.Loans`, `Destination.Investments`, `Destination.Reports`).

**Step 2: Run compile check**
Run: `./gradlew :app:compileDevDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: Expand UI Test & Final Verification

**Files:**
- Modify: `app/src/androidTest/java/prasad/vennam/moneypilot/SmokeUiTest.kt` (Assert back navigation from Settings to Dashboard works)

**Step 1: Update SmokeUiTest.kt**
Add an assertion at the end of `SmokeUiTest.kt` that clicks the back button on the Settings screen (the button with content description `"Back"`) and verifies that the Dashboard has reloaded (checking that `"Quick Actions"` is visible on screen).

**Step 2: Run instrumented tests**
Run: `./gradlew connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=prasad.vennam.moneypilot.SmokeUiTest`
Expected: BUILD SUCCESSFUL (1 test passed, 0 failures)
