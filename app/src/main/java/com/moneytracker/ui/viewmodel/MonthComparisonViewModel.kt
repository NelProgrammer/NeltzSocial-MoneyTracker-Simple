package com.moneytracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.local.entity.TransactionWithCategory
import com.moneytracker.data.repository.MonthlySummary
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class DrilldownLevel {
    SUMMARY,
    CATEGORY,
    SUBCATEGORY
}

data class PayMonthData(
    val monthName: String,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val summary: MonthlySummary,
    val transactions: List<TransactionWithCategory>
)

data class CategoryComparisonRow(
    val name: String,
    val type: TransactionType,
    val prevAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val nextAmount: Double = 0.0
)

class MonthComparisonViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val payDateDay = SettingsManager.getPayDateDay()
    private val _anchorPayMonthDate = MutableStateFlow(DateUtils.currentPayMonthLocalDate(LocalDate.now(), payDateDay))
    val anchorPayMonthDate: StateFlow<LocalDate> = _anchorPayMonthDate.asStateFlow()

    fun setAnchorPayMonth(date: LocalDate) {
        _anchorPayMonthDate.value = date
    }

    private val _drilldownLevel = MutableStateFlow(DrilldownLevel.SUMMARY)
    val drilldownLevel: StateFlow<DrilldownLevel> = _drilldownLevel.asStateFlow()

    fun setDrilldownLevel(level: DrilldownLevel) {
        _drilldownLevel.value = level
    }

    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MMM")

    @OptIn(ExperimentalCoroutinesApi::class)
    val prevMonthData: StateFlow<PayMonthData> = _anchorPayMonthDate.flatMapLatest { anchor ->
        val prevDate = anchor.minusMonths(1)
        val startMillis = DateUtils.toEpochMillis(prevDate)
        val endMillis = DateUtils.toEpochMillis(anchor) - 1
        val name = prevDate.format(monthFormatter)

        repository.observeTransactionsForMonth(startMillis, endMillis)
            .combine(repository.observeMonthlySummary(startMillis, endMillis)) { list, summary ->
                PayMonthData(name, startMillis, endMillis, summary, list)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PayMonthData("Prev", 0L, 0L, MonthlySummary(0.0, 0.0, 0.0, 0.0), emptyList()))

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentMonthData: StateFlow<PayMonthData> = _anchorPayMonthDate.flatMapLatest { anchor ->
        val nextDate = anchor.plusMonths(1)
        val startMillis = DateUtils.toEpochMillis(anchor)
        val endMillis = DateUtils.toEpochMillis(nextDate) - 1
        val name = anchor.format(monthFormatter)

        repository.observeTransactionsForMonth(startMillis, endMillis)
            .combine(repository.observeMonthlySummary(startMillis, endMillis)) { list, summary ->
                PayMonthData(name, startMillis, endMillis, summary, list)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PayMonthData("Anchor", 0L, 0L, MonthlySummary(0.0, 0.0, 0.0, 0.0), emptyList()))

    @OptIn(ExperimentalCoroutinesApi::class)
    val nextMonthData: StateFlow<PayMonthData> = _anchorPayMonthDate.flatMapLatest { anchor ->
        val nextDate = anchor.plusMonths(1)
        val afterNextDate = anchor.plusMonths(2)
        val startMillis = DateUtils.toEpochMillis(nextDate)
        val endMillis = DateUtils.toEpochMillis(afterNextDate) - 1
        val name = nextDate.format(monthFormatter)

        repository.observeTransactionsForMonth(startMillis, endMillis)
            .combine(repository.observeMonthlySummary(startMillis, endMillis)) { list, summary ->
                PayMonthData(name, startMillis, endMillis, summary, list)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PayMonthData("Next", 0L, 0L, MonthlySummary(0.0, 0.0, 0.0, 0.0), emptyList()))

    // Category Level comparison rows across 3 months
    val categoryRows: StateFlow<List<CategoryComparisonRow>> = combine(
        prevMonthData,
        currentMonthData,
        nextMonthData
    ) { prev, curr, next ->
        val prevGrouped = prev.transactions.groupBy { it.type }
        val currGrouped = curr.transactions.groupBy { it.type }
        val nextGrouped = next.transactions.groupBy { it.type }

        TransactionType.values().map { type ->
            CategoryComparisonRow(
                name = type.name,
                type = type,
                prevAmount = prevGrouped[type]?.sumOf { kotlin.math.abs(it.amount) } ?: 0.0,
                currentAmount = currGrouped[type]?.sumOf { kotlin.math.abs(it.amount) } ?: 0.0,
                nextAmount = nextGrouped[type]?.sumOf { kotlin.math.abs(it.amount) } ?: 0.0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // SubCategory Level comparison rows across 3 months
    val subCategoryRows: StateFlow<List<CategoryComparisonRow>> = combine(
        prevMonthData,
        currentMonthData,
        nextMonthData
    ) { prev, curr, next ->
        val allTxns = prev.transactions + curr.transactions + next.transactions
        val uniqueCategories = allTxns.map { Pair(it.categoryId, Pair(it.categoryName, it.type)) }.distinct()

        val prevMap = prev.transactions.groupBy { it.categoryId }
        val currMap = curr.transactions.groupBy { it.categoryId }
        val nextMap = next.transactions.groupBy { it.categoryId }

        uniqueCategories.map { (catId, pair) ->
            val (name, type) = pair
            CategoryComparisonRow(
                name = name,
                type = type,
                prevAmount = prevMap[catId]?.sumOf { kotlin.math.abs(it.amount) } ?: 0.0,
                currentAmount = currMap[catId]?.sumOf { kotlin.math.abs(it.amount) } ?: 0.0,
                nextAmount = nextMap[catId]?.sumOf { kotlin.math.abs(it.amount) } ?: 0.0
            )
        }.sortedWith(
            compareBy<CategoryComparisonRow> {
                when (it.type) {
                    TransactionType.INCOME -> 0
                    TransactionType.INVESTMENT -> 1
                    TransactionType.EDUCATION -> 2
                    TransactionType.EXPENSE -> 3
                }
            }.thenBy { it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
