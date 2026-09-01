package com.moneytracker.data.local

import android.content.Context
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.DetailEntity
import com.moneytracker.data.local.entity.RecurrenceFrequency
import com.moneytracker.data.local.entity.SubCategoryEntity
import com.moneytracker.data.local.entity.TransactionEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.util.DateUtils
import com.moneytracker.util.RecurringTransactionManager
import org.json.JSONArray
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

data class SeedItem(
    val categoryName: String,
    val transactionType: TransactionType,
    val subCategory: String,
    val detail: String,
    val amount: Double,
    val note: String,
    val isRecurring: Boolean,
    val recurrenceFrequency: RecurrenceFrequency?,
    val recurCount: Int?,
    val date: Long? = null
)

object ProfileSeeder {

    private const val SEED_FILE = "Seed_Standard_Guest.json"
    // Fixed seed date: 20 July 2026
    private val SEED_DATE_MILLIS = LocalDate.of(2026, 7, 20)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    fun loadSeedItems(context: Context): List<SeedItem> {
        val jsonString = context.assets.open(SEED_FILE).bufferedReader().use { it.readText() }
        val array = JSONArray(jsonString)
        val items = mutableListOf<SeedItem>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val catName = obj.getString("categoryName")
            val subCat = obj.getString("subCategory")
            val detail = obj.getString("detail")
            val amt = obj.getDouble("amount")
            val note = obj.optString("note", detail)
            val isRec = obj.optBoolean("isRecurring", false)
            val freqStr = if (obj.has("recurrenceFrequency") && !obj.isNull("recurrenceFrequency")) obj.getString("recurrenceFrequency") else null
            val recurCount = if (obj.has("recurCount") && !obj.isNull("recurCount")) obj.getInt("recurCount") else null
            val dateMillis = if (obj.has("date") && !obj.isNull("date")) {
                // Expect ISO-8601 date string, e.g., "2026-07-20"
                try {
                    val parsed = java.time.LocalDate.parse(obj.getString("date"))
                    parsed.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                } catch (e: Exception) {
                    null
                }
            } else null

            val type = when (catName.trim().uppercase()) {
                "INCOME" -> TransactionType.INCOME
                "INVESTMENT", "INVEST" -> TransactionType.INVESTMENT
                "EDUCATION", "EDU" -> TransactionType.EDUCATION
                else -> TransactionType.EXPENSE
            }
            val freq = freqStr?.let { try { RecurrenceFrequency.valueOf(it) } catch (e: Exception) { null } }

            items.add(
                SeedItem(
                    categoryName = catName,
                    transactionType = type,
                    subCategory = subCat,
                    detail = detail,
                    amount = amt,
                    note = note,
                    isRecurring = isRec,
                    recurrenceFrequency = freq,
                    recurCount = recurCount
                )
            )
        }
        return items
    }

    suspend fun seedProfileIfEmpty(
        context: Context,
        database: MoneyTrackerDatabase,
        repository: TransactionRepository,
        profileId: Long,
        isGuest: Boolean,
        forceReseed: Boolean = false
    ) {
        val prefs = context.getSharedPreferences("money_tracker_seeding_prefs", Context.MODE_PRIVATE)
        val isAlreadySeeded = prefs.getBoolean("is_seeded_profile_$profileId", false)
        val count = database.transactionDao().countForProfile(profileId)

        if (!forceReseed && (isAlreadySeeded || count > 0)) {
            if (!isAlreadySeeded && count > 0) {
                prefs.edit().putBoolean("is_seeded_profile_$profileId", true).apply()
            }
            return // Already seeded or has data! Never re-seed automatically!
        }

        // Clear existing transactions, categories, subcategories, and details for this profile if re-seeding
        if (count > 0) {
            database.transactionDao().getAllEntities(profileId).forEach {
                database.transactionDao().delete(it)
            }
            database.categoryDao().deleteAllForProfile(profileId)
            database.subCategoryDao().deleteAllForProfile(profileId)
            database.detailDao().deleteAllForProfile(profileId)
        }

        val seedItems = loadSeedItems(context)
        val categoryMap = mutableMapOf<String, Long>()
        val subCategoryMap = mutableMapOf<String, Long>()

        // 1. Seed Categories & SubCategories for this profile
        seedItems.forEach { item ->
            val catKey = "${item.categoryName}_${item.transactionType.name}"
            if (!categoryMap.containsKey(catKey)) {
                val categoryId = database.categoryDao().insert(
                    CategoryEntity(
                        profileId = profileId,
                        name = item.categoryName,
                        type = item.transactionType
                    )
                )
                categoryMap[catKey] = categoryId
            }

            val categoryId = categoryMap[catKey]!!
            val subCatKey = "${item.subCategory}_$categoryId"
            if (!subCategoryMap.containsKey(subCatKey)) {
                val subCategoryId = database.subCategoryDao().insert(
                    SubCategoryEntity(
                        profileId = profileId,
                        name = item.subCategory,
                        categoryId = categoryId,
                        type = item.transactionType
                    )
                )
                subCategoryMap[subCatKey] = subCategoryId
            }

            val subCategoryId = subCategoryMap[subCatKey]!!
            database.detailDao().insert(
                DetailEntity(
                    profileId = profileId,
                    name = item.detail,
                    subCategoryId = subCategoryId,
                    categoryId = categoryId,
                    type = item.transactionType
                )
            )
        }

        // 2. Insert July 20, 2026 Transactions for this profile (41 line items)
        seedItems.forEach { item ->
            val catKey = "${item.categoryName}_${item.transactionType.name}"
            val categoryId = categoryMap[catKey] ?: 1L

            val finalAmount = if (isGuest) {
                // Apply dynamic R100-R500 variation preserving positive/negative nature
                transformGuestAmount(item.amount)
            } else {
                // Direct import for Ryu (pre-divided amounts in JSON)
                item.amount
            }

            val txId = database.transactionDao().insert(
                TransactionEntity(
                    profileId = profileId,
                    amount = finalAmount,
                    type = item.transactionType,
                    categoryId = categoryId,
                    subCategory = item.subCategory,
                    detail = item.detail,
                    note = item.note,
                    date = item.date ?: SEED_DATE_MILLIS,
                    isRecurring = item.isRecurring,
                    recurrenceFrequency = item.recurrenceFrequency,
                    recurCount = item.recurCount
                )
            )

            // Trigger standard AddEdit recurrence rule for parent transaction
            if (item.isRecurring && item.recurrenceFrequency != null) {
                val entity = database.transactionDao().getById(txId)
                if (entity != null) {
                    RecurringTransactionManager.processSingleTransactionRecurrence(repository, entity)
                }
            }
        }
        prefs.edit().putBoolean("is_seeded_profile_$profileId", true).apply()
    }

    private fun transformGuestAmount(baseAmount: Double): Double {
        if (baseAmount == 0.0) return 0.0

        val delta = Random.nextDouble(100.0, 500.0)
        val sign = if (Random.nextBoolean()) 1.0 else -1.0
        val variation = delta * sign

        return if (baseAmount > 0.0) {
            maxOf(100.0, Math.round((baseAmount + variation) * 100.0) / 100.0)
        } else {
            minOf(-100.0, Math.round((baseAmount + variation) * 100.0) / 100.0)
        }
    }
}
