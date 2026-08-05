package com.moneytracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                """
                UPDATE transactions
                SET sortOrder = (
                    SELECT COUNT(*)
                    FROM transactions AS newer
                    WHERE newer.date > transactions.date
                       OR (newer.date = transactions.date AND newer.id > transactions.id)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_sortOrder ON transactions(sortOrder)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN subType TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions RENAME COLUMN subType TO subCategory")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sub_categories` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `categoryId` INTEGER,
                    `iconName` TEXT NOT NULL DEFAULT 'default'
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE transactions ADD COLUMN recurrenceFrequency TEXT")
            db.execSQL("ALTER TABLE transactions ADD COLUMN recurTillDate INTEGER")
            db.execSQL("ALTER TABLE transactions ADD COLUMN recurCount INTEGER")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE sub_categories ADD COLUMN type TEXT")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `details` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `subCategoryId` INTEGER,
                    `categoryId` INTEGER,
                    `iconName` TEXT NOT NULL DEFAULT 'default',
                    `type` TEXT
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Foreign key cascade schema update
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `grocery_items` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `date` INTEGER NOT NULL,
                    `itemName` TEXT NOT NULL,
                    `size` REAL NOT NULL DEFAULT 1.0,
                    `sizeUnit` TEXT NOT NULL DEFAULT 'pack',
                    `category` TEXT NOT NULL DEFAULT 'Beverages',
                    `subCategory` TEXT NOT NULL DEFAULT 'Milk',
                    `unitPrice` REAL NOT NULL DEFAULT 0.0,
                    `quantity` INTEGER NOT NULL DEFAULT 1,
                    `totalPrice` REAL NOT NULL DEFAULT 0.0,
                    `isChecked` INTEGER NOT NULL DEFAULT 0,
                    `transactionId` INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `taxi_fares` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `routeName` TEXT NOT NULL,
                    `farePerTrip` REAL NOT NULL,
                    `tripsPerDay` INTEGER NOT NULL DEFAULT 2,
                    `workingDaysPerMonth` INTEGER NOT NULL DEFAULT 20,
                    `monthlyTotal` REAL NOT NULL,
                    `date` INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `profiles` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `username` TEXT NOT NULL,
                    `isGuest` INTEGER NOT NULL DEFAULT 0,
                    `isPasswordProtected` INTEGER NOT NULL DEFAULT 0,
                    `passwordHash` TEXT,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("ALTER TABLE transactions ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE categories ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE sub_categories ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE details ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE grocery_items ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE taxi_fares ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_profileId ON transactions(profileId)")
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN detail TEXT NOT NULL DEFAULT ''")
        }
    }

    // Migration 12 -> 13: Reorder columns so `date` is the first user column
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create new table with correct column order
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transactions_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `date` INTEGER NOT NULL,
                    `profileId` INTEGER NOT NULL,
                    `amount` REAL NOT NULL,
                    `type` TEXT NOT NULL,
                    `categoryId` INTEGER NOT NULL,
                    `note` TEXT NOT NULL,
                    `sortOrder` INTEGER NOT NULL,
                    `subCategory` TEXT NOT NULL,
                    `detail` TEXT NOT NULL,
                    `isRecurring` INTEGER NOT NULL,
                    `recurrenceFrequency` TEXT,
                    `recurTillDate` INTEGER,
                    `recurCount` INTEGER,
                    FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            // 2. Copy data from old table to new table preserving values
            db.execSQL(
                """
                INSERT INTO `transactions_new` (id, date, profileId, amount, type, categoryId, note, sortOrder, subCategory, detail, isRecurring, recurrenceFrequency, recurTillDate, recurCount)
                SELECT id, date, profileId, amount, type, categoryId, note, sortOrder, subCategory, detail, isRecurring, recurrenceFrequency, recurTillDate, recurCount FROM `transactions`
                """.trimIndent()
            )
            // 3. Drop old table and rename new one
            db.execSQL("DROP TABLE `transactions`")
            db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
            // 4. Re‑create indexes used elsewhere
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_sortOrder ON transactions(sortOrder)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_profileId ON transactions(profileId)")
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `grocery_budget_items` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `profileId` INTEGER NOT NULL DEFAULT 1,
                    `date` INTEGER NOT NULL,
                    `category` TEXT NOT NULL DEFAULT 'Starch',
                    `subCategory` TEXT NOT NULL DEFAULT 'Rice',
                    `itemDetail` TEXT NOT NULL DEFAULT '',
                    `unitSize` TEXT NOT NULL DEFAULT 'pack',
                    `note` TEXT NOT NULL DEFAULT '',
                    `quantityBudget` INTEGER NOT NULL DEFAULT 1,
                    `unitPriceBudget` REAL NOT NULL DEFAULT 0.0,
                    `costBudget` REAL NOT NULL DEFAULT 0.0,
                    `isRecurring` INTEGER NOT NULL DEFAULT 0,
                    `quantityActual` INTEGER NOT NULL DEFAULT 0,
                    `unitPriceActual` REAL NOT NULL DEFAULT 0.0,
                    `costActual` REAL NOT NULL DEFAULT 0.0
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `unit_sizes` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `profileId` INTEGER NOT NULL DEFAULT 1,
                    `name` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `shopping_lists` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `profileId` INTEGER NOT NULL DEFAULT 1,
                    `payMonthDate` INTEGER NOT NULL,
                    `shoppingDate` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `status` TEXT NOT NULL DEFAULT 'OPEN',
                    `totalBudgetCost` REAL NOT NULL DEFAULT 0.0,
                    `totalActualCost` REAL NOT NULL DEFAULT 0.0,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `shopping_list_items` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `shoppingListId` INTEGER NOT NULL,
                    `budgetItemId` INTEGER,
                    `category` TEXT NOT NULL,
                    `subCategory` TEXT NOT NULL,
                    `itemDetail` TEXT NOT NULL,
                    `unitSize` TEXT NOT NULL,
                    `quantityBudget` INTEGER NOT NULL,
                    `unitPriceBudget` REAL NOT NULL,
                    `quantityActual` INTEGER NOT NULL,
                    `unitPriceActual` REAL NOT NULL,
                    `isChecked` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }
    }
}
