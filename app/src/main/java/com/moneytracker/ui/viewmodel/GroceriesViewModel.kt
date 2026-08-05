package com.moneytracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.local.entity.GroceryBudgetItemEntity
import com.moneytracker.data.local.entity.ShoppingListEntity
import com.moneytracker.data.local.entity.ShoppingListItemEntity
import com.moneytracker.data.local.entity.UnitSizeEntity
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.util.DateUtils
import com.moneytracker.util.SettingsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GroceryBudgetOverview(
    val totalBudget: Double = 0.0,
    val totalActual: Double = 0.0,
    val netVariance: Double = 0.0, // Budget - Actual
    val isOverBudget: Boolean = false,
    val overAmount: Double = 0.0
)

class GroceriesViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val payDateDay = SettingsManager.getPayDateDay()
    private val _selectedPayMonthDate = MutableStateFlow(DateUtils.currentPayMonthLocalDate(LocalDate.now(), payDateDay))
    val selectedPayMonthDate: StateFlow<LocalDate> = _selectedPayMonthDate.asStateFlow()

    fun setPayMonth(date: LocalDate) {
        _selectedPayMonthDate.value = date
        triggerAutoPopulate()
    }

    val monthTimestamp: StateFlow<Long> = _selectedPayMonthDate
        .map { DateUtils.toEpochMillis(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DateUtils.toEpochMillis(_selectedPayMonthDate.value))

    @OptIn(ExperimentalCoroutinesApi::class)
    val budgetItems: StateFlow<List<GroceryBudgetItemEntity>> = monthTimestamp
        .flatMapLatest { ts ->
            repository.observeGroceryBudgetForMonth(ts)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unitSizes: StateFlow<List<UnitSizeEntity>> = repository
        .observeUnitSizes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val shoppingLists: StateFlow<List<ShoppingListEntity>> = monthTimestamp
        .flatMapLatest { ts ->
            repository.observeShoppingListsForMonth(ts)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Selection set for generating shopping lists
    private val _selectedBudgetItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedBudgetItemIds: StateFlow<Set<Long>> = _selectedBudgetItemIds.asStateFlow()

    // Currently selected shopping list for popup dialog view
    private val _activeShoppingList = MutableStateFlow<ShoppingListEntity?>(null)
    val activeShoppingList: StateFlow<ShoppingListEntity?> = _activeShoppingList.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeShoppingListItems: StateFlow<List<ShoppingListItemEntity>> = _activeShoppingList
        .flatMapLatest { list ->
            if (list == null) flowOf(emptyList())
            else repository.observeShoppingListItems(list.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val overview: StateFlow<GroceryBudgetOverview> = budgetItems.map { items ->
        val totalB = items.sumOf { it.costBudget }
        val totalA = items.sumOf { it.costActual }
        val variance = totalB - totalA
        val isOver = totalA > totalB && totalB > 0.0
        val overAmt = if (isOver) totalA - totalB else 0.0

        GroceryBudgetOverview(
            totalBudget = totalB,
            totalActual = totalA,
            netVariance = variance,
            isOverBudget = isOver,
            overAmount = overAmt
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroceryBudgetOverview())

    init {
        seedDefaultUnitSizesIfNeeded()
        triggerAutoPopulate()
    }

    private fun triggerAutoPopulate() {
        viewModelScope.launch {
            val ts = monthTimestamp.value
            repository.autoPopulateRecurringAndPlannedGroceryItems(ts)
        }
    }

    private fun seedDefaultUnitSizesIfNeeded() {
        viewModelScope.launch {
            unitSizes.collect { sizes ->
                if (sizes.isEmpty()) {
                    val defaults = listOf(
                        "kg", "g", "Lit", "mL", "Bag", "Pocket", "Bottle", "Box", "Pack", "Tin", "Tray",
                        "6s", "12s", "18s", "24s", "30s", "48s", "60s"
                    )
                    defaults.forEach { name ->
                        repository.saveUnitSize(UnitSizeEntity(name = name))
                    }
                }
            }
        }
    }

    // Budget Item Mutations
    fun saveGroceryBudgetItem(
        id: Long = 0,
        category: String,
        subCategory: String,
        itemDetail: String,
        unitSize: String,
        note: String = "",
        quantityBudget: Int,
        unitPriceBudget: Double,
        isRecurring: Int // 0 = Once-off, 1 = Monthly Recurring, 2 = Planned
    ) {
        viewModelScope.launch {
            val ts = monthTimestamp.value
            val costB = quantityBudget * unitPriceBudget
            val item = GroceryBudgetItemEntity(
                id = id,
                date = ts,
                category = category.ifBlank { "Groceries" },
                subCategory = subCategory.ifBlank { "General" },
                itemDetail = itemDetail.ifBlank { subCategory },
                unitSize = unitSize.ifBlank { "pack" },
                note = note,
                quantityBudget = quantityBudget,
                unitPriceBudget = unitPriceBudget,
                costBudget = costB,
                isRecurring = isRecurring,
                quantityActual = 0,
                unitPriceActual = 0.0,
                costActual = 0.0
            )
            repository.saveGroceryBudgetItem(item)
        }
    }

    fun deleteGroceryBudgetItem(item: GroceryBudgetItemEntity) {
        viewModelScope.launch {
            repository.deleteGroceryBudgetItem(item)
        }
    }

    // Selection Handling
    fun toggleBudgetItemSelection(id: Long) {
        val current = _selectedBudgetItemIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedBudgetItemIds.value = current
    }

    fun selectAllBudgetItems() {
        _selectedBudgetItemIds.value = budgetItems.value.map { it.id }.toSet()
    }

    fun clearBudgetItemSelection() {
        _selectedBudgetItemIds.value = emptySet()
    }

    // Unit Size CRUD
    fun addUnitSize(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.saveUnitSize(UnitSizeEntity(name = name.trim()))
        }
    }

    fun deleteUnitSize(unitSize: UnitSizeEntity) {
        viewModelScope.launch {
            repository.deleteUnitSize(unitSize)
        }
    }

    // Shopping List Generation & In-Store Actions
    fun generateShoppingList(title: String, shoppingDateTimestamp: Long = System.currentTimeMillis()) {
        val selectedIds = _selectedBudgetItemIds.value
        val itemsToInclude = budgetItems.value.filter { selectedIds.contains(it.id) }
        if (itemsToInclude.isEmpty()) return

        viewModelScope.launch {
            val listTitle = title.ifBlank { "Shopping List (${itemsToInclude.size} items)" }
            val listId = repository.generateShoppingListFromBudget(
                payMonthTimestamp = monthTimestamp.value,
                shoppingDateTimestamp = shoppingDateTimestamp,
                title = listTitle,
                selectedBudgetItems = itemsToInclude
            )
            clearBudgetItemSelection()
            // Open newly created shopping list
            val sLists = repository.getGroceryBudgetForMonth(monthTimestamp.value)
            val newList = ShoppingListEntity(id = listId, payMonthDate = monthTimestamp.value, shoppingDate = shoppingDateTimestamp, title = listTitle)
            _activeShoppingList.value = newList
        }
    }

    fun openShoppingList(list: ShoppingListEntity?) {
        _activeShoppingList.value = list
    }

    fun toggleShoppingListItemChecked(item: ShoppingListItemEntity) {
        viewModelScope.launch {
            repository.updateShoppingListItem(item.copy(isChecked = !item.isChecked))
        }
    }

    fun updateShoppingListItemActuals(item: ShoppingListItemEntity, qtyActual: Int, priceActual: Double) {
        viewModelScope.launch {
            repository.updateShoppingListItem(
                item.copy(
                    quantityActual = qtyActual,
                    unitPriceActual = priceActual
                )
            )
        }
    }

    fun confirmAndCloseActiveShoppingList(createExpenseTxn: Boolean = true) {
        val active = _activeShoppingList.value ?: return
        viewModelScope.launch {
            repository.confirmAndCloseShoppingList(
                shoppingListId = active.id,
                createExpenseTransaction = createExpenseTxn
            )
            _activeShoppingList.value = null
        }
    }

    fun deleteShoppingList(list: ShoppingListEntity) {
        viewModelScope.launch {
            if (_activeShoppingList.value?.id == list.id) {
                _activeShoppingList.value = null
            }
            repository.deleteShoppingList(list)
        }
    }
}
