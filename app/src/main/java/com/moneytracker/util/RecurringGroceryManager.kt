package com.moneytracker.util

import com.moneytracker.data.local.entity.GroceryBudgetItemEntity
import com.moneytracker.data.repository.TransactionRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Utility manager for Grocery recurrence calculations and automatic recurring grocery item generation.
 * - Thread-safe guarded with Mutex to prevent perpetual or concurrent calculations.
 * - Deduplicates recurring templates so each distinct item (Category, SubCategory, ItemDetail) generates exactly one child.
 * - Current Month (M0) entries are materialized when cutoff has passed.
 * - Next Month (M+1) entries calculated BEFORE cutoff are solely for budget view:
 *   they are captured with tentative once-off fields (isRecurring = 0, actuals = 0).
 * - When recurrence recalculations run, any stale or orphaned empty/tentative entries are dropped.
 * - Once cutoff arrives, entries in Next Month are materialized with active recurring status (isRecurring = 1).
 */
object RecurringGroceryManager {

    private val calculationMutex = Mutex()

    fun isBeforeCutoffForDate(targetPayMonthDate: LocalDate, today: LocalDate = LocalDate.now()): Boolean {
        val cutoffDay = SettingsManager.getCutoffDay()
        val payDateDay = SettingsManager.getPayDateDay()

        // Calculate the exact cutoff date for the target pay month cycle
        val cutoffDate = if (cutoffDay <= payDateDay) {
            targetPayMonthDate.withDayOfMonth(cutoffDay.coerceAtMost(targetPayMonthDate.lengthOfMonth()))
        } else {
            val prevMonth = targetPayMonthDate.minusMonths(1)
            prevMonth.withDayOfMonth(cutoffDay.coerceAtMost(prevMonth.lengthOfMonth()))
        }

        return today.isBefore(cutoffDate)
    }

    suspend fun processRecurringGroceryItemsIfDue(repository: TransactionRepository) {
        processRecurringGroceryItems(repository)
    }

    suspend fun processRecurringGroceryItems(repository: TransactionRepository) {
        if (!calculationMutex.tryLock()) {
            return // Prevent perpetual loop / concurrent runs
        }
        try {
            val today = LocalDate.now()
            val payDateDay = SettingsManager.getPayDateDay()

            val currentPayMonthStart = DateUtils.currentPayMonthLocalDate(today, payDateDay)
            val nextPayMonthStart = currentPayMonthStart.plusMonths(1)
            val nextStartMillis = DateUtils.startOfPayMonth(nextPayMonthStart, payDateDay)
            val nextEndMillis = DateUtils.startOfNextPayMonth(nextPayMonthStart, payDateDay)

            val profiles = repository.getAllProfileEntities()
            val profileIds = if (profiles.isNotEmpty()) profiles.map { it.id }.distinct() else listOf(repository.activeProfileId)

            for (pid in profileIds) {
                var passes = 0
                var hasNewCreations = true

                while (hasNewCreations && passes < 3) {
                    hasNewCreations = false
                    passes++

                    val allItems = repository.getAllGroceryBudgetItemsForProfile(pid).toMutableList()

                    // Distinct active recurring parent templates (1 = Monthly, 2 = Planned)
                    val recurringTemplates = allItems
                        .filter { it.isRecurring == 1 || it.isRecurring == 2 }
                        .groupBy { "${it.category.trim().lowercase()}|||${it.subCategory.trim().lowercase()}|||${it.itemDetail.trim().lowercase()}" }
                        .mapValues { (_, list) -> list.maxByOrNull { it.date }!! }
                        .values

                    // Clean up orphaned tentative forecast entries (isRecurring == 3) whose recurring parent was deleted/canceled
                    val tentativeInNextMonth = allItems.filter { item ->
                        item.date in nextStartMillis until nextEndMillis &&
                        item.isRecurring == 3
                    }

                    for (tentative in tentativeInNextMonth) {
                        val hasMatchingParent = recurringTemplates.any { parent ->
                            parent.category.equals(tentative.category, ignoreCase = true) &&
                            parent.subCategory.equals(tentative.subCategory, ignoreCase = true) &&
                            parent.itemDetail.equals(tentative.itemDetail, ignoreCase = true)
                        }
                        if (!hasMatchingParent) {
                            repository.deleteGroceryBudgetItemDirect(tentative)
                            allItems.remove(tentative)
                        }
                    }

                    // Process each active recurring parent
                    for (parent in recurringTemplates) {
                        val created = processSingleGroceryParent(repository, parent, allItems)
                        if (created) {
                            hasNewCreations = true
                        }
                    }
                }
            }
        } finally {
            calculationMutex.unlock()
        }
    }

    suspend fun processSingleGroceryItemRecurrence(
        repository: TransactionRepository,
        parent: GroceryBudgetItemEntity
    ) {
        calculationMutex.withLock {
            val allItems = repository.getAllGroceryBudgetItemsForProfile(parent.profileId).toMutableList()
            processSingleGroceryParent(repository, parent, allItems)
        }
    }

    private suspend fun processSingleGroceryParent(
        repository: TransactionRepository,
        parent: GroceryBudgetItemEntity,
        allItems: MutableList<GroceryBudgetItemEntity>
    ): Boolean {
        val startDate = DateUtils.toLocalDate(parent.date)

        val today = LocalDate.now()
        val payDateDay = SettingsManager.getPayDateDay()

        val currentPayMonthStart = DateUtils.currentPayMonthLocalDate(today, payDateDay)
        val nextPayMonthEnd = currentPayMonthStart.plusMonths(2).minusDays(1)

        val targetMonth = startDate.plusMonths(1)
        val candidateDay = payDateDay.coerceAtMost(targetMonth.lengthOfMonth())
        val candidateDate = targetMonth.withDayOfMonth(candidateDay)

        // Stop if candidateDate is beyond the 2-month horizon
        if (candidateDate.isAfter(nextPayMonthEnd)) return false

        val candidateEpoch = DateUtils.toEpochMillis(candidateDate)
        val targetMonthStartMillis = DateUtils.startOfPayMonth(targetMonth, payDateDay)
        val targetMonthEndMillis = DateUtils.startOfNextPayMonth(targetMonth, payDateDay)

        // Find existing child in target month by category, subcategory & itemDetail
        val existingChild = allItems.firstOrNull { existing ->
            existing.date in targetMonthStartMillis until targetMonthEndMillis &&
            existing.category.equals(parent.category, ignoreCase = true) &&
            existing.subCategory.equals(parent.subCategory, ignoreCase = true) &&
            existing.itemDetail.equals(parent.itemDetail, ignoreCase = true)
        }

        // If parent recurrence was turned off (isRecurring == 0)
        if (parent.isRecurring == 0) {
            if (existingChild != null && existingChild.isRecurring == 3 && existingChild.quantityActual == 0 && existingChild.unitPriceActual == 0.0) {
                repository.deleteGroceryBudgetItemDirect(existingChild)
                allItems.remove(existingChild)
            }
            return false
        }

        val isBeforeCutoff = isBeforeCutoffForDate(candidateDate, today)
        val nextIsRecurring = if (isBeforeCutoff) 3 else parent.isRecurring // 3 = Tentative Forecast

        val costB = parent.quantityBudget * parent.unitPriceBudget

        if (existingChild != null) {
            // Only update if existing child has not been bought/ticked yet
            if (existingChild.quantityActual == 0 && existingChild.unitPriceActual == 0.0) {
                val shouldPropagateParent = parent.updatedAt > existingChild.updatedAt
                val qB = if (shouldPropagateParent) parent.quantityBudget else (if (existingChild.quantityBudget > 0) existingChild.quantityBudget else parent.quantityBudget)
                val pB = if (shouldPropagateParent) parent.unitPriceBudget else (if (existingChild.unitPriceBudget > 0.0) existingChild.unitPriceBudget else parent.unitPriceBudget)
                val costB = qB * pB
                val updatedChild = existingChild.copy(
                    category = parent.category,
                    subCategory = parent.subCategory,
                    itemDetail = parent.itemDetail,
                    unitSize = if (shouldPropagateParent) parent.unitSize else (if (existingChild.unitSize.isNotBlank()) existingChild.unitSize else parent.unitSize),
                    note = if (shouldPropagateParent) parent.note else (if (existingChild.note.isNotBlank()) existingChild.note else parent.note),
                    quantityBudget = qB,
                    unitPriceBudget = pB,
                    costBudget = costB,
                    isRecurring = nextIsRecurring,
                    updatedAt = existingChild.updatedAt
                )
                repository.saveGroceryBudgetItemDirect(updatedChild)
                val index = allItems.indexOfFirst { it.id == existingChild.id }
                if (index >= 0) allItems[index] = updatedChild
            }
            return false
        } else {
            // Insert new next month budget view / recurring child
            val newChild = GroceryBudgetItemEntity(
                profileId = parent.profileId,
                date = candidateEpoch,
                category = parent.category,
                subCategory = parent.subCategory,
                itemDetail = parent.itemDetail,
                unitSize = parent.unitSize,
                note = parent.note,
                quantityBudget = parent.quantityBudget,
                unitPriceBudget = parent.unitPriceBudget,
                costBudget = costB,
                isRecurring = nextIsRecurring,
                quantityActual = 0,
                unitPriceActual = 0.0,
                costActual = 0.0
            )
            val insertedId = repository.saveGroceryBudgetItemDirect(newChild)
            allItems.add(newChild.copy(id = insertedId))
            return true
        }
    }
}
