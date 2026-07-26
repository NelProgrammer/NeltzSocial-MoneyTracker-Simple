package com.moneytracker.ui.viewmodel

import com.moneytracker.data.repository.MonthSummaryItem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.repository.MonthlySummary
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MonthComparisonViewModel(
    private val repository: TransactionRepository,
    private val monthsCount: Int = 6 // default number of past months
) : ViewModel() {
    // Holds list of MonthSummaryItem for each month, most recent first
    private val _summaries = MutableStateFlow<List<MonthSummaryItem>>(emptyList())
    val summaries: StateFlow<List<MonthSummaryItem>> = _summaries

    // Export current summaries as CSV string
    fun exportCsv(): String {
        val header = "Month,Income,Expense,Balance"
        val rows = _summaries.value.map { item ->
            // Convert start date millis to month string
            val date = java.time.Instant.ofEpochMilli(item.startDateMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"))
            "${date},${item.income},${item.expense},${item.balance}"
        }
        return (listOf(header) + rows).joinToString("\n")
    }
}
