package com.moneytracker.util

import java.util.Locale

object CurrencyUtils {
    fun format(amount: Double): String {
        val formatted = String.format(Locale.US, "%,.2f", amount)
        return "R $formatted"
    }

    fun formatZar(amount: Double): String = format(amount)
}
