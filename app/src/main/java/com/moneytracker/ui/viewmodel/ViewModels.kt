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
import java.time.LocalDate

// Sort Data Models
data class SortCriterion(
    val field: TransactionsViewModel.SortField,
    val direction: TransactionsViewModel.SortDirection = TransactionsViewModel.SortDirection.ASC
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
            MonthlySummary(
                income = incomeTotal,
                investment = investTotal,
                education = eduTotal,
                expense = expenseTotal,
                balance = incomeTotal - investTotal - eduTotal - expenseTotal
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

    // 2nd Pie: Income Funding / Utilization (How Income is Used & Remaining Balance)
    val incomeUsageBreakdown: StateFlow<List<CategorySummary>> = payMonthTransactions
        .map { list ->
            val active = list.filter { it.recurrenceFrequency != RecurrenceFrequency.PLAN_FUTURE }
            val incomeTotal = active.filter { it.type == TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }
            val expenseTotal = active.filter { it.type == TransactionType.EXPENSE }.sumOf { kotlin.math.abs(it.amount) }
            val investTotal = active.filter { it.type == TransactionType.INVESTMENT }.sumOf { kotlin.math.abs(it.amount) }
            val eduTotal = active.filter { it.type == TransactionType.EDUCATION }.sumOf { kotlin.math.abs(it.amount) }
            val totalOutflows = expenseTotal + investTotal + eduTotal
            val hasDeficit = totalOutflows > incomeTotal

            val items = mutableListOf<CategorySummary>()
            if (expenseTotal > 0) {
                items.add(CategorySummary(101L, "Expenses", expenseTotal, isDebtFunding = hasDeficit, customColorHex = 0xFFD32F2F))
            }
            if (investTotal > 0) {
                items.add(CategorySummary(102L, "Investments", investTotal, isDebtFunding = false, customColorHex = 0xFF1976D2))
            }
            if (eduTotal > 0) {
                items.add(CategorySummary(103L, "Education", eduTotal, isDebtFunding = false, customColorHex = 0xFF7B1FA2))
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
            val totalOutflows = active.filter { it.type != TransactionType.INCOME }.sumOf { kotlin.math.abs(it.amount) }

            val outflowDetails = active.filter { it.type != TransactionType.INCOME }
                .groupBy { it.detail.ifBlank { it.subCategory.ifBlank { it.categoryName } } }
                .map { (detailName, txns) ->
                    val type = txns.first().type
                    val hex = when (type) {
                        TransactionType.INVESTMENT -> 0xFF1976D2
                        TransactionType.EDUCATION -> 0xFF7B1FA2
                        else -> 0xFFD32F2F
                    }
                    CategorySummary(
                        categoryId = txns.first().categoryId,
                        categoryName = detailName,
                        total = txns.sumOf { kotlin.math.abs(it.amount) },
                        customColorHex = hex
                    )
                }
                .sortedByDescending { it.total }

            var runningOutflow = 0.0
            val items = outflowDetails.map { detail ->
                runningOutflow += detail.total
                val needsDebt = runningOutflow > incomeTotal
                detail.copy(isDebtFunding = needsDebt)
            }.toMutableList()

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
    val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val recurTillDate: LocalDate? = null,
    val recurCount: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

// Add/Edit ViewModel
class AddEditViewModel(
    private val repository: TransactionRepository,
    private val transactionId: Long?
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()
    private var existingSortOrder: Int = 0

    init {
        viewModelScope.launch {
            if (transactionId != null) {
                val transaction = repository.getTransaction(transactionId)
                if (transaction != null) {
                    existingSortOrder = transaction.sortOrder
                    val absAmt = kotlin.math.abs(transaction.amount)
                    val formattedAmt = if (absAmt == absAmt.toLong().toDouble()) absAmt.toLong().toString() else absAmt.toString()
                    _uiState.value = _uiState.value.copy(
                        amount = formattedAmt,
                        type = transaction.type,
                        categoryId = transaction.categoryId,
                        date = DateUtils.toLocalDate(transaction.date),
                        note = transaction.note,
                        subCategory = transaction.subCategory,
                        detail = transaction.detail,
                        isRecurring = transaction.isRecurring,
                        recurrenceFrequency = transaction.recurrenceFrequency ?: RecurrenceFrequency.MONTHLY,
                        recurTillDate = transaction.recurTillDate?.let { DateUtils.toLocalDate(it) },
                        recurCount = transaction.recurCount?.toString() ?: "",
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
                observeCategories(TransactionType.EXPENSE)
            }
        }
    }

    private fun observeCategories(type: TransactionType) {
        viewModelScope.launch {
            repository.observeCategories(type).collect { categories ->
                val current = _uiState.value
                val selectedCategory = when {
                    current.categoryId != null && categories.any { it.id == current.categoryId } -> current.categoryId
                    categories.isNotEmpty() -> categories.first().id
                    else -> null
                }
                _uiState.value = current.copy(categories = categories, categoryId = selectedCategory)
            }
        }
    }

    fun updateAmount(value: String) {
        val cleanValue = value.replace("+", "").replace("-", "").trim()
        if (cleanValue.isEmpty() || cleanValue.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
            _uiState.value = _uiState.value.copy(amount = cleanValue, errorMessage = null)
        }
    }

    fun updateType(type: TransactionType) {
        if (_uiState.value.type == type) return
        _uiState.value = _uiState.value.copy(type = type, categoryId = null)
        observeCategories(type)
    }

    fun updateCategory(categoryId: Long) {
        _uiState.value = _uiState.value.copy(categoryId = categoryId)
    }

    fun updateDate(date: LocalDate) {
        val state = _uiState.value
        _uiState.value = state.copy(date = date)
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
        _uiState.value = _uiState.value.copy(isRecurring = isRecurring)
    }

    fun updateRecurrenceFrequency(freq: RecurrenceFrequency) {
        val state = _uiState.value
        _uiState.value = state.copy(recurrenceFrequency = freq)
        recalculateRecurrence(freq = freq)
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

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull()
        val categoryId = state.categoryId

        when {
            amount == null || amount <= 0 -> {
                _uiState.value = state.copy(errorMessage = "Enter a valid amount")
                return
            }
            categoryId == null -> {
                _uiState.value = state.copy(errorMessage = "Select a category")
                return
            }
        }

        val absAmount = kotlin.math.abs(amount)
        val isContinuous = state.recurrenceFrequency == RecurrenceFrequency.CONTINUOUS
        val isPlanFuture = state.recurrenceFrequency == RecurrenceFrequency.PLAN_FUTURE

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            val entityToSave = TransactionEntity(
                id = transactionId ?: 0L,
                amount = absAmount,
                type = state.type,
                categoryId = categoryId,
                date = DateUtils.toEpochMillis(state.date),
                note = state.note.trim(),
                sortOrder = existingSortOrder,
                subCategory = state.subCategory.trim(),
                detail = state.detail.trim(),
                isRecurring = state.isRecurring,
                recurrenceFrequency = if (state.isRecurring) state.recurrenceFrequency else null,
                recurTillDate = if (state.isRecurring && !isContinuous && !isPlanFuture && state.recurTillDate != null) DateUtils.toEpochMillis(state.recurTillDate) else null,
                recurCount = if (state.isRecurring && !isContinuous && !isPlanFuture) state.recurCount.toIntOrNull() else null,
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
        if (transactionId == null || transactionId == 0L) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val transaction = repository.getTransaction(transactionId)
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
