# Task List - MoneyPilot Compliance & Restructuring

This task list tracks the status of the current publishing compliance tasks and upcoming architectural improvements.

## Active Tasks (Store Publishing & Compliance)
- [x] Host public terms and conditions and privacy policy in `/docs`
- [x] Integrate official Google multi-colored G vector logo in drawables
- [x] Redesign Google Sign-In button container, layout, and colors in `AuthScreen.kt`
- [x] Update onboarding layout policy link targets to point to GitHub Pages
- [x] Verify project builds and compile checks pass
- [x] Perform batch commits and resolve git rebase conflicts

## Next Phase Tasks (Hilt & Code Architecture Refactoring)
- [ ] Add `androidx.hilt:hilt-work` and KSP processor extension dependencies
- [ ] Implement `Configuration.Provider` in `MoneyPilotApplication` and disable default WorkManager initialization
- [ ] Refactor `DailyNewsWorker` and `GoogleSheetsSyncWorker` to use `@HiltWorker` and constructor injection
- [ ] Verify background scheduling operations and Hilt factories compile correctly
