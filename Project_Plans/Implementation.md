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
- [ ] **Verify Room migrations**
  - Review `DatabaseMigrations.kt` for each schema version.
  - Write unit tests that simulate upgrading from older versions.

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

### 📚 Documentation
- [ ] **Update README**
  - Build/run instructions, architecture diagram, contribution guide.
- [ ] **Add KDoc** to public classes/functions.
- [ ] **Create a simple CONTRIBUTING.md** with code‑style guidelines.

---
*Work through each numbered section sequentially, ticking off tasks as they are completed.*
