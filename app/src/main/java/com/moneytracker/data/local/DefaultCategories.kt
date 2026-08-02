package com.moneytracker.data.local

import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.TransactionType

object DefaultCategories {
    suspend fun seed(categoryDao: CategoryDao) {
        if (categoryDao.count() > 0) return

        val categories = listOf(
            // Income Categories
            CategoryEntity(name = "Salary & Wages", type = TransactionType.INCOME, iconName = "salary"),
            CategoryEntity(name = "Business & Freelance", type = TransactionType.INCOME, iconName = "freelance"),
            CategoryEntity(name = "Investments & Dividends", type = TransactionType.INCOME, iconName = "investment"),
            CategoryEntity(name = "Gifts & Grants", type = TransactionType.INCOME, iconName = "gift"),
            CategoryEntity(name = "Rental Income", type = TransactionType.INCOME, iconName = "rent"),
            CategoryEntity(name = "Other Income", type = TransactionType.INCOME, iconName = "other"),

            // Investment Categories
            CategoryEntity(name = "Stocks & Shares", type = TransactionType.INVESTMENT, iconName = "investment"),
            CategoryEntity(name = "Real Estate & Property", type = TransactionType.INVESTMENT, iconName = "rent"),
            CategoryEntity(name = "Crypto & Digital Assets", type = TransactionType.INVESTMENT, iconName = "other"),
            CategoryEntity(name = "Savings & Fixed Deposit", type = TransactionType.INVESTMENT, iconName = "salary"),
            CategoryEntity(name = "Retirement & Pension", type = TransactionType.INVESTMENT, iconName = "investment"),

            // Expense Categories
            CategoryEntity(name = "Food & Dining", type = TransactionType.EXPENSE, iconName = "food"),
            CategoryEntity(name = "Housing & Rent", type = TransactionType.EXPENSE, iconName = "rent"),
            CategoryEntity(name = "Utilities & Bills", type = TransactionType.EXPENSE, iconName = "utilities"),
            CategoryEntity(name = "Transportation", type = TransactionType.EXPENSE, iconName = "transport"),
            CategoryEntity(name = "Shopping & Retail", type = TransactionType.EXPENSE, iconName = "shopping"),
            CategoryEntity(name = "Entertainment & Leisure", type = TransactionType.EXPENSE, iconName = "entertainment"),
            CategoryEntity(name = "Healthcare & Medical", type = TransactionType.EXPENSE, iconName = "healthcare"),
            CategoryEntity(name = "Education & School", type = TransactionType.EXPENSE, iconName = "school"),
            CategoryEntity(name = "Insurances", type = TransactionType.EXPENSE, iconName = "insurance"),
            CategoryEntity(name = "Debts & Bank Charges", type = TransactionType.EXPENSE, iconName = "debt"),
            CategoryEntity(name = "Black Tax & Charity", type = TransactionType.EXPENSE, iconName = "gift"),
            CategoryEntity(name = "Other Expense", type = TransactionType.EXPENSE, iconName = "other")
        )

        categories.forEach { categoryDao.insert(it) }
    }
}
