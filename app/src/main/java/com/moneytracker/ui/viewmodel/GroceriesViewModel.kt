package com.moneytracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.local.entity.GroceryItemEntity
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

data class GroceriesBudgetSummary(
    val mainBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val isOverBudget: Boolean = false,
    val overBudgetAmount: Double = 0.0
)

class GroceriesViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val payDateDay = SettingsManager.getPayDateDay()
    private val _selectedPayMonthDate = MutableStateFlow(DateUtils.currentPayMonthLocalDate(LocalDate.now(), payDateDay))
    val selectedPayMonthDate: StateFlow<LocalDate> = _selectedPayMonthDate.asStateFlow()

    fun setPayMonth(date: LocalDate) {
        _selectedPayMonthDate.value = date
    }

    val items: StateFlow<List<GroceryItemEntity>> = repository
        .observeAllGroceryItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetSummary: StateFlow<GroceriesBudgetSummary> = combine(
        _selectedPayMonthDate.flatMapLatest { date ->
            val startMillis = DateUtils.toEpochMillis(date)
            val endMillis = DateUtils.toEpochMillis(date.plusMonths(1)) - 1
            repository.observeTransactionsForMonth(startMillis, endMillis)
        },
        items
    ) { mainTxns, groceryList ->
        val groceriesCategoryBudget = mainTxns
            .filter { it.type == TransactionType.EXPENSE && (it.categoryName.equals("Groceries", ignoreCase = true) || it.subCategory.contains("Groceries", ignoreCase = true)) }
            .sumOf { kotlin.math.abs(it.amount) }

        val totalSpent = groceryList.sumOf { it.totalPrice }
        val remaining = groceriesCategoryBudget - totalSpent
        val isOver = totalSpent > groceriesCategoryBudget && groceriesCategoryBudget > 0.0
        val overAmt = if (isOver) totalSpent - groceriesCategoryBudget else 0.0

        GroceriesBudgetSummary(
            mainBudget = groceriesCategoryBudget,
            totalSpent = totalSpent,
            remainingBudget = remaining,
            isOverBudget = isOver,
            overBudgetAmount = overAmt
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroceriesBudgetSummary())

    fun toggleCheck(item: GroceryItemEntity) {
        viewModelScope.launch {
            repository.saveGroceryItem(item.copy(isChecked = !item.isChecked))
        }
    }

    fun saveGroceryItem(
        id: Long = 0,
        itemName: String,
        unitPrice: Double,
        quantity: Int,
        sizeUnit: String
    ) {
        viewModelScope.launch {
            val total = unitPrice * quantity
            val item = GroceryItemEntity(
                id = id,
                date = System.currentTimeMillis(),
                itemName = itemName,
                unitPrice = unitPrice,
                quantity = quantity,
                sizeUnit = sizeUnit,
                totalPrice = total
            )
            repository.saveGroceryItem(item)
        }
    }

    fun deleteGroceryItem(item: GroceryItemEntity) {
        viewModelScope.launch {
            repository.deleteGroceryItem(item)
        }
    }
}
