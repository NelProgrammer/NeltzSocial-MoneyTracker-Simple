package com.moneytracker.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    fun startOfMonth(date: LocalDate = LocalDate.now()): Long =
        date.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun startOfNextMonth(date: LocalDate = LocalDate.now()): Long =
        date.withDayOfMonth(1).plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun toEpochMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun formatDate(epochMillis: Long): String =
        displayFormatter.format(toLocalDate(epochMillis))
}

object CurrencyUtils {
    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    fun format(amount: Double): String = formatter.format(amount)
}
