package com.moneytracker

import android.app.Application
import com.moneytracker.data.local.DefaultCategories
import com.moneytracker.data.local.MoneyTrackerDatabase
import com.moneytracker.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MoneyTrackerApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: TransactionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = MoneyTrackerDatabase.getInstance(this)
        applicationScope.launch {
            DefaultCategories.seed(database.categoryDao())
        }
        repository = TransactionRepository(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao()
        )
    }
}
