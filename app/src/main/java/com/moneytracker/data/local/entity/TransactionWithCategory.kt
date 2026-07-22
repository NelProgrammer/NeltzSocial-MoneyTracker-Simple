package com.moneytracker.data.local.entity

data class TransactionWithCategory(
    val id: Long,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val date: Long,
    val note: String,
    val sortOrder: Int,
    val categoryName: String,
    val categoryIconName: String
) {
    fun toEntity(): TransactionEntity = TransactionEntity(
        id = id,
        amount = amount,
        type = type,
        categoryId = categoryId,
        date = date,
        note = note,
        sortOrder = sortOrder
    )
}
