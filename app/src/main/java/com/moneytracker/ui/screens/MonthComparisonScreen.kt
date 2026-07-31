package com.moneytracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneytracker.data.repository.MonthSummaryItem
import com.moneytracker.ui.viewmodel.MonthComparisonViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MonthComparisonScreen(
    viewModel: MonthComparisonViewModel,
    contentPadding: PaddingValues
) {
    val summaries = viewModel.summaries.collectAsState().value
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(summaries) { summaryItem ->
            MonthSummaryCard(summary = summaryItem)
        }
    }
}

@Composable
private fun MonthSummaryCard(summary: MonthSummaryItem) {
    // Convert epoch to month name
    val formatter = DateTimeFormatter.ofPattern("MMM yyyy")
    val month = Instant.ofEpochMilli(summary.startDateMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = month, style = MaterialTheme.typography.titleMedium)
            Text(text = "Income: ${com.moneytracker.util.CurrencyUtils.format(summary.income)}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Expense: ${com.moneytracker.util.CurrencyUtils.format(summary.expense)}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Balance: ${com.moneytracker.util.CurrencyUtils.format(summary.balance)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
