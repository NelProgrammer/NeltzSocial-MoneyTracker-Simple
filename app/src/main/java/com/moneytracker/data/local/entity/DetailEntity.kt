package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "details")
data class DetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val subCategoryId: Long? = null,
    val categoryId: Long? = null,
    val iconName: String = "default",
    val type: TransactionType? = null
)
