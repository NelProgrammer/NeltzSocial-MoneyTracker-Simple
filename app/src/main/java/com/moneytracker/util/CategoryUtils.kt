package com.moneytracker.util

import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.TransactionType

/**
 * Extension that returns categories sorted by the required priority:
 * Income → Investment → Expense, then alphabetically by name.
 */
fun List<CategoryEntity>.sortedByPriority(): List<CategoryEntity> =
    sortedWith(compareBy({
        when (it.type) {
            TransactionType.INCOME -> 0
            TransactionType.INVESTMENT -> 1
            TransactionType.EXPENSE -> 2
            else -> 3
        }
    }, { it.name }))
