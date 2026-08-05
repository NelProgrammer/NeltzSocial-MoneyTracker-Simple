package com.moneytracker.data.repository

import com.moneytracker.data.local.CategoryDao
import com.moneytracker.data.local.DetailDao
import com.moneytracker.data.local.GroceryDao
import com.moneytracker.data.local.ProfileDao
import com.moneytracker.data.local.SubCategoryDao
import com.moneytracker.data.local.TaxiFareDao
import com.moneytracker.data.local.TransactionDao
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
import com.moneytracker.util.ProfileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val subCategoryDao: SubCategoryDao,
    private val detailDao: DetailDao,
    private val groceryDao: GroceryDao? = null,
    private val taxiFareDao: TaxiFareDao? = null,
    private val profileDao: ProfileDao? = null
) {
    val activeProfileId: Long
        get() = ProfileManager.activeProfile.value?.id ?: 1L

    // Profile Operations
    fun observeAllProfiles(): Flow<List<ProfileEntity>> =
        profileDao?.observeAll() ?: flowOf(emptyList())

    fun observePermanentProfiles(): Flow<List<ProfileEntity>> =
        profileDao?.observePermanentProfiles() ?: flowOf(emptyList())

    suspend fun getGuestProfile(): ProfileEntity? =
        profileDao?.getGuestProfile()

    suspend fun saveProfile(profile: ProfileEntity): Long {
        return if (profile.id == 0L) {
            profileDao?.insert(profile) ?: 0L
        } else {
            profileDao?.update(profile)
            profile.id
        }
    }

    suspend fun convertGuestToPermanent(
        guestId: Long,
        newUsername: String,
        isPasswordProtected: Boolean,
        passwordHash: String?
    ): Boolean {
        val guest = profileDao?.getById(guestId) ?: return false
        val updated = guest.copy(
            username = newUsername,
            isGuest = false,
            isPasswordProtected = isPasswordProtected,
            passwordHash = passwordHash
        )
        profileDao?.update(updated)
        return true
    }

    suspend fun deleteProfile(profile: ProfileEntity) {
        profileDao?.delete(profile)
    }

    fun observeAllTransactions(): Flow<List<TransactionWithCategory>> =
        transactionDao.observeAllWithCategory(activeProfileId)

    fun observeTransactionsForMonth(startDate: Long, endDate: Long): Flow<List<TransactionWithCategory>> =
        transactionDao.observeByDateRange(activeProfileId, startDate, endDate)

    fun observeCategories(type: TransactionType): Flow<List<CategoryEntity>> =
        categoryDao.observeByType(activeProfileId, type)

    fun observeAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.observeAll(activeProfileId)

    fun observeAllSubCategories(): Flow<List<SubCategoryEntity>> =
        subCategoryDao.observeAll(activeProfileId)

    fun observeSubCategoriesForCategory(categoryId: Long): Flow<List<SubCategoryEntity>> =
        subCategoryDao.observeForCategory(activeProfileId, categoryId)

    fun observeAllDetails(): Flow<List<DetailEntity>> =
        detailDao.observeAll(activeProfileId)

    fun observeMonthlySummary(startDate: Long, endDate: Long): Flow<MonthlySummary> {
        val pid = activeProfileId
        val income = transactionDao.observeTotalByTypeAndDateRange(
            pid,
            TransactionType.INCOME,
            startDate,
            endDate
        )
        val investment = transactionDao.observeTotalByTypeAndDateRange(
            pid,
            TransactionType.INVESTMENT,
            startDate,
            endDate
        )
        val expense = transactionDao.observeTotalByTypeAndDateRange(
            pid,
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

    fun observeMonthlySummaries(monthRanges: List<Pair<Long, Long>>): Flow<List<MonthlySummary>> {
        val flows = monthRanges.map { (start, end) -> observeMonthlySummary(start, end) }
        return combine(flows) { summaries ->
            summaries.toList()
        }
    }

    fun observeExpenseCategorySummaries(startDate: Long, endDate: Long): Flow<List<CategorySummary>> =
        transactionDao.observeCategorySummaries(activeProfileId, TransactionType.EXPENSE, startDate, endDate)

    fun observeIncomeCategorySummaries(startDate: Long, endDate: Long): Flow<List<CategorySummary>> =
        transactionDao.observeCategorySummaries(activeProfileId, TransactionType.INCOME, startDate, endDate)

    fun observeInvestmentCategorySummaries(startDate: Long, endDate: Long): Flow<List<CategorySummary>> =
        transactionDao.observeCategorySummaries(activeProfileId, TransactionType.INVESTMENT, startDate, endDate)

    suspend fun getTransaction(id: Long): TransactionEntity? =
        transactionDao.getById(id)

    suspend fun getRecurringTransactions(): List<TransactionEntity> =
        transactionDao.getRecurringTransactions(activeProfileId)

    suspend fun getAllEntities(): List<TransactionEntity> =
        transactionDao.getAllEntities(activeProfileId)

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        val pid = if (transaction.profileId == 0L) activeProfileId else transaction.profileId
        val sortOrder = if (transaction.id == 0L) transactionDao.nextSortOrder(pid) else transaction.sortOrder
        val entity = transaction.copy(profileId = pid, sortOrder = sortOrder)
        return if (entity.id == 0L) {
            transactionDao.insert(entity)
        } else {
            transactionDao.update(entity)
            entity.id
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.delete(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionWithCategory) {
        val entity = transactionDao.getById(transaction.id)
        if (entity != null) {
            transactionDao.delete(entity)
        }
    }

    suspend fun reorderTransactions(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            transactionDao.updateSortOrder(id, index)
        }
    }

    suspend fun saveCategory(category: CategoryEntity): Long {
        val pid = if (category.profileId == 0L) activeProfileId else category.profileId
        val entity = category.copy(profileId = pid)
        return if (entity.id == 0L) {
            categoryDao.insert(entity)
        } else {
            categoryDao.update(entity)
            entity.id
        }
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.delete(category)
    }

    suspend fun saveSubCategory(subCategory: SubCategoryEntity): Long {
        val pid = if (subCategory.profileId == 0L) activeProfileId else subCategory.profileId
        val entity = subCategory.copy(profileId = pid)
        return if (entity.id == 0L) {
            subCategoryDao.insert(entity)
        } else {
            subCategoryDao.update(entity)
            entity.id
        }
    }

    suspend fun deleteSubCategory(subCategory: SubCategoryEntity) {
        subCategoryDao.delete(subCategory)
    }

    suspend fun saveDetail(detail: DetailEntity): Long {
        val pid = if (detail.profileId == 0L) activeProfileId else detail.profileId
        val entity = detail.copy(profileId = pid)
        return if (entity.id == 0L) {
            detailDao.insert(entity)
        } else {
            detailDao.update(entity)
            entity.id
        }
    }

    suspend fun deleteDetail(detail: DetailEntity) {
        detailDao.delete(detail)
    }

    // Grocery Operations
    fun observeAllGroceryItems(): Flow<List<GroceryItemEntity>> =
        groceryDao?.observeAll(activeProfileId) ?: flowOf(emptyList())

    suspend fun saveGroceryItem(item: GroceryItemEntity): Long {
        val pid = if (item.profileId == 0L) activeProfileId else item.profileId
        val entity = item.copy(profileId = pid)
        return if (entity.id == 0L) {
            groceryDao?.insert(entity) ?: 0L
        } else {
            groceryDao?.update(entity)
            entity.id
        }
    }

    suspend fun deleteGroceryItem(item: GroceryItemEntity) {
        groceryDao?.delete(item)
    }

    // Taxi Fare Operations
    fun observeAllTaxiFares(): Flow<List<TaxiFareEntity>> =
        taxiFareDao?.observeAll(activeProfileId) ?: flowOf(emptyList())

    suspend fun saveTaxiFare(fare: TaxiFareEntity): Long {
        val pid = if (fare.profileId == 0L) activeProfileId else fare.profileId
        val entity = fare.copy(profileId = pid)
        return if (entity.id == 0L) {
            taxiFareDao?.insert(fare) ?: 0L
        } else {
            taxiFareDao?.update(fare)
            fare.id
        }
    }

    suspend fun deleteTaxiFare(fare: TaxiFareEntity) {
        taxiFareDao?.delete(fare)
    }
}

data class MonthlySummary(
    val income: Double = 0.0,
    val investment: Double = 0.0,
    val education: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0
)
