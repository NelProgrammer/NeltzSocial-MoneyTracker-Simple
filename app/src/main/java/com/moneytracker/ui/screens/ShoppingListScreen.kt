package com.moneytracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneytracker.data.local.entity.GroceryItemEntity
import com.moneytracker.ui.components.AppTopBar
import com.moneytracker.ui.components.EmptyState
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.viewmodel.GroceriesViewModel
import com.moneytracker.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    viewModel: GroceriesViewModel,
    contentPadding: PaddingValues,
    onBack: (() -> Unit)? = null
) {
    val items by viewModel.items.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val uncheckedItems = items.filter { !it.isChecked }
    val checkedItems = items.filter { it.isChecked }

    val totalListCost = items.sumOf { it.totalPrice }
    val cartSpent = checkedItems.sumOf { it.totalPrice }
    val progress = if (items.isNotEmpty()) checkedItems.size.toFloat() / items.size else 0f

    Scaffold(
        topBar = {
            AppTopBar(
                screenTitle = "Grocery Shopping List",
                showBack = onBack != null,
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item to Shopping List")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 80.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cart Live Banner Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "In Store Cart Live Total",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = CurrencyUtils.format(cartSpent),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = IncomeColor
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estimated Total List: ${CurrencyUtils.format(totalListCost)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "${checkedItems.size} of ${items.size} Items in Cart",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = IncomeColor,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // Section 1: Items To Buy (Unchecked)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "To Buy (${uncheckedItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (uncheckedItems.isEmpty()) {
                item {
                    EmptyState(
                        message = if (items.isEmpty()) "Your shopping list is empty. Tap + to add items to buy!"
                        else "All items are in your cart! Great job!"
                    )
                }
            } else {
                items(uncheckedItems, key = { it.id }) { item ->
                    ShoppingListItemRow(
                        item = item,
                        onToggle = { viewModel.toggleCheck(item) },
                        onDelete = { viewModel.deleteGroceryItem(item) }
                    )
                }
            }

            // Section 2: Items in Cart (Checked)
            if (checkedItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "In Cart / Bought (${checkedItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeColor
                        )
                    }
                }

                items(checkedItems, key = { it.id }) { item ->
                    ShoppingListItemRow(
                        item = item,
                        onToggle = { viewModel.toggleCheck(item) },
                        onDelete = { viewModel.deleteGroceryItem(item) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddShoppingListItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, price, qty, unit ->
                viewModel.saveGroceryItem(
                    itemName = name,
                    unitPrice = price,
                    quantity = qty,
                    sizeUnit = unit
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ShoppingListItemRow(
    item: GroceryItemEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onToggle() }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${item.quantity} × ${CurrencyUtils.format(item.unitPrice)} (${item.sizeUnit})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = CurrencyUtils.format(item.totalPrice),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.isChecked) IncomeColor else ExpenseColor,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Item",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AddShoppingListItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("pack") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shopping List Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name (e.g. Milk 2L)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Unit Price (R)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit / Size (e.g. bottle, 2L, kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val qty = qtyStr.toIntOrNull() ?: 1
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), price, qty, unit.trim())
                    }
                }
            ) {
                Text("Add to List")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
