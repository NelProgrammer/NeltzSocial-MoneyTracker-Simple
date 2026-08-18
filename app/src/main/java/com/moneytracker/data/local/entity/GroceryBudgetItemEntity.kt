package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grocery_budget_items")
data class GroceryBudgetItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 0,
    val date: Long, // Epoch timestamp millis for human readability
    val category: String = "Starch",
    val subCategory: String = "Rice",
    val itemDetail: String = "",
    val unitSize: String = "pack",
    val note: String = "",
    val quantityBudget: Int = 1,
    val unitPriceBudget: Double = 0.0,
    val costBudget: Double = quantityBudget * unitPriceBudget,
    val isRecurring: Int = 0, // 0 = Once-off, 1 = Monthly Recurring, 2 = Planned
    val quantityActual: Int = 0,
    val unitPriceActual: Double = 0.0,
    val costActual: Double = quantityActual * unitPriceActual
)
