package com.moneytracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moneytracker.data.local.entity.ShoppingListEntity
import com.moneytracker.data.local.entity.ShoppingListItemEntity
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShoppingListPopupDialog(
    shoppingList: ShoppingListEntity,
    items: List<ShoppingListItemEntity>,
    onDismiss: () -> Unit,
    onToggleItemChecked: (ShoppingListItemEntity) -> Unit,
    onUpdateActuals: (ShoppingListItemEntity, Int, Double) -> Unit,
    onConfirmAndClose: (createExpenseTxn: Boolean) -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var createTxnOnClose by remember { mutableStateOf(true) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(shoppingList.shoppingDate) { dateFormat.format(Date(shoppingList.shoppingDate)) }

    val checkedItemsCount = items.count { it.isChecked }
    val totalActualSpent = items.filter { it.isChecked }.sumOf { it.quantityActual * it.unitPriceActual }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = shoppingList.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Date: $formattedDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cart Live Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "In Cart Total",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.formatZar(totalActualSpent),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Items Picked",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$checkedItemsCount / ${items.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Item List
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items in this shopping list",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            ShoppingListItemRow(
                                item = item,
                                onToggleChecked = { onToggleItemChecked(item) },
                                onUpdateActuals = { qty, price -> onUpdateActuals(item, qty, price) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Keep Open")
                    }

                    if (shoppingList.status != "CLOSED") {
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = IncomeColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Confirm & Close")
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm & Close Shopping List?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will update actual quantities and prices back into your Monthly Grocery Budget for ticked items ($checkedItemsCount items, total ${CurrencyUtils.formatZar(totalActualSpent)}).")
                    Text("Unchecked items will be removed from this list.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Checkbox(
                            checked = createTxnOnClose,
                            onCheckedChange = { createTxnOnClose = it }
                        )
                        Text(
                            text = "Log as Expense Transaction under Groceries",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onConfirmAndClose(createTxnOnClose)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeColor)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ShoppingListItemRow(
    item: ShoppingListItemEntity,
    onToggleChecked: () -> Unit,
    onUpdateActuals: (Int, Double) -> Unit
) {
    var qtyText by remember(item.quantityActual) { mutableStateOf(item.quantityActual.toString()) }
    var priceText by remember(item.unitPriceActual) { mutableStateOf(if (item.unitPriceActual > 0) item.unitPriceActual.toString() else "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onToggleChecked() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.itemDetail.ifBlank { "${item.category} - ${item.subCategory}" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "${item.category} • ${item.subCategory} (${item.unitSize})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Budget: ${item.quantityBudget} x ${CurrencyUtils.formatZar(item.unitPriceBudget)} = ${CurrencyUtils.formatZar(item.quantityBudget * item.unitPriceBudget)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Plain Numeric Input fields without currency sign
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Qty:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }
                            qtyText = clean
                            val qty = clean.toIntOrNull() ?: item.quantityBudget
                            val price = priceText.toDoubleOrNull() ?: item.unitPriceBudget
                            onUpdateActuals(qty, price)
                        },
                        modifier = Modifier.width(55.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Price:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() || it == '.' }
                            priceText = clean
                            val qty = qtyText.toIntOrNull() ?: item.quantityBudget
                            val price = clean.toDoubleOrNull() ?: item.unitPriceBudget
                            onUpdateActuals(qty, price)
                        },
                        modifier = Modifier.width(75.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.bodySmall,
                        placeholder = { Text(item.unitPriceBudget.toString(), style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }
        }
    }
}
