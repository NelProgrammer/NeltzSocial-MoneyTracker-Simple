package com.moneytracker

import android.app.Application
import com.moneytracker.data.local.DefaultCategories
import com.moneytracker.data.local.DefaultSubCategories
import com.moneytracker.data.local.MoneyTrackerDatabase
import com.moneytracker.data.repository.TransactionRepository
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
        applicationScope.launch {
            DefaultCategories.seed(database.categoryDao())
            DefaultSubCategories.seed(database.subCategoryDao())
        }
        repository = TransactionRepository(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            subCategoryDao = database.subCategoryDao()
        )
    }
}
