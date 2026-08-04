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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.moneytracker.ui.components.AppTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.moneytracker.ui.components.*
import com.moneytracker.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    contentPadding: PaddingValues,
    onAddTransaction: () -> Unit,
    onViewAll: () -> Unit,
    onEditTransaction: (Long) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val summary by viewModel.summary.collectAsState()
    val subCategorySummaries by viewModel.subCategorySummaries.collectAsState()
    val selectedPayMonthDate by viewModel.selectedPayMonthDate.collectAsState()

    Scaffold(
        modifier = Modifier,
        topBar = {
            AppTopBar(
                screenTitle = "Dashboard",
                showBack = false,
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
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
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // PayMonth Filter Header (Prev, Current, Next, Dropdown)
            PayMonthFilterHeader(
                selectedPayMonthDate = selectedPayMonthDate,
                onPayMonthSelected = { viewModel.setPayMonth(it) }
            )

            // Balance Card
            BalanceCard(
                balance = summary.balance,
                income = summary.income,
                investment = summary.investment,
                education = summary.education,
                expense = summary.expense
            )

            // Sub-Category Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sub-Category Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onViewAll) {
                    Text("View All")
                }
            }

            // Table takes remaining space
            CategorySummaryTable(
                summaries = subCategorySummaries,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
