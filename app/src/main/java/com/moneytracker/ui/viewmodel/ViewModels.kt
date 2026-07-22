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
import java.time.LocalDate

class DashboardViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    private val monthStart = DateUtils.startOfMonth()
    private val monthEnd = DateUtils.startOfNextMonth()

    val summary: StateFlow<MonthlySummary> = repository
        .observeMonthlySummary(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlySummary(0.0, 0.0, 0.0))

    val recentTransactions: StateFlow<List<TransactionWithCategory>> = repository
        .observeTransactionsForMonth(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class TransactionsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    private val _filterType = MutableStateFlow<TransactionType?>(null)
    val filterType: StateFlow<TransactionType?> = _filterType.asStateFlow()

    val transactions: StateFlow<List<TransactionWithCategory>> = combine(
        repository.observeAllTransactions(),
        _filterType
    ) { list, type ->
        if (type == null) list else list.filter { it.type == type }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(type: TransactionType?) {
        _filterType.value = type
    }

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
                    current.categoryId != null && categories.any { it.id == current.categoryId } ->
                        current.categoryId
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

class StatsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    private val monthStart = DateUtils.startOfMonth()
    private val monthEnd = DateUtils.startOfNextMonth()

    val summary: StateFlow<MonthlySummary> = repository
        .observeMonthlySummary(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlySummary(0.0, 0.0, 0.0))

    val expenseBreakdown: StateFlow<List<CategorySummary>> = repository
        .observeExpenseCategorySummaries(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeBreakdown: StateFlow<List<CategorySummary>> = repository
        .observeIncomeCategorySummaries(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class ViewModelFactory(
    private val repository: TransactionRepository,
    private val transactionId: Long? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(TransactionsViewModel::class.java) ->
                TransactionsViewModel(repository) as T
            modelClass.isAssignableFrom(AddEditViewModel::class.java) ->
                AddEditViewModel(repository, transactionId) as T
            modelClass.isAssignableFrom(StatsViewModel::class.java) ->
                StatsViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
