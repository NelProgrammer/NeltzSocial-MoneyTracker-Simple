package com.moneytracker.data.local

import com.moneytracker.data.local.entity.SubCategoryEntity
import com.moneytracker.data.local.entity.TransactionType

object DefaultSubCategories {
    suspend fun seed(subCategoryDao: SubCategoryDao) {
        if (subCategoryDao.count() > 0) return

        val defaults = listOf(
            SubCategoryEntity(name = "Living Expenses", type = TransactionType.EXPENSE),
            SubCategoryEntity(name = "Debts & Bank Charges", type = TransactionType.EXPENSE),
            SubCategoryEntity(name = "School", type = TransactionType.EXPENSE),
            SubCategoryEntity(name = "Black Tax & Charity", type = TransactionType.EXPENSE),
            SubCategoryEntity(name = "Insurances", type = TransactionType.EXPENSE)
        )

        defaults.forEach { subCategoryDao.insert(it) }
    }
}
