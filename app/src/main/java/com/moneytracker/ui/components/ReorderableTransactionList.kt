package com.moneytracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.local.entity.TransactionWithCategory
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.viewmodel.SortCriterion
import com.moneytracker.ui.viewmodel.TransactionsViewModel.SortDirection
import com.moneytracker.ui.viewmodel.TransactionsViewModel.SortField
import com.moneytracker.util.CurrencyUtils
import com.moneytracker.util.DateUtils
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Unified Card Table Component shared across DashboardScreen and TransactionsScreen.
 * Features:
 * - 5 Columns: Date, Category, SubCategory, Amount, Running Balance.
 * - Interactive sorting headers (Click to sort/toggle direction, Long-press for multi-sort).
 * - Collapsible & Expandable Category Group Headers.
 * - Drag-to-reorder support with persistable callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderableTransactionList(
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
    LaunchedEffect(transactions) {
        localTransactions = transactions
    }

    var collapsedCategoryKeys by remember { mutableStateOf(setOf<String>()) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localTransactions = localTransactions.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onReorder(localTransactions)
    }

    // Precompute cumulative running balances in chronological sequence
    val runningBalances = remember(localTransactions) {
        val chronological = localTransactions.sortedWith(
            compareBy<TransactionWithCategory> { it.date }.thenBy { it.id }
        )
        var balance = 0.0
        val map = mutableMapOf<Long, Double>()
        for (t in chronological) {
            when (t.type) {
                TransactionType.INCOME -> balance += t.amount
                TransactionType.EXPENSE -> balance -= t.amount
                TransactionType.INVESTMENT -> balance -= t.amount
            }
            map[t.id] = balance
        }
        map
    }

    // Group transactions by Type => Category
    val typeCategoryGroups = remember(localTransactions) {
        val types = listOf(TransactionType.INCOME, TransactionType.INVESTMENT, TransactionType.EXPENSE)
        types.mapNotNull { type ->
            val typeItems = localTransactions.filter { it.type == type }
            if (typeItems.isNotEmpty()) {
                val categoryGroups = typeItems.groupBy { it.categoryName }
                type to categoryGroups
            } else null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            // Interactive Table Header Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (reorderEnabled) {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                    TableHeaderCell(
                        label = "Date",
                        field = SortField.DATE,
                        secondarySorts = secondarySorts,
                        onHeaderClicked = onHeaderClicked,
                        onHeaderLongPressed = onHeaderLongPressed,
                        modifier = Modifier.weight(1f)
                    )
                    TableHeaderCell(
                        label = "Category",
                        field = SortField.CATEGORY,
                        secondarySorts = secondarySorts,
                        onHeaderClicked = onHeaderClicked,
                        onHeaderLongPressed = onHeaderLongPressed,
                        modifier = Modifier.weight(1.1f)
                    )
                    TableHeaderCell(
                        label = "SubCategory",
                        field = SortField.SUBCATEGORY,
                        secondarySorts = secondarySorts,
                        onHeaderClicked = onHeaderClicked,
                        onHeaderLongPressed = onHeaderLongPressed,
                        modifier = Modifier.weight(1.2f)
                    )
                    TableHeaderCell(
                        label = "Amount",
                        field = SortField.AMOUNT,
                        secondarySorts = secondarySorts,
                        onHeaderClicked = onHeaderClicked,
                        onHeaderLongPressed = onHeaderLongPressed,
                        modifier = Modifier.weight(1f)
                    )
                    TableHeaderCell(
                        label = "Running",
                        field = null,
                        secondarySorts = secondarySorts,
                        onHeaderClicked = onHeaderClicked,
                        onHeaderLongPressed = onHeaderLongPressed,
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                typeCategoryGroups.forEach { (type, categoryGroups) ->
                    categoryGroups.forEach { (categoryName, categoryItems) ->
                        val groupKey = "${type.name}_$categoryName"
                        val isCollapsed = collapsedCategoryKeys.contains(groupKey)
                        val categorySubtotal = categoryItems.sumOf { it.amount }

                        // Expandable Category Group Header Row
                        item(key = "header_$groupKey") {
                            CategoryGroupHeader(
                                categoryName = categoryName,
                                itemCount = categoryItems.size,
                                subtotal = categorySubtotal,
                                isCollapsed = isCollapsed,
                                onToggle = {
                                    collapsedCategoryKeys = if (isCollapsed) {
                                        collapsedCategoryKeys - groupKey
                                    } else {
                                        collapsedCategoryKeys + groupKey
                                    }
                                }
                            )
                        }

                        // Render Transaction Rows if expanded
                        if (!isCollapsed) {
                            items(categoryItems, key = { it.id }) { transaction ->
                                val runBal = runningBalances[transaction.id] ?: 0.0
                                if (reorderEnabled) {
                                    ReorderableItem(reorderableState, key = transaction.id) { isDragging ->
                                        TableDataRow(
                                            transaction = transaction,
                                            runningBalance = runBal,
                                            isDragging = isDragging,
                                            showDragHandle = true,
                                            onEditTransaction = onEditTransaction,
                                            onDeleteTransaction = onDeleteTransaction,
                                            dragHandle = {
                                                Icon(
                                                    imageVector = Icons.Default.DragHandle,
                                                    contentDescription = "Drag to reorder",
                                                    modifier = Modifier
                                                        .draggableHandle()
                                                        .size(20.dp),
                                                    tint = if (isDragging) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        )
                                    }
                                } else {
                                    TableDataRow(
                                        transaction = transaction,
                                        runningBalance = runBal,
                                        isDragging = false,
                                        showDragHandle = false,
                                        onEditTransaction = onEditTransaction,
                                        onDeleteTransaction = onDeleteTransaction,
                                        dragHandle = null
                                    )
                                }
                            }
                        }
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
        Surface(
            modifier = modifier.padding(horizontal = 2.dp),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 2.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
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
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    } else {
        val secondaryIndex = secondarySorts.indexOfFirst { it.field == field }
        val isSorted = secondaryIndex >= 0
        val direction = if (secondaryIndex >= 0) secondarySorts[secondaryIndex].direction else SortDirection.ASC
        val badgeText = if (secondaryIndex >= 0) "#${secondaryIndex + 1}" else null

        Surface(
            modifier = modifier
                .padding(horizontal = 2.dp)
                .combinedClickable(
                    onClick = { onHeaderClicked(field) },
                    onLongClick = { onHeaderLongPressed(field) }
                ),
            color = if (isSorted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f) else Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 2.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSorted) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSorted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (badgeText != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (direction == SortDirection.ASC) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                            contentDescription = if (direction == SortDirection.ASC) "Ascending" else "Descending",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(12.dp)
                                .padding(start = 1.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryGroupHeader(
    categoryName: String,
    itemCount: Int,
    subtotal: Double,
    isCollapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 3.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isCollapsed) "Expand category" else "Collapse category",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "$categoryName ($itemCount)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = CurrencyUtils.format(subtotal),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TableDataRow(
    transaction: TransactionWithCategory,
    runningBalance: Double,
    isDragging: Boolean,
    showDragHandle: Boolean,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (TransactionWithCategory) -> Unit,
    dragHandle: (@Composable () -> Unit)?
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDeleteTransaction(transaction)
                true
            } else {
                false
            }
        }
    )

    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) IncomeColor else ExpenseColor
    val prefix = if (isIncome) "+" else "-"

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val containerColor = if (isSwiping) MaterialTheme.colorScheme.errorContainer else Color.Transparent
            val contentColor = if (isSwiping) MaterialTheme.colorScheme.onErrorContainer else Color.Transparent

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(containerColor, shape = RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isSwiping) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = contentColor
                        )
                    }
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditTransaction(transaction.id) }
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showDragHandle && dragHandle != null) {
                        dragHandle()
                    }
                    Text(
                        text = DateUtils.formatDate(transaction.date),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = transaction.categoryName,
                        modifier = Modifier.weight(1.1f),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (transaction.subCategory.isNotBlank()) transaction.subCategory else "-",
                        modifier = Modifier.weight(1.2f),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (transaction.subCategory.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$prefix${CurrencyUtils.format(transaction.amount)}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = amountColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = CurrencyUtils.format(runningBalance),
                        modifier = Modifier.weight(1.1f),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}
