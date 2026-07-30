package com.moneytracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneytracker.ui.components.BalanceCard
import com.moneytracker.ui.components.EmptyState
import com.moneytracker.ui.components.TransactionListDivider
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.ui.components.TransactionRow
import com.moneytracker.ui.viewmodel.DashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    contentPadding: PaddingValues,
    onAddTransaction: () -> Unit,
    onViewAll: () -> Unit
) {
    val summary by viewModel.summary.collectAsState()
    val transactions by viewModel.recentTransactions.collectAsState()
    val monthLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(title = { Text("Money Tracker") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                BalanceCard(
                    balance = summary.balance,
                    income = summary.income,
                    investment = summary.investment,
                    expense = summary.expense
                )
            }

            // Expense breakdown section removed (not applicable on Dashboard)

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onViewAll) {
                        Text("View all")
                    }
                }
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyState("No transactions this month. Tap + to add one.")
                }
            } else {
                val sorted = transactions.sortedBy { transaction ->
    when (transaction.type) {
        TransactionType.INCOME -> 0
        TransactionType.INVESTMENT -> 1
        TransactionType.EXPENSE -> 2
        else -> 3
    }
}.take(5)
items(sorted, key = { it.id }) { transaction ->
    TransactionRow(transaction = transaction)
    TransactionListDivider()
                }
            }
        }
    }
}
