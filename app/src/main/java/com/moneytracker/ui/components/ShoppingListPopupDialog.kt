package com.moneytracker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val GROCERY_DEFAULT_CATEGORIES_MAP: Map<String, List<String>> = mapOf(
    "Starch" to listOf("Rice", "Maize Meal", "Pasta", "Flour", "Cereal", "Oats", "Potatoes", "Samp", "Bread"),
    "Dairy" to listOf("Fresh Milk", "Long Life Milk", "Cheese", "Butter", "Yoghurt", "Cream", "Eggs", "Margarine"),
    "Meat & Poultry" to listOf("Chicken", "Beef", "Pork", "Lamb", "Mince", "Sausages", "Fish", "Bacon", "Cold Meats"),
    "Produce" to listOf("Tomatoes", "Onions", "Potatoes", "Carrots", "Bananas", "Apples", "Oranges", "Spinach", "Cabbage", "Lettuce", "Avocado", "Garlic & Ginger"),
    "Bakery" to listOf("Bread", "Rolls", "Buns", "Pies", "Cakes", "Biscuits", "Rusk"),
    "Beverages" to listOf("Coffee", "Tea", "Juice", "Soft Drinks", "Water", "Squash", "Energy Drinks", "Milkshake"),
    "Pantry & Condiments" to listOf("Cooking Oil", "Sugar", "Salt & Spices", "Sauces", "Soup Powder", "Vinegar", "Mayonnaise", "Jam", "Peanut Butter", "Honey"),
    "Canned Goods" to listOf("Baked Beans", "Tinned Fish", "Chopped Tomatoes", "Sweetcorn", "Tinned Peas", "Tinned Fruit", "Soup"),
    "Snacks & Sweets" to listOf("Chips", "Chocolates", "Sweets", "Nuts", "Dried Fruit", "Popcorn", "Crackers"),
    "Frozen" to listOf("Frozen Veg", "Frozen Chips", "Ice Cream", "Frozen Pastry", "Frozen Fish", "Frozen Meals"),
    "Household & Cleaning" to listOf("Washing Powder", "Dishwashing Liquid", "Bleach", "Fabric Softener", "Trash Bags", "Surface Cleaner", "Sponges"),
    "Personal Care" to listOf("Soap", "Shampoo", "Toothpaste", "Deodorant", "Toilet Paper", "Lotion", "Shaving"),
    "Baby" to listOf("Nappies", "Baby Wipes", "Baby Food", "Formula", "Baby Lotion"),
    "Pet Care" to listOf("Dog Food", "Cat Food", "Pet Treats", "Cat Litter"),
    "Other" to listOf("General", "Miscellaneous")
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListPopupDialog(
    shoppingList: ShoppingListEntity,
    items: List<ShoppingListItemEntity>,
    onDismiss: () -> Unit,
    onToggleItemChecked: (ShoppingListItemEntity) -> Unit,
    onUpdateActuals: (ShoppingListItemEntity, Int, Double) -> Unit,
    onAddItem: (category: String, subCategory: String, itemDetail: String, unitSize: String, qtyBudget: Int, priceBudget: Double, qtyActual: Int, priceActual: Double) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteItem: (ShoppingListItemEntity) -> Unit = {},
    onConfirmAndClose: (createExpenseTxn: Boolean) -> Unit,
    onReopen: () -> Unit = {}
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var createTxnOnClose by remember { mutableStateOf(true) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingActualsItem by remember { mutableStateOf<ShoppingListItemEntity?>(null) }
    var actionItem by remember { mutableStateOf<ShoppingListItemEntity?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(shoppingList.shoppingDate) { dateFormat.format(Date(shoppingList.shoppingDate)) }

    val checkedItemsCount = items.count { it.isChecked }
    val totalEstimatedBudget = remember(items) { items.sumOf { it.quantityBudget * it.unitPriceBudget } }
    val totalActualSpent = items.filter { it.isChecked }.sumOf { it.quantityActual * it.unitPriceActual }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .padding(horizontal = 8.dp, vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = shoppingList.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Date: $formattedDate",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick In-Store Add Button
                        IconButton(
                            onClick = { showAddItemDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Item",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Summary Stats Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Estimated Budget",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyUtils.formatZar(totalEstimatedBudget),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$checkedItemsCount / ${items.size} Ticked",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Ticked Actual Total",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val isOver = totalActualSpent > totalEstimatedBudget && totalEstimatedBudget > 0
                        Text(
                            text = CurrencyUtils.formatZar(totalActualSpent),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (isOver) ExpenseColor else IncomeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Unified Shopping List Table Grid
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items in this shopping list.\nTap '+' above to add an item.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    UnifiedShoppingListTable(
                        items = items,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        onToggleChecked = { item ->
                            if (!item.isChecked && item.quantityActual == 0 && item.unitPriceActual == 0.0) {
                                onUpdateActuals(item, item.quantityBudget, item.unitPriceBudget)
                            }
                            onToggleItemChecked(item)
                        },
                        onEditActuals = { editingActualsItem = it },
                        onLongClick = { actionItem = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }

                    if (shoppingList.status != "CLOSED") {
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(containerColor = IncomeColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Confirm & Close")
                        }
                    } else {
                        Button(
                            onClick = onReopen,
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Reopen List")
                        }
                    }
                }
            }
        }
    }

    // Confirm & Close Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm & Close Shopping List?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will update actual quantities and prices back into your Monthly Grocery Budget for ticked items ($checkedItemsCount items, total ${CurrencyUtils.formatZar(totalActualSpent)}).")

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

    // Edit Actuals Dialog (Double-Click)
    if (editingActualsItem != null) {
        val item = editingActualsItem!!
        EditShoppingListItemActualsDialog(
            item = item,
            onDismiss = { editingActualsItem = null },
            onSave = { qty, price ->
                onUpdateActuals(item, qty, price)
                editingActualsItem = null
            }
        )
    }

    // Add Item Dialog (+ Button)
    if (showAddItemDialog) {
        AddShoppingListItemDialog(
            existingItems = items,
            onDismiss = { showAddItemDialog = false },
            onSave = { cat, subCat, detail, unitSize, qty, price ->
                onAddItem(cat, subCat, detail, unitSize, qty, price, qty, price)
                showAddItemDialog = false
            }
        )
    }

    // Long-Press Action Dialog
    if (actionItem != null) {
        val item = actionItem!!
        ShoppingListItemActionDialog(
            item = item,
            onDismiss = { actionItem = null },
            onEditActuals = {
                actionItem = null
                editingActualsItem = item
            },
            onDelete = {
                onDeleteItem(item)
                actionItem = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UnifiedShoppingListTable(
    items: List<ShoppingListItemEntity>,
    modifier: Modifier = Modifier,
    onToggleChecked: (ShoppingListItemEntity) -> Unit,
    onEditActuals: (ShoppingListItemEntity) -> Unit,
    onLongClick: (ShoppingListItemEntity) -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Table Headers: ✓ | Category | Item | Budget | Actual | Remain
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Check Column
                Box(
                    modifier = Modifier.width(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 2. Category Column
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.9f)
                )

                // 3. Item Column
                Text(
                    text = "Item",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1.2f)
                )

                // 4. Budget Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Budget",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "Qty×Unit | Cost",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 5. Actual Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Actual",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "Qty×Unit | Cost",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 6. Remain Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Remain",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "Qty×Unit | Cost",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.End
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Table Rows
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    }
                    UnifiedShoppingListItemTableRow(
                        item = item,
                        onToggleChecked = { onToggleChecked(item) },
                        onEditActuals = { onEditActuals(item) },
                        onLongClick = { onLongClick(item) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UnifiedShoppingListItemTableRow(
    item: ShoppingListItemEntity,
    onToggleChecked: () -> Unit,
    onEditActuals: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (item.isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else Color.Transparent
            )
            .combinedClickable(
                onClick = onToggleChecked,
                onDoubleClick = onEditActuals,
                onLongClick = onLongClick
            )
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Checkbox Column
        Box(
            modifier = Modifier.width(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onToggleChecked() },
                modifier = Modifier.size(24.dp)
            )
        }

        // 2. Category Column
        Column(
            modifier = Modifier
                .weight(0.9f)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // 3. Item Column (Subcategory bold on top, Name & Unit Size, Strikethrough if checked)
        Column(
            modifier = Modifier
                .weight(1.2f)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // 1. Subcategory
            Text(
                text = item.subCategory,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                ),
                fontWeight = FontWeight.Bold,
                color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 2. Name & Unit Size
            val nameAndUnit = if (item.itemDetail.isNotBlank()) {
                if (item.unitSize.isNotBlank()) "${item.itemDetail} (${item.unitSize})" else item.itemDetail
            } else {
                if (item.unitSize.isNotBlank()) "(${item.unitSize})" else ""
            }
            if (nameAndUnit.isNotBlank()) {
                Text(
                    text = nameAndUnit,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.5.sp,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 4. Budget Column (Qty × Unit Price -> Total Cost)
        val budgetCost = item.quantityBudget * item.unitPriceBudget
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${item.quantityBudget} × ${CurrencyUtils.formatZar(item.unitPriceBudget)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1
            )
            Text(
                text = CurrencyUtils.formatZar(budgetCost),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 5. Actual Column (Qty × Unit Price -> Total Cost)
        val actualCost = item.quantityActual * item.unitPriceActual
        val isOver = actualCost > budgetCost && budgetCost > 0
        val actualColor = if (isOver) ExpenseColor else if (item.isChecked) IncomeColor else MaterialTheme.colorScheme.onSurface

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${item.quantityActual} × ${CurrencyUtils.formatZar(item.unitPriceActual)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1
            )
            Text(
                text = CurrencyUtils.formatZar(actualCost),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                fontWeight = FontWeight.Bold,
                color = actualColor,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 6. Remain Column (Qty × Unit Price -> Remaining Cost)
        val remQty = item.quantityBudget - item.quantityActual
        val remCost = budgetCost - actualCost
        val remainColor = when {
            remQty < 0 || remCost < 0 -> ExpenseColor
            remQty == 0 -> MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.secondary
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "$remQty × ${CurrencyUtils.formatZar(item.unitPriceBudget)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1
            )
            Text(
                text = CurrencyUtils.formatZar(remCost),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                fontWeight = FontWeight.Bold,
                color = remainColor,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
    }
}

// Edit Actuals Dialog (Double Click)
@Composable
private fun EditShoppingListItemActualsDialog(
    item: ShoppingListItemEntity,
    onDismiss: () -> Unit,
    onSave: (qtyActual: Int, priceActual: Double) -> Unit
) {
    var qtyText by remember(item.quantityActual) { mutableStateOf(item.quantityActual.toString()) }
    var priceText by remember(item.unitPriceActual) { mutableStateOf(if (item.unitPriceActual > 0) item.unitPriceActual.toString() else "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Edit In-Store Actuals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.category} • ${item.subCategory}${if (item.unitSize.isNotBlank()) " (${item.unitSize})" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Item Name Detail
                if (item.itemDetail.isNotBlank()) {
                    Text(
                        text = "Item: ${item.itemDetail}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Budget Target Reference Banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Budget Target:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${item.quantityBudget} × ${CurrencyUtils.formatZar(item.unitPriceBudget)} = ${CurrencyUtils.formatZar(item.quantityBudget * item.unitPriceBudget)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Quantity & Unit Price Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                        label = { Text("Actual Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Actual Price (R)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Action: Match Budget
                OutlinedButton(
                    onClick = {
                        qtyText = item.quantityBudget.toString()
                        priceText = item.unitPriceBudget.toString()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy Budget Target (Qty: ${item.quantityBudget}, Price: ${CurrencyUtils.formatZar(item.unitPriceBudget)})")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyText.toIntOrNull() ?: item.quantityBudget
                    val price = priceText.toDoubleOrNull() ?: item.unitPriceBudget
                    onSave(qty, price)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Actuals")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Add Item Dialog (+ Button)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddShoppingListItemDialog(
    existingItems: List<ShoppingListItemEntity>,
    onDismiss: () -> Unit,
    onSave: (cat: String, subCat: String, detail: String, unitSize: String, qty: Int, price: Double) -> Unit
) {
    var categoryInput by remember { mutableStateOf("Starch") }
    var subCategoryInput by remember { mutableStateOf("Rice") }
    var itemDetailInput by remember { mutableStateOf("") }
    var unitSizeInput by remember { mutableStateOf("1kg") }
    var qtyInput by remember { mutableStateOf("1") }
    var priceInput by remember { mutableStateOf("0") }

    var catExpanded by remember { mutableStateOf(false) }
    var subCatExpanded by remember { mutableStateOf(false) }

    val availableCategories = remember(existingItems) {
        (GROCERY_DEFAULT_CATEGORIES_MAP.keys + existingItems.map { it.category }.filter { it.isNotBlank() }).distinct()
    }

    val availableSubCats = remember(categoryInput, existingItems) {
        val catSubs = GROCERY_DEFAULT_CATEGORIES_MAP[categoryInput] ?: emptyList()
        val existingSubs = existingItems.filter { it.category.equals(categoryInput, ignoreCase = true) }.map { it.subCategory }
        (catSubs + existingSubs + GROCERY_DEFAULT_CATEGORIES_MAP.values.flatten()).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Item to Shopping List",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category (ManagedComboboxWithPills)
                ManagedComboboxWithPills(
                    label = "Category",
                    selectedValue = categoryInput,
                    onValueChange = { cat ->
                        categoryInput = cat
                        val catSubs = GROCERY_DEFAULT_CATEGORIES_MAP[cat]
                        if (catSubs != null && catSubs.isNotEmpty()) {
                            subCategoryInput = catSubs.first()
                        }
                    },
                    items = availableCategories,
                    itemToText = { it }
                )

                // SubCategory (ManagedComboboxWithPills)
                ManagedComboboxWithPills(
                    label = "Sub-Category",
                    selectedValue = subCategoryInput,
                    onValueChange = { subCategoryInput = it },
                    items = availableSubCats,
                    itemToText = { it }
                )

                // Detail / Name
                OutlinedTextField(
                    value = itemDetailInput,
                    onValueChange = { itemDetailInput = it },
                    label = { Text("Item Name / Detail (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Unit Size
                OutlinedTextField(
                    value = unitSizeInput,
                    onValueChange = { unitSizeInput = it },
                    label = { Text("Unit Size (e.g. 2L, 1kg, Pack)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Qty & Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { qtyInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Price (R)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyInput.toIntOrNull() ?: 1
                    val price = priceInput.toDoubleOrNull() ?: 0.0
                    onSave(categoryInput, subCategoryInput, itemDetailInput, unitSizeInput, qty, price)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Add Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Long-Press Action Dialog
@Composable
private fun ShoppingListItemActionDialog(
    item: ShoppingListItemEntity,
    onDismiss: () -> Unit,
    onEditActuals: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = item.itemDetail.ifBlank { item.subCategory },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category: ${item.category} • ${item.subCategory} (${item.unitSize})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Budget: ${item.quantityBudget} × ${CurrencyUtils.formatZar(item.unitPriceBudget)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Actual: ${item.quantityActual} × ${CurrencyUtils.formatZar(item.unitPriceActual)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onEditActuals,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Edit Actuals")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseColor)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Remove")
                }
            }
        }
    )
}
