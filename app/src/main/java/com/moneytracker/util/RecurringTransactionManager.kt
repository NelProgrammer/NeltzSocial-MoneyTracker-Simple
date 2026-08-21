package com.moneytracker.util

import com.moneytracker.data.local.entity.RecurrenceFrequency
import com.moneytracker.data.local.entity.TransactionEntity
import com.moneytracker.data.repository.TransactionRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Utility manager for recurrence calculations and automatic recurring transaction generation.
 * - Thread-safe guarded with Mutex to prevent perpetual or concurrent calculations.
 * - Deduplicates recurring templates so each distinct item (Category, SubCategory, Detail) generates exactly one child.
 * - Current Month (M0) entries are materialized when cutoff has passed.
 * - Next Month (M+1) entries calculated BEFORE cutoff are solely for budget view:
 *   they are captured with null/blank recurrence fields (isRecurring = false, recurrenceFrequency = null).
 * - When recurrence recalculations run, any stale or orphaned empty/tentative entries are dropped.
 * - Once cutoff arrives, entries are materialized with active recurrence fields and parent is marked isRecurred = true.
 */
object RecurringTransactionManager {

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
        if (!calculationMutex.tryLock()) {
            return // Prevent perpetual loop / concurrent runs
        }
        try {
            repository.sanitizeLegacyData()
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

                    val allEntities = repository.getAllEntitiesForProfile(pid).toMutableList()

                    // Distinct active recurring parent templates: pick the latest date for each unique (categoryId, subCategory, detail)
                    val recurringTemplates = allEntities
                        .filter { it.isRecurring }
                        .groupBy { "${it.categoryId}|||${it.subCategory.trim().lowercase()}|||${it.detail.trim().lowercase()}" }
                        .mapValues { (_, list) -> list.maxByOrNull { it.date }!! }
                        .values

                    // Clean up orphaned tentative forecast entries (whose recurring parent was canceled/deleted)
                    val tentativeInNextMonth = allEntities.filter { txn ->
                        txn.date in nextStartMillis until nextEndMillis &&
                        txn.recurrenceFrequency == RecurrenceFrequency.TENTATIVE_FORECAST
                    }

                    for (tentative in tentativeInNextMonth) {
                        val hasMatchingParent = recurringTemplates.any { parent ->
                            parent.categoryId == tentative.categoryId &&
                            parent.subCategory.equals(tentative.subCategory, ignoreCase = true) &&
                            parent.detail.equals(tentative.detail, ignoreCase = true)
                        }
                        if (!hasMatchingParent) {
                            repository.deleteTransaction(tentative)
                            allEntities.remove(tentative)
                        }
                    }

                    for (parent in recurringTemplates) {
                        val created = processSingleParent(repository, parent, allEntities)
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

    suspend fun processSingleTransactionRecurrence(
        repository: TransactionRepository,
        parent: TransactionEntity
    ) {
        calculationMutex.withLock {
            val allTransactions = repository.getAllEntitiesForProfile(parent.profileId).toMutableList()
            processSingleParent(repository, parent, allTransactions)
        }
    }

    private suspend fun processSingleParent(
        repository: TransactionRepository,
        parent: TransactionEntity,
        allTransactions: MutableList<TransactionEntity>
    ): Boolean {
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
        if (candidateDate.isAfter(nextPayMonthEnd)) return false

        val candidateEpoch = DateUtils.toEpochMillis(candidateDate)
        val targetMonthStartMillis = DateUtils.startOfPayMonth(targetMonth, payDateDay)
        val targetMonthEndMillis = DateUtils.startOfNextPayMonth(targetMonth, payDateDay)

        // If parent recurrence was turned off
        if (!parent.isRecurring) {
            val existingChild = allTransactions.firstOrNull { existing ->
                existing.categoryId == parent.categoryId &&
                existing.subCategory.equals(parent.subCategory, ignoreCase = true) &&
                existing.detail.equals(parent.detail, ignoreCase = true) &&
                existing.date in targetMonthStartMillis until targetMonthEndMillis &&
                existing.recurrenceFrequency == RecurrenceFrequency.TENTATIVE_FORECAST
            }
            if (existingChild != null) {
                repository.deleteTransaction(existingChild)
                allTransactions.remove(existingChild)
            }
            return false
        }

        // Respect user-specified end date if applicable
        if (maxTillDate != null && candidateDate.isAfter(maxTillDate)) {
            if (!parent.isRecurred) {
                repository.saveTransaction(parent.copy(isRecurred = true))
            }
            return false
        }

        // Find existing child in target month by category, subcategory & detail
        val existingChild = allTransactions.firstOrNull { existing ->
            existing.categoryId == parent.categoryId &&
            existing.subCategory.equals(parent.subCategory, ignoreCase = true) &&
            existing.detail.equals(parent.detail, ignoreCase = true) &&
            existing.date in targetMonthStartMillis until targetMonthEndMillis
        }

        val isBeforeCutoff = isBeforeCutoffForDate(candidateDate, today)

        val isContinuous = parent.recurrenceFrequency == RecurrenceFrequency.CONTINUOUS
        val isPlanFuture = parent.recurrenceFrequency == RecurrenceFrequency.PLAN_FUTURE
        val remainingCount = parent.recurCount?.let { (it - 1).coerceAtLeast(0) }

        val nextIsRecurring = when {
            isBeforeCutoff -> false // Before cutoff: tentative forecast
            isContinuous -> true
            isPlanFuture -> true
            remainingCount != null && remainingCount <= 1 -> false
            maxTillDate != null && candidateDate.isEqual(maxTillDate) -> false
            else -> parent.isRecurring
        }

        val nextRecurrenceFrequency = if (isBeforeCutoff) RecurrenceFrequency.TENTATIVE_FORECAST else parent.recurrenceFrequency
        val nextRecurTillDate = if (isBeforeCutoff) null else parent.recurTillDate
        val nextRecurCount = if (isBeforeCutoff) null else remainingCount
        val nextIsRecurred = if (isBeforeCutoff) false else !nextIsRecurring

        if (existingChild != null) {
            val shouldPropagateParent = parent.updatedAt > existingChild.updatedAt
            val finalAmount = if (shouldPropagateParent) kotlin.math.abs(parent.amount) else (if (existingChild.amount > 0.0) existingChild.amount else kotlin.math.abs(parent.amount))
            val finalNote = if (shouldPropagateParent) parent.note else (if (existingChild.note.isNotBlank()) existingChild.note else parent.note)
            val updatedChild = existingChild.copy(
                amount = finalAmount,
                type = parent.type,
                categoryId = parent.categoryId,
                note = finalNote,
                subCategory = parent.subCategory,
                detail = parent.detail,
                isRecurring = nextIsRecurring,
                recurrenceFrequency = nextRecurrenceFrequency,
                recurTillDate = nextRecurTillDate,
                recurCount = nextRecurCount,
                isRecurred = nextIsRecurred,
                updatedAt = existingChild.updatedAt
            )
            repository.saveTransactionDirect(updatedChild)
            val index = allTransactions.indexOfFirst { it.id == existingChild.id }
            if (index >= 0) allTransactions[index] = updatedChild
            return false
        } else {
            // Insert new budget view / recurrence child
            val newChild = TransactionEntity(
                profileId = parent.profileId,
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

            // Only mark current parent as recurred if cutoff milestone has been reached
            if (!isBeforeCutoff && !parent.isRecurred) {
                repository.saveTransaction(parent.copy(isRecurred = true))
            }
            return true
        }
    }
}
