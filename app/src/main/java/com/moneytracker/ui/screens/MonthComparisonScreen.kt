package com.moneytracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.moneytracker.ui.components.AppTopBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.ui.components.EmptyState
import com.moneytracker.ui.theme.EducationColor
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.theme.InvestmentColor
import com.moneytracker.ui.viewmodel.CategoryComparisonRow
import com.moneytracker.ui.viewmodel.DrilldownLevel
import com.moneytracker.ui.viewmodel.MonthComparisonViewModel
import com.moneytracker.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthComparisonScreen(
    viewModel: MonthComparisonViewModel,
    contentPadding: PaddingValues
) {
    val drilldownLevel by viewModel.drilldownLevel.collectAsState()
    val anchorPayMonthDate by viewModel.anchorPayMonthDate.collectAsState()
    val prevData by viewModel.prevMonthData.collectAsState()
    val currData by viewModel.currentMonthData.collectAsState()
    val nextData by viewModel.nextMonthData.collectAsState()

    val categoryRows by viewModel.categoryRows.collectAsState()
    val subCategoryRows by viewModel.subCategoryRows.collectAsState()

    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Scaffold(
        topBar = {
            AppTopBar(
                screenTitle = "Month-to-Month Comparison",
                showBack = false
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            com.moneytracker.ui.components.PayMonthFilterHeader(
                selectedPayMonthDate = anchorPayMonthDate,
                onPayMonthSelected = { viewModel.setAnchorPayMonth(it) },
                isAnchorCenteringMode = true
            )
            // 1. Drilldown Level Selector Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = drilldownLevel == DrilldownLevel.SUMMARY,
                    onClick = { viewModel.setDrilldownLevel(DrilldownLevel.SUMMARY) },
                    label = { Text("Summary", maxLines = 1) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = drilldownLevel == DrilldownLevel.CATEGORY,
                    onClick = { viewModel.setDrilldownLevel(DrilldownLevel.CATEGORY) },
                    label = { Text("Category", maxLines = 1) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = drilldownLevel == DrilldownLevel.SUBCATEGORY,
                    onClick = { viewModel.setDrilldownLevel(DrilldownLevel.SUBCATEGORY) },
                    label = { Text("SubCategory", maxLines = 1) },
                    modifier = Modifier.weight(1f)
                )
            }

            // 2. Active View Display Based on Selected Drilldown Level
            when (drilldownLevel) {
                DrilldownLevel.SUMMARY -> {
                    SummaryLevelView(
                        prevName = prevData.monthName,
                        currName = currData.monthName,
                        nextName = nextData.monthName,
                        prevData = prevData.summary,
                        currData = currData.summary,
                        nextData = nextData.summary,
                        gridLineColor = gridLineColor
                    )
                }
                DrilldownLevel.CATEGORY -> {
                    ComparisonTableView(
                        prevName = prevData.monthName,
                        currName = currData.monthName,
                        nextName = nextData.monthName,
                        rows = categoryRows,
                        gridLineColor = gridLineColor
                    )
                }
                DrilldownLevel.SUBCATEGORY -> {
                    ComparisonTableView(
                        prevName = prevData.monthName,
                        currName = currData.monthName,
                        nextName = nextData.monthName,
                        rows = subCategoryRows,
                        gridLineColor = gridLineColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryLevelView(
    prevName: String,
    currName: String,
    nextName: String,
    prevData: com.moneytracker.data.repository.MonthlySummary,
    currData: com.moneytracker.data.repository.MonthlySummary,
    nextData: com.moneytracker.data.repository.MonthlySummary,
    gridLineColor: androidx.compose.ui.graphics.Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SummaryMetricCard("Income (+)", prevData.income, currData.income, nextData.income, prevName, currName, nextName, IncomeColor)
        }
        item {
            SummaryMetricCard("Investment (−)", prevData.investment, currData.investment, nextData.investment, prevName, currName, nextName, InvestmentColor)
        }
        item {
            SummaryMetricCard("Expense (−)", prevData.expense, currData.expense, nextData.expense, prevName, currName, nextName, ExpenseColor)
        }
        item {
            SummaryMetricCard("Net Balance", prevData.balance, currData.balance, nextData.balance, prevName, currName, nextName, MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    prevAmt: Double,
    currAmt: Double,
    nextAmt: Double,
    prevName: String,
    currName: String,
    nextName: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = "Prev ($prevName)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = CurrencyUtils.format(prevAmt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = "Current ($currName)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = CurrencyUtils.format(currAmt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(text = "Next ($nextName)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = CurrencyUtils.format(nextAmt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
                }
            }
        }
    }
}

@Composable
private fun ComparisonTableView(
    prevName: String,
    currName: String,
    nextName: String,
    rows: List<CategoryComparisonRow>,
    gridLineColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, gridLineColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar - 4 Columns fitting 100% screen width
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell(text = "Name", weight = 1.2f, isHeader = true)
                    VerticalDivider(modifier = Modifier.height(28.dp), color = gridLineColor)
                    TableCell(text = prevName, weight = 1.0f, isHeader = true)
                    VerticalDivider(modifier = Modifier.height(28.dp), color = gridLineColor)
                    TableCell(text = currName, weight = 1.0f, isHeader = true)
                    VerticalDivider(modifier = Modifier.height(28.dp), color = gridLineColor)
                    TableCell(text = nextName, weight = 1.0f, isHeader = true)
                }
            }

            HorizontalDivider(color = gridLineColor, thickness = 1.dp)

            if (rows.isEmpty()) {
                EmptyState("No comparison data available for this view.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(rows) { row ->
                        ComparisonRowItem(row = row, gridLineColor = gridLineColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonRowItem(
    row: CategoryComparisonRow,
    gridLineColor: androidx.compose.ui.graphics.Color
) {
    val rowColor = when (row.type) {
        TransactionType.INCOME -> IncomeColor
        TransactionType.INVESTMENT -> InvestmentColor
        TransactionType.EDUCATION -> EducationColor
        TransactionType.EXPENSE -> ExpenseColor
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TableCell(text = row.name, weight = 1.2f, color = rowColor, fontWeight = FontWeight.Bold)
            VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)
            TableCell(text = CurrencyUtils.format(row.prevAmount), weight = 1.0f, color = rowColor)
            VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)
            TableCell(text = CurrencyUtils.format(row.currentAmount), weight = 1.0f, color = rowColor)
            VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)
            TableCell(text = CurrencyUtils.format(row.nextAmount), weight = 1.0f, color = rowColor)
        }
        HorizontalDivider(color = gridLineColor, thickness = 1.dp)
    }
}

@Composable
private fun TableCell(
    text: String,
    weight: Float,
    isHeader: Boolean = false,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if (isHeader) MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodySmall.copy(fontWeight = fontWeight),
            color = if (isHeader) MaterialTheme.colorScheme.onSurfaceVariant else color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
