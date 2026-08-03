package com.moneytracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.local.entity.TaxiFareEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.util.DateUtils
import com.moneytracker.util.SettingsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TaxiFareBudgetSummary(
    val mainBudget: Double = 0.0,
    val totalEstimatedFare: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val isOverBudget: Boolean = false,
    val overBudgetAmount: Double = 0.0
)

class TaxiFareViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val payDateDay = SettingsManager.getPayDateDay()
    private val _selectedPayMonthDate = MutableStateFlow(DateUtils.currentPayMonthLocalDate(LocalDate.now(), payDateDay))
    val selectedPayMonthDate: StateFlow<LocalDate> = _selectedPayMonthDate.asStateFlow()

    fun setPayMonth(date: LocalDate) {
        _selectedPayMonthDate.value = date
    }

    val routes: StateFlow<List<TaxiFareEntity>> = repository
        .observeAllTaxiFares()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetSummary: StateFlow<TaxiFareBudgetSummary> = combine(
        _selectedPayMonthDate.flatMapLatest { date ->
            val startMillis = DateUtils.toEpochMillis(date)
            val endMillis = DateUtils.toEpochMillis(date.plusMonths(1)) - 1
            repository.observeTransactionsForMonth(startMillis, endMillis)
        },
        routes
    ) { mainTxns, routeList ->
        val taxiCategoryBudget = mainTxns
            .filter { it.type == TransactionType.EXPENSE && (it.categoryName.contains("Taxi", ignoreCase = true) || it.subCategory.contains("Taxi", ignoreCase = true) || it.categoryName.contains("Transport", ignoreCase = true)) }
            .sumOf { kotlin.math.abs(it.amount) }

        val totalEstimated = routeList.sumOf { it.monthlyTotal }
        val remaining = taxiCategoryBudget - totalEstimated
        val isOver = totalEstimated > taxiCategoryBudget && taxiCategoryBudget > 0.0
        val overAmt = if (isOver) totalEstimated - taxiCategoryBudget else 0.0

        TaxiFareBudgetSummary(
            mainBudget = taxiCategoryBudget,
            totalEstimatedFare = totalEstimated,
            remainingBudget = remaining,
            isOverBudget = isOver,
            overBudgetAmount = overAmt
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaxiFareBudgetSummary())

    fun saveRoute(
        id: Long = 0,
        routeName: String,
        farePerTrip: Double,
        tripsPerDay: Int,
        workingDaysPerMonth: Int
    ) {
        viewModelScope.launch {
            val monthlyTotal = farePerTrip * tripsPerDay * workingDaysPerMonth
            val fareEntity = TaxiFareEntity(
                id = id,
                routeName = routeName,
                farePerTrip = farePerTrip,
                tripsPerDay = tripsPerDay,
                workingDaysPerMonth = workingDaysPerMonth,
                monthlyTotal = monthlyTotal,
                date = System.currentTimeMillis()
            )
            repository.saveTaxiFare(fareEntity)
        }
    }

    fun deleteRoute(fare: TaxiFareEntity) {
        viewModelScope.launch {
            repository.deleteTaxiFare(fare)
        }
    }
}
