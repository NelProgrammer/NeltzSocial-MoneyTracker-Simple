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
}
