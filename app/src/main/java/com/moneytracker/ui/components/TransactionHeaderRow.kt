package com.moneytracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneytracker.ui.viewmodel.SortCriterion
import com.moneytracker.ui.viewmodel.TransactionsViewModel.SortDirection
import com.moneytracker.ui.viewmodel.TransactionsViewModel.SortField

/**
 * Header row for the unified Card Table in TransactionsScreen.
 * Design:
 * - Header labels on top, sort priority badges & arrows positioned directly BELOW text to ensure clear visibility.
 * - Proportional column weights prevent overlapping (Type is hidden from headers).
 * - Click amends or declicks, long-press adds/toggles multi-sort priority.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionHeaderRow(
    secondarySorts: List<SortCriterion>,
    onHeaderClicked: (SortField) -> Unit,
    onHeaderLongPressed: (SortField) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderItem(
                label = "Date",
                field = SortField.DATE,
                secondarySorts = secondarySorts,
                onHeaderClicked = onHeaderClicked,
                onHeaderLongPressed = onHeaderLongPressed,
                modifier = Modifier.weight(1f)
            )
            HeaderItem(
                label = "Category",
                field = SortField.CATEGORY,
                secondarySorts = secondarySorts,
                onHeaderClicked = onHeaderClicked,
                onHeaderLongPressed = onHeaderLongPressed,
                modifier = Modifier.weight(1.2f)
            )
            HeaderItem(
                label = "Description",
                field = SortField.DESCRIPTION,
                secondarySorts = secondarySorts,
                onHeaderClicked = onHeaderClicked,
                onHeaderLongPressed = onHeaderLongPressed,
                modifier = Modifier.weight(1.3f)
            )
            HeaderItem(
                label = "Amount",
                field = SortField.AMOUNT,
                secondarySorts = secondarySorts,
                onHeaderClicked = onHeaderClicked,
                onHeaderLongPressed = onHeaderLongPressed,
                modifier = Modifier.weight(1.1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeaderItem(
    label: String,
    field: SortField,
    secondarySorts: List<SortCriterion>,
    onHeaderClicked: (SortField) -> Unit,
    onHeaderLongPressed: (SortField) -> Unit,
    modifier: Modifier = Modifier
) {
    val secondaryIndex = secondarySorts.indexOfFirst { it.field == field }
    val isSorted = secondaryIndex >= 0
    val direction = if (secondaryIndex >= 0) secondarySorts[secondaryIndex].direction else SortDirection.ASC

    val badgeText = if (secondaryIndex >= 0) "#${secondaryIndex + 2}" else null

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
