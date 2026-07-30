package com.moneytracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneytracker.data.local.entity.TransactionWithCategory
import com.moneytracker.ui.components.ReorderableTransactionList

@Composable
fun TransactionSectionCard(
    title: String,
    transactions: List<TransactionWithCategory>,
    reorderEnabled: Boolean,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (TransactionWithCategory) -> Unit,
    onReorder: (List<TransactionWithCategory>) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (transactions.isEmpty()) {
                Text(
                    text = "No $title transactions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ReorderableTransactionList(
                    transactions = transactions,
                    reorderEnabled = reorderEnabled,
                    contentPadding = contentPadding,
                    onEditTransaction = onEditTransaction,
                    onDeleteTransaction = onDeleteTransaction,
                    onReorder = onReorder
                )
            }
        }
    }
}
