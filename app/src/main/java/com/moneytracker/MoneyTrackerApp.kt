package com.moneytracker

import android.app.Application
import com.moneytracker.data.local.MoneyTrackerDatabase
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.util.ProfileManager
import com.moneytracker.util.SettingsManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class MoneyTrackerApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: TransactionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = MoneyTrackerDatabase.getInstance(this)
        SettingsManager.init(this)

        repository = TransactionRepository(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            subCategoryDao = database.subCategoryDao(),
            detailDao = database.detailDao(),
            groceryDao = database.groceryDao(),
            taxiFareDao = database.taxiFareDao(),
            profileDao = database.profileDao(),
            groceryBudgetDao = database.groceryBudgetDao(),
            unitSizeDao = database.unitSizeDao(),
            shoppingListDao = database.shoppingListDao(),
            shoppingListItemDao = database.shoppingListItemDao(),
            taxiExhaustionDao = database.taxiExhaustionDao()
        )

        applicationScope.launch {
            ProfileManager.initSession(this@MoneyTrackerApp, database, repository)
            repository.cleanDuplicateUnitSizes()
        }
    }
}
