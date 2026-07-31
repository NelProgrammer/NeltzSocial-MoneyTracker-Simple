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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneytracker.ui.components.BalanceCard
import com.moneytracker.ui.components.EmptyState
import com.moneytracker.ui.components.ReorderableTransactionList
import com.moneytracker.ui.viewmodel.DashboardViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    contentPadding: PaddingValues,
    onAddTransaction: () -> Unit,
    onViewAll: () -> Unit,
    onEditTransaction: (Long) -> Unit = {}
) {
    val summary by viewModel.summary.collectAsState()
    val transactions by viewModel.recentTransactions.collectAsState()
    val secondarySorts by viewModel.secondarySorts.collectAsState()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            BalanceCard(
                balance = summary.balance,
                income = summary.income,
                investment = summary.investment,
                expense = summary.expense
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onViewAll) {
                    Text("View all")
                }
            }

            if (transactions.isEmpty()) {
                EmptyState("No transactions this month. Tap + to add one.")
            } else {
                ReorderableTransactionList(
                    transactions = transactions,
                    reorderEnabled = true,
                    secondarySorts = secondarySorts,
                    onHeaderClicked = viewModel::onHeaderClicked,
                    onHeaderLongPressed = viewModel::onHeaderLongPressed,
                    onReorder = viewModel::reorderTransactions,
                    onEditTransaction = onEditTransaction
                )
            }
        }
    }
}
