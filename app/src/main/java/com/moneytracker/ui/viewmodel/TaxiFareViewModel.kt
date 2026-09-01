package com.moneytracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.local.entity.TaxiExhaustionEntity
import com.moneytracker.data.local.entity.TaxiFareEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.util.DateUtils
import com.moneytracker.util.RecurringTaxiManager
import com.moneytracker.util.SettingsManager
import com.moneytracker.util.TaxiExhaustionItemUiModel
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
    val totalActualSpent: Double = 0.0,
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

    private val _activeExhaustionRoute = MutableStateFlow<TaxiFareEntity?>(null)
    val activeExhaustionRoute: StateFlow<TaxiFareEntity?> = _activeExhaustionRoute.asStateFlow()

    init {
        triggerDailyExhaustion()
    }

    fun setPayMonth(date: LocalDate) {
        _selectedPayMonthDate.value = date
        triggerDailyExhaustion()
    }

    fun openExhaustionPopup(route: TaxiFareEntity?) {
        _activeExhaustionRoute.value = route
    }

    private fun triggerDailyExhaustion() {
        viewModelScope.launch {
            RecurringTaxiManager.processDailyAutomaticExhaustion(repository)
        }
    }

    val routes: StateFlow<List<TaxiFareEntity>> = repository
        .observeAllTaxiFares()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyExhaustions: StateFlow<List<TaxiExhaustionEntity>> = _selectedPayMonthDate.flatMapLatest { date ->
        val startMillis = DateUtils.toEpochMillis(date)
        val endMillis = DateUtils.toEpochMillis(date.plusMonths(1)) - 1
        repository.observeTaxiExhaustionsForMonth(startMillis, endMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val collatedExhaustionUiList: StateFlow<List<TaxiExhaustionItemUiModel>> = combine(
        monthlyExhaustions,
        _activeExhaustionRoute
    ) { exhaustions, activeRoute ->
        val filtered = if (activeRoute != null) exhaustions.filter { it.routeId == activeRoute.id } else exhaustions
        RecurringTaxiManager.collateMonthlyExhaustions(filtered, activeRoute)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetSummary: StateFlow<TaxiFareBudgetSummary> = combine(
        _selectedPayMonthDate.flatMapLatest { date ->
            val startMillis = DateUtils.toEpochMillis(date)
            val endMillis = DateUtils.toEpochMillis(date.plusMonths(1)) - 1
            repository.observeTransactionsForMonth(startMillis, endMillis)
        },
        routes,
        monthlyExhaustions
    ) { mainTxns, routeList, exhaustions ->
        val taxiCategoryBudget = mainTxns
            .filter { it.type == TransactionType.EXPENSE && (it.categoryName.contains("Taxi", ignoreCase = true) || it.subCategory.contains("Taxi", ignoreCase = true) || it.categoryName.contains("Transport", ignoreCase = true)) }
            .sumOf { kotlin.math.abs(it.amount) }

        val totalEstimated = routeList.sumOf { it.monthlyTotal }
        val totalSpent = exhaustions.sumOf { it.totalCost }
        val baseline = if (taxiCategoryBudget > 0.0) taxiCategoryBudget else totalEstimated
        val remaining = baseline - totalSpent
        val isOver = totalSpent > baseline && baseline > 0.0
        val overAmt = if (isOver) totalSpent - baseline else 0.0

        TaxiFareBudgetSummary(
            mainBudget = taxiCategoryBudget,
            totalEstimatedFare = totalEstimated,
            totalActualSpent = totalSpent,
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
        workingDaysPerMonth: Int,
        startDate: Long? = null
    ) {
        viewModelScope.launch {
            val startTs = startDate ?: DateUtils.toEpochMillis(_selectedPayMonthDate.value)
            val monthlyTotal = farePerTrip * tripsPerDay * workingDaysPerMonth
            val fareEntity = TaxiFareEntity(
                id = id,
                profileId = repository.activeProfileId,
                routeName = routeName.trim(),
                farePerTrip = farePerTrip,
                tripsPerDay = tripsPerDay,
                workingDaysPerMonth = workingDaysPerMonth,
                monthlyTotal = monthlyTotal,
                date = startTs
            )
            repository.saveTaxiFare(fareEntity)
        }
    }

    fun deleteRoute(fare: TaxiFareEntity) {
        viewModelScope.launch {
            repository.deleteTaxiFare(fare)
            repository.deleteTaxiExhaustionsForRoute(fare.id)
        }
    }

    fun quickLogTrip(route: TaxiFareEntity, timeOfDay: String, isMorning: Boolean) {
        viewModelScope.launch {
            val payMonthStartTs = DateUtils.toEpochMillis(_selectedPayMonthDate.value)
            val entry = TaxiExhaustionEntity(
                profileId = repository.activeProfileId,
                routeId = route.id,
                payMonthDate = payMonthStartTs,
                date = System.currentTimeMillis(),
                units = 1,
                farePerTrip = route.farePerTrip,
                totalCost = route.farePerTrip,
                isAutoGenerated = false,
                isCustomOutlier = false,
                timeOfDay = timeOfDay,
                note = if (isMorning) "Morning Trip" else "After-Hours Trip"
            )
            repository.saveTaxiExhaustion(entry)
        }
    }

    fun saveTrip(trip: TaxiExhaustionEntity) {
        viewModelScope.launch {
            repository.saveTaxiExhaustion(trip)
        }
    }

    fun deleteTrip(trip: TaxiExhaustionEntity) {
        viewModelScope.launch {
            repository.deleteTaxiExhaustion(trip)
        }
    }

    fun updateMorningCutoffHour(hour: Int) {
        SettingsManager.updateMorningCutoffHour(hour)
        triggerDailyExhaustion()
    }
}
