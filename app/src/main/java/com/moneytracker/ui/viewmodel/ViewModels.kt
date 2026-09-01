package com.moneytracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.CategorySummary
import com.moneytracker.data.local.entity.RecurrenceFrequency
import com.moneytracker.data.local.entity.TransactionEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.local.entity.TransactionWithCategory
import com.moneytracker.data.repository.MonthlySummary
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import com.moneytracker.ui.theme.getCategoryColorHex
import java.time.LocalDate

// Sort Data Models
data class SortCriterion(
    val field: TransactionsViewModel.SortField,
    val direction: TransactionsViewModel.SortDirection = TransactionsViewModel.SortDirection.ASC
)

data class CustomCategoryBreakdown(
    val categoryName: String,
    val totalAmount: Double,
    val subCategorySummaries: List<CategorySummary>,
    val detailSummaries: List<CategorySummary>
)

fun buildTransactionComparator(secondarySorts: List<SortCriterion>): Comparator<TransactionWithCategory> {
    // 1. Fixed #1 Primary Sort: Category (INCOME = 0, INVESTMENT = 1, EXPENSE = 2)
    var comparator: Comparator<TransactionWithCategory> = compareBy { t ->
        when (t.type) {
            TransactionType.INCOME -> 0
            TransactionType.INVESTMENT -> 1
            TransactionType.EDUCATION -> 2
            TransactionType.EXPENSE -> 3
        }
    }

    // 2. Chained secondary sorts (excluding TYPE since Category is fixed #1)
    val filteredSecondary = secondarySorts.filter { it.field != TransactionsViewModel.SortField.TYPE }

    if (filteredSecondary.isEmpty()) {
        return comparator.thenByDescending { it.date }.thenBy { it.id }
    }

    for (criterion in filteredSecondary) {
        val selector: (TransactionWithCategory) -> Comparable<*>? = when (criterion.field) {
            TransactionsViewModel.SortField.ID -> { t -> t.id }
            TransactionsViewModel.SortField.DATE -> { t -> t.date }
            TransactionsViewModel.SortField.AMOUNT -> { t -> t.amount }
            TransactionsViewModel.SortField.CATEGORY -> { t -> t.categoryName.lowercase() }
            TransactionsViewModel.SortField.SUBCATEGORY -> { t -> t.subCategory.lowercase() }
            TransactionsViewModel.SortField.DESCRIPTION -> { t -> t.note.lowercase() }
            TransactionsViewModel.SortField.TYPE -> { t -> 0 }
        }
        val nextComp = if (criterion.direction == TransactionsViewModel.SortDirection.ASC) {
            compareBy(selector)
        } else {
            compareByDescending(selector)
        }
        comparator = comparator.thenComparing(nextComp)
    }

    return comparator.thenByDescending { it.date }.thenBy { it.id }
}

// SubCategory Summary Data Model for Dashboard
data class SubCategorySummary(
    val categoryId: Long,
    val categoryName: String,
    val type: TransactionType,
    val transactionCount: Int,
    val totalAmount: Double,
    val percentage: Double,
    val transactions: List<TransactionWithCategory> = emptyList()
)

// Dashboard ViewModel
class DashboardViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            com.moneytracker.util.RecurringTransactionManager.processRecurringTransactionsIfDue(repository)
            com.moneytracker.util.RecurringGroceryManager.processRecurringGroceryItemsIfDue(repository)
        }
    }

    private val payDateDay = com.moneytracker.util.SettingsManager.getPayDateDay()
    private val _selectedPayMonthDate = MutableStateFlow(DateUtils.currentPayMonthLocalDate(LocalDate.now(), payDateDay))
    val selectedPayMonthDate: StateFlow<LocalDate> = _selectedPayMonthDate.asStateFlow()

    fun setPayMonth(date: LocalDate) {
        _selectedPayMonthDate.value = date
        viewModelScope.launch {
            com.moneytracker.util.RecurringTransactionManager.processRecurringTransactions(repository)
            com.moneytracker.util.RecurringGroceryManager.processRecurringGroceryItems(repository)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val payMonthTransactions = _selectedPayMonthDate.flatMapLatest { date ->
        val startMillis = DateUtils.startOfPayMonth(date, payDateDay)
        val endMillis = DateUtils.startOfNextPayMonth(date, payDateDay)
        repository.observeTransactionsForMonth(startMillis, endMillis)
    }

    val summary: StateFlow<MonthlySummary> = payMonthTransactions
        .map { list ->
            val active = list.filter { it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
            val incomeTotal = active.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
            val investTotal = active.filter { it.type == TransactionType.INVESTMENT }.sumOf { kotlin.math.abs(it.amount) }
            val eduTotal = active.filter { it.type == TransactionType.EDUCATION }.sumOf { kotlin.math.abs(it.amount) }
            val expenseTotal = active.filter { it.type == TransactionType.EXPENSE }.sumOf { kotlin.math.abs(it.amount) }
            val totalOutflows = active.filter { it.type != TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
            MonthlySummary(
                income = incomeTotal,
                investment = investTotal,
                education = eduTotal,
                expense = expenseTotal,
                balance = incomeTotal - totalOutflows
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlySummary())

    val subCategorySummaries: StateFlow<List<SubCategorySummary>> = payMonthTransactions
        .map { list ->
            val active = list.filter { it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
            val incomeTotal = active.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
            val investTotal = active.filter { it.type == TransactionType.INVESTMENT }.sumOf { kotlin.math.abs(it.amount) }
            val eduTotal = active.filter { it.type == TransactionType.EDUCATION }.sumOf { kotlin.math.abs(it.amount) }
            val expenseTotal = active.filter { it.type == TransactionType.EXPENSE }.sumOf { kotlin.math.abs(it.amount) }

            list.groupBy { it.categoryId }
                .map { (catId, txns) ->
                    val first = txns.first()
                    val activeForCat = txns.filter { it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
                    val total = activeForCat.sumOf { kotlin.math.abs(it.amount) }
                    val typeTotal = when (first.type) {
                        TransactionType.INCOME -> incomeTotal
                        TransactionType.INVESTMENT -> investTotal
                        TransactionType.EDUCATION -> eduTotal
                        TransactionType.EXPENSE -> expenseTotal
                    }
                    val pct = if (typeTotal > 0.0) (total / typeTotal) * 100.0 else 0.0
                    SubCategorySummary(
                        categoryId = catId,
                        categoryName = first.categoryName,
                        type = first.type,
                        transactionCount = txns.size,
                        totalAmount = total,
                        percentage = pct,
                        transactions = txns.sortedByDescending { it.date }
                    )
                }
                .sortedWith(
                    compareBy<SubCategorySummary> {
                        when (it.type) {
                            TransactionType.INCOME -> 0
                            TransactionType.INVESTMENT -> 1
                            TransactionType.EDUCATION -> 2
                            TransactionType.EXPENSE -> 3
                        }
                    }.thenBy { it.categoryName.lowercase() }
                )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeBreakdown: StateFlow<List<CategorySummary>> = payMonthTransactions
        .map { list ->
            list.filter { it.type == TransactionType.INCOME && it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
                .groupBy { it.subCategory.ifBlank { it.categoryName } }
                .map { (subCatName, txns) ->
                    CategorySummary(
                        categoryId = txns.first().categoryId,
                        categoryName = subCatName,
                        total = txns.sumOf { kotlin.math.abs(it.amount) }
                    )
                }
                .sortedByDescending { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeDetailBreakdown: StateFlow<List<CategorySummary>> = payMonthTransactions
        .map { list ->
            list.filter { it.type == TransactionType.INCOME && it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
                .groupBy { it.detail.ifBlank { it.subCategory.ifBlank { it.categoryName } } }
                .map { (detailName, txns) ->
                    CategorySummary(
                        categoryId = txns.first().categoryId,
                        categoryName = detailName,
                        total = txns.sumOf { kotlin.math.abs(it.amount) }
                    )
                }
                .sortedByDescending { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 2nd Pie: Income Funding / Utilization (How Income is Used & Remaining Balance / Debt Funding)
    val incomeUsageBreakdown: StateFlow<List<CategorySummary>> = payMonthTransactions
        .map { list ->
            val active = list.filter { it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
            val incomeTotal = active.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
            val outflowTxns = active.filter { it.type != TransactionType.INCOME }
            val totalOutflows = outflowTxns.sumOf { kotlin.math.abs(it.amount) }

            // Dynamically group all outflow transactions by category name
            val groupedCategories = outflowTxns
                .groupBy { it.categoryName.ifBlank { it.type.name } }
                .map { (catName, txns) ->
                    val total = txns.sumOf { kotlin.math.abs(it.amount) }
                    val colorHex = getCategoryColorHex(catName, txns.firstOrNull()?.type)
                    CategorySummary(
                        categoryId = txns.first().categoryId,
                        categoryName = catName,
                        total = total,
                        isDebtFunding = false,
                        customColorHex = colorHex
                    )
                }
                .sortedByDescending { it.total }

            val items = mutableListOf<CategorySummary>()
            var runningOutflow = 0.0

            groupedCategories.forEach { catSummary ->
                val start = runningOutflow
                runningOutflow += catSummary.total

                if (runningOutflow <= incomeTotal) {
                    // Fully income funded
                    items.add(catSummary.copy(isDebtFunding = false))
                } else if (start >= incomeTotal) {
                    // Fully debt funded
                    items.add(catSummary.copy(
                        categoryName = "${catSummary.categoryName} (Debt)",
                        isDebtFunding = true
                    ))
                } else {
                    // Partially income funded & partially debt funded
                    val incomePart = (incomeTotal - start).coerceAtLeast(0.0)
                    val debtPart = (runningOutflow - incomeTotal).coerceAtLeast(0.0)
                    if (incomePart > 0) {
                        items.add(catSummary.copy(total = incomePart, isDebtFunding = false))
                    }
                    if (debtPart > 0) {
                        items.add(catSummary.copy(
                            categoryName = "${catSummary.categoryName} (Debt)",
                            total = debtPart,
                            isDebtFunding = true
                        ))
                    }
                }
            }

            if (incomeTotal > totalOutflows) {
                val remaining = incomeTotal - totalOutflows
                items.add(CategorySummary(104L, "Remaining Income", remaining, isDebtFunding = false, customColorHex = 0xFF2E7D32))
            }
            items
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeUsageDetailBreakdown: StateFlow<List<CategorySummary>> = payMonthTransactions
        .map { list ->
            val active = list.filter { it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
            val incomeTotal = active.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
            val outflowTxns = active.filter { it.type != TransactionType.INCOME }
            val totalOutflows = outflowTxns.sumOf { kotlin.math.abs(it.amount) }

            val outflowDetails = outflowTxns
                .groupBy { it.detail.ifBlank { it.subCategory.ifBlank { it.categoryName } } }
                .map { (detailName, txns) ->
                    val catName = txns.first().categoryName
                    val colorHex = getCategoryColorHex(catName, txns.first().type)
                    CategorySummary(
                        categoryId = txns.first().categoryId,
                        categoryName = detailName,
                        total = txns.sumOf { kotlin.math.abs(it.amount) },
                        customColorHex = colorHex,
                        isDebtFunding = false
                    )
                }
                .sortedByDescending { it.total }

            val items = mutableListOf<CategorySummary>()
            var runningOutflow = 0.0
            outflowDetails.forEach { detail ->
                val start = runningOutflow
                runningOutflow += detail.total
                if (runningOutflow <= incomeTotal) {
                    items.add(detail.copy(isDebtFunding = false))
                } else if (start >= incomeTotal) {
                    items.add(detail.copy(
                        categoryName = "${detail.categoryName} (Debt)",
                        isDebtFunding = true
                    ))
                } else {
                    val incomePart = (incomeTotal - start).coerceAtLeast(0.0)
                    val debtPart = (runningOutflow - incomeTotal).coerceAtLeast(0.0)
                    if (incomePart > 0) {
                        items.add(detail.copy(total = incomePart, isDebtFunding = false))
                    }
                    if (debtPart > 0) {
                        items.add(detail.copy(
                            categoryName = "${detail.categoryName} (Debt)",
                            total = debtPart,
                            isDebtFunding = true
                        ))
                    }
                }
            }

            if (incomeTotal > totalOutflows) {
                val remaining = incomeTotal - totalOutflows
                items.add(CategorySummary(104L, "Remaining Income", remaining, isDebtFunding = false, customColorHex = 0xFF2E7D32))
            }
            items
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val investmentBreakdown: StateFlow<List<CategorySummary>> = payMonthTransactions
        .map { list ->
            list.filter { it.type == TransactionType.INVESTMENT && it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
                .groupBy { it.subCategory.ifBlank { it.categoryName } }
                .map { (subCatName, txns) ->
                    CategorySummary(
                        categoryId = txns.first().categoryId,
                        categoryName = subCatName,
                        total = txns.sumOf { kotlin.math.abs(it.amount) }
                    )
                }
                .sortedByDescending { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val investmentDetailBreakdown: StateFlow<List<CategorySummary>> = payMonthTransactions
        .map { list ->
            list.filter { it.type == TransactionType.INVESTMENT && it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
                .groupBy { it.detail.ifBlank { it.subCategory.ifBlank { it.categoryName } } }
                .map { (detailName, txns) ->
                    CategorySummary(
                        categoryId = txns.first().categoryId,
                        categoryName = detailName,
                        total = txns.sumOf { kotlin.math.abs(it.amount) }
                    )
                }
                .sortedByDescending { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val educationBreakdown: StateFlow<List<CategorySummary>> = payMonthTransactions
        .map { list ->
            list.filter { it.type == TransactionType.EDUCATION && it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
                .groupBy { it.subCategory.ifBlank { it.categoryName } }
                .map { (subCatName, txns) ->
                    CategorySummary(
                        categoryId = txns.first().categoryId,
                        categoryName = subCatName,
                        total = txns.sumOf { kotlin.math.abs(it.amount) }
                    )
                }
                .sortedByDescending { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val educationDetailBreakdown: StateFlow<List<CategorySummary>> = payMonthTransactions
        .map { list ->
            list.filter { it.type == TransactionType.EDUCATION && it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
                .groupBy { it.detail.ifBlank { it.subCategory.ifBlank { it.categoryName } } }
                .map { (detailName, txns) ->
                    CategorySummary(
                        categoryId = txns.first().categoryId,
                        categoryName = detailName,
                        total = txns.sumOf { kotlin.math.abs(it.amount) }
                    )
                }
                .sortedByDescending { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val expenseBreakdown: StateFlow<List<CategorySummary>> = payMonthTransactions
        .map { list ->
            list.filter { it.type == TransactionType.EXPENSE && it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
                .groupBy { it.subCategory.ifBlank { it.categoryName } }
                .map { (subCatName, txns) ->
                    CategorySummary(
                        categoryId = txns.first().categoryId,
                        categoryName = subCatName,
                        total = txns.sumOf { kotlin.math.abs(it.amount) }
                    )
                }
                .sortedByDescending { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val expenseDetailBreakdown: StateFlow<List<CategorySummary>> = payMonthTransactions
        .map { list ->
            list.filter { it.type == TransactionType.EXPENSE && it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
                .groupBy { it.detail.ifBlank { it.subCategory.ifBlank { it.categoryName } } }
                .map { (detailName, txns) ->
                    CategorySummary(
                        categoryId = txns.first().categoryId,
                        categoryName = detailName,
                        total = txns.sumOf { kotlin.math.abs(it.amount) }
                    )
                }
                .sortedByDescending { it.total }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Dynamic Breakdown for any Custom Categories with transactions in the pay period
    val customCategoryBreakdowns: StateFlow<List<CustomCategoryBreakdown>> = payMonthTransactions
        .map { list ->
            val standardNames = setOf("income", "investment", "education", "expense")
            val nonStandardTxns = list.filter {
                it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE &&
                !standardNames.contains(it.categoryName.trim().lowercase())
            }
            nonStandardTxns.groupBy { it.categoryName.trim() }
                .map { (catName, txns) ->
                    val total = txns.sumOf { kotlin.math.abs(it.amount) }
                    val subCatSummaries = txns.groupBy { it.subCategory.ifBlank { it.categoryName } }
                        .map { (subCat, subTxns) ->
                            CategorySummary(
                                categoryId = subTxns.first().categoryId,
                                categoryName = subCat,
                                total = subTxns.sumOf { kotlin.math.abs(it.amount) }
                            )
                        }.sortedByDescending { it.total }

                    val detailSummaries = txns.groupBy { it.detail.ifBlank { it.subCategory.ifBlank { it.categoryName } } }
                        .map { (det, detTxns) ->
                            CategorySummary(
                                categoryId = detTxns.first().categoryId,
                                categoryName = det,
                                total = detTxns.sumOf { kotlin.math.abs(it.amount) }
                            )
                        }.sortedByDescending { it.total }

                    CustomCategoryBreakdown(
                        categoryName = catName,
                        totalAmount = total,
                        subCategorySummaries = subCatSummaries,
                        detailSummaries = detailSummaries
                    )
                }
                .sortedByDescending { it.totalAmount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Selectable sort criteria for Dashboard (default: Amount DESC)
    private val _secondarySorts = MutableStateFlow<List<SortCriterion>>(
        listOf(SortCriterion(TransactionsViewModel.SortField.AMOUNT, TransactionsViewModel.SortDirection.DESC))
    )
    val secondarySorts: StateFlow<List<SortCriterion>> = _secondarySorts.asStateFlow()

    fun onHeaderClicked(field: TransactionsViewModel.SortField) {
        if (field == TransactionsViewModel.SortField.TYPE) return
        val current = _secondarySorts.value
        if (current.size > 1) {
            val index = current.indexOfFirst { it.field == field }
            if (index >= 0) {
                _secondarySorts.value = current.filter { it.field != field }
            } else {
                _secondarySorts.value = listOf(SortCriterion(field, TransactionsViewModel.SortDirection.ASC))
            }
        } else if (current.size == 1) {
            val single = current.first()
            if (single.field == field) {
                val newDirection = if (single.direction == TransactionsViewModel.SortDirection.ASC) TransactionsViewModel.SortDirection.DESC else TransactionsViewModel.SortDirection.ASC
                _secondarySorts.value = listOf(single.copy(direction = newDirection))
            } else {
                _secondarySorts.value = listOf(SortCriterion(field, TransactionsViewModel.SortDirection.ASC))
            }
        } else {
            _secondarySorts.value = listOf(SortCriterion(field, TransactionsViewModel.SortDirection.ASC))
        }
    }

    fun onHeaderLongPressed(field: TransactionsViewModel.SortField) {
        if (field == TransactionsViewModel.SortField.TYPE) return
        val current = _secondarySorts.value.toMutableList()
        val index = current.indexOfFirst { it.field == field }
        if (index >= 0) {
            val item = current[index]
            val newDirection = if (item.direction == TransactionsViewModel.SortDirection.ASC) TransactionsViewModel.SortDirection.DESC else TransactionsViewModel.SortDirection.ASC
            current[index] = item.copy(direction = newDirection)
        } else {
            current.add(SortCriterion(field, TransactionsViewModel.SortDirection.ASC))
        }
        _secondarySorts.value = current
    }

    val recentTransactions: StateFlow<List<TransactionWithCategory>> = combine(
        repository.observeAllTransactions(),
        _secondarySorts
    ) { list, secondarySorts ->
        list.sortedWith(buildTransactionComparator(secondarySorts))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun reorderTransactions(reordered: List<TransactionWithCategory>) {
        viewModelScope.launch {
            repository.reorderTransactions(reordered.map { it.id })
        }
    }
}

// Transactions ViewModel
class TransactionsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            com.moneytracker.util.RecurringTransactionManager.processRecurringTransactionsIfDue(repository)
        }
    }

    private val payDateDay = com.moneytracker.util.SettingsManager.getPayDateDay()
    private val _selectedPayMonthDate = MutableStateFlow(DateUtils.currentPayMonthLocalDate(LocalDate.now(), payDateDay))
    val selectedPayMonthDate: StateFlow<LocalDate> = _selectedPayMonthDate.asStateFlow()

    fun setPayMonth(date: LocalDate) {
        _selectedPayMonthDate.value = date
        viewModelScope.launch {
            com.moneytracker.util.RecurringTransactionManager.processRecurringTransactions(repository)
        }
    }

    private val _filterType = MutableStateFlow<TransactionType?>(null)
    val filterType: StateFlow<TransactionType?> = _filterType.asStateFlow()

    enum class SortField { ID, DATE, AMOUNT, CATEGORY, SUBCATEGORY, DESCRIPTION, TYPE }
    enum class SortDirection { ASC, DESC }

    // Secondary and multi-column sort priority list (default selectable sort is AMOUNT)
    private val _secondarySorts = MutableStateFlow<List<SortCriterion>>(
        listOf(SortCriterion(SortField.AMOUNT, SortDirection.DESC))
    )
    val secondarySorts: StateFlow<List<SortCriterion>> = _secondarySorts.asStateFlow()

    fun onHeaderClicked(field: SortField) {
        if (field == SortField.TYPE) return
        val current = _secondarySorts.value
        if (current.size > 1) {
            val index = current.indexOfFirst { it.field == field }
            if (index >= 0) {
                _secondarySorts.value = current.filter { it.field != field }
            } else {
                _secondarySorts.value = listOf(SortCriterion(field, SortDirection.ASC))
            }
        } else if (current.size == 1) {
            val single = current.first()
            if (single.field == field) {
                val newDirection = if (single.direction == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
                _secondarySorts.value = listOf(single.copy(direction = newDirection))
            } else {
                _secondarySorts.value = listOf(SortCriterion(field, SortDirection.ASC))
            }
        } else {
            _secondarySorts.value = listOf(SortCriterion(field, SortDirection.ASC))
        }
    }

    fun onHeaderLongPressed(field: SortField) {
        if (field == SortField.TYPE) return
        val current = _secondarySorts.value.toMutableList()
        val index = current.indexOfFirst { it.field == field }
        if (index >= 0) {
            val item = current[index]
            val newDirection = if (item.direction == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
            current[index] = item.copy(direction = newDirection)
        } else {
            current.add(SortCriterion(field, SortDirection.ASC))
        }
        _secondarySorts.value = current
    }

    val transactions: StateFlow<List<TransactionWithCategory>> = combine(
        repository.observeAllTransactions(),
        _filterType,
        _secondarySorts,
        _selectedPayMonthDate
    ) { list, type, secondarySorts, monthDate ->
        val startMillis = DateUtils.startOfPayMonth(monthDate, payDateDay)
        val endMillis = DateUtils.startOfNextPayMonth(monthDate, payDateDay) - 1
        val monthFiltered = list.filter { it.date in startMillis..endMillis }
        val filtered = if (type == null) monthFiltered else monthFiltered.filter { it.type == type }
        filtered.sortedWith(buildTransactionComparator(secondarySorts))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(type: TransactionType?) {
        Log.d("TransactionsViewModel", "setFilter called – type: $type")
        _filterType.value = type
    }

    fun clearFilter() { setFilter(null) }

    fun deleteTransaction(transaction: TransactionWithCategory) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction.toEntity())
        }
    }

    fun reorderTransactions(reordered: List<TransactionWithCategory>) {
        viewModelScope.launch {
            repository.reorderTransactions(reordered.map { it.id })
        }
    }
}

// UI State for Add/Edit Screen
data class AddEditUiState(
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val subCategory: String = "",
    val detail: String = "",
    val isRecurring: Boolean = false,
    val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.ONCE_OFF,
    val recurTillDate: LocalDate? = null,
    val recurCount: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val updatedAt: Long = 0L,
    val duplicateCandidate: com.moneytracker.ui.components.DuplicateComparisonData? = null,
    val pendingNewItem: com.moneytracker.ui.components.DuplicateComparisonData? = null,
    val duplicateCandidateEntity: TransactionEntity? = null
)

// Add/Edit ViewModel
class AddEditViewModel(
    private val repository: TransactionRepository,
    private val initialTransactionId: Long?
) : ViewModel() {
    private var currentTransactionId: Long? = initialTransactionId
    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()
    private var existingSortOrder: Int = 0

    init {
        viewModelScope.launch {
            if (initialTransactionId != null) {
                val transaction = repository.getTransaction(initialTransactionId)
                if (transaction != null) {
                    existingSortOrder = transaction.sortOrder
                    val absAmt = kotlin.math.abs(transaction.amount)
                    val formattedAmt = if (transaction.formula.isNotBlank()) {
                        transaction.formula
                    } else if (absAmt == absAmt.toLong().toDouble()) {
                        absAmt.toLong().toString()
                    } else {
                        absAmt.toString()
                    }
                    val initialFreq = transaction.recurrenceFrequency ?: (if (transaction.isRecurring) RecurrenceFrequency.MONTHLY else RecurrenceFrequency.ONCE_OFF)
                    val normalizedFreq = if (initialFreq == RecurrenceFrequency.CONTINUOUS) RecurrenceFrequency.MONTHLY else initialFreq
                    _uiState.value = _uiState.value.copy(
                        amount = formattedAmt,
                        type = transaction.type,
                        categoryId = transaction.categoryId,
                        date = DateUtils.toLocalDate(transaction.date),
                        note = transaction.note,
                        subCategory = transaction.subCategory,
                        detail = transaction.detail,
                        isRecurring = transaction.isRecurring,
                        recurrenceFrequency = normalizedFreq,
                        recurTillDate = transaction.recurTillDate?.let { DateUtils.toLocalDate(it) },
                        recurCount = transaction.recurCount?.toString() ?: "",
                        updatedAt = transaction.updatedAt,
                        isLoading = false
                    )
                    observeCategories(transaction.type)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Transaction not found"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
                observeCategories(_uiState.value.type)
            }
        }
    }

    private fun observeCategories(type: TransactionType) {
        viewModelScope.launch {
            repository.observeCategories(type).collect { categories ->
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    categoryId = _uiState.value.categoryId ?: categories.firstOrNull()?.id
                )
            }
        }
    }

    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
    }

    fun updateType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type, categoryId = null)
        observeCategories(type)
    }

    fun updateCategory(categoryId: Long) {
        _uiState.value = _uiState.value.copy(categoryId = categoryId)
    }

    fun updateDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
        recalculateRecurrence(date = date)
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun updateSubCategory(subCategory: String) {
        _uiState.value = _uiState.value.copy(subCategory = subCategory)
    }

    fun updateDetail(detail: String) {
        _uiState.value = _uiState.value.copy(detail = detail)
    }

    fun updateIsRecurring(isRecurring: Boolean) {
        val currentFreq = _uiState.value.recurrenceFrequency
        val newFreq = if (isRecurring) {
            if (currentFreq == RecurrenceFrequency.ONCE_OFF || currentFreq == RecurrenceFrequency.TENTATIVE_FORECAST) RecurrenceFrequency.MONTHLY else currentFreq
        } else {
            RecurrenceFrequency.ONCE_OFF
        }
        _uiState.value = _uiState.value.copy(isRecurring = isRecurring, recurrenceFrequency = newFreq)
    }

    fun updateRecurrenceFrequency(freq: RecurrenceFrequency) {
        val state = _uiState.value
        val normalized = if (freq == RecurrenceFrequency.CONTINUOUS) RecurrenceFrequency.MONTHLY else freq
        val isRec = normalized != RecurrenceFrequency.ONCE_OFF
        _uiState.value = state.copy(isRecurring = isRec, recurrenceFrequency = normalized)
        recalculateRecurrence(freq = normalized)
    }

    fun updateRecurTillDate(date: LocalDate?) {
        val state = _uiState.value
        if (date == null) {
            _uiState.value = state.copy(recurTillDate = null, recurCount = "")
            return
        }
        val calcCount = com.moneytracker.util.RecurringTransactionManager.calculateCount(state.date, state.recurrenceFrequency, date)
        _uiState.value = state.copy(recurTillDate = date, recurCount = calcCount.toString())
    }

    fun updateRecurCount(count: String) {
        val state = _uiState.value
        if (count.isEmpty()) {
            _uiState.value = state.copy(recurCount = "", recurTillDate = null)
            return
        }
        if (count.all { it.isDigit() }) {
            val n = count.toIntOrNull()
            if (n != null && n > 0) {
                val calcTill = com.moneytracker.util.RecurringTransactionManager.calculateTillDate(state.date, state.recurrenceFrequency, n)
                _uiState.value = state.copy(recurCount = count, recurTillDate = calcTill)
            } else {
                _uiState.value = state.copy(recurCount = count)
            }
        }
    }

    private fun recalculateRecurrence(
        date: LocalDate = _uiState.value.date,
        freq: RecurrenceFrequency = _uiState.value.recurrenceFrequency
    ) {
        val state = _uiState.value
        val countInt = state.recurCount.toIntOrNull()
        if (countInt != null && countInt > 0) {
            val newTill = com.moneytracker.util.RecurringTransactionManager.calculateTillDate(date, freq, countInt)
            _uiState.value = state.copy(recurTillDate = newTill)
        } else if (state.recurTillDate != null) {
            val newCount = com.moneytracker.util.RecurringTransactionManager.calculateCount(date, freq, state.recurTillDate)
            _uiState.value = state.copy(recurCount = newCount.toString())
        }
    }

    fun dismissDuplicateDialog() {
        _uiState.value = _uiState.value.copy(
            duplicateCandidate = null,
            pendingNewItem = null,
            duplicateCandidateEntity = null
        )
    }

    fun swapToExistingDuplicate() {
        val existing = _uiState.value.duplicateCandidateEntity ?: return
        val absAmt = kotlin.math.abs(existing.amount)
        val formattedAmt = if (existing.formula.isNotBlank()) {
            existing.formula
        } else if (absAmt == absAmt.toLong().toDouble()) {
            absAmt.toLong().toString()
        } else {
            absAmt.toString()
        }
        val initialFreq = existing.recurrenceFrequency ?: (if (existing.isRecurring) RecurrenceFrequency.MONTHLY else RecurrenceFrequency.ONCE_OFF)
        val normalizedFreq = if (initialFreq == RecurrenceFrequency.CONTINUOUS) RecurrenceFrequency.MONTHLY else initialFreq
        _uiState.value = _uiState.value.copy(
            amount = formattedAmt,
            type = existing.type,
            categoryId = existing.categoryId,
            date = DateUtils.toLocalDate(existing.date),
            note = existing.note,
            subCategory = existing.subCategory,
            detail = existing.detail,
            isRecurring = existing.isRecurring,
            recurrenceFrequency = normalizedFreq,
            recurTillDate = existing.recurTillDate?.let { DateUtils.toLocalDate(it) },
            recurCount = existing.recurCount?.toString() ?: "",
            updatedAt = existing.updatedAt,
            duplicateCandidate = null,
            pendingNewItem = null,
            duplicateCandidateEntity = null
        )
        existingSortOrder = existing.sortOrder
        currentTransactionId = existing.id
    }

    fun save(onSuccess: () -> Unit, forceSave: Boolean = false) {
        val state = _uiState.value
        val isFormulaInput = com.moneytracker.util.FormulaEvaluator.isFormula(state.amount)
        val amount = if (isFormulaInput) {
            com.moneytracker.util.FormulaEvaluator.evaluate(state.amount)
        } else {
            state.amount.toDoubleOrNull()
        }
        val categoryId = state.categoryId

        when {
            amount == null || amount <= 0 -> {
                _uiState.value = state.copy(errorMessage = "Enter a valid amount or formula (e.g. = 2*5*30.00)")
                return
            }
            categoryId == null -> {
                _uiState.value = state.copy(errorMessage = "Select a category")
                return
            }
        }

        val absAmount = kotlin.math.abs(amount)
        val formulaToSave = if (isFormulaInput) state.amount.trim() else ""
        val isPlanFuture = state.recurrenceFrequency == RecurrenceFrequency.PLAN_FUTURE

        viewModelScope.launch {
            // Check 3-point duplicate (Category + SubCategory + Detail) for active entities
            if (!forceSave && (currentTransactionId == null || currentTransactionId == 0L)) {
                val payDateDay = com.moneytracker.util.SettingsManager.getPayDateDay()
                val currentPayMonthStartMillis = DateUtils.startOfPayMonth(DateUtils.currentPayMonthLocalDate(LocalDate.now(), payDateDay), payDateDay)
                val allTxns = repository.getAllEntitiesForProfile(repository.activeProfileId)
                val matchingDuplicate = allTxns.firstOrNull { existing ->
                    existing.id != (currentTransactionId ?: 0L) &&
                    existing.categoryId == categoryId &&
                    existing.subCategory.trim().equals(state.subCategory.trim(), ignoreCase = true) &&
                    existing.detail.trim().equals(state.detail.trim(), ignoreCase = true) &&
                    // Exclude ended recurrences and past once-offs
                    !(existing.isRecurred && !existing.isRecurring) &&
                    !(existing.date < currentPayMonthStartMillis && !existing.isRecurring)
                }

                if (matchingDuplicate != null) {
                    val catName = state.categories.find { it.id == categoryId }?.name ?: "Category"
                    val existCatName = state.categories.find { it.id == matchingDuplicate.categoryId }?.name ?: catName
                    val existingData = com.moneytracker.ui.components.DuplicateComparisonData(
                        categoryName = existCatName,
                        subCategory = matchingDuplicate.subCategory,
                        detail = matchingDuplicate.detail,
                        amount = matchingDuplicate.amount,
                        dateMillis = matchingDuplicate.date,
                        typeName = matchingDuplicate.type.name,
                        isRecurring = matchingDuplicate.isRecurring,
                        recurrenceFreqName = matchingDuplicate.recurrenceFrequency?.name ?: "ONCE_OFF",
                        note = matchingDuplicate.note
                    )
                    val newData = com.moneytracker.ui.components.DuplicateComparisonData(
                        categoryName = catName,
                        subCategory = state.subCategory,
                        detail = state.detail,
                        amount = absAmount,
                        dateMillis = DateUtils.toEpochMillis(state.date),
                        typeName = state.type.name,
                        isRecurring = state.isRecurring,
                        recurrenceFreqName = state.recurrenceFrequency.name,
                        note = state.note
                    )
                    _uiState.value = state.copy(
                        duplicateCandidate = existingData,
                        pendingNewItem = newData,
                        duplicateCandidateEntity = matchingDuplicate
                    )
                    return@launch
                }
            }

            _uiState.value = state.copy(
                isSaving = true,
                errorMessage = null,
                duplicateCandidate = null,
                pendingNewItem = null,
                duplicateCandidateEntity = null
            )
            val finalFreq = if (state.isRecurring) {
                if (state.recurrenceFrequency == RecurrenceFrequency.ONCE_OFF) RecurrenceFrequency.MONTHLY else state.recurrenceFrequency
            } else {
                RecurrenceFrequency.ONCE_OFF
            }
            val entityToSave = TransactionEntity(
                id = currentTransactionId ?: 0L,
                amount = absAmount,
                type = state.type,
                categoryId = categoryId,
                date = DateUtils.toEpochMillis(state.date),
                note = state.note.trim(),
                sortOrder = existingSortOrder,
                subCategory = state.subCategory.trim(),
                detail = state.detail.trim(),
                formula = formulaToSave,
                isRecurring = state.isRecurring && finalFreq != RecurrenceFrequency.ONCE_OFF,
                recurrenceFrequency = finalFreq,
                recurTillDate = if (state.isRecurring && !isPlanFuture && state.recurTillDate != null) DateUtils.toEpochMillis(state.recurTillDate) else null,
                recurCount = if (state.isRecurring && !isPlanFuture) state.recurCount.toIntOrNull() else null,
                isRecurred = false
            )
            val savedId = repository.saveTransaction(entityToSave)
            val savedEntity = entityToSave.copy(id = if (entityToSave.id == 0L) savedId else entityToSave.id)

            // Auto-generate due recurring transaction instances specifically for this item
            if (savedEntity.isRecurring) {
                com.moneytracker.util.RecurringTransactionManager.processSingleTransactionRecurrence(repository, savedEntity)
            }

            // Run sort process across transactions
            val all = repository.observeAllTransactions().firstOrNull() ?: emptyList()
            val sorted = all.sortedWith(buildTransactionComparator(emptyList()))
            repository.reorderTransactions(sorted.map { it.id })

            _uiState.value = _uiState.value.copy(isSaving = false)
            onSuccess()
        }
    }

    fun delete(onSuccess: () -> Unit) {
        val idToDelete = currentTransactionId ?: return
        if (idToDelete == 0L) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val transaction = repository.getTransaction(idToDelete)
            if (transaction != null) {
                repository.deleteTransaction(transaction)
            }
            _uiState.value = _uiState.value.copy(isSaving = false)
            onSuccess()
        }
    }
}

// Stats ViewModel
class StatsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    private val payDateDay = com.moneytracker.util.SettingsManager.getPayDateDay()
    private val _selectedPayMonthDate = MutableStateFlow(DateUtils.currentPayMonthLocalDate(LocalDate.now(), payDateDay))
    val selectedPayMonthDate: StateFlow<LocalDate> = _selectedPayMonthDate.asStateFlow()

    fun setPayMonth(date: LocalDate) {
        _selectedPayMonthDate.value = date
    }

    private val _filterType = MutableStateFlow<TransactionType?>(null)
    val filterType: StateFlow<TransactionType?> = _filterType.asStateFlow()

    fun setFilter(type: TransactionType?) {
        _filterType.value = type
    }

    fun clearFilter() {
        setFilter(null)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val summary: StateFlow<MonthlySummary> = _selectedPayMonthDate.flatMapLatest { date ->
        val startMillis = DateUtils.toEpochMillis(date)
        val endMillis = DateUtils.toEpochMillis(date.plusMonths(1)) - 1
        repository.observeMonthlySummary(startMillis, endMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlySummary(0.0, 0.0, 0.0, 0.0))

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val expenseBreakdown: StateFlow<List<CategorySummary>> = _selectedPayMonthDate.flatMapLatest { date ->
        val startMillis = DateUtils.toEpochMillis(date)
        val endMillis = DateUtils.toEpochMillis(date.plusMonths(1)) - 1
        repository.observeExpenseCategorySummaries(startMillis, endMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val incomeBreakdown: StateFlow<List<CategorySummary>> = _selectedPayMonthDate.flatMapLatest { date ->
        val startMillis = DateUtils.toEpochMillis(date)
        val endMillis = DateUtils.toEpochMillis(date.plusMonths(1)) - 1
        repository.observeIncomeCategorySummaries(startMillis, endMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val investmentBreakdown: StateFlow<List<CategorySummary>> = _selectedPayMonthDate.flatMapLatest { date ->
        val startMillis = DateUtils.toEpochMillis(date)
        val endMillis = DateUtils.toEpochMillis(date.plusMonths(1)) - 1
        repository.observeInvestmentCategorySummaries(startMillis, endMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

// Settings ViewModel
class SettingsViewModel : ViewModel() {
    val settings: StateFlow<com.moneytracker.util.UserSettings> = com.moneytracker.util.SettingsManager.settings

    fun updatePayDateDay(day: Int) {
        com.moneytracker.util.SettingsManager.updatePayDateDay(day)
    }

    fun updateCutoffDay(day: Int) {
        com.moneytracker.util.SettingsManager.updateCutoffDay(day)
    }

    fun updateMorningCutoffHour(hour: Int) {
        com.moneytracker.util.SettingsManager.updateMorningCutoffHour(hour)
    }

    fun updateIsRyuHidden(hidden: Boolean) {
        com.moneytracker.util.SettingsManager.updateIsRyuHidden(hidden)
    }

    fun updateThemePalette(palette: com.moneytracker.util.AppThemePalette) {
        com.moneytracker.util.SettingsManager.updateThemePalette(palette)
    }

    fun updateThemeMode(mode: com.moneytracker.util.AppThemeMode) {
        com.moneytracker.util.SettingsManager.updateThemeMode(mode)
    }
}

// ViewModel Factory
class ViewModelFactory(
    private val repository: TransactionRepository,
    private val transactionId: Long? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(TransactionsViewModel::class.java) -> TransactionsViewModel(repository) as T
            modelClass.isAssignableFrom(CategoriesViewModel::class.java) -> CategoriesViewModel(repository) as T
            modelClass.isAssignableFrom(SubCategoriesViewModel::class.java) -> SubCategoriesViewModel(repository) as T
            modelClass.isAssignableFrom(DetailsViewModel::class.java) -> DetailsViewModel(repository) as T
            modelClass.isAssignableFrom(AddEditViewModel::class.java) -> AddEditViewModel(repository, transactionId) as T
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> StatsViewModel(repository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel() as T
            modelClass.isAssignableFrom(GroceriesViewModel::class.java) -> GroceriesViewModel(repository) as T
            modelClass.isAssignableFrom(MonthComparisonViewModel::class.java) -> MonthComparisonViewModel(repository) as T
            modelClass.isAssignableFrom(TaxiFareViewModel::class.java) -> TaxiFareViewModel(repository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
