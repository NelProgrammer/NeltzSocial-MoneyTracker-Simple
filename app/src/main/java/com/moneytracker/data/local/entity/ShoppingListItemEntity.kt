package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_list_items")
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val shoppingListId: Long,
    val budgetItemId: Long? = null,
    val category: String,
    val subCategory: String,
    val itemDetail: String,
    val unitSize: String,
    val quantityBudget: Int,
    val unitPriceBudget: Double,
    val quantityActual: Int,
    val unitPriceActual: Double,
    val isChecked: Boolean = false
)
