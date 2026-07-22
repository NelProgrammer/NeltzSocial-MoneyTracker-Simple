package com.moneytracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.TransactionEntity
import com.moneytracker.data.local.entity.TransactionType

class TransactionTypeConverter {
    @TypeConverter
    fun fromType(type: TransactionType): String = type.name

    @TypeConverter
    fun toType(value: String): TransactionType = TransactionType.valueOf(value)
}

@Database(
    entities = [CategoryEntity::class, TransactionEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(TransactionTypeConverter::class)
abstract class MoneyTrackerDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var instance: MoneyTrackerDatabase? = null

        fun getInstance(context: Context): MoneyTrackerDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): MoneyTrackerDatabase {
            return Room.databaseBuilder(
                context,
                MoneyTrackerDatabase::class.java,
                "money_tracker.db"
            )
                .addMigrations(DatabaseMigrations.MIGRATION_1_2)
                .build()
        }
    }
}
