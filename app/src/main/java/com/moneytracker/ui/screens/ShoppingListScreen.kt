package com.moneytracker.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneytracker.ui.components.AppTopBar
import com.moneytracker.ui.components.ShoppingListPopupDialog
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.viewmodel.GroceriesViewModel
import com.moneytracker.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: GroceriesViewModel,
    contentPadding: PaddingValues,
    onBack: (() -> Unit)? = null
) {
    val shoppingLists by viewModel.shoppingLists.collectAsState()
    val activeShoppingList by viewModel.activeShoppingList.collectAsState()
    val activeShoppingListItems by viewModel.activeShoppingListItems.collectAsState()

    val dateFormat = rememberDateFormat()

    Scaffold(
        topBar = {
            AppTopBar(
                screenTitle = "Shopping Lists",
                showBack = onBack != null,
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Shopping Trips (${shoppingLists.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (shoppingLists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.height(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No shopping lists created for this month yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Go to 'Monthly Groceries' tab, select items, and tap 'Create Shopping List'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(shoppingLists, key = { it.id }) { list ->
                        val dateStr = dateFormat.format(Date(list.shoppingDate))
                        val isClosed = list.status == "CLOSED"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.openShoppingList(list) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isClosed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = list.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Date: $dateStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Budget: ${CurrencyUtils.formatZar(list.totalBudgetCost)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (isClosed) {
                                            Text(
                                                text = "Spent: ${CurrencyUtils.formatZar(list.totalActualCost)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = IncomeColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isClosed) IncomeColor.copy(alpha = 0.2f) else ExpenseColor.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = list.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isClosed) IncomeColor else ExpenseColor,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }

                                    IconButton(onClick = { viewModel.deleteShoppingList(list) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Active Shopping List Popup Dialog
    if (activeShoppingList != null) {
        ShoppingListPopupDialog(
            shoppingList = activeShoppingList!!,
            items = activeShoppingListItems,
            onDismiss = { viewModel.openShoppingList(null) },
            onToggleItemChecked = { viewModel.toggleShoppingListItemChecked(it) },
            onUpdateActuals = { item, qty, price -> viewModel.updateShoppingListItemActuals(item, qty, price) },
            onConfirmAndClose = { createTxn -> viewModel.confirmAndCloseActiveShoppingList(createTxn) },
            onReopen = { viewModel.reopenShoppingList(activeShoppingList!!) }
        )
    }
}

@Composable
private fun rememberDateFormat(): SimpleDateFormat {
    return androidx.compose.runtime.remember {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    }
}
