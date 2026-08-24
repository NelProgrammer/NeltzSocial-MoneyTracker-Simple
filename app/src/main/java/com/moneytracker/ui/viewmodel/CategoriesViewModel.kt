package com.moneytracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    // Observe all categories regardless of type
    val categories: StateFlow<List<CategoryEntity>> = repository
        .observeAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // UI state for editing a category
    data class EditState(
        val id: Long = 0L,
        val name: String = "",
        val type: TransactionType = TransactionType.EXPENSE,
        val iconName: String = ""
    )

    private val _editState = MutableStateFlow(EditState())
    val editState: StateFlow<EditState> = _editState.asStateFlow()

    fun startEdit(category: CategoryEntity?) {
        if (category == null) {
            _editState.value = EditState()
        } else {
            _editState.value = EditState(
                id = category.id,
                name = category.name,
                type = category.type,
                iconName = category.iconName
            )
        }
    }

    fun updateName(name: String) { _editState.value = _editState.value.copy(name = name) }
    fun updateType(type: TransactionType) { _editState.value = _editState.value.copy(type = type) }
    fun updateIcon(iconName: String) { _editState.value = _editState.value.copy(iconName = iconName) }

    fun saveCategory(onDone: (Long) -> Unit = {}) {
        val state = _editState.value
        if (state.name.isBlank()) return
        viewModelScope.launch {
            val savedCategoryId = repository.saveCategory(
                CategoryEntity(
                    id = state.id,
                    name = state.name.trim(),
                    type = state.type,
                    iconName = state.iconName
                )
            )
            runSortProcess()
            onDone(savedCategoryId)
        }
    }

    fun deleteCategory(category: CategoryEntity, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(category)
                runSortProcess()
            } catch (e: Exception) {
                onError("Failed to delete category: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun runSortProcess() {
        val all = repository.observeAllTransactions().firstOrNull() ?: emptyList()
        val sorted = all.sortedWith(buildTransactionComparator(emptyList()))
        repository.reorderTransactions(sorted.map { it.id })
    }
}
