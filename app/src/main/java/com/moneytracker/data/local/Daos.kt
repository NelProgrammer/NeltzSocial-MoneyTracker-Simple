package com.moneytracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.CategorySummary
import com.moneytracker.data.local.entity.DetailEntity
import com.moneytracker.data.local.entity.GroceryItemEntity
import com.moneytracker.data.local.entity.ProfileEntity
import com.moneytracker.data.local.entity.SubCategoryEntity
import com.moneytracker.data.local.entity.TaxiFareEntity
import com.moneytracker.data.local.entity.TransactionEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.local.entity.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY isGuest DESC, username ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE isGuest = 0 ORDER BY username ASC")
    fun observePermanentProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles")
    suspend fun getAllProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE isGuest = 1 LIMIT 1")
    suspend fun getGuestProfile(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun count(): Int
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE profileId = :profileId ORDER BY name ASC")
    fun observeAll(profileId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE profileId = :profileId AND type = :type ORDER BY name ASC")
    fun observeByType(profileId: Long, type: TransactionType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE profileId = :profileId")
    suspend fun getAllCategories(profileId: Long): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: Long)

    @Query("SELECT COUNT(*) FROM categories WHERE profileId = :profileId")
    suspend fun count(profileId: Long): Int
}

@Dao
interface SubCategoryDao {
    @Query("SELECT * FROM sub_categories WHERE profileId = :profileId ORDER BY name ASC")
    fun observeAll(profileId: Long): Flow<List<SubCategoryEntity>>

    @Query("SELECT * FROM sub_categories WHERE profileId = :profileId AND categoryId = :categoryId ORDER BY name ASC")
    fun observeForCategory(profileId: Long, categoryId: Long): Flow<List<SubCategoryEntity>>

    @Query("SELECT * FROM sub_categories WHERE id = :id")
    suspend fun getById(id: Long): SubCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subCategory: SubCategoryEntity): Long

    @Update
    suspend fun update(subCategory: SubCategoryEntity)

    @Query("UPDATE sub_categories SET type = :type WHERE profileId = :profileId AND categoryId = :categoryId")
    suspend fun updateTypeForCategory(profileId: Long, categoryId: Long, type: TransactionType)

    @Delete
    suspend fun delete(subCategory: SubCategoryEntity)

    @Query("DELETE FROM sub_categories WHERE profileId = :profileId AND categoryId = :categoryId")
    suspend fun deleteAllForCategory(profileId: Long, categoryId: Long)

    @Query("DELETE FROM sub_categories WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: Long)

    @Query("SELECT COUNT(*) FROM sub_categories WHERE profileId = :profileId")
    suspend fun count(profileId: Long): Int
}

@Dao
interface DetailDao {
    @Query("SELECT * FROM details WHERE profileId = :profileId ORDER BY name ASC")
    fun observeAll(profileId: Long): Flow<List<DetailEntity>>

    @Query("SELECT * FROM details WHERE id = :id")
    suspend fun getById(id: Long): DetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detail: DetailEntity): Long

    @Update
    suspend fun update(detail: DetailEntity)

    @Query("UPDATE details SET type = :type WHERE profileId = :profileId AND categoryId = :categoryId")
    suspend fun updateTypeForCategory(profileId: Long, categoryId: Long, type: TransactionType)

    @Query("UPDATE details SET categoryId = :categoryId, type = COALESCE(:type, type) WHERE profileId = :profileId AND subCategoryId = :subCategoryId")
    suspend fun updateCategoryForSubCategory(profileId: Long, subCategoryId: Long, categoryId: Long, type: TransactionType?)

    @Query("UPDATE details SET type = :type WHERE profileId = :profileId AND subCategoryId = :subCategoryId")
    suspend fun updateTypeForSubCategory(profileId: Long, subCategoryId: Long, type: TransactionType)

    @Delete
    suspend fun delete(detail: DetailEntity)

    @Query("DELETE FROM details WHERE profileId = :profileId AND categoryId = :categoryId")
    suspend fun deleteAllForCategory(profileId: Long, categoryId: Long)

    @Query("DELETE FROM details WHERE profileId = :profileId AND subCategoryId = :subCategoryId")
    suspend fun deleteAllForSubCategory(profileId: Long, subCategoryId: Long)

    @Query("DELETE FROM details WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: Long)

    @Query("SELECT COUNT(*) FROM details WHERE profileId = :profileId")
    suspend fun count(profileId: Long): Int
}

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT t.id, t.amount, t.type, t.categoryId, t.date, t.note, t.sortOrder, t.subCategory, t.detail,
               t.isRecurring, t.recurrenceFrequency, t.recurTillDate, t.recurCount, t.isRecurred,
               c.name AS categoryName, c.iconName AS categoryIconName
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.profileId = :profileId
        ORDER BY t.sortOrder ASC, t.date DESC, t.id DESC
        """
    )
    fun observeAllWithCategory(profileId: Long): Flow<List<TransactionWithCategory>>

    @Query(
        """
        SELECT t.id, t.amount, t.type, t.categoryId, t.date, t.note, t.sortOrder, t.subCategory, t.detail,
               t.isRecurring, t.recurrenceFrequency, t.recurTillDate, t.recurCount, t.isRecurred,
               c.name AS categoryName, c.iconName AS categoryIconName
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.profileId = :profileId AND t.date >= :startDate AND t.date < :endDate
        ORDER BY t.sortOrder ASC, t.date DESC, t.id DESC
        """
    )
    fun observeByDateRange(profileId: Long, startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query(
        """
        SELECT COALESCE(SUM(ABS(amount)), 0)
        FROM transactions
        WHERE profileId = :profileId AND type = :type AND date >= :startDate AND date < :endDate
          AND (recurrenceFrequency IS NULL OR recurrenceFrequency != 'PLAN_FUTURE')
        """
    )
    fun observeTotalByTypeAndDateRange(
        profileId: Long,
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<Double>

    @Query(
        """
        SELECT c.id AS categoryId, c.name AS categoryName, COALESCE(SUM(ABS(t.amount)), 0) AS total,
               0 AS isDebtFunding, NULL AS customColorHex
        FROM categories c
        LEFT JOIN transactions t ON t.categoryId = c.id
            AND t.profileId = :profileId
            AND t.type = :type
            AND t.date >= :startDate
            AND t.date < :endDate
            AND (t.recurrenceFrequency IS NULL OR t.recurrenceFrequency != 'PLAN_FUTURE')
        WHERE c.profileId = :profileId AND c.type = :type
        GROUP BY c.id, c.name
        HAVING total > 0
        ORDER BY total DESC
        """
    )
    fun observeCategorySummaries(
        profileId: Long,
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<CategorySummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET type = :type WHERE profileId = :profileId AND categoryId = :categoryId")
    suspend fun updateTypeForCategory(profileId: Long, categoryId: Long, type: TransactionType)

    @Query("UPDATE transactions SET subCategory = :newName WHERE profileId = :profileId AND subCategory = :oldName AND (:categoryId IS NULL OR categoryId = :categoryId)")
    suspend fun renameSubCategory(profileId: Long, oldName: String, newName: String, categoryId: Long?)

    @Query("UPDATE transactions SET categoryId = :categoryId, type = COALESCE(:type, type) WHERE profileId = :profileId AND subCategory = :subCategoryName")
    suspend fun updateCategoryForSubCategory(profileId: Long, subCategoryName: String, categoryId: Long, type: TransactionType?)

    @Query("UPDATE transactions SET type = :type WHERE profileId = :profileId AND subCategory = :subCategoryName")
    suspend fun updateTypeForSubCategory(profileId: Long, subCategoryName: String, type: TransactionType)

    @Query("UPDATE transactions SET detail = :newName WHERE profileId = :profileId AND detail = :oldName")
    suspend fun renameDetail(profileId: Long, oldName: String, newName: String)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT COALESCE(MIN(sortOrder), 0) - 1 FROM transactions WHERE profileId = :profileId")
    suspend fun nextSortOrder(profileId: Long): Int

    @Query("UPDATE transactions SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    @Query("SELECT * FROM transactions WHERE profileId = :profileId AND isRecurring = 1")
    suspend fun getRecurringTransactions(profileId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE profileId = :profileId")
    suspend fun getAllEntities(profileId: Long): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE profileId = :profileId")
    suspend fun countForProfile(profileId: Long): Int
}

@Dao
interface GroceryDao {
    @Query("SELECT * FROM grocery_items WHERE profileId = :profileId ORDER BY date DESC, id DESC")
    fun observeAll(profileId: Long): Flow<List<GroceryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GroceryItemEntity): Long

    @Update
    suspend fun update(item: GroceryItemEntity)

    @Delete
    suspend fun delete(item: GroceryItemEntity)

    @Query("DELETE FROM grocery_items WHERE profileId = :profileId")
    suspend fun deleteAll(profileId: Long)
}

@Dao
interface TaxiFareDao {
    @Query("SELECT * FROM taxi_fares WHERE profileId = :profileId ORDER BY date DESC, id DESC")
    fun observeAll(profileId: Long): Flow<List<TaxiFareEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fare: TaxiFareEntity): Long

    @Update
    suspend fun update(fare: TaxiFareEntity)

    @Delete
    suspend fun delete(fare: TaxiFareEntity)
}

@Dao
interface GroceryBudgetDao {
    @Query("SELECT * FROM grocery_budget_items WHERE profileId = :profileId AND date >= :startDate AND date < :endDate ORDER BY category ASC, subCategory ASC, itemDetail ASC")
    fun observeForMonth(profileId: Long, startDate: Long, endDate: Long): Flow<List<com.moneytracker.data.local.entity.GroceryBudgetItemEntity>>

    @Query("SELECT * FROM grocery_budget_items WHERE profileId = :profileId AND date >= :startDate AND date < :endDate")
    suspend fun getForMonth(profileId: Long, startDate: Long, endDate: Long): List<com.moneytracker.data.local.entity.GroceryBudgetItemEntity>

    @Query("SELECT * FROM grocery_budget_items WHERE profileId = :profileId AND (isRecurring = 1 OR isRecurring = 2)")
    suspend fun getRecurringAndPlannedItems(profileId: Long): List<com.moneytracker.data.local.entity.GroceryBudgetItemEntity>

    @Query("SELECT * FROM grocery_budget_items WHERE profileId = :profileId")
    suspend fun getAllForProfile(profileId: Long): List<com.moneytracker.data.local.entity.GroceryBudgetItemEntity>

    @Query("SELECT * FROM grocery_budget_items WHERE id = :id")
    suspend fun getById(id: Long): com.moneytracker.data.local.entity.GroceryBudgetItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: com.moneytracker.data.local.entity.GroceryBudgetItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<com.moneytracker.data.local.entity.GroceryBudgetItemEntity>)

    @Update
    suspend fun update(item: com.moneytracker.data.local.entity.GroceryBudgetItemEntity)

    @Delete
    suspend fun delete(item: com.moneytracker.data.local.entity.GroceryBudgetItemEntity)
}

@Dao
interface UnitSizeDao {
    @Query("SELECT * FROM unit_sizes WHERE profileId = :profileId GROUP BY LOWER(TRIM(name)) ORDER BY name ASC")
    fun observeAll(profileId: Long): Flow<List<com.moneytracker.data.local.entity.UnitSizeEntity>>

    @Query("SELECT * FROM unit_sizes WHERE profileId = :profileId AND LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getByName(profileId: Long, name: String): com.moneytracker.data.local.entity.UnitSizeEntity?

    @Query("SELECT COUNT(*) FROM unit_sizes WHERE profileId = :profileId")
    suspend fun count(profileId: Long): Int

    @Query("DELETE FROM unit_sizes WHERE profileId = :profileId AND id NOT IN (SELECT MIN(id) FROM unit_sizes WHERE profileId = :profileId GROUP BY LOWER(TRIM(name)))")
    suspend fun deleteDuplicates(profileId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(unitSize: com.moneytracker.data.local.entity.UnitSizeEntity): Long

    @Delete
    suspend fun delete(unitSize: com.moneytracker.data.local.entity.UnitSizeEntity)
}

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists WHERE profileId = :profileId AND payMonthDate >= :startDate AND payMonthDate < :endDate ORDER BY shoppingDate DESC, id DESC")
    fun observeForMonth(profileId: Long, startDate: Long, endDate: Long): Flow<List<com.moneytracker.data.local.entity.ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    suspend fun getById(id: Long): com.moneytracker.data.local.entity.ShoppingListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shoppingList: com.moneytracker.data.local.entity.ShoppingListEntity): Long

    @Update
    suspend fun update(shoppingList: com.moneytracker.data.local.entity.ShoppingListEntity)

    @Delete
    suspend fun delete(shoppingList: com.moneytracker.data.local.entity.ShoppingListEntity)
}

@Dao
interface ShoppingListItemDao {
    @Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :listId ORDER BY id ASC")
    fun observeForList(listId: Long): Flow<List<com.moneytracker.data.local.entity.ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :listId ORDER BY id ASC")
    suspend fun getForList(listId: Long): List<com.moneytracker.data.local.entity.ShoppingListItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: com.moneytracker.data.local.entity.ShoppingListItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<com.moneytracker.data.local.entity.ShoppingListItemEntity>)

    @Update
    suspend fun update(item: com.moneytracker.data.local.entity.ShoppingListItemEntity)

    @Delete
    suspend fun delete(item: com.moneytracker.data.local.entity.ShoppingListItemEntity)

    @Query("DELETE FROM shopping_list_items WHERE shoppingListId = :listId AND isChecked = 0")
    suspend fun deleteUncheckedItems(listId: Long)
}

@Dao
interface TaxiExhaustionDao {
    @Query("SELECT * FROM taxi_exhaustions WHERE profileId = :profileId AND payMonthDate >= :startDate AND payMonthDate < :endDate ORDER BY date DESC, id DESC")
    fun observeForMonth(profileId: Long, startDate: Long, endDate: Long): Flow<List<com.moneytracker.data.local.entity.TaxiExhaustionEntity>>

    @Query("SELECT * FROM taxi_exhaustions WHERE profileId = :profileId AND payMonthDate >= :startDate AND payMonthDate < :endDate ORDER BY date DESC, id DESC")
    suspend fun getForMonth(profileId: Long, startDate: Long, endDate: Long): List<com.moneytracker.data.local.entity.TaxiExhaustionEntity>

    @Query("SELECT * FROM taxi_exhaustions WHERE profileId = :profileId AND routeId = :routeId AND payMonthDate >= :startDate AND payMonthDate < :endDate ORDER BY date DESC, id DESC")
    suspend fun getForRouteAndMonth(profileId: Long, routeId: Long, startDate: Long, endDate: Long): List<com.moneytracker.data.local.entity.TaxiExhaustionEntity>

    @Query("SELECT * FROM taxi_exhaustions WHERE profileId = :profileId AND routeId = :routeId AND date >= :dayStart AND date < :dayEnd AND timeOfDay = :timeOfDay LIMIT 1")
    suspend fun getDailyEntry(profileId: Long, routeId: Long, dayStart: Long, dayEnd: Long, timeOfDay: String): com.moneytracker.data.local.entity.TaxiExhaustionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: com.moneytracker.data.local.entity.TaxiExhaustionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<com.moneytracker.data.local.entity.TaxiExhaustionEntity>)

    @Update
    suspend fun update(item: com.moneytracker.data.local.entity.TaxiExhaustionEntity)

    @Delete
    suspend fun delete(item: com.moneytracker.data.local.entity.TaxiExhaustionEntity)

    @Query("DELETE FROM taxi_exhaustions WHERE profileId = :profileId AND routeId = :routeId")
    suspend fun deleteForRoute(profileId: Long, routeId: Long)
}

@Dao
interface CommuteJourneyDao {
    @androidx.room.Transaction
    @Query("SELECT * FROM commute_journeys WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun observeJourneysWithLegs(profileId: Long): Flow<List<com.moneytracker.data.local.entity.JourneyWithLegs>>

    @androidx.room.Transaction
    @Query("SELECT * FROM commute_journeys WHERE id = :id")
    suspend fun getJourneyWithLegs(id: Long): com.moneytracker.data.local.entity.JourneyWithLegs?

    @Query("SELECT * FROM commute_journeys WHERE profileId = :profileId AND isDefaultWorkday = 1 LIMIT 1")
    suspend fun getDefaultWorkdayJourney(profileId: Long): com.moneytracker.data.local.entity.CommuteJourneyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: com.moneytracker.data.local.entity.CommuteJourneyEntity): Long

    @Update
    suspend fun updateJourney(journey: com.moneytracker.data.local.entity.CommuteJourneyEntity)

    @Delete
    suspend fun deleteJourney(journey: com.moneytracker.data.local.entity.CommuteJourneyEntity)

    @Query("SELECT * FROM commute_legs WHERE journeyId = :journeyId ORDER BY legOrder ASC")
    fun observeLegsForJourney(journeyId: Long): Flow<List<com.moneytracker.data.local.entity.CommuteLegEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeg(leg: com.moneytracker.data.local.entity.CommuteLegEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLegs(legs: List<com.moneytracker.data.local.entity.CommuteLegEntity>)

    @Update
    suspend fun updateLeg(leg: com.moneytracker.data.local.entity.CommuteLegEntity)

    @Delete
    suspend fun deleteLeg(leg: com.moneytracker.data.local.entity.CommuteLegEntity)

    @Query("DELETE FROM commute_legs WHERE journeyId = :journeyId")
    suspend fun deleteLegsForJourney(journeyId: Long)
}

