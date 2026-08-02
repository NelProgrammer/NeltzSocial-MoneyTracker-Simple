package com.moneytracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.local.entity.TransactionWithCategory
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.viewmodel.SortCriterion
import com.moneytracker.ui.viewmodel.TransactionsViewModel.SortDirection
import com.moneytracker.ui.viewmodel.TransactionsViewModel.SortField
import com.moneytracker.util.CurrencyUtils
import com.moneytracker.util.DateUtils

/**
 * Compact Grid Table Component for TransactionsScreen.
 * Columns: Date (80dp), Category (95dp), SubCategory (115dp), Detail (115dp), Amount (95dp), Note (120dp).
 * Total width: 620dp.
 */
@Composable
fun TransactionTable(
    transactions: List<TransactionWithCategory>,
    secondarySorts: List<SortCriterion> = emptyList(),
    onHeaderClicked: (SortField) -> Unit = {},
    onHeaderLongPressed: (SortField) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onEditTransaction: (Long) -> Unit = {},
    onDeleteTransaction: (TransactionWithCategory) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            gridLineColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            Column(modifier = Modifier.width(620.dp)) {
                // Table Header Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TableHeaderCell(
                            label = "Date",
                            field = SortField.DATE,
                            secondarySorts = secondarySorts,
                            onHeaderClicked = onHeaderClicked,
                            onHeaderLongPressed = onHeaderLongPressed,
                            modifier = Modifier.width(80.dp)
                        )
                        VerticalDivider(modifier = Modifier.height(28.dp), color = gridLineColor)
                        TableHeaderCell(
                            label = "Category",
                            field = SortField.TYPE,
                            secondarySorts = secondarySorts,
                            onHeaderClicked = onHeaderClicked,
                            onHeaderLongPressed = onHeaderLongPressed,
                            modifier = Modifier.width(95.dp)
                        )
                        VerticalDivider(modifier = Modifier.height(28.dp), color = gridLineColor)
                        TableHeaderCell(
                            label = "SubCategory",
                            field = SortField.CATEGORY,
                            secondarySorts = secondarySorts,
                            onHeaderClicked = onHeaderClicked,
                            onHeaderLongPressed = onHeaderLongPressed,
                            modifier = Modifier.width(115.dp)
                        )
                        VerticalDivider(modifier = Modifier.height(28.dp), color = gridLineColor)
                        TableHeaderCell(
                            label = "Detail",
                            field = SortField.SUBCATEGORY,
                            secondarySorts = secondarySorts,
                            onHeaderClicked = onHeaderClicked,
                            onHeaderLongPressed = onHeaderLongPressed,
                            modifier = Modifier.width(115.dp)
                        )
                        VerticalDivider(modifier = Modifier.height(28.dp), color = gridLineColor)
                        TableHeaderCell(
                            label = "Amount",
                            field = SortField.AMOUNT,
                            secondarySorts = secondarySorts,
                            onHeaderClicked = onHeaderClicked,
                            onHeaderLongPressed = onHeaderLongPressed,
                            modifier = Modifier.width(95.dp)
                        )
                        VerticalDivider(modifier = Modifier.height(28.dp), color = gridLineColor)
                        TableHeaderCell(
                            label = "Note",
                            field = null,
                            secondarySorts = secondarySorts,
                            onHeaderClicked = onHeaderClicked,
                            onHeaderLongPressed = onHeaderLongPressed,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = gridLineColor,
                    thickness = 1.dp
                )

                // Grid Table Data Rows
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding
                ) {
                    items(transactions, key = { it.id }) { transaction ->
                        TableRow(
                            transaction = transaction,
                            onEditTransaction = onEditTransaction
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TableHeaderCell(
    label: String,
    field: SortField?,
    secondarySorts: List<SortCriterion>,
    onHeaderClicked: (SortField) -> Unit,
    onHeaderLongPressed: (SortField) -> Unit,
    modifier: Modifier = Modifier
) {
    if (field == null) {
        Box(
            modifier = modifier.padding(horizontal = 2.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else if (field == SortField.TYPE) {
        // Fixed #1 Primary Sort Header: Category
        Surface(
            modifier = modifier.padding(horizontal = 2.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category (Fixed #1)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    textAlign = TextAlign.Center,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        // Interactive Multi-Sort Header
        val criterion = secondarySorts.find { it.field == field }
        val sortIndex = secondarySorts.indexOfFirst { it.field == field }
        val isSorted = criterion != null

        Box(
            modifier = modifier
                .combinedClickable(
                    onClick = { onHeaderClicked(field) },
                    onLongClick = { onHeaderLongPressed(field) }
                )
                .padding(horizontal = 2.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSorted) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSorted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isSorted && criterion != null) {
                    Spacer(modifier = Modifier.width(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${sortIndex + 2}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (criterion.direction == SortDirection.ASC) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TableRow(
    transaction: TransactionWithCategory,
    onEditTransaction: (Long) -> Unit
) {
    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> IncomeColor
        TransactionType.EXPENSE -> ExpenseColor
        TransactionType.INVESTMENT -> MaterialTheme.colorScheme.tertiary
    }
    val prefix = when (transaction.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.INVESTMENT -> "-"
    }
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditTransaction(transaction.id) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Date
            Text(
                text = DateUtils.formatDate(transaction.date),
                modifier = Modifier.width(80.dp),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)

            // 2. Category (Transaction Type)
            Box(
                modifier = Modifier.width(95.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = when (transaction.type) {
                        TransactionType.INCOME -> IncomeColor.copy(alpha = 0.15f)
                        TransactionType.EXPENSE -> ExpenseColor.copy(alpha = 0.15f)
                        TransactionType.INVESTMENT -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = transaction.type.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when (transaction.type) {
                            TransactionType.INCOME -> IncomeColor
                            TransactionType.EXPENSE -> ExpenseColor
                            TransactionType.INVESTMENT -> MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)

            // 3. SubCategory (Category Name)
            Text(
                text = transaction.categoryName,
                modifier = Modifier.width(115.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)

            // 4. Detail (SubCategory text)
            Text(
                text = if (transaction.subCategory.isNotBlank()) transaction.subCategory else "-",
                modifier = Modifier.width(115.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (transaction.subCategory.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)

            // 5. Amount
            Text(
                text = "$prefix${CurrencyUtils.format(transaction.amount)}",
                modifier = Modifier.width(95.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = amountColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)

            // 6. Note (End of Table)
            Text(
                text = if (transaction.note.isNotBlank()) transaction.note else "-",
                modifier = Modifier.width(120.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (transaction.note.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        HorizontalDivider(
            color = gridLineColor,
            thickness = 1.dp
        )
    }
}
