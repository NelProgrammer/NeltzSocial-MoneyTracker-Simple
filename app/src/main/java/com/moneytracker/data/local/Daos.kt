package com.moneytracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.CategorySummary
import com.moneytracker.data.local.entity.TransactionEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.local.entity.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun observeByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT t.id, t.amount, t.type, t.categoryId, t.date, t.note, t.sortOrder,
               c.name AS categoryName, c.iconName AS categoryIconName
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        ORDER BY t.sortOrder ASC, t.date DESC, t.id DESC
        """
    )
    fun observeAllWithCategory(): Flow<List<TransactionWithCategory>>

    @Query(
        """
        SELECT t.id, t.amount, t.type, t.categoryId, t.date, t.note, t.sortOrder,
               c.name AS categoryName, c.iconName AS categoryIconName
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.date >= :startDate AND t.date < :endDate
        ORDER BY t.sortOrder ASC, t.date DESC, t.id DESC
        """
    )
    fun observeByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE type = :type AND date >= :startDate AND date < :endDate
        """
    )
    fun observeTotalByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<Double>

    @Query(
        """
        SELECT c.id AS categoryId, c.name AS categoryName, COALESCE(SUM(t.amount), 0) AS total
        FROM categories c
        LEFT JOIN transactions t ON t.categoryId = c.id
            AND t.type = :type
            AND t.date >= :startDate
            AND t.date < :endDate
        WHERE c.type = :type
        GROUP BY c.id, c.name
        HAVING total > 0
        ORDER BY total DESC
        """
    )
    fun observeCategorySummaries(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Flow<List<CategorySummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT COALESCE(MIN(sortOrder), 0) - 1 FROM transactions")
    suspend fun nextSortOrder(): Int

    @Query("UPDATE transactions SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
}
