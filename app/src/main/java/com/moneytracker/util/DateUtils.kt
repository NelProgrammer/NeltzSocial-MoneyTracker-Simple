package com.moneytracker.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.Instant
import java.time.format.DateTimeFormatter

object DateUtils {
    private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    /**
     * Returns the epoch milliseconds for the start of the current month (UTC).
     */
    fun startOfMonth(): Long =
        LocalDate.now().let { startOfMonth(it) }

    /**
     * Returns the epoch milliseconds for the start of the next month after the current month (UTC).
     */
    fun startOfNextMonth(): Long =
        LocalDate.now().let { startOfNextMonth(it) }

    /**
     * Returns the epoch milliseconds for the start of the given month (UTC).
     */
    fun startOfMonth(date: LocalDate): Long =
        date.withDayOfMonth(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    /**
     * Returns the epoch milliseconds for the start of the month following the given month (UTC).
     */
    fun startOfNextMonth(date: LocalDate): Long =
        date.plusMonths(1)
            .withDayOfMonth(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun toEpochMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun formatDate(epochMillis: Long): String =
        displayFormatter.format(toLocalDate(epochMillis))
}

