# MoneyPilot - Project Status & Specifications

MoneyPilot is a premium personal finance application designed for the diverse Indian demographic (including salaried professionals, business owners, and agriculture specialists). It leverages a clean card-based design aesthetic inspired by leading financial applications like CRED and INDmoney.

---

## Technical Specifications

| Component | Technology | Detail |
| :--- | :--- | :--- |
| **Language** | Kotlin | Standard JVM Android development |
| **UI Framework** | Jetpack Compose | Declarative layouts (Material Design 3) |
| **Theme** | Custom Premium Theme | HSL color palettes, 20dp card corners, dynamic Dark Mode |
| **Local Storage** | Room DB & DataStore | Relational data persistence & key-value settings store |
| **DI Engine** | Hilt | Standard dependency injection scope management |
| **Background Work** | WorkManager | Timezone-aware news fetching & sync tasks |
| **Navigation** | Navigation 3 | Type-safe, state-driven 5-tab navigation system |
| **Cloud Integration**| Google Sheets API | Optional background backup and sheets synchronization |
| **Local OCR** | Google ML Kit | Optical character recognition parser for scanning receipts |

---

## Current Status & Completed Iterations

1. **Iteration 1: Database & Core Theme**
   - Created Room tables for Transactions, Categories, and Budgets.
   - Built dynamic light and dark theme configurations with Edge-to-Edge display support.
2. **Iteration 2: State-Driven Navigation**
   - Implemented type-safe state navigation using Jetpack Navigation 3.
   - Designed Add/Edit transaction screens and transactional history lists.
3. **Iteration 3: Dashboard & Budget Progress**
   - Built a high-fidelity visual dashboard containing a custom Canvas spending Donut Chart.
   - Added budget limit setting, progress metrics, and adaptive layouts for larger device profiles.
4. **Iteration 4: Premium Investments Monitor**
   - Implemented investment portfolio tracking and advanced analytics metrics.
   - Evolved dashboard layouts to a modern card-based system.
5. **Iteration 5: Timezone-Aware Daily News notifications**
   - Created background `DailyNewsWorker` to fetch CNBC/Economic Times RSS XML feeds.
   - Parsed local currency choices dynamically to choose target wealth feeds.
   - Managed local notification schedules and added Android 13+ runtime notification permissions.
6. **Iteration 6: Policies Hosting & Google Auth compliance**
   - Designed public HTML documentation templates (`privacy.html`, `terms.html`, and `style.css`) inside the `/docs` directory to be hosted for free via GitHub Pages.
   - Redesigned the "Continue with Google" button to match standard Google Identity guidelines.
