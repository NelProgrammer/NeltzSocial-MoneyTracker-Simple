package com.moneytracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneytracker.data.local.entity.CategorySummary
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.ui.components.BalanceCard
import com.moneytracker.ui.components.EmptyState
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.theme.InvestmentColor
import com.moneytracker.ui.viewmodel.StatsViewModel
import com.moneytracker.util.CurrencyUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    contentPadding: PaddingValues
) {
    val summary by viewModel.summary.collectAsState()
    val expenseBreakdown by viewModel.expenseBreakdown.collectAsState()
    val incomeBreakdown by viewModel.incomeBreakdown.collectAsState()
    val investmentBreakdown by viewModel.investmentBreakdown.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val selectedPayMonthDate by viewModel.selectedPayMonthDate.collectAsState()

    Scaffold(
        modifier = Modifier,
        topBar = { TopAppBar(title = { Text("Stats") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                com.moneytracker.ui.components.PayMonthFilterHeader(
                    selectedPayMonthDate = selectedPayMonthDate,
                    onPayMonthSelected = { viewModel.setPayMonth(it) }
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

            item {
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
            }

            if (filterType == null || filterType == TransactionType.INCOME) {
                item {
                    CategoryBreakdownSection(
                        title = "Income by Category",
                        summaries = incomeBreakdown,
                        total = summary.income,
                        barColor = IncomeColor,
                        emptyMessage = "No income recorded this month."
                    )
                }
            }

            if (filterType == null || filterType == TransactionType.INVESTMENT) {
                item {
                    CategoryBreakdownSection(
                        title = "Investments by Category",
                        summaries = investmentBreakdown,
                        total = summary.investment,
                        barColor = InvestmentColor,
                        emptyMessage = "No investments recorded this month."
                    )
                }
            }

            if (filterType == null || filterType == TransactionType.EXPENSE) {
                item {
                    CategoryBreakdownSection(
                        title = "Expenses by Category",
                        summaries = expenseBreakdown,
                        total = summary.expense,
                        barColor = ExpenseColor,
                        emptyMessage = "No expenses recorded this month."
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownSection(
    title: String,
    summaries: List<CategorySummary>,
    total: Double,
    barColor: androidx.compose.ui.graphics.Color,
    emptyMessage: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (summaries.isEmpty()) {
                EmptyState(emptyMessage)
            } else {
                summaries.forEach { summary ->
                    val fraction = if (total > 0) (summary.total / total).toFloat() else 0f
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Column {
                            Text(
                                text = summary.categoryName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = CurrencyUtils.format(summary.total),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = { fraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}
