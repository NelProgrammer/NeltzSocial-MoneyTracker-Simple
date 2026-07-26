package com.moneytracker.data.repository

data class MonthSummaryItem(
    val startDateMillis: Long,
    val income: Double,
    val expense: Double,
    val balance: Double
)
