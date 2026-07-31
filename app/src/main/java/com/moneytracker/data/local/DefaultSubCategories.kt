package com.moneytracker.data.local

import com.moneytracker.data.local.entity.SubCategoryEntity

object DefaultSubCategories {
    suspend fun seed(subCategoryDao: SubCategoryDao) {
        if (subCategoryDao.count() > 0) return

        val defaults = listOf(
            SubCategoryEntity(name = "Living Expenses"),
            SubCategoryEntity(name = "Debts & Bank Charges"),
            SubCategoryEntity(name = "School"),
            SubCategoryEntity(name = "Black Tax & Charity"),
            SubCategoryEntity(name = "Insurances")
        )

        defaults.forEach { subCategoryDao.insert(it) }
    }
}
