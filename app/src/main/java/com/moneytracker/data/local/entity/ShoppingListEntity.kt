package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val payMonthDate: Long, // Epoch timestamp millis
    val shoppingDate: Long, // Epoch timestamp millis
    val title: String,
    val status: String = "OPEN", // "OPEN" vs "CLOSED"
    val totalBudgetCost: Double = 0.0,
    val totalActualCost: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
