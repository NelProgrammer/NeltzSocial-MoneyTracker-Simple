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

            // Clean, crisp Grid Table with Add Item button in footer
            TransactionTable(
                transactions = transactions,
                secondarySorts = secondarySorts,
                onHeaderClicked = { viewModel.onHeaderClicked(it) },
                onHeaderLongPressed = { viewModel.onHeaderLongPressed(it) },
                onAddItem = onAddTransaction,
                onEditTransaction = { id -> onEditTransaction(id) },
                onDeleteTransaction = { viewModel.deleteTransaction(it) }
            )
        }
    }
}
