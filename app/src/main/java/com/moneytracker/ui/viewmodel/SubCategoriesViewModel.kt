package com.moneytracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.local.entity.SubCategoryEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubCategoriesViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    val subCategories: StateFlow<List<SubCategoryEntity>> = repository
        .observeAllSubCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    data class EditState(
        val id: Long = 0L,
        val name: String = "",
        val categoryId: Long? = null,
        val type: TransactionType? = null,
        val iconName: String = "default"
    )

    private val _editState = MutableStateFlow(EditState())
    val editState: StateFlow<EditState> = _editState.asStateFlow()

    fun startEdit(subCategory: SubCategoryEntity?) {
        if (subCategory == null) {
            _editState.value = EditState()
        } else {
            _editState.value = EditState(
                id = subCategory.id,
                name = subCategory.name,
                categoryId = subCategory.categoryId,
                type = subCategory.type,
                iconName = subCategory.iconName
            )
        }
    }

    fun updateName(name: String) { _editState.value = _editState.value.copy(name = name) }
    fun updateCategoryId(categoryId: Long?) { _editState.value = _editState.value.copy(categoryId = categoryId) }
    fun updateType(type: TransactionType?) { _editState.value = _editState.value.copy(type = type) }

    fun saveSubCategory(onDone: () -> Unit = {}) {
        val state = _editState.value
        if (state.name.isBlank()) return
        viewModelScope.launch {
            repository.saveSubCategory(
                SubCategoryEntity(
                    id = state.id,
                    name = state.name.trim(),
                    categoryId = state.categoryId,
                    type = state.type,
                    iconName = state.iconName
                )
            )
            runSortProcess()
            onDone()
        }
    }

    fun deleteSubCategory(subCategory: SubCategoryEntity, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteSubCategory(subCategory)
                runSortProcess()
            } catch (e: Exception) {
                onError("Failed to delete subcategory: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun runSortProcess() {
        val all = repository.observeAllTransactions().firstOrNull() ?: emptyList()
        val sorted = all.sortedWith(buildTransactionComparator(emptyList()))
        repository.reorderTransactions(sorted.map { it.id })
    }
}
