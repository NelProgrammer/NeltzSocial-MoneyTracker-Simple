package com.moneytracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneytracker.ui.components.TransactionSectionCard
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.ui.components.TransactionHeaderRow
import com.moneytracker.ui.components.EmptyState
import com.moneytracker.ui.components.ReorderableTransactionList
import androidx.compose.foundation.clickable
import com.moneytracker.ui.viewmodel.TransactionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    contentPadding: PaddingValues,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val reorderEnabled = filterType == null

    Scaffold(
        modifier = Modifier,
        topBar = { TopAppBar(title = { Text("Transactions") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = filterType == null,
                    onClick = { viewModel.clearFilter() },
                    label = { Text("All", maxLines = 1) }
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = filterType == TransactionType.INCOME,
                    onClick = { viewModel.setFilter(TransactionType.INCOME) },
                    label = { Text("Income", maxLines = 1) }
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = filterType == TransactionType.INVESTMENT,
                    onClick = { viewModel.setFilter(TransactionType.INVESTMENT) },
                    label = { Text("Investment", maxLines = 1) }
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = filterType == TransactionType.EXPENSE,
                    onClick = { viewModel.setFilter(TransactionType.EXPENSE) },
                    label = { Text("Expenses", maxLines = 1) }
                )
            }
            if (!reorderEnabled) {
                Text(
                    text = "Clear filters to drag and reorder transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (transactions.isEmpty()) {
                EmptyState("No transactions found.")
            } else {
                // Determine which sections to show based on the selected filter
                val sections = if (filterType == null) {
                    listOf(TransactionType.INCOME, TransactionType.INVESTMENT, TransactionType.EXPENSE)
                } else {
                    listOf(filterType)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    sections.forEach { type ->
                        val filtered = transactions.filter { it.type == type }
                        val title = when (type) {
                            TransactionType.INCOME -> "Income"
                            TransactionType.INVESTMENT -> "Investment"
                            TransactionType.EXPENSE -> "Expenses"
                            else -> ""
                        }
                        TransactionHeaderRow(
                            currentSort = viewModel.sortField.collectAsState().value,
                            sortDirection = viewModel.sortDirection.collectAsState().value,
                            onSortChange = viewModel::setSortField
                        )
                        TransactionSectionCard(
                            title = title,
                            transactions = filtered,
                            reorderEnabled = reorderEnabled,
                            onEditTransaction = onEditTransaction,
                            onDeleteTransaction = viewModel::deleteTransaction,
                            onReorder = viewModel::reorderTransactions,
                            contentPadding = PaddingValues(bottom = 8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
