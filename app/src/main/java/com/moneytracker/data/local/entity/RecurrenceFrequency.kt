package com.moneytracker.data.local.entity

enum class RecurrenceFrequency(val label: String) {
    ONCE_OFF("One-time"),
    MONTHLY("Monthly"),
    CONTINUOUS("Continuous"), // Retained for backwards compatibility migration
    PLAN_FUTURE("Plan / Future Date"),
    TENTATIVE_FORECAST("Tentative Forecast")
}
