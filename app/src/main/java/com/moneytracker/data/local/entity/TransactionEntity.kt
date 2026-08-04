package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId"), Index("date"), Index("sortOrder"), Index("profileId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val profileId: Long = 1,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val note: String = "",
    val sortOrder: Int = 0,
    val subCategory: String = "",
    val detail: String = "",
    val isRecurring: Boolean = false,
    val recurrenceFrequency: RecurrenceFrequency? = null,
    val recurTillDate: Long? = null,
    val recurCount: Int? = null
)
