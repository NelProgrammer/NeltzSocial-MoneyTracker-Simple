package com.moneytracker.data.local

import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.TransactionType

object DefaultCategories {
    suspend fun seed(categoryDao: CategoryDao) {
        if (categoryDao.count() > 0) return

        val categories = listOf(
            CategoryEntity(name = "Salary", type = TransactionType.INCOME, iconName = "salary"),
            CategoryEntity(name = "Freelance", type = TransactionType.INCOME, iconName = "freelance"),
            CategoryEntity(name = "Investment", type = TransactionType.INCOME, iconName = "investment"),
            CategoryEntity(name = "Gift", type = TransactionType.INCOME, iconName = "gift"),
            CategoryEntity(name = "Other Income", type = TransactionType.INCOME, iconName = "other"),
            CategoryEntity(name = "Food", type = TransactionType.EXPENSE, iconName = "food"),
            CategoryEntity(name = "Transport", type = TransactionType.EXPENSE, iconName = "transport"),
            CategoryEntity(name = "Rent", type = TransactionType.EXPENSE, iconName = "rent"),
            CategoryEntity(name = "Utilities", type = TransactionType.EXPENSE, iconName = "utilities"),
            CategoryEntity(name = "Shopping", type = TransactionType.EXPENSE, iconName = "shopping"),
            CategoryEntity(name = "Entertainment", type = TransactionType.EXPENSE, iconName = "entertainment"),
            CategoryEntity(name = "Healthcare", type = TransactionType.EXPENSE, iconName = "healthcare"),
            CategoryEntity(name = "Other Expense", type = TransactionType.EXPENSE, iconName = "other")
        )

        categories.forEach { categoryDao.insert(it) }
    }
}
