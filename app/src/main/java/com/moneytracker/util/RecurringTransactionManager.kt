package com.moneytracker.util

import com.moneytracker.data.local.entity.RecurrenceFrequency
import com.moneytracker.data.local.entity.TransactionEntity
import com.moneytracker.data.repository.TransactionRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Utility manager for monthly recurrence calculations and automatic recurring transaction generation.
 * - Recurrence cycle starts on PayDate (default: 20th of the month, editable in Settings).
 * - Scheduled calculation milestone: Calculation Cutoff Day (default: 18th of the month, editable in Settings).
 * - Generated recurring child instances fix candidate dates to the PayDate (e.g. 20th) of each target month.
 * - Stores last calculation date to eliminate duplicate effort.
 * - Generates instances for the current PayMonth and the next PayMonth (budgeting window).
 */
object RecurringTransactionManager {

    private val lastCheckDate = AtomicReference<LocalDate?>(null)

    /**
     * Returns the most recent calculation cutoff milestone date based on user settings.
     */
    fun getMostRecentCutoffMilestone(today: LocalDate = LocalDate.now()): LocalDate {
        val cutoffDay = SettingsManager.getCutoffDay()
        val day = cutoffDay.coerceAtMost(today.lengthOfMonth())
        return if (today.dayOfMonth >= day) {
            today.withDayOfMonth(day)
        } else {
            val prev = today.minusMonths(1)
            prev.withDayOfMonth(cutoffDay.coerceAtMost(prev.lengthOfMonth()))
        }
    }

    /**
     * Runs full recurrence calculation sweep ONLY if last calculation date is older than the most recent cutoff milestone.
     */
    suspend fun processRecurringTransactionsIfDue(repository: TransactionRepository) {
        val today = LocalDate.now()
        val milestone = getMostRecentCutoffMilestone(today)
        val lastCheck = lastCheckDate.get()

        if (lastCheck != null && !lastCheck.isBefore(milestone)) {
            return // Already calculated on or after the most recent cutoff milestone!
        }

        processRecurringTransactions(repository)
        lastCheckDate.set(today)
    }

    /**
     * Calculates end date based on starting date and occurrence count (N) for monthly recurrence.
     */
    fun calculateTillDate(startDate: LocalDate, freq: RecurrenceFrequency = RecurrenceFrequency.MONTHLY, count: Int): LocalDate {
        val steps = (count - 1).coerceAtLeast(0).toLong()
        return startDate.plusMonths(steps)
    }

    /**
     * Calculates occurrence count (N) between starting date and end date for monthly recurrence.
     */
    fun calculateCount(startDate: LocalDate, freq: RecurrenceFrequency = RecurrenceFrequency.MONTHLY, tillDate: LocalDate): Long {
        if (tillDate.isBefore(startDate)) return 1L
        val diff = ChronoUnit.MONTHS.between(startDate, tillDate)
        return (diff + 1).coerceAtLeast(1L)
    }

    /**
     * Processes all active recurring transactions for the current PayMonth and next PayMonth.
     */
    suspend fun processRecurringTransactions(repository: TransactionRepository) {
        val allTransactions = repository.getAllEntities()
        cleanupLegacyInvalidChildTransactions(repository, allTransactions)

        val recurringParents = repository.getRecurringTransactions()
        val freshTransactions = repository.getAllEntities()

        for (parent in recurringParents) {
            processSingleParent(repository, parent, freshTransactions)
        }
    }

    /**
     * Triggers recurrence calculation specifically for a single transaction being added or edited on AddEditScreen.
     */
    suspend fun processSingleTransactionRecurrence(
        repository: TransactionRepository,
        parent: TransactionEntity
    ) {
        if (!parent.isRecurring) return
        val allTransactions = repository.getAllEntities()
        processSingleParent(repository, parent, allTransactions)
    }

    private suspend fun cleanupLegacyInvalidChildTransactions(
        repository: TransactionRepository,
        allTransactions: List<TransactionEntity>
    ) {
        val today = LocalDate.now()
        val payDateDay = SettingsManager.getPayDateDay()

        val currentPayMonthStart = DateUtils.currentPayMonthLocalDate(today, payDateDay)
        val nextPayMonthEnd = currentPayMonthStart.plusMonths(2).minusDays(1)

        val invalidChildren = allTransactions.filter { entity ->
            if (entity.isRecurring) return@filter false
            val date = DateUtils.toLocalDate(entity.date)
            val isBeyondNextMonth = date.isAfter(nextPayMonthEnd)
            val isNotPayDate = date.dayOfMonth != payDateDay
            isBeyondNextMonth || isNotPayDate
        }

        for (child in invalidChildren) {
            repository.deleteTransaction(child)
        }
    }

    private suspend fun processSingleParent(
        repository: TransactionRepository,
        parent: TransactionEntity,
        allTransactions: List<TransactionEntity>
    ) {
        val startDate = DateUtils.toLocalDate(parent.date)
        val maxCount = parent.recurCount ?: Int.MAX_VALUE
        val maxTillDate = parent.recurTillDate?.let { DateUtils.toLocalDate(it) }

        val today = LocalDate.now()
        val payDateDay = SettingsManager.getPayDateDay()

        // Horizon covers Current PayMonth and Next PayMonth (PayDate cycle)
        val currentPayMonthStart = DateUtils.currentPayMonthLocalDate(today, payDateDay)
        val nextPayMonthEnd = currentPayMonthStart.plusMonths(2).minusDays(1)

        var step = 1
        while (step < maxCount) {
            val targetMonth = startDate.plusMonths(step.toLong())
            // Fix candidate date for recurring instances to the PayDate (e.g. 20th) of target month
            val candidateDay = payDateDay.coerceAtMost(targetMonth.lengthOfMonth())
            val candidateDate = targetMonth.withDayOfMonth(candidateDay)

            // Do not generate beyond next PayMonth horizon
            if (candidateDate.isAfter(nextPayMonthEnd)) break

            // Respect user-specified max till date
            if (maxTillDate != null && candidateDate.isAfter(maxTillDate)) break

            val candidateEpoch = DateUtils.toEpochMillis(candidateDate)

            // Duplicate check for existing transactions on candidateDate
            val exists = allTransactions.any { existing ->
                existing.categoryId == parent.categoryId &&
                        existing.amount == parent.amount &&
                        existing.subCategory == parent.subCategory &&
                        DateUtils.toLocalDate(existing.date) == candidateDate
            }

            if (!exists) {
                repository.saveTransaction(
                    TransactionEntity(
                        amount = parent.amount,
                        type = parent.type,
                        categoryId = parent.categoryId,
                        date = candidateEpoch,
                        note = parent.note,
                        subCategory = parent.subCategory,
                        isRecurring = false,
                        recurrenceFrequency = null,
                        recurTillDate = null,
                        recurCount = null
                    )
                )
            }

            step++
        }
    }
}
