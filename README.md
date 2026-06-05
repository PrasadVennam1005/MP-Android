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
