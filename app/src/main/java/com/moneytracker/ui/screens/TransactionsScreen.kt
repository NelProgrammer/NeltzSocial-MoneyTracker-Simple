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
//
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import com.moneytracker.ui.components.AppTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign

import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.ui.components.PayMonthFilterHeader
import com.moneytracker.ui.components.TransactionTable
import com.moneytracker.ui.viewmodel.TransactionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    contentPadding: PaddingValues,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val secondarySorts by viewModel.secondarySorts.collectAsState()
    val selectedPayMonthDate by viewModel.selectedPayMonthDate.collectAsState()

    Scaffold(
    modifier = Modifier,
    topBar = {
        AppTopBar(
            screenTitle = "Transactions",
            showBack = false
        )
    },
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
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // PayMonth Filter Header (Prev, Current, Next, Dropdown)
            PayMonthFilterHeader(
                selectedPayMonthDate = selectedPayMonthDate,
                onPayMonthSelected = { viewModel.setPayMonth(it) }
            )

            // Category Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                    label = { Text("Invest", maxLines = 1) }
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = filterType == TransactionType.EDUCATION,
                    onClick = { viewModel.setFilter(TransactionType.EDUCATION) },
                    label = { Text("Edu", maxLines = 1) }
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = filterType == TransactionType.EXPENSE,
                    onClick = { viewModel.setFilter(TransactionType.EXPENSE) },
                    label = { Text("Expense", maxLines = 1) }
                )
            }

            // Clean, crisp Grid Table without drag handles
            TransactionTable(
                transactions = transactions,
                secondarySorts = secondarySorts,
                onHeaderClicked = { viewModel.onHeaderClicked(it) },
                onHeaderLongPressed = { viewModel.onHeaderLongPressed(it) },
                onEditTransaction = { id -> onEditTransaction(id) },
                onDeleteTransaction = { viewModel.deleteTransaction(it) }
            )
        }
    }
}
