# Money Tracker

A simple offline Android app to track income and expenses.

## Features

- **Dashboard** — monthly balance, income, expenses, and recent transactions
- **Transactions** — full list with income/expense filters, tap to edit, swipe to delete
- **Add / Edit** — amount, type, category, date, and optional note
- **Stats** — monthly breakdown by category with progress bars
- **Offline-first** — all data stored locally with Room (SQLite)

## Tech stack

- Kotlin
- Jetpack Compose + Material 3
- Room + Flow
- MVVM + Repository pattern

## Requirements

- Android Studio Ladybug (2024.2+) or newer
- JDK 17
- Android SDK 35

## Getting started

1. Open Android Studio
2. **File → Open** and select this folder (`MoneyTracker`)
3. Wait for Gradle sync to finish
4. Run on an emulator or device (API 26+)

Or from the command line (after generating the Gradle wrapper):

```bash
./gradlew assembleDebug
```

## Project structure

```
app/src/main/java/com/moneytracker/
├── data/local/          # Room entities, DAOs, database
├── data/repository/     # TransactionRepository
├── ui/screens/          # Dashboard, Transactions, AddEdit, Stats
├── ui/viewmodel/        # ViewModels
├── ui/navigation/       # NavHost + bottom navigation
└── util/                # Date and currency formatting
```

## Default categories

**Income:** Salary, Freelance, Investment, Gift, Other Income

**Expenses:** Food, Transport, Rent, Utilities, Shopping, Entertainment, Healthcare, Other Expense

## License

MIT
