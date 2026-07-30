package com.moneytracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneytracker.ui.viewmodel.SortCriterion
import com.moneytracker.ui.viewmodel.TransactionsViewModel.SortDirection
import com.moneytracker.ui.viewmodel.TransactionsViewModel.SortField

/**
 * Header row for the unified Card Table in TransactionsScreen.
 * Displays column titles with clear sort priority indicators:
 * - Type column is fixed as 1st sort (#1 🔒).
 * - Click amends the 2nd sort criterion.
 * - Long-press adds or toggles additional sort criteria (#2, #3, etc.) in priority order.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionHeaderRow(
    secondarySorts: List<SortCriterion>,
    onHeaderClicked: (SortField) -> Unit,
    onHeaderLongPressed: (SortField) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        @Composable
        fun HeaderItem(label: String, field: SortField) {
            val isType = field == SortField.TYPE
            val secondaryIndex = secondarySorts.indexOfFirst { it.field == field }
            val isSorted = isType || secondaryIndex >= 0
            val direction = if (isType) SortDirection.ASC else if (secondaryIndex >= 0) secondarySorts[secondaryIndex].direction else SortDirection.ASC
            val sortBadgeText = when {
                isType -> "#1"
                secondaryIndex >= 0 -> "#${secondaryIndex + 2}"
                else -> null
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.combinedClickable(
                    onClick = { onHeaderClicked(field) },
                    onLongClick = { onHeaderLongPressed(field) }
                )
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSorted) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSorted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                if (isType) {
                    Text(
                        text = " #1",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Fixed 1st sort",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .fillMaxWidth(0.04f)
                    )
                } else if (sortBadgeText != null) {
                    Text(
                        text = " $sortBadgeText",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                    Icon(
                        imageVector = if (direction == SortDirection.ASC) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = if (direction == SortDirection.ASC) "Ascending" else "Descending",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        HeaderItem(label = "Type", field = SortField.TYPE)
        HeaderItem(label = "Date", field = SortField.DATE)
        HeaderItem(label = "Category", field = SortField.CATEGORY)
        HeaderItem(label = "Description", field = SortField.DESCRIPTION)
        HeaderItem(label = "Amount", field = SortField.AMOUNT)
    }
}
