package com.moneytracker.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    fun format(amount: Double): String = formatter.format(amount)
}
