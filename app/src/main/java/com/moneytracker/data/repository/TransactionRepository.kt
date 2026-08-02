package com.moneytracker.data.repository

import com.moneytracker.data.local.CategoryDao
import com.moneytracker.data.local.SubCategoryDao
import com.moneytracker.data.local.TransactionDao
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.CategorySummary
import com.moneytracker.data.local.entity.SubCategoryEntity
import com.moneytracker.data.local.entity.TransactionEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.local.entity.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

import com.moneytracker.data.local.DetailDao
import com.moneytracker.data.local.entity.DetailEntity

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val subCategoryDao: SubCategoryDao,
    private val detailDao: DetailDao
) {
    fun observeAllTransactions(): Flow<List<TransactionWithCategory>> =
        transactionDao.observeAllWithCategory()

    fun observeTransactionsForMonth(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>> =
        transactionDao.observeByDateRange(startDate, endDate)

    fun observeCategories(type: TransactionType): Flow<List<CategoryEntity>> =
        categoryDao.observeByType(type)

    fun observeAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.observeAll()

    fun observeAllSubCategories(): Flow<List<SubCategoryEntity>> =
        subCategoryDao.observeAll()

    fun observeSubCategoriesForCategory(categoryId: Long): Flow<List<SubCategoryEntity>> =
        subCategoryDao.observeForCategory(categoryId)

    fun observeAllDetails(): Flow<List<DetailEntity>> =
        detailDao.observeAll()

    fun observeMonthlySummary(startDate: Long, endDate: Long): Flow<MonthlySummary> {
        val income = transactionDao.observeTotalByTypeAndDateRange(
            TransactionType.INCOME,
            startDate,
            endDate
        )
        val investment = transactionDao.observeTotalByTypeAndDateRange(
            TransactionType.INVESTMENT,
            startDate,
            endDate
        )
        val expense = transactionDao.observeTotalByTypeAndDateRange(
            TransactionType.EXPENSE,
            startDate,
            endDate
        )
        return combine(income, investment, expense) { incomeTotal, investmentTotal, expenseTotal ->
            val absIncome = kotlin.math.abs(incomeTotal)
            val absInvestment = kotlin.math.abs(investmentTotal)
            val absExpense = kotlin.math.abs(expenseTotal)
            MonthlySummary(
                income = absIncome,
                investment = absInvestment,
                expense = absExpense,
                balance = absIncome - absInvestment - absExpense
            )
        }
    }

    // Observe summaries for a list of month ranges
    fun observeMonthlySummaries(monthRanges: List<Pair<Long, Long>>): Flow<List<MonthlySummary>> {
        val flows = monthRanges.map { (start, end) -> observeMonthlySummary(start, end) }
        return combine(flows) { summaries ->
            summaries.toList()
        }
    }


    fun observeExpenseCategorySummaries(startDate: Long, endDate: Long): Flow<List<CategorySummary>> =
        transactionDao.observeCategorySummaries(TransactionType.EXPENSE, startDate, endDate)

    fun observeIncomeCategorySummaries(startDate: Long, endDate: Long): Flow<List<CategorySummary>> =
        transactionDao.observeCategorySummaries(TransactionType.INCOME, startDate, endDate)

    fun observeInvestmentCategorySummaries(startDate: Long, endDate: Long): Flow<List<CategorySummary>> =
        transactionDao.observeCategorySummaries(TransactionType.INVESTMENT, startDate, endDate)

    suspend fun getTransaction(id: Long): TransactionEntity? =
        transactionDao.getById(id)

    suspend fun getRecurringTransactions(): List<TransactionEntity> =
        transactionDao.getRecurringTransactions()

    suspend fun getAllEntities(): List<TransactionEntity> =
        transactionDao.getAllEntities()

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

    suspend fun saveSubCategory(subCategory: SubCategoryEntity): Long {
        return if (subCategory.id == 0L) {
            subCategoryDao.insert(subCategory)
        } else {
            subCategoryDao.update(subCategory)
            subCategory.id
        }
    }

    suspend fun deleteSubCategory(subCategory: SubCategoryEntity) {
        subCategoryDao.delete(subCategory)
    }

    suspend fun saveDetail(detail: DetailEntity): Long {
        return if (detail.id == 0L) {
            detailDao.insert(detail)
        } else {
            detailDao.update(detail)
            detail.id
        }
    }

    suspend fun deleteDetail(detail: DetailEntity) {
        detailDao.delete(detail)
    }
}

data class MonthlySummary(
    val income: Double,
    val investment: Double,
    val expense: Double,
    val balance: Double
)
