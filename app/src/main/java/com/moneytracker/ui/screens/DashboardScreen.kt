package com.moneytracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneytracker.ui.components.AppTopBar
import com.moneytracker.ui.components.BalanceCard
import com.moneytracker.ui.components.CategoryPieChartCard
import com.moneytracker.ui.components.DualIncomePieChartCard
import com.moneytracker.ui.components.DualEducationInvestmentPieChartCard
import com.moneytracker.ui.components.PayMonthFilterHeader
import androidx.compose.foundation.lazy.items
import com.moneytracker.ui.theme.getCategoryBaseColor
import com.moneytracker.ui.theme.EducationColor
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.theme.InvestmentColor
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
    val incomeBreakdown by viewModel.incomeBreakdown.collectAsState()
    val incomeDetailBreakdown by viewModel.incomeDetailBreakdown.collectAsState()
    val incomeUsageBreakdown by viewModel.incomeUsageBreakdown.collectAsState()
    val incomeUsageDetailBreakdown by viewModel.incomeUsageDetailBreakdown.collectAsState()
    val investmentBreakdown by viewModel.investmentBreakdown.collectAsState()
    val investmentDetailBreakdown by viewModel.investmentDetailBreakdown.collectAsState()
    val educationBreakdown by viewModel.educationBreakdown.collectAsState()
    val educationDetailBreakdown by viewModel.educationDetailBreakdown.collectAsState()
    val expenseBreakdown by viewModel.expenseBreakdown.collectAsState()
    val expenseDetailBreakdown by viewModel.expenseDetailBreakdown.collectAsState()
    val customCategoryBreakdowns by viewModel.customCategoryBreakdowns.collectAsState()
    val selectedPayMonthDate by viewModel.selectedPayMonthDate.collectAsState()

    Scaffold(
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. PayMonth Filter Header (Prev, Current, Next, Dropdown)
            item {
                PayMonthFilterHeader(
                    selectedPayMonthDate = selectedPayMonthDate,
                    onPayMonthSelected = { viewModel.setPayMonth(it) }
                )
            }

            // 2. Balance Card Overview
            item {
                BalanceCard(
                    balance = summary.balance,
                    income = summary.income,
                    investment = summary.investment,
                    education = summary.education,
                    expense = summary.expense
                )
            }

            // 3. Section Header for Visual Breakdown Charts with View Table Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Analytics & Visual Graphs",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onViewAll) {
                        Text("View Table")
                    }
                }
            }

            // 4. Combined Income Overview (Income Sources and Utilization)
            item {
                val totalOutflows = incomeUsageBreakdown.filter { !it.categoryName.startsWith("Remaining Income") }.sumOf { it.total }
                val usageTotal = if (summary.income > 0) maxOf(summary.income, totalOutflows) else totalOutflows
                DualIncomePieChartCard(
                    title = "Income Sources and Utilization",
                    sourceSummaries = incomeBreakdown,
                    sourceDetailSummaries = incomeDetailBreakdown,
                    sourceTotal = summary.income,
                    usageSummaries = incomeUsageBreakdown,
                    usageDetailSummaries = incomeUsageDetailBreakdown,
                    usageTotal = usageTotal,
                    baseColor = IncomeColor
                )
            }

            // 6. Expense Pie Chart (Where Money is Spent)
            item {
                CategoryPieChartCard(
                    title = "Expense Breakdown (Where Money is Spent)",
                    summaries = expenseBreakdown,
                    detailSummaries = expenseDetailBreakdown,
                    totalAmount = summary.expense,
                    baseColor = ExpenseColor,
                    emptyMessage = "No expenses recorded for this pay period.",
                    useTwoColumns = true
                )
            }

            // 7. Combined Education & Investment Pie Chart Card
            item {
                DualEducationInvestmentPieChartCard(
                    title = "Education & Investment Breakdown",
                    educationSummaries = educationBreakdown,
                    educationDetailSummaries = educationDetailBreakdown,
                    educationTotal = summary.education,
                    investmentSummaries = investmentBreakdown,
                    investmentDetailSummaries = investmentDetailBreakdown,
                    investmentTotal = summary.investment
                )
            }

            // 9. Custom Category Pie Charts (Dynamically added for any custom category with transactions)
            items(
                items = customCategoryBreakdowns,
                key = { it.categoryName }
            ) { customBreakdown ->
                CategoryPieChartCard(
                    title = "${customBreakdown.categoryName} Breakdown",
                    summaries = customBreakdown.subCategorySummaries,
                    detailSummaries = customBreakdown.detailSummaries,
                    totalAmount = customBreakdown.totalAmount,
                    baseColor = getCategoryBaseColor(customBreakdown.categoryName),
                    emptyMessage = "No ${customBreakdown.categoryName} transactions recorded for this pay period."
                )
            }
        }
    }
}
