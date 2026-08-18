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
import kotlinx.coroutines.flow.firstOrNull
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
        val pid = if (transaction.id == 0L || transaction.profileId <= 0L) activeProfileId else transaction.profileId
        val sortOrder = if (transaction.id == 0L) transactionDao.nextSortOrder(pid) else transaction.sortOrder
        val entity = transaction.copy(
            profileId = pid,
            sortOrder = sortOrder,
            amount = kotlin.math.abs(transaction.amount)
        )
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
        val pid = if (category.id == 0L || category.profileId <= 0L) activeProfileId else category.profileId
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
        val pid = if (subCategory.id == 0L || subCategory.profileId <= 0L) activeProfileId else subCategory.profileId
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
        val pid = if (detail.id == 0L || detail.profileId <= 0L) activeProfileId else detail.profileId
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

    suspend fun sanitizeLegacyData() {
        try {
            val all = transactionDao.getAllEntities(activeProfileId)
            for (t in all) {
                if (t.amount < 0) {
                    transactionDao.update(t.copy(amount = kotlin.math.abs(t.amount)))
                }
            }
            val currentSubs = subCategoryDao.observeAll(activeProfileId).firstOrNull() ?: emptyList()
            if (currentSubs.isEmpty() && activeProfileId != 1L) {
                val profile1Subs = subCategoryDao.observeAll(1L).firstOrNull() ?: emptyList()
                for (s in profile1Subs) {
                    subCategoryDao.insert(s.copy(id = 0L, profileId = activeProfileId))
                }
            }
        } catch (e: Exception) {}
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
        val dao = taxiFareDao ?: return 0L
        val pid = if (fare.profileId == 0L) activeProfileId else fare.profileId
        val entity = fare.copy(profileId = pid)
        return if (entity.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            entity.id
        }
    }

    suspend fun deleteTaxiFare(fare: TaxiFareEntity) {
        taxiFareDao?.delete(fare)
    }

    // Grocery Budget Operations
    fun observeGroceryBudgetForMonth(startDate: Long, endDate: Long): Flow<List<GroceryBudgetItemEntity>> =
        groceryBudgetDao?.observeForMonth(activeProfileId, startDate, endDate) ?: flowOf(emptyList())

    suspend fun getGroceryBudgetForMonth(startDate: Long, endDate: Long): List<GroceryBudgetItemEntity> =
        groceryBudgetDao?.getForMonth(activeProfileId, startDate, endDate) ?: emptyList()

    suspend fun getGroceryBudgetItemById(id: Long): GroceryBudgetItemEntity? =
        groceryBudgetDao?.getById(id)

    suspend fun autoPopulateRecurringAndPlannedGroceryItems(startDate: Long, endDate: Long, targetDate: Long) {
        val dao = groceryBudgetDao ?: return
        val currentMonthItems = dao.getForMonth(activeProfileId, startDate, endDate)
        val allRecurringAndPlanned = dao.getRecurringAndPlannedItems(activeProfileId)

        // Deduplicate templates before startDate: pick the most recent one for each (category, subCategory, itemDetail)
        val templates = allRecurringAndPlanned
            .filter { it.date < startDate }
            .groupBy { "${it.category.trim().lowercase()}|||${it.subCategory.trim().lowercase()}|||${it.itemDetail.trim().lowercase()}" }
            .mapValues { (_, list) -> list.maxByOrNull { it.date }!! }
            .values

        val toCopy = templates.filter { template ->
            currentMonthItems.none { existing ->
                existing.category.equals(template.category, ignoreCase = true) &&
                existing.subCategory.equals(template.subCategory, ignoreCase = true) &&
                existing.itemDetail.equals(template.itemDetail, ignoreCase = true)
            }
        }.map { template ->
            template.copy(
                id = 0,
                profileId = activeProfileId,
                date = targetDate,
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
        val existing = if (item.id != 0L) dao.getById(item.id) else null
        val qA = if (item.quantityActual > 0) item.quantityActual else (existing?.quantityActual ?: 0)
        val pA = if (item.unitPriceActual > 0.0) item.unitPriceActual else (existing?.unitPriceActual ?: 0.0)
        val cA = if (item.costActual > 0.0) item.costActual else (qA * pA)
        val costB = item.quantityBudget * item.unitPriceBudget
        val entity = item.copy(profileId = pid, costBudget = costB, quantityActual = qA, unitPriceActual = pA, costActual = cA)
        return if (entity.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            entity.id
        }
    }

    suspend fun updateGroceryBudgetItemWithRecurrenceChange(
        existingItemId: Long,
        item: GroceryBudgetItemEntity,
        currentMonthStart: Long
    ): Long {
        val dao = groceryBudgetDao ?: return 0L
        val pid = if (item.profileId == 0L) activeProfileId else item.profileId
        val existing = if (existingItemId != 0L) dao.getById(existingItemId) else null
        val qA = if (item.quantityActual > 0) item.quantityActual else (existing?.quantityActual ?: 0)
        val pA = if (item.unitPriceActual > 0.0) item.unitPriceActual else (existing?.unitPriceActual ?: 0.0)
        val cA = if (item.costActual > 0.0) item.costActual else (qA * pA)
        val costB = item.quantityBudget * item.unitPriceBudget
        val entity = item.copy(
            id = existingItemId,
            profileId = pid,
            costBudget = costB,
            quantityActual = qA,
            unitPriceActual = pA,
            costActual = cA
        )

        val id = if (existingItemId == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            existingItemId
        }

        // Amend older active recurring templates of the same item before current month to isRecurring = 0
        val allItems = dao.getAllForProfile(pid)
        val olderDuplicates = allItems.filter {
            it.id != id &&
            it.date < currentMonthStart &&
            it.isRecurring != 0 &&
            it.category.equals(item.category, ignoreCase = true) &&
            it.subCategory.equals(item.subCategory, ignoreCase = true) &&
            it.itemDetail.equals(item.itemDetail, ignoreCase = true)
        }
        olderDuplicates.forEach { older ->
            dao.update(older.copy(isRecurring = 0))
        }

        return id
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
    fun observeShoppingListsForMonth(startDate: Long, endDate: Long): Flow<List<ShoppingListEntity>> =
        shoppingListDao?.observeForMonth(activeProfileId, startDate, endDate) ?: flowOf(emptyList())

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

    suspend fun reopenShoppingList(shoppingListId: Long) {
        val sListDao = shoppingListDao ?: return
        val sItemDao = shoppingListItemDao ?: return
        val bDao = groceryBudgetDao ?: return

        val shoppingList = sListDao.getById(shoppingListId) ?: return
        if (shoppingList.status != "CLOSED") return

        val items = sItemDao.getForList(shoppingListId)
        val checkedItems = items.filter { it.isChecked }

        // Subtract checked item actuals from the associated grocery budget items
        checkedItems.forEach { sItem ->
            if (sItem.budgetItemId != null) {
                val budgetItem = bDao.getById(sItem.budgetItemId)
                if (budgetItem != null) {
                    val updatedQtyActual = (budgetItem.quantityActual - sItem.quantityActual).coerceAtLeast(0)
                    val updatedCostActual = updatedQtyActual * budgetItem.unitPriceActual
                    bDao.update(
                        budgetItem.copy(
                            quantityActual = updatedQtyActual,
                            costActual = updatedCostActual
                        )
                    )
                }
            }
        }

        // Delete the expense transaction that was logged on close (if any)
        val noteText = "Logged from Shopping List: ${shoppingList.title}"
        val allTxns = transactionDao.getAllEntities(activeProfileId)
        val matchingTxn = allTxns.find { it.note.equals(noteText, ignoreCase = true) && it.date == shoppingList.shoppingDate }
        if (matchingTxn != null) {
            transactionDao.delete(matchingTxn)
        }

        sListDao.update(
            shoppingList.copy(
                status = "OPEN",
                totalActualCost = 0.0
            )
        )
    }

    suspend fun getShoppingListById(shoppingListId: Long): ShoppingListEntity? =
        shoppingListDao?.getById(shoppingListId)

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
