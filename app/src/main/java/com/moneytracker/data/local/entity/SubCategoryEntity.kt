package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sub_categories")
data class SubCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 0,
    val name: String,
    val categoryId: Long? = null,
    val iconName: String = "default",
    val type: TransactionType? = null
)
