package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grocery_items")
data class GroceryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val date: Long,
    val itemName: String,
    val size: Double = 1.0,
    val sizeUnit: String = "pack",
    val category: String = "Beverages",
    val subCategory: String = "Milk",
    val unitPrice: Double = 0.0,
    val quantity: Int = 1,
    val totalPrice: Double = 0.0,
    val isChecked: Boolean = false,
    val transactionId: Long? = null
)
