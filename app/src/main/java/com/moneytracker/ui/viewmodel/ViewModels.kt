package com.moneytracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.CategorySummary
import com.moneytracker.data.local.entity.TransactionEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.local.entity.TransactionWithCategory
import com.moneytracker.data.repository.MonthlySummary
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import java.time.LocalDate

// Dashboard ViewModel
class DashboardViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    private val monthStart = DateUtils.startOfMonth()
    private val monthEnd = DateUtils.startOfNextMonth()

    val summary: StateFlow<MonthlySummary> = repository
        .observeMonthlySummary(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlySummary(0.0, 0.0, 0.0, 0.0))

    val recentTransactions: StateFlow<List<TransactionWithCategory>> = repository
        .observeTransactionsForMonth(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

// Transactions ViewModel
data class SortCriterion(
    val field: TransactionsViewModel.SortField,
    val direction: TransactionsViewModel.SortDirection = TransactionsViewModel.SortDirection.ASC
)

class TransactionsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    private val _filterType = MutableStateFlow<TransactionType?>(null)
    val filterType: StateFlow<TransactionType?> = _filterType.asStateFlow()

    enum class SortField { ID, DATE, AMOUNT, CATEGORY, DESCRIPTION, TYPE }
    enum class SortDirection { ASC, DESC }

    // Secondary and multi-column sort priority list
    private val _secondarySorts = MutableStateFlow<List<SortCriterion>>(
        listOf(SortCriterion(SortField.DATE, SortDirection.DESC))
    )
    val secondarySorts: StateFlow<List<SortCriterion>> = _secondarySorts.asStateFlow()

    fun onHeaderClicked(field: SortField) {
        if (field == SortField.TYPE) return
        val current = _secondarySorts.value
        if (current.isNotEmpty() && current.first().field == field) {
            val first = current.first()
            val newDirection = if (first.direction == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
            _secondarySorts.value = listOf(first.copy(direction = newDirection))
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
        _secondarySorts
    ) { list, type, secondarySorts ->
        val filtered = if (type == null) list else list.filter { it.type == type }

        var comparator: Comparator<TransactionWithCategory> = compareBy {
            when (it.type) {
                TransactionType.INCOME -> 0
                TransactionType.INVESTMENT -> 1
                TransactionType.EXPENSE -> 2
            }
        }

        for (criterion in secondarySorts) {
            val selector: (TransactionWithCategory) -> Comparable<*>? = when (criterion.field) {
                SortField.ID -> { t -> t.id }
                SortField.DATE -> { t -> t.date }
                SortField.AMOUNT -> { t -> t.amount }
                SortField.CATEGORY -> { t -> t.categoryName }
                SortField.DESCRIPTION -> { t -> t.note }
                SortField.TYPE -> { t ->
                    when (t.type) {
                        TransactionType.INCOME -> 0
                        TransactionType.INVESTMENT -> 1
                        TransactionType.EXPENSE -> 2
                    }
                }
            }
            comparator = if (criterion.direction == SortDirection.ASC) {
                comparator.thenBy(selector)
            } else {
                comparator.thenByDescending(selector)
            }
        }

        filtered.sortedWith(comparator)
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
                    _uiState.value = _uiState.value.copy(
                        amount = transaction.amount.toString(),
                        type = transaction.type,
                        categoryId = transaction.categoryId,
                        date = DateUtils.toLocalDate(transaction.date),
                        note = transaction.note,
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
        if (value.isEmpty() || value.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
            _uiState.value = _uiState.value.copy(amount = value, errorMessage = null)
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
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
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

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            repository.saveTransaction(
                TransactionEntity(
                    id = transactionId ?: 0L,
                    amount = amount,
                    type = state.type,
                    categoryId = categoryId,
                    date = DateUtils.toEpochMillis(state.date),
                    note = state.note.trim(),
                    sortOrder = existingSortOrder
                )
            )
            _uiState.value = _uiState.value.copy(isSaving = false)
            onSuccess()
        }
    }
}

// Stats ViewModel
class StatsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    private val monthStart = DateUtils.startOfMonth()
    private val monthEnd = DateUtils.startOfNextMonth()

    private val _filterType = MutableStateFlow<TransactionType?>(null)
    val filterType: StateFlow<TransactionType?> = _filterType.asStateFlow()

    fun setFilter(type: TransactionType?) {
        _filterType.value = type
    }

    fun clearFilter() {
        setFilter(null)
    }

    val summary: StateFlow<MonthlySummary> = repository
        .observeMonthlySummary(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlySummary(0.0, 0.0, 0.0, 0.0))

    val expenseBreakdown: StateFlow<List<CategorySummary>> = repository
        .observeExpenseCategorySummaries(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeBreakdown: StateFlow<List<CategorySummary>> = repository
        .observeIncomeCategorySummaries(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val investmentBreakdown: StateFlow<List<CategorySummary>> = repository
        .observeInvestmentCategorySummaries(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
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
            modelClass.isAssignableFrom(AddEditViewModel::class.java) -> AddEditViewModel(repository, transactionId) as T
            modelClass.isAssignableFrom(StatsViewModel::class.java) -> StatsViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
