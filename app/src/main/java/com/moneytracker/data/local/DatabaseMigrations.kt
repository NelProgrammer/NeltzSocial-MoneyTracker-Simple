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
}
