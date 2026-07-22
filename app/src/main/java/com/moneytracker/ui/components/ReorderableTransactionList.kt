package com.moneytracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.moneytracker.data.local.entity.TransactionWithCategory
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderableTransactionList(
    transactions: List<TransactionWithCategory>,
    reorderEnabled: Boolean,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onReorder: (List<TransactionWithCategory>) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (TransactionWithCategory) -> Unit
) {
    var localTransactions by remember { mutableStateOf(transactions) }
    LaunchedEffect(transactions) {
        localTransactions = transactions
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localTransactions = localTransactions.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onReorder(localTransactions)
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        items(localTransactions, key = { it.id }) { transaction ->
            if (reorderEnabled) {
                ReorderableItem(reorderableState, key = transaction.id) { isDragging ->
                    TransactionListItem(
                        transaction = transaction,
                        isDragging = isDragging,
                        showDragHandle = true,
                        onEditTransaction = onEditTransaction,
                        onDeleteTransaction = onDeleteTransaction,
                        dragHandle = {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag to reorder",
                                modifier = Modifier.draggableHandle(),
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
                TransactionListItem(
                    transaction = transaction,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionListItem(
    transaction: TransactionWithCategory,
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

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    ) {
        TransactionRow(
            transaction = transaction,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditTransaction(transaction.id) },
            leadingContent = if (showDragHandle) dragHandle else null
        )
        TransactionListDivider()
    }
}
