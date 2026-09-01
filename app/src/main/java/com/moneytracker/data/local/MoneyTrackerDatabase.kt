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
import com.moneytracker.data.local.entity.GroceryBudgetItemEntity
import com.moneytracker.data.local.entity.UnitSizeEntity
import com.moneytracker.data.local.entity.ShoppingListEntity
import com.moneytracker.data.local.entity.ShoppingListItemEntity
import com.moneytracker.data.local.entity.TaxiFareEntity
import com.moneytracker.data.local.entity.TaxiExhaustionEntity
import com.moneytracker.data.local.entity.CommuteJourneyEntity
import com.moneytracker.data.local.entity.CommuteLegEntity
import com.moneytracker.data.local.entity.TransportMode
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
    fun fromFrequency(freq: RecurrenceFrequency?): String? = (freq ?: RecurrenceFrequency.ONCE_OFF).name

    @TypeConverter
    fun toFrequency(value: String?): RecurrenceFrequency? = value?.let {
        try {
            val f = RecurrenceFrequency.valueOf(it)
            if (f == RecurrenceFrequency.CONTINUOUS) RecurrenceFrequency.MONTHLY else f
        } catch (e: Exception) {
            RecurrenceFrequency.ONCE_OFF
        }
    } ?: RecurrenceFrequency.ONCE_OFF
}

class TransportModeConverter {
    @TypeConverter
    fun fromMode(mode: TransportMode?): String = (mode ?: TransportMode.TAXI).name

    @TypeConverter
    fun toMode(value: String?): TransportMode = value?.let {
        try {
            TransportMode.valueOf(it)
        } catch (e: Exception) {
            TransportMode.TAXI
        }
    } ?: TransportMode.TAXI
}

@Database(
    entities = [
        CategoryEntity::class,
        SubCategoryEntity::class,
        DetailEntity::class,
        TransactionEntity::class,
        GroceryItemEntity::class,
        TaxiFareEntity::class,
        TaxiExhaustionEntity::class,
        ProfileEntity::class,
        GroceryBudgetItemEntity::class,
        UnitSizeEntity::class,
        ShoppingListEntity::class,
        ShoppingListItemEntity::class,
        CommuteJourneyEntity::class,
        CommuteLegEntity::class
    ],
    version = 19,
    exportSchema = false
)
@TypeConverters(
    TransactionTypeConverter::class,
    RecurrenceFrequencyConverter::class,
    TransportModeConverter::class
)
abstract class MoneyTrackerDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun subCategoryDao(): SubCategoryDao
    abstract fun detailDao(): DetailDao
    abstract fun transactionDao(): TransactionDao
    abstract fun groceryDao(): GroceryDao
    abstract fun taxiFareDao(): TaxiFareDao
    abstract fun taxiExhaustionDao(): TaxiExhaustionDao
    abstract fun commuteJourneyDao(): CommuteJourneyDao
    abstract fun profileDao(): ProfileDao
    abstract fun groceryBudgetDao(): GroceryBudgetDao
    abstract fun unitSizeDao(): UnitSizeDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao

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
                    DatabaseMigrations.MIGRATION_11_12,
                    DatabaseMigrations.MIGRATION_12_13,
                    DatabaseMigrations.MIGRATION_13_14,
                    DatabaseMigrations.MIGRATION_14_15,
                    DatabaseMigrations.MIGRATION_15_16,
                    DatabaseMigrations.MIGRATION_16_17,
                    DatabaseMigrations.MIGRATION_17_18,
                    DatabaseMigrations.MIGRATION_18_19
                )
                .build()
        }
    }
}
