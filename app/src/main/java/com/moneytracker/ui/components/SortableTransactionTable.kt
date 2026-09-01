package com.moneytracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.data.local.entity.RecurrenceFrequency
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.local.entity.TransactionWithCategory
import com.moneytracker.ui.theme.EducationColor
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.theme.InvestmentColor
import com.moneytracker.util.CurrencyUtils
import com.moneytracker.util.DateUtils

@Composable
fun SortableTransactionTable(
    transactions: List<TransactionWithCategory>,
    modifier: Modifier = Modifier,
    tableName: String = "Transactions",
    reorderEnabled: Boolean = false,
    secondarySorts: List<com.moneytracker.ui.viewmodel.SortCriterion> = emptyList(),
    onHeaderClicked: (com.moneytracker.ui.viewmodel.TransactionsViewModel.SortField) -> Unit = {},
    onHeaderLongPressed: (com.moneytracker.ui.viewmodel.TransactionsViewModel.SortField) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onReorder: ((List<TransactionWithCategory>) -> Unit)? = null,
    onEditTransaction: (Long) -> Unit = {},
    onDeleteTransaction: (TransactionWithCategory) -> Unit = {}
) {
    val columns = listOf(
        GridColumnDefinition<TransactionWithCategory>(
            id = "date",
            title = "Date",
            width = 85.dp,
            isSortable = true,
            defaultStrategy = ColumnSortStrategy.ASCENDING,
            valueExtractor = { DateUtils.toLocalDate(it.date).toString() },
            cellContent = { item, _, wrapText ->
                Text(
                    text = DateUtils.toLocalDate(item.date).toString(),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        ),
        GridColumnDefinition<TransactionWithCategory>(
            id = "category",
            title = "Category",
            width = 105.dp,
            isSortable = true,
            defaultStrategy = ColumnSortStrategy.CUSTOM_PRIORITY,
            valueExtractor = { it.categoryName },
            cellContent = { item, _, wrapText ->
                val categoryColor = when (item.type) {
                    TransactionType.INCOME -> IncomeColor
                    TransactionType.INVESTMENT -> InvestmentColor
                    TransactionType.EDUCATION -> EducationColor
                    TransactionType.EXPENSE -> ExpenseColor
                }
                Surface(
                    color = categoryColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        textAlign = TextAlign.Start,
                        maxLines = if (wrapText) 2 else 1,
                        overflow = if (wrapText) TextOverflow.Clip else TextOverflow.Ellipsis
                    )
                }
            }
        ),
        GridColumnDefinition<TransactionWithCategory>(
            id = "subCategory",
            title = "SubCategory",
            width = 120.dp,
            isSortable = true,
            defaultStrategy = ColumnSortStrategy.CUSTOM_PRIORITY,
            valueExtractor = { it.subCategory },
            cellContent = { item, _, wrapText ->
                Text(
                    text = item.subCategory,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
                    maxLines = if (wrapText) 2 else 1,
                    overflow = if (wrapText) TextOverflow.Clip else TextOverflow.Ellipsis
                )
            }
        ),
        GridColumnDefinition<TransactionWithCategory>(
            id = "detail",
            title = "Detail",
            width = 120.dp,
            isSortable = true,
            defaultStrategy = ColumnSortStrategy.CUSTOM_PRIORITY,
            valueExtractor = { it.detail },
            cellContent = { item, _, wrapText ->
                Text(
                    text = if (item.detail.isNotBlank()) item.detail else "-",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = if (item.detail.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = if (wrapText) 2 else 1,
                    overflow = if (wrapText) TextOverflow.Clip else TextOverflow.Ellipsis
                )
            }
        ),
        GridColumnDefinition<TransactionWithCategory>(
            id = "amount",
            title = "Amount",
            width = 105.dp,
            isSortable = true,
            defaultStrategy = ColumnSortStrategy.DESCENDING,
            valueExtractor = { it.amount.toString() },
            cellContent = { item, _, wrapText ->
                val categoryColor = when (item.type) {
                    TransactionType.INCOME -> IncomeColor
                    TransactionType.INVESTMENT -> InvestmentColor
                    TransactionType.EDUCATION -> EducationColor
                    TransactionType.EXPENSE -> ExpenseColor
                }
                val prefix = if (item.type == TransactionType.INCOME) "+" else "-"
                val isPlanFuture = item.recurrenceFrequency == RecurrenceFrequency.PLAN_FUTURE

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$prefix${CurrencyUtils.format(kotlin.math.abs(item.amount))}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = if (isPlanFuture) MaterialTheme.colorScheme.onSurfaceVariant else categoryColor,
                        textAlign = TextAlign.End,
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
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 2.5.dp, vertical = 0.5.dp)
                            )
                        }
                    }
                }
            }
        ),
        GridColumnDefinition<TransactionWithCategory>(
            id = "note",
            title = "Note",
            width = 130.dp,
            isSortable = true,
            defaultStrategy = ColumnSortStrategy.CUSTOM_PRIORITY,
            valueExtractor = { item -> item.note },
            cellContent = { item, _, wrapText ->
                Text(
                    text = if (item.note.isNotBlank()) item.note else "-",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = if (item.note.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = if (wrapText) 2 else 1,
                    overflow = if (wrapText) TextOverflow.Clip else TextOverflow.Ellipsis
                )
            }
        )
    )

    SocialNeltz_Grid_Sortable_Pilled(
        items = transactions,
        columns = columns,
        itemKey = { it.id },
        tableName = tableName,
        modifier = Modifier.padding(contentPadding),
        initialPillColumnId = "category",
        onRowClick = { onEditTransaction(it.id) },
        onRowDoubleClick = { onEditTransaction(it.id) }
    )
}
