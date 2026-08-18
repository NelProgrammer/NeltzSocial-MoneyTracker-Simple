// SortableTransactionTable.kt
// This file contains the renamed component formerly known as ReorderableTransactionList.

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
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.local.entity.TransactionWithCategory
import com.moneytracker.ui.theme.EducationColor
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.theme.InvestmentColor
import com.moneytracker.ui.viewmodel.SortCriterion
import com.moneytracker.ui.viewmodel.TransactionsViewModel.SortDirection
import com.moneytracker.ui.viewmodel.TransactionsViewModel.SortField
import com.moneytracker.util.CurrencyUtils
import com.moneytracker.util.DateUtils
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Compact Flattened Grid Table Component shared across DashboardScreen.
 * Columns: DragHandle (32dp), Date (80dp), Category (95dp), SubCategory (115dp), Detail (115dp), Amount (95dp), Note (120dp).
 * Total width: 652dp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortableTransactionTable(
    transactions: List<TransactionWithCategory>,
    reorderEnabled: Boolean,
    secondarySorts: List<SortCriterion> = emptyList(),
    onHeaderClicked: (SortField) -> Unit = {},
    onHeaderLongPressed: (SortField) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onReorder: (List<TransactionWithCategory>) -> Unit = {},
    onEditTransaction: (Long) -> Unit = {},
    onDeleteTransaction: (TransactionWithCategory) -> Unit = {}
) {
    var localTransactions by remember { mutableStateOf(transactions) }
    LaunchedEffect(transactions) { localTransactions = transactions }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localTransactions = localTransactions.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onReorder(localTransactions)
    }

    val horizontalScrollState = rememberScrollState()
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, gridLineColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            Column(
                modifier = Modifier
                    .width(if (reorderEnabled) 652.dp else 620.dp)
            ) {
                // Interactive Table Header Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (reorderEnabled) {
                            Spacer(modifier = Modifier.width(32.dp))
                            VerticalDivider(modifier = Modifier.height(28.dp), color = gridLineColor)
                        }
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

                HorizontalDivider(color = gridLineColor, thickness = 1.dp)

                // Flat Transaction List
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding
                ) {
                    items(localTransactions, key = { it.id }) { transaction ->
                        ReorderableItem(
                            state = reorderableState,
                            key = transaction.id,
                            enabled = reorderEnabled
                        ) { isDragging ->
                            TableRow(
                                transaction = transaction,
                                reorderEnabled = reorderEnabled,
                                isDragging = isDragging,
                                onEditTransaction = onEditTransaction,
                                dragHandleModifier = if (reorderEnabled) Modifier.draggableHandle() else Modifier
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TableRow(
    transaction: TransactionWithCategory,
    reorderEnabled: Boolean,
    isDragging: Boolean,
    onEditTransaction: (Long) -> Unit,
    dragHandleModifier: Modifier = Modifier
) {
    val categoryColor = when (transaction.type) {
        TransactionType.INCOME -> IncomeColor
        TransactionType.INVESTMENT -> InvestmentColor
        TransactionType.EDUCATION -> EducationColor
        TransactionType.EXPENSE -> ExpenseColor
    }
    val prefix = when (transaction.type) {
        TransactionType.INCOME -> "+"
        else -> "-"
    }
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable { onEditTransaction(transaction.id) }
            .background(if (isDragging) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (reorderEnabled) {
            Box(
                modifier = dragHandleModifier
                    .size(32.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)
        }
        // Date
        Text(
            text = DateUtils.formatDate(transaction.date),
            modifier = Modifier.width(80.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)
        // Category
        Box(
            modifier = Modifier.width(95.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = categoryColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = transaction.categoryName,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = categoryColor,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)
        // SubCategory
        Text(
            text = transaction.subCategory,
            modifier = Modifier.width(115.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)
        // Detail
        Text(
            text = if (transaction.detail.isNotBlank()) transaction.detail else "-",
            modifier = Modifier.width(115.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (transaction.detail.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)
        // Amount
        val isPlanFuture = transaction.recurrenceFrequency == com.moneytracker.data.local.entity.RecurrenceFrequency.PLAN_FUTURE
        Column(
            modifier = Modifier.width(95.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${prefix}${CurrencyUtils.format(kotlin.math.abs(transaction.amount))}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (isPlanFuture) MaterialTheme.colorScheme.onSurfaceVariant else categoryColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isPlanFuture) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = "Plan",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                    )
                }
            }
        }
        VerticalDivider(modifier = Modifier.height(20.dp), color = gridLineColor)
        // Note
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
    HorizontalDivider(color = gridLineColor, thickness = 1.dp)
}

