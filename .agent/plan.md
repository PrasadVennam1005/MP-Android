# MoneyPilot - Core Development Plan

This document outlines the iteration plans and task tracking records for the MoneyPilot personal finance project.

## Current Iteration: Compliance & Publishing Readiness
*Focus: Google Play Console compliance, public document hosting, and Google authentication interface standardization.*

### Active Goals
- **Host Policy Pages**: Create and host a public Privacy Policy and Terms of Service for store reviews using **GitHub Pages** (free hosting via `/docs` directory).
- **Google Sign-In Compliance**: Revamp the Google login button to follow Google Identity Brand guidelines (using official vector icons, white container background, standard dimensions, and text).
- **Link Redirections**: Point the interactive policy text links in the app's onboarding screen directly to the live GitHub Pages URLs.

---

## Future Roadmap: Restructuring & Modularization

### Phase 1: Native Hilt WorkManager Integration
- Migrate `DailyNewsWorker` and `GoogleSheetsSyncWorker` to use `@HiltWorker` with Assisted Inject.
- Implement custom `Configuration.Provider` in `MoneyPilotApplication` to initialize Hilt worker factory.

### Phase 2: Domain Layer (Use Cases)
- Extract logic from ViewModels into single-responsibility Use Cases:
  - `SyncSheetsUseCase` (Handles backing up transactions to Google Sheets)
  - `CalculateBudgetsUseCase` (Evaluates categories limits)
  - `ParseReceiptUseCase` (OCR parsing from receipts)

### Phase 3: Project Modularization
- Split the monolithic `:app` module into clean Gradle sub-projects:
  - `:core:database` (DB, Room entities, migrations)
  - `:core:network` (Retrofit, feed fetching API, RSS parsers)
  - `:core:ui` (Theme styling, custom charts, profile buttons)
  - `:features:dashboard`, `:features:budget`, `:features:settings` (Self-contained feature scopes)

### Phase 4: Model-View-Intent (MVI) State Flow
- Convert complex screen architectures to a single-stream immutable `UiState`, `UiIntent`, and `UiEffect` flows to prevent UI state inconsistencies.

### Phase 5: Compose Component Decomposition
- Decouple Stateful Screen orchestrators from Stateless layout hosts and stateless sub-components to support full Preview support and isolated unit UI testing.
