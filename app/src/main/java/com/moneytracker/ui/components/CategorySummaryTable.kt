package com.moneytracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.local.entity.TransactionWithCategory
import com.moneytracker.ui.theme.EducationColor
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.theme.InvestmentColor
import com.moneytracker.ui.viewmodel.SubCategorySummary
import com.moneytracker.util.CurrencyUtils
import com.moneytracker.util.DateUtils

/**
 * Grid Table displaying the Summary of SubCategories for the Dashboard.
 * - Always 100% visible on screen (No horizontal scrolling).
 * - 3 Columns: SubCategory (weight 1.3f), Total Amount (weight 1.0f), Running Total (weight 1.0f).
 * - Table Text Colors match Category Colors (Income -> Green, Expense -> Red, Investment -> Blue, Education -> Purple).
 * - Tapping a row expands/collapses detailed child transactions underneath.
 */
@Composable
fun CategorySummaryTable(
    summaries: List<SubCategorySummary>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    // Calculate sequential running totals down displayed summary rows
    val runningTotals = remember(summaries) {
        var accumulated = 0.0
        val map = mutableMapOf<Long, Double>()
        for (item in summaries) {
            val delta = when (item.type) {
                TransactionType.INCOME -> item.totalAmount
                TransactionType.INVESTMENT -> -item.totalAmount
                TransactionType.EDUCATION -> -item.totalAmount
                TransactionType.EXPENSE -> -item.totalAmount
            }
            accumulated += delta
            map[item.categoryId] = accumulated
        }
        map
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, gridLineColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SubCategory",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1.3f),
                        textAlign = TextAlign.Center
                    )
                    VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)
                    Text(
                        text = "Total Amount",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1.0f),
                        textAlign = TextAlign.Center
                    )
                    VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)
                    Text(
                        text = "Running Total",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1.0f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            HorizontalDivider(color = gridLineColor, thickness = 1.dp)

            // Data Rows with Expandable Details
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = contentPadding
            ) {
                items(summaries, key = { it.categoryId }) { item ->
                    SummaryTableRow(
                        item = item,
                        runningTotal = runningTotals[item.categoryId] ?: 0.0,
                        gridLineColor = gridLineColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryTableRow(
    item: SubCategorySummary,
    runningTotal: Double,
    gridLineColor: androidx.compose.ui.graphics.Color
) {
    var isExpanded by remember { mutableStateOf(false) }

    val categoryColor = when (item.type) {
        TransactionType.INCOME -> IncomeColor
        TransactionType.INVESTMENT -> InvestmentColor
        TransactionType.EDUCATION -> EducationColor
        TransactionType.EXPENSE -> ExpenseColor
    }
    val prefix = when (item.type) {
        TransactionType.INCOME -> "+"
        TransactionType.INVESTMENT -> "-"
        TransactionType.EDUCATION -> "-"
        TransactionType.EXPENSE -> "-"
    }
    val runningColor = if (runningTotal >= 0) IncomeColor else ExpenseColor

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. SubCategory Name + Expand Arrow (Text color matches Category color)
            Row(
                modifier = Modifier
                    .weight(1.3f)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = categoryColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = categoryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)

            // 2. Total Amount (Formatted with prefix and matching Category color)
            Text(
                text = "$prefix${CurrencyUtils.format(item.totalAmount)}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = categoryColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.0f)
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)

            // 3. Running Total (Color reflects positive/negative)
            Text(
                text = CurrencyUtils.format(runningTotal),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = runningColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.0f)
            )
        }

        // Expandable Child Transactions List
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, bottom = 8.dp)
            ) {
                HorizontalDivider(color = gridLineColor, thickness = 0.5.dp)
                item.transactions.forEach { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (tx.subCategory.isNotBlank()) tx.subCategory else tx.note,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = DateUtils.formatDate(tx.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = "$prefix${CurrencyUtils.format(tx.amount)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = categoryColor
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = gridLineColor, thickness = 0.5.dp)
    }
}
