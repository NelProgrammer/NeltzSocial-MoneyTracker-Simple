package com.moneytracker.util

import com.moneytracker.data.local.entity.RecurrenceFrequency
import com.moneytracker.data.local.entity.TransactionEntity
import com.moneytracker.data.repository.TransactionRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Utility manager for recurrence calculations and automatic recurring transaction generation.
 * - Next Month entries calculated BEFORE cutoff are solely for budget view:
 *   they are captured with null/blank recurrence fields (isRecurring = false, recurrenceFrequency = null).
 * - When recurrence recalculations run, any stale or orphaned empty/tentative entries are dropped.
 * - Once cutoff arrives, entries are materialized with active recurrence fields and parent is marked isRecurred = true.
 */
object RecurringTransactionManager {

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

    suspend fun processRecurringTransactionsIfDue(repository: TransactionRepository) {
        processRecurringTransactions(repository)
    }

    fun calculateTillDate(startDate: LocalDate, freq: RecurrenceFrequency = RecurrenceFrequency.MONTHLY, count: Int): LocalDate {
        val steps = (count - 1).coerceAtLeast(0).toLong()
        return startDate.plusMonths(steps)
    }

    fun calculateCount(startDate: LocalDate, freq: RecurrenceFrequency = RecurrenceFrequency.MONTHLY, tillDate: LocalDate): Long {
        if (tillDate.isBefore(startDate)) return 1L
        val diff = ChronoUnit.MONTHS.between(startDate, tillDate)
        return (diff + 1).coerceAtLeast(1L)
    }

    suspend fun processRecurringTransactions(repository: TransactionRepository) {
        repository.sanitizeLegacyData()
        val recurringParents = repository.getRecurringTransactions()
        val allTransactions = repository.getAllEntities().toMutableList()

        val today = LocalDate.now()
        val payDateDay = SettingsManager.getPayDateDay()
        val currentPayMonthStart = DateUtils.currentPayMonthLocalDate(today, payDateDay)
        val nextPayMonthStart = currentPayMonthStart.plusMonths(1)
        val nextStartMillis = DateUtils.startOfPayMonth(nextPayMonthStart, payDateDay)
        val nextEndMillis = DateUtils.startOfNextPayMonth(nextPayMonthStart, payDateDay)

        // Drop orphaned tentative entries in next month that have null recurrence and no matching active recurring parent
        val tentativeInNextMonth = allTransactions.filter { txn ->
            txn.date in nextStartMillis until nextEndMillis &&
            !txn.isRecurring &&
            txn.recurrenceFrequency == null &&
            !txn.isRecurred
        }

        for (tentative in tentativeInNextMonth) {
            val hasMatchingParent = recurringParents.any { parent ->
                parent.categoryId == tentative.categoryId &&
                parent.subCategory.equals(tentative.subCategory, ignoreCase = true) &&
                parent.detail.equals(tentative.detail, ignoreCase = true)
            }
            if (!hasMatchingParent) {
                repository.deleteTransaction(tentative)
                allTransactions.remove(tentative)
            }
        }

        for (parent in recurringParents) {
            processSingleParent(repository, parent, allTransactions)
        }
    }

    suspend fun processSingleTransactionRecurrence(
        repository: TransactionRepository,
        parent: TransactionEntity
    ) {
        val allTransactions = repository.getAllEntities().toMutableList()
        processSingleParent(repository, parent, allTransactions)
    }

    private suspend fun processSingleParent(
        repository: TransactionRepository,
        parent: TransactionEntity,
        allTransactions: MutableList<TransactionEntity>
    ) {
        val startDate = DateUtils.toLocalDate(parent.date)
        val maxTillDate = parent.recurTillDate?.let { DateUtils.toLocalDate(it) }

        val today = LocalDate.now()
        val payDateDay = SettingsManager.getPayDateDay()

        val currentPayMonthStart = DateUtils.currentPayMonthLocalDate(today, payDateDay)
        val nextPayMonthEnd = currentPayMonthStart.plusMonths(2).minusDays(1)

        val targetMonth = startDate.plusMonths(1)
        val candidateDay = payDateDay.coerceAtMost(targetMonth.lengthOfMonth())
        val candidateDate = targetMonth.withDayOfMonth(candidateDay)

        // Stop if candidateDate is beyond the 2-month horizon
        if (candidateDate.isAfter(nextPayMonthEnd)) return

        val candidateEpoch = DateUtils.toEpochMillis(candidateDate)

        // If parent recurrence was turned off
        if (!parent.isRecurring) {
            val existingChild = allTransactions.firstOrNull { existing ->
                existing.categoryId == parent.categoryId &&
                existing.subCategory.equals(parent.subCategory, ignoreCase = true) &&
                existing.detail.equals(parent.detail, ignoreCase = true) &&
                DateUtils.toLocalDate(existing.date) == candidateDate
            }
            if (existingChild != null) {
                repository.deleteTransaction(existingChild)
                allTransactions.remove(existingChild)
            }
            return
        }

        // Respect user-specified end date if applicable
        if (maxTillDate != null && candidateDate.isAfter(maxTillDate)) {
            repository.saveTransaction(parent.copy(isRecurred = true))
            return
        }

        // Find existing child on candidateDate by category, subcategory & detail
        val existingChild = allTransactions.firstOrNull { existing ->
            existing.categoryId == parent.categoryId &&
            existing.subCategory.equals(parent.subCategory, ignoreCase = true) &&
            existing.detail.equals(parent.detail, ignoreCase = true) &&
            DateUtils.toLocalDate(existing.date) == candidateDate
        }

        val isBeforeCutoff = isBeforeCutoffForDate(candidateDate, today)

        val isContinuous = parent.recurrenceFrequency == RecurrenceFrequency.CONTINUOUS
        val isPlanFuture = parent.recurrenceFrequency == RecurrenceFrequency.PLAN_FUTURE
        val remainingCount = parent.recurCount?.let { (it - 1).coerceAtLeast(0) }

        val nextIsRecurring = when {
            isBeforeCutoff -> false // Before cutoff: null/empty recurrence solely for budget view
            isContinuous -> true
            isPlanFuture -> true
            remainingCount != null && remainingCount <= 1 -> false
            maxTillDate != null && candidateDate.isEqual(maxTillDate) -> false
            else -> parent.isRecurring
        }

        val nextRecurrenceFrequency = if (isBeforeCutoff) null else parent.recurrenceFrequency
        val nextRecurTillDate = if (isBeforeCutoff) null else parent.recurTillDate
        val nextRecurCount = if (isBeforeCutoff) null else remainingCount
        val nextIsRecurred = if (isBeforeCutoff) false else !nextIsRecurring

        if (existingChild != null) {
            // Update the existing next month child with the latest amount, notes, and calculated recurrence fields
            val updatedChild = existingChild.copy(
                amount = kotlin.math.abs(parent.amount),
                type = parent.type,
                categoryId = parent.categoryId,
                note = parent.note,
                subCategory = parent.subCategory,
                detail = parent.detail,
                isRecurring = nextIsRecurring,
                recurrenceFrequency = nextRecurrenceFrequency,
                recurTillDate = nextRecurTillDate,
                recurCount = nextRecurCount,
                isRecurred = nextIsRecurred
            )
            repository.saveTransaction(updatedChild)
            val index = allTransactions.indexOfFirst { it.id == existingChild.id }
            if (index >= 0) allTransactions[index] = updatedChild
        } else {
            // Insert new next month budget view / recurrence child
            val newChild = TransactionEntity(
                amount = kotlin.math.abs(parent.amount),
                type = parent.type,
                categoryId = parent.categoryId,
                date = candidateEpoch,
                note = parent.note,
                subCategory = parent.subCategory,
                detail = parent.detail,
                isRecurring = nextIsRecurring,
                recurrenceFrequency = nextRecurrenceFrequency,
                recurTillDate = nextRecurTillDate,
                recurCount = nextRecurCount,
                isRecurred = nextIsRecurred
            )
            val insertedId = repository.saveTransaction(newChild)
            allTransactions.add(newChild.copy(id = insertedId))
        }

        // Only mark current parent as recurred if cutoff milestone has been reached
        if (!isBeforeCutoff) {
            repository.saveTransaction(parent.copy(isRecurred = true))
        }
    }
}
