# Implementation.md

## 📋 Implementation To‑Do List for MoneyTracker (Kotlin Android)

### 1️⃣ Architecture & Dependency Injection
- [ ] **Add Hilt (Dagger‑Hilt)**
  - Add `hilt-android` and `hilt-compiler` dependencies.
  - Annotate `MoneyTrackerApp` with `@HiltAndroidApp`.
  - Create `@Module` classes for Room DB, Repository, and any external services.
- [ ] **Introduce Use‑Case / Interactor layer**
  - Define a `usecase` package (e.g., `AddTransactionUseCase`, `GetStatsUseCase`).
  - Wire use‑cases into ViewModels via constructor injection.
- [ ] **Refactor existing ViewModels** to depend on the new use‑cases only.

### 2️⃣ UI / UX Polish
- [ ] **Migrate to Material 3 (Dynamic Color)**
  - Update `build.gradle.kts` with `material3` dependency.
  - Switch theme to `MaterialTheme` with `colorScheme = dynamicLightColorScheme()` / `dynamicDarkColorScheme()`.
- [ ] **Add Dark‑Mode support**
  - Provide `Theme.kt` adjustments to respect `isSystemInDarkTheme()`.
- [ ] **Implement animated navigation**
  - Use `AnimatedNavHost` (Compose‑Navigation‑Animation) for screen transitions.
- [ ] **Design empty‑state screens**
  - Show friendly messages & illustrations when transaction list or categories are empty.
- [ ] **Add placeholder illustrations**
  - Generate simple SVG/PNG assets (e.g., via `generate_image`) and reference them in empty states.

### 3️⃣ Data Handling & Security
- [ ] **Encrypt sensitive local data**
  - Add `androidx.security:security-crypto` dependency.
  - Store any backup passwords or API keys in `EncryptedSharedPreferences`.
- [x] **Verify Room migrations**
  - Implemented `MIGRATION_10_11` for profiles and data scoping.
  - Maintained schema backward compatibility.

### 4️⃣ Performance Optimizations
- [ ] **Integrate Paging 3**
  - Wrap DAO queries with `PagingSource`.
  - Replace `LazyColumn` data source with `collectAsLazyPagingItems()`.
- [ ] **Improve recomposition**
  - Add stable `key`s to `LazyColumn` items (e.g., `key = transaction.id`).
  - Use `itemContentType` where applicable.
- [ ] **Off‑load heavy calculations**
  - Move stats aggregation to `Dispatchers.Default` coroutines.
  - Cache results in a `StateFlow` refreshed only when data changes.

### 5️⃣ Testing Suite
- [ ] **Write unit tests**
  - Test ViewModel logic with `runBlockingTest`.
  - Mock Repository using **MockK**.
- [ ] **Add UI tests**
  - Use **Compose Test** for screen navigation, adding a transaction, editing, deleting.
  - Add a few **Espresso** tests for platform‑specific interactions if needed.
- [ ] **Configure test coverage reporting** (e.g., Jacoco).

### 6️⃣ CI/CD Pipeline
- [ ] **Create GitHub Actions workflow**
  - Lint (`ktlint`), unit tests, UI tests, build AAB.
  - Publish a draft release artifact on successful builds.
- [ ] **Add secrets** (e.g., `PLAY_STORE_SERVICE_ACCOUNT`) for future Play Store deployment.

### 7️⃣ Backup & Sync Features
- [ ] **Implement CSV export/import**
  - Add a “Share” button that writes transactions to a CSV file.
  - Parse CSV for import with basic validation.
- [ ] **Add optional Google Drive backup**
  - Use Google Drive REST API to upload/download the encrypted backup file.
  - Store last‑backup timestamp in `SharedPreferences`.

### 8️⃣ Accessibility Enhancements
- [ ] **Add content descriptions** for all icons/buttons.
- [ ] **Check color contrast** with Android Studio’s Accessibility Scanner.
- [ ] **Test with TalkBack** to ensure logical navigation order.

### 9️⃣ Localization
- [ ] **Extract all UI strings** to `strings.xml`.
- [ ] **Create translations** (e.g., Spanish) using Android Studio’s translation editor.
- [ ] **Verify layout RTL support** (optional).

### 🔟 Analytics & Crash Reporting
- [ ] **Integrate Firebase Analytics**
  - Log key events: `add_transaction`, `delete_transaction`, `view_stats`.
- [ ] **Add Firebase Crashlytics**
  - Enable crash reporting and verify a test crash logs correctly.

---

### 11️⃣ Local Profiles, Data Scoping & Specialized Tabs System

#### Local Profiles System & Data Isolation
- [x] **Database Migration & Data Scoping (`MIGRATION_10_11`)**
  - Added `ProfileEntity` and `ProfileDao`.
  - Added `profileId` column to all data tables (`transactions`, `categories`, `sub_categories`, `details`, `grocery_items`, `taxi_fares`).
  - Scoped all repository and DAO queries to the active `profileId`.
- [x] **Character Validation Rules (No Spaces)**
  - **Username**: 4 to 20 characters (`a-z`, `A-Z`, `0-9`, `#`, `@`, `_`). No spaces allowed.
  - **Password**: 4 to 10 characters (`a-z`, `A-Z`, `0-9`, `#`, `@`, `_`) when password protection is enabled. No spaces allowed.
- [x] **Pre-Divided Seed Asset (`Seed_Standard_Guest.json`)**
  - Generated asset `Seed_Standard_Guest.json` from `Import CalcTape` (Columns 1, 2, 3, 4, 5, 7) with all amount values pre-divided by 4.
  - JSON schema matches app Kotlin data structures (`categoryName`, `transactionType`, `subCategory`, `detail`, `amount`, `note`, `isRecurring`, `recurrenceFrequency`, `recurCount`).
- [x] **Initial Launch Profile Creation & Seeding (20 July 2026)**
  - Automatically creates profile **"Ryu"** (`isRyuHidden = true`) and seeds it once-off for **20 July 2026** directly from `Seed_Standard_Guest.json` (from `Import CalcTape`) if not already seeded.
  - Automatically creates profile **"Guest"** (`isGuest = true`) and seeds it for **20 July 2026** with dynamic amount variance (random R100 to R500 adjustment preserving positive/negative nature).
- [x] **Testing Control ("Hide Ryu Profile" Toggle)**
  - Added **"Hide Ryu Profile (Testing Mode)"** switch toggle in Settings (default `true`).
  - When enabled, profile "Ryu" is hidden from startup checks and profile selection, allowing testing of both local profile existence scenarios.
- [x] **Profile Selection & Guest Conversion**
  - Profile selection screen presents available profiles, password entry dialog, and 1-tap conversion of Guest profile to Permanent profile (preserving all guest transactions).

#### Specialized Feature Tabs
- [x] **Groceries Tab (`GroceriesScreen.kt`)**
  - Reads master Groceries Budget target from Main Table without modifying main transactions.
  - Displays Over Budget / Under Budget status cards and itemized shopping checklist.
- [x] **Month Compare Tab (`MonthComparisonScreen.kt`)**
  - Compares 3 PayMonth periods: Prev Month, Current Month, Next Month.
  - Drilldown level buttons: Summary Level, Category Level, SubCategory / Detail Level.
- [x] **Taxi Fare Tab (`TaxiFareScreen.kt`)**
  - Reads master Taxi commute budget from Main Table and calculates total monthly commute fare progress.

---
*Work through each numbered section sequentially, ticking off tasks as they are completed.*
