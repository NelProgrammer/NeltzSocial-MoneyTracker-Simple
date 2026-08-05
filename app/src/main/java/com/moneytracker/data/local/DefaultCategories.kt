package com.moneytracker.data.local

import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.TransactionType

object DefaultCategories {
    suspend fun seed(categoryDao: CategoryDao, profileId: Long = 1L) {
        if (categoryDao.count(profileId) > 0) return

        val categories = listOf(
            // Income Categories
            CategoryEntity(profileId = profileId, name = "Salary & Wages", type = TransactionType.INCOME, iconName = "salary"),
            CategoryEntity(profileId = profileId, name = "Business & Freelance", type = TransactionType.INCOME, iconName = "freelance"),
            CategoryEntity(profileId = profileId, name = "Investments & Dividends", type = TransactionType.INCOME, iconName = "investment"),
            CategoryEntity(profileId = profileId, name = "Gifts & Grants", type = TransactionType.INCOME, iconName = "gift"),
            CategoryEntity(profileId = profileId, name = "Rental Income", type = TransactionType.INCOME, iconName = "rent"),
            CategoryEntity(profileId = profileId, name = "Other Income", type = TransactionType.INCOME, iconName = "other"),

            // Investment Categories
            CategoryEntity(profileId = profileId, name = "Stocks & Shares", type = TransactionType.INVESTMENT, iconName = "investment"),
            CategoryEntity(profileId = profileId, name = "Real Estate & Property", type = TransactionType.INVESTMENT, iconName = "rent"),
            CategoryEntity(profileId = profileId, name = "Crypto & Digital Assets", type = TransactionType.INVESTMENT, iconName = "other"),
            CategoryEntity(profileId = profileId, name = "Savings & Fixed Deposit", type = TransactionType.INVESTMENT, iconName = "salary"),
            CategoryEntity(profileId = profileId, name = "Retirement & Pension", type = TransactionType.INVESTMENT, iconName = "investment"),

            // Education Categories
            CategoryEntity(profileId = profileId, name = "University", type = TransactionType.EDUCATION, iconName = "school"),
            CategoryEntity(profileId = profileId, name = "Certificate & Courses", type = TransactionType.EDUCATION, iconName = "school"),
            CategoryEntity(profileId = profileId, name = "School & Books", type = TransactionType.EDUCATION, iconName = "school"),

            // Expense Categories
            CategoryEntity(profileId = profileId, name = "Food & Dining", type = TransactionType.EXPENSE, iconName = "food"),
            CategoryEntity(profileId = profileId, name = "Housing & Rent", type = TransactionType.EXPENSE, iconName = "rent"),
            CategoryEntity(profileId = profileId, name = "Utilities & Bills", type = TransactionType.EXPENSE, iconName = "utilities"),
            CategoryEntity(profileId = profileId, name = "Transportation", type = TransactionType.EXPENSE, iconName = "transport"),
            CategoryEntity(profileId = profileId, name = "Shopping & Retail", type = TransactionType.EXPENSE, iconName = "shopping"),
            CategoryEntity(profileId = profileId, name = "Entertainment & Leisure", type = TransactionType.EXPENSE, iconName = "entertainment"),
            CategoryEntity(profileId = profileId, name = "Healthcare & Medical", type = TransactionType.EXPENSE, iconName = "healthcare"),
            CategoryEntity(profileId = profileId, name = "Insurances", type = TransactionType.EXPENSE, iconName = "insurance"),
            CategoryEntity(profileId = profileId, name = "Debts & Bank Charges", type = TransactionType.EXPENSE, iconName = "debt"),
            CategoryEntity(profileId = profileId, name = "Black Tax & Charity", type = TransactionType.EXPENSE, iconName = "gift"),
            CategoryEntity(profileId = profileId, name = "Other Expense", type = TransactionType.EXPENSE, iconName = "other")
        )

        categories.forEach { categoryDao.insert(it) }
    }
}
