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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.ui.components.EmptyState
import com.moneytracker.ui.components.TransactionTable
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
    val secondarySorts by viewModel.secondarySorts.collectAsState()

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            if (transactions.isEmpty()) {
                EmptyState("No transactions found.")
            } else {
                TransactionTable(
                    transactions = transactions,
                    secondarySorts = secondarySorts,
                    onHeaderClicked = viewModel::onHeaderClicked,
                    onHeaderLongPressed = viewModel::onHeaderLongPressed,
                    onEditTransaction = onEditTransaction,
                    onDeleteTransaction = viewModel::deleteTransaction
                )
            }
        }
    }
}
