// ReorderableTransactionList.kt - Deprecated wrapper
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
 * Deprecated wrapper for ReorderableTransactionList.
 * Use [SortableTransactionTable] for the new implementation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Deprecated(
    message = "ReorderableTransactionList is deprecated. Use SortableTransactionTable.",
    replaceWith = ReplaceWith("SortableTransactionTable")
)
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
    SortableTransactionTable(
        transactions = transactions,
        reorderEnabled = reorderEnabled,
        secondarySorts = secondarySorts,
        onHeaderClicked = onHeaderClicked,
        onHeaderLongPressed = onHeaderLongPressed,
        contentPadding = contentPadding,
        onReorder = onReorder,
        onEditTransaction = onEditTransaction,
        onDeleteTransaction = onDeleteTransaction
    )
}
