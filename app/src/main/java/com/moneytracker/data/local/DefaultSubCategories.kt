package com.moneytracker.data.local

import com.moneytracker.data.local.entity.SubCategoryEntity
import com.moneytracker.data.local.entity.TransactionType

object DefaultSubCategories {
    suspend fun seed(subCategoryDao: SubCategoryDao, profileId: Long = 1L) {
        if (subCategoryDao.count(profileId) > 0) return

        val defaults = listOf(
            SubCategoryEntity(profileId = profileId, name = "Living Expenses", type = TransactionType.EXPENSE),
            SubCategoryEntity(profileId = profileId, name = "Debts & Bank Charges", type = TransactionType.EXPENSE),
            SubCategoryEntity(profileId = profileId, name = "School", type = TransactionType.EXPENSE),
            SubCategoryEntity(profileId = profileId, name = "Black Tax & Charity", type = TransactionType.EXPENSE),
            SubCategoryEntity(profileId = profileId, name = "Insurances", type = TransactionType.EXPENSE)
        )

        defaults.forEach { subCategoryDao.insert(it) }
    }
}
