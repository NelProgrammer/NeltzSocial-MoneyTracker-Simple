package com.moneytracker.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    /**
     * Returns epoch milliseconds for the start of the PayMonth (PayDate of the month, system local timezone).
     * If date's day of month is >= payDateDay, cycle starts on payDateDay of this month.
     * If date's day of month is < payDateDay, cycle starts on payDateDay of previous month.
     */
    fun startOfPayMonth(date: LocalDate = LocalDate.now(), payDateDay: Int = SettingsManager.getPayDateDay()): Long =
        currentPayMonthLocalDate(date, payDateDay)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /**
     * Returns epoch milliseconds for the start of the next PayMonth (PayDate of next month, system local timezone).
     */
    fun startOfNextPayMonth(date: LocalDate = LocalDate.now(), payDateDay: Int = SettingsManager.getPayDateDay()): Long =
        currentPayMonthLocalDate(date, payDateDay)
            .plusMonths(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /**
     * Returns the LocalDate for the PayDate that marks the start of the current PayMonth cycle.
     */
    fun currentPayMonthLocalDate(date: LocalDate = LocalDate.now(), payDateDay: Int = SettingsManager.getPayDateDay()): LocalDate {
        val targetDay = payDateDay.coerceAtMost(date.lengthOfMonth())
        return if (date.dayOfMonth >= targetDay) {
            date.withDayOfMonth(targetDay)
        } else {
            val prev = date.minusMonths(1)
            prev.withDayOfMonth(payDateDay.coerceAtMost(prev.lengthOfMonth()))
        }
    }

    fun calculateNextPayDate(currentMillis: Long, payDateDay: Int = 20, monthsToAdd: Int = 1): Long {
        val currentDate = toLocalDate(currentMillis)
        val nextDate = currentDate.plusMonths(monthsToAdd.toLong())
        val targetDay = payDateDay.coerceAtMost(nextDate.lengthOfMonth())
        return toEpochMillis(nextDate.withDayOfMonth(targetDay))
    }

    /**
     * Returns the epoch milliseconds for the start of the current calendar month (system local timezone).
     */
    fun startOfMonth(): Long =
        LocalDate.now().let { startOfMonth(it) }

    /**
     * Returns the epoch milliseconds for the start of the next calendar month (system local timezone).
     */
    fun startOfNextMonth(): Long =
        LocalDate.now().let { startOfNextMonth(it) }

    /**
     * Returns the epoch milliseconds for the start of the given calendar month (system local timezone).
     */
    fun startOfMonth(date: LocalDate): Long =
        date.withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /**
     * Returns the epoch milliseconds for the start of the month following the given calendar month (system local timezone).
     */
    fun startOfNextMonth(date: LocalDate): Long =
        date.plusMonths(1)
            .withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun toEpochMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun formatDate(epochMillis: Long): String =
        displayFormatter.format(toLocalDate(epochMillis))

    private val payMonthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

    fun formatPayMonth(date: LocalDate, payDateDay: Int = SettingsManager.getPayDateDay()): String =
        "${date.format(payMonthFormatter)} (${payDateDay}th)"

    fun formatMonth(date: LocalDate): String =
        date.format(payMonthFormatter)
}
