package com.moneytracker.data.local.entity

data class TransactionWithCategory(
    val id: Long,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val date: Long,
    val note: String,
    val sortOrder: Int,
    val subCategory: String = "",
    val detail: String = "",
    val isRecurring: Boolean = false,
    val recurrenceFrequency: RecurrenceFrequency? = null,
    val recurTillDate: Long? = null,
    val recurCount: Int? = null,
    val isRecurred: Boolean = false,
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
        sortOrder = sortOrder,
        subCategory = subCategory,
        detail = detail,
        isRecurring = isRecurring,
        recurrenceFrequency = recurrenceFrequency,
        recurTillDate = recurTillDate,
        recurCount = recurCount,
        isRecurred = isRecurred
    )
}
