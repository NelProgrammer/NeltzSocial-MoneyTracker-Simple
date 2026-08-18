package com.moneytracker.data.local.entity

import androidx.room.Ignore

data class CategorySummary(
    val categoryId: Long,
    val categoryName: String,
    val total: Double,
    @Ignore
    val isDebtFunding: Boolean = false,
    @Ignore
    val customColorHex: Long? = null
) {
    constructor(categoryId: Long, categoryName: String, total: Double) : this(
        categoryId = categoryId,
        categoryName = categoryName,
        total = total,
        isDebtFunding = false,
        customColorHex = null
    )
}
