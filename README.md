# MoneyPilot

MoneyPilot is a premium, client-first personal finance tracking and investment management Android application designed with a card-based visual design language inspired by CRED and INDmoney.

---

## Technical Stack & Architecture

- **UI Layer**: Declarative UI built with **Jetpack Compose (Material Design 3)**. Handles dark/light theme shifts dynamically, with support for phone, tablet, and foldable form factors (`NavigationSuiteScaffold`).
- **Dependency Injection**: Orchestrated via **Hilt** for compile-time safe injection graphs.
- **Persistence**: Relational SQLite storage via **Room DB** (supporting schemas, auto-seeding default categories on first launch, and versioned database migrations). User preferences and configurations (theme, base currency) are saved using **Jetpack DataStore**.
- **Background Tasks**: Powered by **WorkManager** to handle timezone-aware daily notifications fetching from wealth feeds, as well as background Google Sheets updates.
- **Credential Auth**: Uses modern **Google Credential Manager API** for quick, one-tap logins.
- **OCR Scanner**: Integrates **CameraX** and **Google ML Kit Text Recognition** to scan physical transaction receipts and automatically extract details (amounts, vendors).

---

## Implemented Features

### 🏛️ Core Platform & Architecture
- **CRED-Inspired UI/UX**: Premium, card-based visual design using HSL color palettes, 20dp card corners, dynamic Dark/Light theme shifts, and smooth micro-animations.
- **Adaptive Layouts**: Material Design 3 structures with `NavigationSuiteScaffold` to auto-resize and optimize UI layout for phones, tablets, and foldables.
- **Relational Local Storage (Room DB & DataStore)**: Relational local storage with category auto-seeding, data schemas, and key-value user preferences via DataStore.
- **State-Driven Navigation**: Type-safe, state-controlled navigation flow using Jetpack Navigation 3.

### 🔒 Security & Privacy
- **One-Tap Google Authentication**: Google Credential Manager API integration adhering to official Google Identity guidelines.
- **Biometric App Lock (`BiometricLockScreen`)**: Auto-locking biometric check when the app returns from background focus.
- **Privacy-First Operations**: Keep sensitive transaction logs secure on-device or synced via user's private Google Drive storage.

### 📊 Dashboard & Smart Budgeting
- **Visual Budget Progress**: Set monthly limits and track remaining balance with custom progress bars and visual meters.
- **Interactive Canvas Chart**: Custom-drawn Canvas donut chart illustrating transaction category distribution.
- **Real-Time Sync Indicator**: Live sync indicator showing connection status (Syncing, Synced, Pending Connection, Failed).

### 💸 Transaction & Investment Portfolio Management
- **Transaction Logger**: Log, edit, and filter Income/Expense entries with categories, payment modes, custom notes, and dates.
- **Portfolio Tracking**: Track asset classes (Stocks, Mutual Funds, Cryptocurrencies, Fixed Deposits, Gold, Real Estate) and compare invested vs. current values.
- **Category Configurations**: Customize, add, or hide transaction categories locally.
- **Google Mobile Ads Integration**: Interstitial ads served to non-premium (free-tier) users during transaction entries.

### 🏛️ Borrowings & Loan Intelligence
- **Active Loan Monitor**: Maintain list of active loans, interest rates, principal, and tenures.
- **In-App EMI Calculator**: Calculate EMI, interest rates, and loan terms; prefill results directly into active loans.
- **Estimated Payoff Calculations**: Extra payments update the remaining payoff dates automatically.
- **Outstanding EMI Reminders**: Startup notification checks for upcoming loan installments.

### 🤖 MoneyPilot AI Copilot (Conversational Assistant)
- **Local On-Device LLM**: Interactive conversational AI running fully offline (1.5GB model downloaded on-demand) ensuring private conversation logs.
- **Conversational Action Parsing**: Natural language queries (e.g., *"Add food expense of ₹500 today"*) auto-generate structured confirmation cards to update local Room DB.
- **Financial habit insights**: Generates automated summaries of habits, budgets, and recommendations.

### 📸 Receipt OCR Scanner
- **Google ML Kit + CameraX**: Capture physical receipts to auto-extract transaction vendor names, dates, and total amounts.

### 📰 Wealth Feeds & Daily Notifications
- **WorkManager Scheduler**: Timezone-aware DailyNewsWorker fetches financial RSS XML news feeds (CNBC/Economic Times) in the background.
- **Currency-Filtered Feeds**: RSS feeds are dynamically filtered depending on the base currency preference.

### ☁️ Cloud Sync & Billing
- **Google Sheets Sync**: Background sync backup to user's custom private Google Sheet.
- **Currency Converter**: Instant currency rates conversion tool.
- **Premium Subscription**: Google Play Billing API integration (`BillingManager`) to remove ads and unlock advanced tools.

---

## Completed Iterations (Changelog)

- **Iteration 6 (Current)**: Privacy & Auth Compliance
  - Designed public legal documentation templates (`privacy.html`, `terms.html`, `style.css`) inside the `docs/` folder for GitHub Pages hosting.
  - Standardized the Google Login button design according to Google Branding Guidelines.
  - Configured in-app redirect links pointing to the hosted policy URLs.
- **Iteration 5**: Timezone-Aware Daily News Notifications
  - Created background `DailyNewsWorker` to fetch CNBC/Economic Times RSS XML feeds.
  - Dynamically parsed base currency selections for target wealth feeds.
  - Managed local notification schedules and added Android 13+ runtime notification permissions.
- **Iteration 4**: Premium Investments Monitor
  - Implemented investment portfolio tracking and advanced analytics metrics.
  - Evolved dashboard layouts to a modern card-based system.
- **Iteration 3**: Dashboard & Budget Progress
  - Built dashboard UI containing a custom Canvas spending Donut Chart.
  - Added budget limit configuration, progress metrics, and adaptive layouts for larger device profiles.
- **Iteration 2**: State-Driven Navigation
  - Implemented type-safe state navigation using Jetpack Navigation 3.
  - Designed Add/Edit transaction screens and transactional history lists.
- **Iteration 1**: Database & Core Theme
  - Created Room tables for Transactions, Categories, and Budgets.
  - Built dynamic light and dark theme configurations with Edge-to-Edge display support.

---

## Project Structure

```
├── app
│   ├── schemas/                 # Room database schema version control logs
│   └── src/main/java/prasad/vennam/moneypilot
│       ├── data/                # Data layer: Database, Converters, Entities, DAOs, Datastores
│       ├── di/                  # Hilt dependency injection configuration modules
│       ├── ui/                  # UI Layer: Composables, themes, navigation graphs, ViewModels
│       ├── util/                # Helper utilities: Currency formatters, OCR parser, sheets sync
│       └── worker/              # WorkManager background tasks and notifications managers
├── docs                         # Static website hosted publicly via GitHub Pages
│   ├── index.html               # Redirection portal
│   ├── privacy.html             # MoneyPilot Privacy Policy document
│   ├── terms.html               # MoneyPilot Terms of Service document
│   └── style.css                # Shared web styling stylesheet
├── plan.md                      # Core development plan and iteration updates
├── project.md                   # Complete app specification and iterations log
└── task.md                      # Active and upcoming task lists
```

---

## Build & Run Instructions

### Prerequisites
- Android Studio Ladybug or later
- JDK 17 configured in toolchain options
- Target SDK: 37 (Min SDK: 24)

### Building the Project
To compile code and perform standard lint and Kotlin checks, run:
```bash
./gradlew compileDebugKotlin
```

To build a debug APK bundle, execute:
```bash
./gradlew assembleDebug
```

---

## Core Documentation References
For detailed notes on current iterations, code checklists, and development targets:
- 🗺️ **[Development Plan](plan.md)**: Roadmap, goals, and multi-module migration strategy.
- 📋 **[Task List](task.md)**: Active compliance items and feature checklists.
- ⚙️ **[Project Specification](project.md)**: Tech specifications and historic iteration logs.

## How to use it:
- To increment the build number: Run ./incrementVersion.sh from your terminal.
- Example: 1.0.0 (1) becomes 1.0.0 (2).
- To change the version name: Simply edit the VERSION_NAME in version.properties.
- Example: Change it to 1.0.1. The next time you run the script, it will become 1.0.1 (3).
