package com.moneytracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneytracker.ui.viewmodel.TransactionsViewModel.SortDirection
import com.moneytracker.ui.viewmodel.TransactionsViewModel.SortField

/**
 * Header row displayed above each transaction list section.
 * Shows column titles and allows the user to change the sort field by clicking on a header.
 * The currently selected sort field is highlighted, and an arrow indicates the sort direction.
 */
@Composable
fun TransactionHeaderRow(
    currentSort: SortField,
    sortDirection: SortDirection,
    onSortChange: (SortField) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        @Composable
        fun HeaderItem(label: String, field: SortField) {
            val isSelected = currentSort == field
            Row(modifier = Modifier.clickable { onSortChange(field) }) {
                Text(
                    text = label,
                    style = if (isSelected) MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary) else MaterialTheme.typography.bodyMedium
                )
                if (isSelected) {
                    Icon(
                        imageVector = if (sortDirection == SortDirection.ASC) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = if (sortDirection == SortDirection.ASC) "Ascending" else "Descending",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        HeaderItem(label = "Date", field = SortField.DATE)
        HeaderItem(label = "Category", field = SortField.CATEGORY)
        HeaderItem(label = "Description", field = SortField.DESCRIPTION)
        HeaderItem(label = "Amount", field = SortField.AMOUNT)
        HeaderItem(label = "Type", field = SortField.TYPE)
    }
}
