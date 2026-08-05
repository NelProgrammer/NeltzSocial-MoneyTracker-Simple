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

import com.moneytracker.data.local.GroceryBudgetDao
import com.moneytracker.data.local.UnitSizeDao
import com.moneytracker.data.local.ShoppingListDao
import com.moneytracker.data.local.ShoppingListItemDao
import com.moneytracker.data.local.entity.GroceryBudgetItemEntity
import com.moneytracker.data.local.entity.UnitSizeEntity
import com.moneytracker.data.local.entity.ShoppingListEntity
import com.moneytracker.data.local.entity.ShoppingListItemEntity

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val subCategoryDao: SubCategoryDao,
    private val detailDao: DetailDao,
    private val groceryDao: GroceryDao? = null,
    private val taxiFareDao: TaxiFareDao? = null,
    private val profileDao: ProfileDao? = null,
    private val groceryBudgetDao: GroceryBudgetDao? = null,
    private val unitSizeDao: UnitSizeDao? = null,
    private val shoppingListDao: ShoppingListDao? = null,
    private val shoppingListItemDao: ShoppingListItemDao? = null
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

    // Grocery Budget Operations
    fun observeGroceryBudgetForMonth(monthTimestamp: Long): Flow<List<GroceryBudgetItemEntity>> =
        groceryBudgetDao?.observeForMonth(activeProfileId, monthTimestamp) ?: flowOf(emptyList())

    suspend fun getGroceryBudgetForMonth(monthTimestamp: Long): List<GroceryBudgetItemEntity> =
        groceryBudgetDao?.getForMonth(activeProfileId, monthTimestamp) ?: emptyList()

    suspend fun autoPopulateRecurringAndPlannedGroceryItems(monthTimestamp: Long) {
        val dao = groceryBudgetDao ?: return
        val currentMonthItems = dao.getForMonth(activeProfileId, monthTimestamp)
        val recurringAndPlanned = dao.getRecurringAndPlannedItems(activeProfileId)

        val toCopy = recurringAndPlanned.filter { template ->
            currentMonthItems.none { existing ->
                existing.category == template.category &&
                existing.subCategory == template.subCategory &&
                existing.itemDetail == template.itemDetail
            }
        }.map { template ->
            template.copy(
                id = 0,
                date = monthTimestamp,
                quantityActual = 0,
                unitPriceActual = 0.0,
                costActual = 0.0
            )
        }

        if (toCopy.isNotEmpty()) {
            dao.insertAll(toCopy)
        }
    }

    suspend fun saveGroceryBudgetItem(item: GroceryBudgetItemEntity): Long {
        val dao = groceryBudgetDao ?: return 0L
        val pid = if (item.profileId == 0L) activeProfileId else item.profileId
        val costB = item.quantityBudget * item.unitPriceBudget
        val costA = item.quantityActual * item.unitPriceActual
        val entity = item.copy(profileId = pid, costBudget = costB, costActual = costA)
        return if (entity.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            entity.id
        }
    }

    suspend fun deleteGroceryBudgetItem(item: GroceryBudgetItemEntity) {
        groceryBudgetDao?.delete(item)
    }

    // Unit Size CRUD Operations
    fun observeUnitSizes(): Flow<List<UnitSizeEntity>> =
        unitSizeDao?.observeAll(activeProfileId) ?: flowOf(emptyList())

    suspend fun saveUnitSize(unitSize: UnitSizeEntity): Long {
        val dao = unitSizeDao ?: return 0L
        val pid = if (unitSize.profileId == 0L) activeProfileId else unitSize.profileId
        return dao.insert(unitSize.copy(profileId = pid))
    }

    suspend fun deleteUnitSize(unitSize: UnitSizeEntity) {
        unitSizeDao?.delete(unitSize)
    }

    // Shopping List Operations
    fun observeShoppingListsForMonth(monthTimestamp: Long): Flow<List<ShoppingListEntity>> =
        shoppingListDao?.observeForMonth(activeProfileId, monthTimestamp) ?: flowOf(emptyList())

    fun observeShoppingListItems(listId: Long): Flow<List<ShoppingListItemEntity>> =
        shoppingListItemDao?.observeForList(listId) ?: flowOf(emptyList())

    suspend fun generateShoppingListFromBudget(
        payMonthTimestamp: Long,
        shoppingDateTimestamp: Long,
        title: String,
        selectedBudgetItems: List<GroceryBudgetItemEntity>
    ): Long {
        val sListDao = shoppingListDao ?: return 0L
        val sItemDao = shoppingListItemDao ?: return 0L

        val totalBudget = selectedBudgetItems.sumOf { it.costBudget }
        val sList = ShoppingListEntity(
            profileId = activeProfileId,
            payMonthDate = payMonthTimestamp,
            shoppingDate = shoppingDateTimestamp,
            title = title,
            status = "OPEN",
            totalBudgetCost = totalBudget,
            totalActualCost = 0.0
        )
        val listId = sListDao.insert(sList)

        val listItems = selectedBudgetItems.map { budgetItem ->
            ShoppingListItemEntity(
                shoppingListId = listId,
                budgetItemId = budgetItem.id,
                category = budgetItem.category,
                subCategory = budgetItem.subCategory,
                itemDetail = budgetItem.itemDetail,
                unitSize = budgetItem.unitSize,
                quantityBudget = budgetItem.quantityBudget,
                unitPriceBudget = budgetItem.unitPriceBudget,
                quantityActual = budgetItem.quantityBudget,
                unitPriceActual = budgetItem.unitPriceBudget,
                isChecked = false
            )
        }
        sItemDao.insertAll(listItems)
        return listId
    }

    suspend fun updateShoppingListItem(item: ShoppingListItemEntity) {
        shoppingListItemDao?.update(item)
    }

    suspend fun confirmAndCloseShoppingList(
        shoppingListId: Long,
        createExpenseTransaction: Boolean = true
    ) {
        val sListDao = shoppingListDao ?: return
        val sItemDao = shoppingListItemDao ?: return
        val bDao = groceryBudgetDao ?: return

        val shoppingList = sListDao.getById(shoppingListId) ?: return
        val items = sItemDao.getForList(shoppingListId)
        val checkedItems = items.filter { it.isChecked }

        var totalActualSpent = 0.0

        checkedItems.forEach { sItem ->
            val actualItemCost = sItem.quantityActual * sItem.unitPriceActual
            totalActualSpent += actualItemCost

            if (sItem.budgetItemId != null) {
                val budgetItem = bDao.getById(sItem.budgetItemId)
                if (budgetItem != null) {
                    val updatedQtyActual = budgetItem.quantityActual + sItem.quantityActual
                    val updatedUnitPriceActual = if (sItem.unitPriceActual > 0) sItem.unitPriceActual else budgetItem.unitPriceActual
                    val updatedCostActual = updatedQtyActual * updatedUnitPriceActual

                    bDao.update(
                        budgetItem.copy(
                            quantityActual = updatedQtyActual,
                            unitPriceActual = updatedUnitPriceActual,
                            costActual = updatedCostActual
                        )
                    )
                }
            }
        }

        sItemDao.deleteUncheckedItems(shoppingListId)

        sListDao.update(
            shoppingList.copy(
                status = "CLOSED",
                totalActualCost = totalActualSpent
            )
        )

        if (createExpenseTransaction && totalActualSpent > 0.0) {
            val categories = categoryDao.getAllCategories(activeProfileId)
            val groceriesCat = categories.find { it.name.equals("Groceries", ignoreCase = true) }
                ?: categories.find { it.type == TransactionType.EXPENSE }

            if (groceriesCat != null) {
                val txn = TransactionEntity(
                    profileId = activeProfileId,
                    date = shoppingList.shoppingDate,
                    amount = totalActualSpent,
                    type = TransactionType.EXPENSE,
                    categoryId = groceriesCat.id,
                    subCategory = "Groceries",
                    detail = shoppingList.title,
                    note = "Logged from Shopping List: ${shoppingList.title}",
                    sortOrder = 0
                )
                transactionDao.insert(txn)
            }
        }
    }

    suspend fun deleteShoppingList(shoppingList: ShoppingListEntity) {
        shoppingListDao?.delete(shoppingList)
    }
}

data class MonthlySummary(
    val income: Double = 0.0,
    val investment: Double = 0.0,
    val education: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0
)
