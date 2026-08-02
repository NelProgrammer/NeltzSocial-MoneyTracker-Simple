package com.moneytracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.local.entity.DetailEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    val details: StateFlow<List<DetailEntity>> = repository
        .observeAllDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    data class EditState(
        val id: Long = 0L,
        val name: String = "",
        val subCategoryId: Long? = null,
        val categoryId: Long? = null,
        val type: TransactionType? = null,
        val iconName: String = "default"
    )

    private val _editState = MutableStateFlow(EditState())
    val editState: StateFlow<EditState> = _editState.asStateFlow()

    fun startEdit(detail: DetailEntity?) {
        if (detail == null) {
            _editState.value = EditState()
        } else {
            _editState.value = EditState(
                id = detail.id,
                name = detail.name,
                subCategoryId = detail.subCategoryId,
                categoryId = detail.categoryId,
                type = detail.type,
                iconName = detail.iconName
            )
        }
    }

    fun updateName(name: String) { _editState.value = _editState.value.copy(name = name) }
    fun updateSubCategoryId(subCategoryId: Long?) { _editState.value = _editState.value.copy(subCategoryId = subCategoryId) }
    fun updateCategoryId(categoryId: Long?) { _editState.value = _editState.value.copy(categoryId = categoryId) }
    fun updateType(type: TransactionType?) { _editState.value = _editState.value.copy(type = type) }

    fun saveDetail(onDone: () -> Unit = {}) {
        val state = _editState.value
        if (state.name.isBlank()) return
        viewModelScope.launch {
            repository.saveDetail(
                DetailEntity(
                    id = state.id,
                    name = state.name.trim(),
                    subCategoryId = state.subCategoryId,
                    categoryId = state.categoryId,
                    type = state.type,
                    iconName = state.iconName
                )
            )
            runSortProcess()
            onDone()
        }
    }

    fun deleteDetail(detail: DetailEntity, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteDetail(detail)
                runSortProcess()
            } catch (e: Exception) {
                onError("Failed to delete detail: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun runSortProcess() {
        val all = repository.observeAllTransactions().firstOrNull() ?: emptyList()
        val sorted = all.sortedWith(buildTransactionComparator(emptyList()))
        repository.reorderTransactions(sorted.map { it.id })
    }
}
