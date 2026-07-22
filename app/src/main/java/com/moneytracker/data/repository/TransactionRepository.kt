package com.moneytracker.data.repository

import com.moneytracker.data.local.CategoryDao
import com.moneytracker.data.local.TransactionDao
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.CategorySummary
import com.moneytracker.data.local.entity.TransactionEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.local.entity.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    fun observeAllTransactions(): Flow<List<TransactionWithCategory>> =
        transactionDao.observeAllWithCategory()

    fun observeTransactionsForMonth(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>> =
        transactionDao.observeByDateRange(startDate, endDate)

    fun observeCategories(type: TransactionType): Flow<List<CategoryEntity>> =
        categoryDao.observeByType(type)

    fun observeAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.observeAll()

    fun observeMonthlySummary(startDate: Long, endDate: Long): Flow<MonthlySummary> {
        val income = transactionDao.observeTotalByTypeAndDateRange(
            TransactionType.INCOME,
            startDate,
            endDate
        )
        val expense = transactionDao.observeTotalByTypeAndDateRange(
            TransactionType.EXPENSE,
            startDate,
            endDate
        )
        return combine(income, expense) { incomeTotal, expenseTotal ->
            MonthlySummary(
                income = incomeTotal,
                expense = expenseTotal,
                balance = incomeTotal - expenseTotal
            )
        }
    }

    fun observeExpenseCategorySummaries(startDate: Long, endDate: Long): Flow<List<CategorySummary>> =
        transactionDao.observeCategorySummaries(TransactionType.EXPENSE, startDate, endDate)

    fun observeIncomeCategorySummaries(startDate: Long, endDate: Long): Flow<List<CategorySummary>> =
        transactionDao.observeCategorySummaries(TransactionType.INCOME, startDate, endDate)

    suspend fun getTransaction(id: Long): TransactionEntity? =
        transactionDao.getById(id)

    suspend fun getCategory(id: Long): CategoryEntity? =
        categoryDao.getById(id)

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        return if (transaction.id == 0L) {
            val sortOrder = if (transaction.sortOrder != 0) {
                transaction.sortOrder
            } else {
                transactionDao.nextSortOrder()
            }
            transactionDao.insert(transaction.copy(sortOrder = sortOrder))
        } else {
            transactionDao.update(transaction)
            transaction.id
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.delete(transaction)
    }

    suspend fun reorderTransactions(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            transactionDao.updateSortOrder(id, index)
        }
    }

    suspend fun saveCategory(category: CategoryEntity): Long {
        return if (category.id == 0L) {
            categoryDao.insert(category)
        } else {
            categoryDao.update(category)
            category.id
        }
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.delete(category)
    }
}

data class MonthlySummary(
    val income: Double,
    val expense: Double,
    val balance: Double
)
