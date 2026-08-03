package com.moneytracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.DetailEntity
import com.moneytracker.data.local.entity.GroceryItemEntity
import com.moneytracker.data.local.entity.ProfileEntity
import com.moneytracker.data.local.entity.RecurrenceFrequency
import com.moneytracker.data.local.entity.SubCategoryEntity
import com.moneytracker.data.local.entity.TaxiFareEntity
import com.moneytracker.data.local.entity.TransactionEntity
import com.moneytracker.data.local.entity.TransactionType

class TransactionTypeConverter {
    @TypeConverter
    fun fromType(type: TransactionType): String = type.name

    @TypeConverter
    fun toType(value: String): TransactionType = TransactionType.valueOf(value)
}

class RecurrenceFrequencyConverter {
    @TypeConverter
    fun fromFrequency(freq: RecurrenceFrequency?): String? = freq?.name

    @TypeConverter
    fun toFrequency(value: String?): RecurrenceFrequency? = value?.let {
        try { RecurrenceFrequency.valueOf(it) } catch (e: Exception) { RecurrenceFrequency.MONTHLY }
    }
}

@Database(
    entities = [
        CategoryEntity::class,
        SubCategoryEntity::class,
        DetailEntity::class,
        TransactionEntity::class,
        GroceryItemEntity::class,
        TaxiFareEntity::class,
        ProfileEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(TransactionTypeConverter::class, RecurrenceFrequencyConverter::class)
abstract class MoneyTrackerDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun subCategoryDao(): SubCategoryDao
    abstract fun detailDao(): DetailDao
    abstract fun transactionDao(): TransactionDao
    abstract fun groceryDao(): GroceryDao
    abstract fun taxiFareDao(): TaxiFareDao
    abstract fun profileDao(): ProfileDao

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
                .addMigrations(
                    DatabaseMigrations.MIGRATION_1_2,
                    DatabaseMigrations.MIGRATION_2_3,
                    DatabaseMigrations.MIGRATION_3_4,
                    DatabaseMigrations.MIGRATION_4_5,
                    DatabaseMigrations.MIGRATION_5_6,
                    DatabaseMigrations.MIGRATION_6_7,
                    DatabaseMigrations.MIGRATION_7_8,
                    DatabaseMigrations.MIGRATION_8_9,
                    DatabaseMigrations.MIGRATION_9_10,
                    DatabaseMigrations.MIGRATION_10_11,
                    DatabaseMigrations.MIGRATION_11_12
                )
                .build()
        }
    }
}
