package com.moneytracker.ui.screens

import java.time.LocalDate
import com.moneytracker.util.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneytracker.data.local.entity.GroceryBudgetItemEntity
import com.moneytracker.data.local.entity.ShoppingListEntity
import com.moneytracker.data.local.entity.UnitSizeEntity
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.ui.components.AppTopBar
import com.moneytracker.ui.components.ShoppingListPopupDialog
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.viewmodel.GroceriesViewModel
import com.moneytracker.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGroceriesScreen(
    viewModel: GroceriesViewModel,
    repository: TransactionRepository
) {
    val selectedPayMonthDate by viewModel.selectedPayMonthDate.collectAsState()
    val overview by viewModel.overview.collectAsState()
    val budgetItems by viewModel.budgetItems.collectAsState()
    val shoppingLists by viewModel.shoppingLists.collectAsState()
    val selectedIds by viewModel.selectedBudgetItemIds.collectAsState()
    val activeShoppingList by viewModel.activeShoppingList.collectAsState()
    val activeShoppingListItems by viewModel.activeShoppingListItems.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<GroceryBudgetItemEntity?>(null) }
    var showGenerateListDialog by remember { mutableStateOf(false) }

    val monthYearFormat = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    val formattedMonth = remember(selectedPayMonthDate) { selectedPayMonthDate.format(monthYearFormat) }

    Scaffold(
        topBar = {
            AppTopBar(screenTitle = "Grocery Budget")
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingItem = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Budget Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Unified PayMonth Filter Header
            com.moneytracker.ui.components.PayMonthFilterHeader(
                selectedPayMonthDate = selectedPayMonthDate,
                onPayMonthSelected = { viewModel.setPayMonth(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Overview Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Budgeted Cost",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = CurrencyUtils.formatZar(overview.totalBudget),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Actual Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = CurrencyUtils.formatZar(overview.totalActual),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (overview.isOverBudget) ExpenseColor else IncomeColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (overview.isOverBudget) "Over Budget" else "Net Savings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (overview.isOverBudget) "- ${CurrencyUtils.formatZar(overview.overAmount)}" else "+ ${CurrencyUtils.formatZar(overview.netVariance)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (overview.isOverBudget) ExpenseColor else IncomeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shopping Lists Dropdown Section
            if (shoppingLists.isNotEmpty()) {
                ShoppingListsDropdownSection(
                    shoppingLists = shoppingLists,
                    onOpenList = { viewModel.openShoppingList(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Multi-Select Header & Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Items (${budgetItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedIds.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearBudgetItemSelection() }) {
                            Text("Deselect (${selectedIds.size})")
                        }
                    } else {
                        TextButton(onClick = { viewModel.selectAllBudgetItems() }) {
                            Text("Select All")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Budget List Grouped by Category
            if (budgetItems.isEmpty()) {
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
                            text = "No budget items for $formattedMonth",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap '+' below to add planned groceries",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        UnifiedGroceryTable(
                            items = budgetItems,
                            selectedIds = selectedIds,
                            onToggleSelect = { viewModel.toggleBudgetItemSelection(it) },
                            onEdit = {
                                editingItem = it
                                showAddEditDialog = true
                            },
                            onDelete = { viewModel.deleteGroceryBudgetItem(it) }
                        )
                    }
                }
            }

            // Bottom Floating Bar when items selected to Generate Shopping List
            if (selectedIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedIds.size} items selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Button(
                            onClick = { showGenerateListDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = IncomeColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCartCheckout,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Create Shopping List")
                        }
                    }
                }
            }
        }
    }

    // Active Shopping List Popup Dialog
    if (activeShoppingList != null) {
        val list = activeShoppingList!!
        ShoppingListPopupDialog(
            shoppingList = list,
            items = activeShoppingListItems,
            onDismiss = { viewModel.openShoppingList(null) },
            onToggleItemChecked = { viewModel.toggleShoppingListItemChecked(it) },
            onUpdateActuals = { item, qty, price -> viewModel.updateShoppingListItemActuals(item, qty, price) },
            onAddItem = { cat, subCat, detail, unitSize, qtyB, priceB, qtyA, priceA ->
                viewModel.addShoppingListItem(
                    shoppingListId = list.id,
                    category = cat,
                    subCategory = subCat,
                    itemDetail = detail,
                    unitSize = unitSize,
                    quantityBudget = qtyB,
                    unitPriceBudget = priceB,
                    quantityActual = qtyA,
                    unitPriceActual = priceA
                )
            },
            onDeleteItem = { viewModel.deleteShoppingListItem(it) },
            onConfirmAndClose = { createTxn -> viewModel.confirmAndCloseActiveShoppingList(createTxn) },
            onReopen = { viewModel.reopenShoppingList(list) }
        )
    }

    // Generate Shopping List Title Dialog
    if (showGenerateListDialog) {
        val defaultShoppingTripTitle = remember(selectedIds) { viewModel.getNextShoppingListTitle() }
        var titleInput by remember(selectedIds) { mutableStateOf(defaultShoppingTripTitle) }
        AlertDialog(
            onDismissRequest = { showGenerateListDialog = false },
            title = { Text("Generate Shopping List") },
            text = {
                Column {
                    Text("Create an in-store shopping list for the ${selectedIds.size} selected budget items:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Shopping List Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGenerateListDialog = false
                        viewModel.generateShoppingList(titleInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeColor)
                ) {
                    Text("Generate List")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateListDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add / Edit Grocery Budget Item Dialog
    if (showAddEditDialog) {
        AddEditBudgetItemDialog(
            item = editingItem,
            repository = repository,
            viewModel = viewModel,
            selectedPayMonthDate = selectedPayMonthDate,
            onDismiss = { showAddEditDialog = false },
            onSave = { cat, subCat, detail, unitSize, note, qtyB, priceB, isRec, startTs ->
                viewModel.saveGroceryBudgetItem(
                    id = editingItem?.id ?: 0L,
                    category = cat,
                    subCategory = subCat,
                    itemDetail = detail,
                    unitSize = unitSize,
                    note = note,
                    quantityBudget = qtyB,
                    unitPriceBudget = priceB,
                    isRecurring = isRec,
                    startDate = startTs
                )
                showAddEditDialog = false
            }
        )
    }
}

@Composable
private fun ShoppingListsDropdownSection(
    shoppingLists: List<ShoppingListEntity>,
    onOpenList: (ShoppingListEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
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
                    Text(
                        text = "Shopping Lists (${shoppingLists.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Expand Lists")
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    shoppingLists.forEach { list ->
                        val dateStr = dateFormat.format(Date(list.shoppingDate))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    expanded = false
                                    onOpenList(list)
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = list.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Date: $dateStr • Budget: ${CurrencyUtils.formatZar(list.totalBudgetCost)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val isClosed = list.status == "CLOSED"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isClosed) IncomeColor.copy(alpha = 0.2f) else ExpenseColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = list.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isClosed) IncomeColor else ExpenseColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UnifiedGroceryTable(
    items: List<GroceryBudgetItemEntity>,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onEdit: (GroceryBudgetItemEntity) -> Unit,
    onDelete: (GroceryBudgetItemEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Table Column Headers: Select | Category | Item | Budget | Actual | Remain
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Select Column
                Box(
                    modifier = Modifier.width(30.dp),
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
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.85f)
                )

                // 3. Item Column
                Text(
                    text = "Item",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1.15f)
                )

                // 4. Budget Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Budget",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "Qty×Unit | Cost",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // 5. Actual Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Actual",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "Qty×Unit | Cost",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // 6. Remain Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Remain",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "Qty×Unit | Cost",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.End
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Table Rows
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                }
                UnifiedGroceryItemTableRow(
                    item = item,
                    isSelected = selectedIds.contains(item.id),
                    onToggleSelect = { onToggleSelect(item.id) },
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UnifiedGroceryItemTableRow(
    item: GroceryBudgetItemEntity,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showActionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else Color.Transparent
            )
            .combinedClickable(
                onClick = onToggleSelect,
                onDoubleClick = onEdit,
                onLongClick = { showActionDialog = true }
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Dedicated Select Column
        Box(
            modifier = Modifier.width(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.size(22.dp)
            )
        }

        // 2. Category Column
        Column(
            modifier = Modifier
                .weight(0.85f)
                .padding(end = 2.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.5.dp)
                )
            }
        }

        // 3. Item Column (Subcategory, Name & Unit Size, Recurrence badge, Note)
        Column(
            modifier = Modifier
                .weight(1.15f)
                .padding(end = 2.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // 1. Subcategory
            Text(
                text = item.subCategory,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
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
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. Recurrence tag (M/P/1x)
            val (recLabel, recColor) = when (item.isRecurring) {
                1 -> "M" to MaterialTheme.colorScheme.primary
                2 -> "P" to MaterialTheme.colorScheme.tertiary
                else -> "1x" to MaterialTheme.colorScheme.outline
            }
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = recColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = recLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                    color = recColor,
                    modifier = Modifier.padding(horizontal = 2.5.dp, vertical = 0.5.dp)
                )
            }

            // 4. Note (Optional)
            if (item.note.isNotBlank()) {
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 4. Budget Column (Qty × Unit Price -> Total Cost)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${item.quantityBudget} × ${CurrencyUtils.formatZar(item.unitPriceBudget)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1
            )
            Text(
                text = CurrencyUtils.formatZar(item.costBudget),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(2.dp))

        // 5. Actual Column (Qty × Unit Price -> Total Cost)
        val isOver = item.costActual > item.costBudget && item.costBudget > 0
        val actualColor = if (isOver) ExpenseColor else IncomeColor

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${item.quantityActual} × ${CurrencyUtils.formatZar(item.unitPriceActual)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1
            )
            Text(
                text = CurrencyUtils.formatZar(item.costActual),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                color = actualColor,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(2.dp))

        // 6. Remain Column (Qty × Unit Price -> Remaining Cost)
        val remainingQty = item.quantityBudget - item.quantityActual
        val remainingCost = item.costBudget - item.costActual
        val remainColor = when {
            remainingQty < 0 || remainingCost < 0 -> ExpenseColor
            remainingQty == 0 -> MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.secondary
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "$remainingQty × ${CurrencyUtils.formatZar(item.unitPriceBudget)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1
            )
            Text(
                text = CurrencyUtils.formatZar(remainingCost),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                color = remainColor,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
    }

    // Long Press Action Dialog
    if (showActionDialog) {
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = {
                Text(
                    text = item.itemDetail.ifBlank { item.subCategory },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Category: ${item.category} • ${item.subCategory} (${item.unitSize})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Budget: ${item.quantityBudget} × ${CurrencyUtils.formatZar(item.unitPriceBudget)} = ${CurrencyUtils.formatZar(item.costBudget)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Actual: ${item.quantityActual} × ${CurrencyUtils.formatZar(item.unitPriceActual)} = ${CurrencyUtils.formatZar(item.costActual)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showActionDialog = false
                        onEdit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Edit Item")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showActionDialog = false }) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            showActionDialog = false
                            showDeleteConfirmDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseColor)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Delete")
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Item?") },
            text = { Text("Are you sure you want to delete '${item.itemDetail.ifBlank { item.subCategory }}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseColor)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditBudgetItemDialog(
    item: GroceryBudgetItemEntity?,
    repository: TransactionRepository,
    viewModel: GroceriesViewModel,
    selectedPayMonthDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (cat: String, subCat: String, detail: String, unitSize: String, note: String, qtyB: Int, priceB: Double, isRec: Int, startTs: Long) -> Unit
) {
    val payDateDay = remember { com.moneytracker.util.SettingsManager.getPayDateDay() }
    val currentPayMonth = selectedPayMonthDate
    val candidateMonths = remember(currentPayMonth) {
        (-3..12).map { currentPayMonth.plusMonths(it.toLong()) }
    }
    var startMonthInput by remember {
        mutableStateOf(
            item?.let { com.moneytracker.util.DateUtils.toLocalDate(it.date) } ?: currentPayMonth
        )
    }
    var startMonthExpanded by remember { mutableStateOf(false) }

    val initialMeasureValue = remember(item) {
        if (item == null || item.unitSize.isBlank()) ""
        else {
            val match = Regex("""^([0-9.,]+)\s*(.*)$""").find(item.unitSize.trim())
            match?.groupValues?.get(1) ?: ""
        }
    }
    val initialUnitMeasure = remember(item) {
        if (item == null || item.unitSize.isBlank()) "g"
        else {
            val match = Regex("""^([0-9.,]+)\s*(.*)$""").find(item.unitSize.trim())
            if (match != null) {
                val rest = match.groupValues[2].trim()
                if (rest.isNotBlank()) rest else "g"
            } else item.unitSize.trim()
        }
    }

    // Dynamic categories, subcategories, and details management
    var customCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    var customSubCategories by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var customDetails by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    val allBudgetItems by viewModel.budgetItems.collectAsState()

    val allCategories = remember(allBudgetItems, customCategories) {
        (GROCERY_DEFAULT_CATEGORIES_MAP.keys + customCategories + allBudgetItems.map { it.category }).filter { it.isNotBlank() }.distinct()
    }

    val subCatFilterMap = remember(allBudgetItems, customSubCategories) {
        val map = mutableMapOf<String, MutableList<String>>()
        GROCERY_DEFAULT_CATEGORIES_MAP.forEach { (cat, subs) ->
            map.getOrPut(cat) { mutableListOf() }.addAll(subs)
        }
        allBudgetItems.forEach { item ->
            if (item.category.isNotBlank() && item.subCategory.isNotBlank()) {
                map.getOrPut(item.category) { mutableListOf() }.add(item.subCategory)
            }
        }
        customSubCategories.forEach { (cat, subs) ->
            map.getOrPut(cat) { mutableListOf() }.addAll(subs)
        }
        map.mapValues { it.value.distinct() }
    }

    val allSubCategories = remember(subCatFilterMap) {
        subCatFilterMap.values.flatten().distinct()
    }

    val detailFilterMap = remember(allBudgetItems, customDetails) {
        val map = mutableMapOf<String, MutableList<String>>()
        allBudgetItems.forEach { item ->
            if (item.subCategory.isNotBlank() && item.itemDetail.isNotBlank()) {
                map.getOrPut(item.subCategory) { mutableListOf() }.add(item.itemDetail)
            }
        }
        customDetails.forEach { (sub, dets) ->
            map.getOrPut(sub) { mutableListOf() }.addAll(dets)
        }
        map.mapValues { it.value.distinct() }
    }

    val allDetails = remember(detailFilterMap) {
        detailFilterMap.values.flatten().distinct()
    }

    // 3-Tier Cascading Orchestrator: Category -> SubCategory -> Detail
    val orchestrator = com.moneytracker.ui.components.rememberSimpleStringComboboxOrchestrator(
        categories = allCategories,
        subCategories = allSubCategories,
        details = allDetails,
        subCatFilterMap = subCatFilterMap,
        detailFilterMap = detailFilterMap,
        initialCategory = item?.category ?: "Starch",
        initialSubCategory = item?.subCategory ?: "Rice",
        initialDetail = item?.itemDetail ?: ""
    )

    var measureValueInput by remember { mutableStateOf(initialMeasureValue) }
    var unitMeasureInput by remember { mutableStateOf(initialUnitMeasure) }
    var noteInput by remember { mutableStateOf(item?.note ?: "") }
    var qtyBudgetInput by remember { mutableStateOf(item?.quantityBudget?.toString() ?: "1") }
    var unitPriceBudgetInput by remember { mutableStateOf(if (item != null) (if (item.unitPriceBudget > 0) item.unitPriceBudget.toString() else "0") else "0") }
    var isRecurringInput by remember { mutableStateOf(item?.isRecurring ?: 1) } // Default 1 = Monthly Recurring

    val unitSizes by viewModel.unitSizes.collectAsState()
    var editingUnitSize by remember { mutableStateOf<UnitSizeEntity?>(null) }
    var showUnitDialog by remember { mutableStateOf(false) }

    // CRUD Dialog States
    var showAddCatDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<String?>(null) }

    var showAddSubCatDialog by remember { mutableStateOf(false) }
    var editingSubCategory by remember { mutableStateOf<String?>(null) }

    var showAddDetailDialog by remember { mutableStateOf(false) }
    var editingDetail by remember { mutableStateOf<String?>(null) }

    var showDuplicateDialog by remember { mutableStateOf<GroceryBudgetItemEntity?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (item == null) "Add Grocery Budget Item" else "Edit Grocery Budget Item",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        if (item != null) {
                            IconButton(
                                onClick = {
                                    viewModel.deleteGroceryBudgetItem(item)
                                    onDismiss()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Item",
                                    tint = ExpenseColor
                                )
                            }
                        }
                        Button(
                            onClick = {
                                val duplicate = viewModel.findDuplicateItem(
                                    category = orchestrator.categoryText,
                                    subCategory = orchestrator.subCategoryText,
                                    itemDetail = orchestrator.detailText,
                                    excludeId = item?.id ?: 0L
                                )

                                if (duplicate != null) {
                                    showDuplicateDialog = duplicate
                                } else {
                                    val qty = qtyBudgetInput.toIntOrNull() ?: 1
                                    val price = unitPriceBudgetInput.toDoubleOrNull() ?: 0.0
                                    val computedUnitSize = if (measureValueInput.isBlank()) {
                                        unitMeasureInput.trim()
                                    } else {
                                        "${measureValueInput.trim()} ${unitMeasureInput.trim()}".trim()
                                    }
                                    val startTs = com.moneytracker.util.DateUtils.startOfPayMonth(startMonthInput, payDateDay)
                                    onSave(
                                        orchestrator.categoryText,
                                        orchestrator.subCategoryText,
                                        orchestrator.detailText,
                                        computedUnitSize,
                                        noteInput,
                                        qty,
                                        price,
                                        isRecurringInput,
                                        startTs
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Save")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Start Month Dropdown
                ExposedDropdownMenuBox(
                    expanded = startMonthExpanded,
                    onExpandedChange = { startMonthExpanded = it }
                ) {
                    OutlinedTextField(
                        value = com.moneytracker.util.DateUtils.formatPayMonth(startMonthInput, payDateDay),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Month") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = startMonthExpanded,
                        onDismissRequest = { startMonthExpanded = false }
                    ) {
                        candidateMonths.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(com.moneytracker.util.DateUtils.formatPayMonth(m, payDateDay)) },
                                onClick = {
                                    startMonthInput = m
                                    startMonthExpanded = false
                                }
                            )
                        }
                    }
                }

                // 1. Category (NeltzSocial_Combo_PilledFilteredCruded - Root)
                com.moneytracker.ui.components.NeltzSocial_Combo_PilledFilteredCruded(
                    label = "Category",
                    selectedValue = orchestrator.categoryText,
                    onValueChange = { catName ->
                        orchestrator.categoryText = catName
                    },
                    items = orchestrator.categories,
                    itemToText = { it },
                    onAddItem = { editingCategory = null; showAddCatDialog = true },
                    onEditItem = { cat -> editingCategory = cat; showAddCatDialog = true },
                    onDeleteItem = { cat ->
                        customCategories = customCategories - cat
                        if (orchestrator.categoryText.equals(cat, ignoreCase = true)) {
                            orchestrator.categoryText = ""
                        }
                    }
                )

                // 2. SubCategory (NeltzSocial_Combo_PilledFilteredCruded - Child of Category)
                com.moneytracker.ui.components.NeltzSocial_Combo_PilledFilteredCruded(
                    label = "Sub-Category",
                    selectedValue = orchestrator.subCategoryText,
                    onValueChange = { subName -> orchestrator.subCategoryText = subName },
                    items = orchestrator.subCategories,
                    parentFilterKey = orchestrator.categoryText,
                    filterPredicate = { orchestrator.isSubCategoryValid(it) },
                    itemToText = { it },
                    onAddItem = { editingSubCategory = null; showAddSubCatDialog = true },
                    onEditItem = { sub -> editingSubCategory = sub; showAddSubCatDialog = true },
                    onDeleteItem = { sub ->
                        val cat = orchestrator.categoryText
                        customSubCategories = customSubCategories + (cat to ((customSubCategories[cat] ?: emptyList()) - sub))
                        if (orchestrator.subCategoryText.equals(sub, ignoreCase = true)) {
                            orchestrator.subCategoryText = ""
                        }
                    }
                )

                // 3. Detail / Item Name (NeltzSocial_Combo_PilledFilteredCruded - Grandchild of SubCategory)
                com.moneytracker.ui.components.NeltzSocial_Combo_PilledFilteredCruded(
                    label = "Detail / Item Name",
                    selectedValue = orchestrator.detailText,
                    onValueChange = { detName -> orchestrator.detailText = detName },
                    items = orchestrator.details,
                    parentFilterKey = Pair(orchestrator.categoryText, orchestrator.subCategoryText),
                    filterPredicate = { orchestrator.isDetailValid(it) },
                    itemToText = { it },
                    onAddItem = { editingDetail = null; showAddDetailDialog = true },
                    onEditItem = { det -> editingDetail = det; showAddDetailDialog = true },
                    onDeleteItem = { det ->
                        val sub = orchestrator.subCategoryText
                        customDetails = customDetails + (sub to ((customDetails[sub] ?: emptyList()) - det))
                        if (orchestrator.detailText.equals(det, ignoreCase = true)) {
                            orchestrator.detailText = ""
                        }
                    }
                )

                // 4. Measure Value (Numeric)
                OutlinedTextField(
                    value = measureValueInput,
                    onValueChange = { measureValueInput = it },
                    label = { Text("Measure (e.g. 500, 2, 1)") },
                    placeholder = { Text("Numeric measure size") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 5. Unit (NeltzSocial_Combo_PilledFilteredCruded - Standalone without parent)
                com.moneytracker.ui.components.NeltzSocial_Combo_PilledFilteredCruded(
                    label = "Unit",
                    selectedValue = unitMeasureInput,
                    onValueChange = { unitMeasureInput = it },
                    items = unitSizes,
                    itemToText = { it.name },
                    onAddItem = { editingUnitSize = null; showUnitDialog = true },
                    onEditItem = { unit -> editingUnitSize = unit; showUnitDialog = true },
                    onDeleteItem = { unit -> viewModel.deleteUnitSize(unit) }
                )

                // Note Field (Optional)
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Note (Optional, e.g., brand, store)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Budget Quantity & Unit Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = qtyBudgetInput,
                        onValueChange = { qtyBudgetInput = it },
                        label = { Text("Qty (Budget)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitPriceBudgetInput,
                        onValueChange = { unitPriceBudgetInput = it },
                        label = { Text("Price (R)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Recurrence Rule Selector
                Text(
                    text = "Recurrence Mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = isRecurringInput == 0,
                        onClick = { isRecurringInput = 0 },
                        label = { Text("Once-off") }
                    )
                    FilterChip(
                        selected = isRecurringInput == 1,
                        onClick = { isRecurringInput = 1 },
                        label = { Text("Monthly") }
                    )
                    FilterChip(
                        selected = isRecurringInput == 2,
                        onClick = { isRecurringInput = 2 },
                        label = { Text("Planned") }
                    )
                }

                if (item != null && item.updatedAt > 0L) {
                    var showDebugInfo by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                        Text(
                            text = if (showDebugInfo) "System Info ▲" else "System Info ▼",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable { showDebugInfo = !showDebugInfo }
                                .padding(vertical = 2.dp)
                        )
                        if (showDebugInfo) {
                            val formattedTime = remember(item.updatedAt) {
                                java.time.Instant.ofEpochMilli(item.updatedAt)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            }
                            Text(
                                text = "Last updated: $formattedTime",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Bottom Action buttons
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val duplicate = viewModel.findDuplicateItem(
                                category = orchestrator.categoryText,
                                subCategory = orchestrator.subCategoryText,
                                itemDetail = orchestrator.detailText,
                                excludeId = item?.id ?: 0L
                            )

                            if (duplicate != null) {
                                showDuplicateDialog = duplicate
                            } else {
                                val qty = qtyBudgetInput.toIntOrNull() ?: 1
                                val price = unitPriceBudgetInput.toDoubleOrNull() ?: 0.0
                                val computedUnitSize = if (measureValueInput.isBlank()) {
                                    unitMeasureInput.trim()
                                } else {
                                    "${measureValueInput.trim()} ${unitMeasureInput.trim()}".trim()
                                }
                                val startTs = com.moneytracker.util.DateUtils.startOfPayMonth(startMonthInput, payDateDay)
                                onSave(
                                    orchestrator.categoryText,
                                    orchestrator.subCategoryText,
                                    orchestrator.detailText,
                                    computedUnitSize,
                                    noteInput,
                                    qty,
                                    price,
                                    isRecurringInput,
                                    startTs
                                )
                            }
                        },
                        modifier = Modifier.weight(if (item != null) 1.5f else 1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (item == null) "Add Item to Budget" else "Save Changes")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Duplicate Capture Confirmation Dialog
    if (showDuplicateDialog != null) {
        val duplicate = showDuplicateDialog!!
        var duplicateNoteError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showDuplicateDialog = null
                duplicateNoteError = false
            },
            title = { Text("Duplicate Item Detected") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val catDisplay = orchestrator.categoryText.ifBlank { "Groceries" }
                    val subDisplay = orchestrator.subCategoryText.ifBlank { "General" }
                    val detDisplay = orchestrator.detailText.ifBlank { subDisplay }
                    Text(
                        text = "An item for '$catDisplay > $subDisplay > $detDisplay' already exists in this month's budget.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Are you updating the existing item's amount or recurrence, or creating a new separate entry?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (duplicateNoteError) {
                        Text(
                            text = "⚠️ To create a new entry with the same item name, please enter a Note to differentiate it (e.g., brand, flavor, or store).",
                            style = MaterialTheme.typography.labelSmall,
                            color = ExpenseColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                // "Yes, Update Existing"
                Button(
                    onClick = {
                        val qty = qtyBudgetInput.toIntOrNull() ?: 1
                        val price = unitPriceBudgetInput.toDoubleOrNull() ?: 0.0
                        val computedUnitSize = if (measureValueInput.isBlank()) {
                            unitMeasureInput.trim()
                        } else {
                            "${measureValueInput.trim()} ${unitMeasureInput.trim()}".trim()
                        }
                        val startTs = com.moneytracker.util.DateUtils.startOfPayMonth(startMonthInput, payDateDay)
                        viewModel.saveGroceryBudgetItem(
                            id = duplicate.id,
                            category = orchestrator.categoryText,
                            subCategory = orchestrator.subCategoryText,
                            itemDetail = orchestrator.detailText,
                            unitSize = computedUnitSize,
                            note = noteInput,
                            quantityBudget = qty,
                            unitPriceBudget = price,
                            isRecurring = isRecurringInput,
                            startDate = startTs,
                            isUpdatingExisting = true
                        )
                        showDuplicateDialog = null
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Yes, Update Existing")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // "No, Create New Entry"
                    TextButton(
                        onClick = {
                            if (noteInput.isBlank()) {
                                duplicateNoteError = true
                            } else {
                                val qty = qtyBudgetInput.toIntOrNull() ?: 1
                                val price = unitPriceBudgetInput.toDoubleOrNull() ?: 0.0
                                val computedUnitSize = if (measureValueInput.isBlank()) {
                                  unitMeasureInput.trim()
                                } else {
                                  "${measureValueInput.trim()} ${unitMeasureInput.trim()}".trim()
                                }
                                val startTs = com.moneytracker.util.DateUtils.startOfPayMonth(startMonthInput, payDateDay)
                                viewModel.saveGroceryBudgetItem(
                                    id = 0L,
                                    category = orchestrator.categoryText,
                                    subCategory = orchestrator.subCategoryText,
                                    itemDetail = orchestrator.detailText,
                                    unitSize = computedUnitSize,
                                    note = noteInput,
                                    quantityBudget = qty,
                                    unitPriceBudget = price,
                                    isRecurring = isRecurringInput,
                                    startDate = startTs,
                                    isUpdatingExisting = false
                                )
                                showDuplicateDialog = null
                                onDismiss()
                            }
                        }
                    ) {
                        Text("No, Create New Entry")
                    }

                    TextButton(
                        onClick = {
                            showDuplicateDialog = null
                            duplicateNoteError = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Category Add/Edit Dialog
    if (showAddCatDialog) {
        var catNameInput by remember(editingCategory) { mutableStateOf(editingCategory ?: "") }
        AlertDialog(
            onDismissRequest = { showAddCatDialog = false },
            title = { Text(if (editingCategory == null) "Add Category" else "Edit Category") },
            text = {
                OutlinedTextField(
                    value = catNameInput,
                    onValueChange = { catNameInput = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = catNameInput.trim()
                    if (trimmed.isNotBlank()) {
                        if (editingCategory != null && editingCategory != trimmed) {
                            customCategories = customCategories.filter { it != editingCategory } + trimmed
                            if (orchestrator.categoryText == editingCategory) {
                                orchestrator.categoryText = trimmed
                            }
                        } else {
                            customCategories = (customCategories + trimmed).distinct()
                            orchestrator.categoryText = trimmed
                        }
                        showAddCatDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // SubCategory Add/Edit Dialog
    if (showAddSubCatDialog) {
        var subCatNameInput by remember(editingSubCategory) { mutableStateOf(editingSubCategory ?: "") }
        val currentCat = orchestrator.categoryText.ifBlank { "General" }
        AlertDialog(
            onDismissRequest = { showAddSubCatDialog = false },
            title = { Text(if (editingSubCategory == null) "Add Sub-Category under '$currentCat'" else "Edit Sub-Category") },
            text = {
                OutlinedTextField(
                    value = subCatNameInput,
                    onValueChange = { subCatNameInput = it },
                    label = { Text("Sub-Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = subCatNameInput.trim()
                    if (trimmed.isNotBlank()) {
                        val existingList = customSubCategories[currentCat] ?: emptyList()
                        if (editingSubCategory != null && editingSubCategory != trimmed) {
                            customSubCategories = customSubCategories + (currentCat to (existingList.filter { it != editingSubCategory } + trimmed).distinct())
                            if (orchestrator.subCategoryText == editingSubCategory) {
                                orchestrator.subCategoryText = trimmed
                            }
                        } else {
                            customSubCategories = customSubCategories + (currentCat to (existingList + trimmed).distinct())
                            orchestrator.subCategoryText = trimmed
                        }
                        showAddSubCatDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubCatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Detail Add/Edit Dialog
    if (showAddDetailDialog) {
        var detNameInput by remember(editingDetail) { mutableStateOf(editingDetail ?: "") }
        val currentSub = orchestrator.subCategoryText.ifBlank { "General" }
        AlertDialog(
            onDismissRequest = { showAddDetailDialog = false },
            title = { Text(if (editingDetail == null) "Add Detail Item under '$currentSub'" else "Edit Detail Item") },
            text = {
                OutlinedTextField(
                    value = detNameInput,
                    onValueChange = { detNameInput = it },
                    label = { Text("Item Name / Detail") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = detNameInput.trim()
                    if (trimmed.isNotBlank()) {
                        val existingList = customDetails[currentSub] ?: emptyList()
                        if (editingDetail != null && editingDetail != trimmed) {
                            customDetails = customDetails + (currentSub to (existingList.filter { it != editingDetail } + trimmed).distinct())
                            if (orchestrator.detailText == editingDetail) {
                                orchestrator.detailText = trimmed
                            }
                        } else {
                            customDetails = customDetails + (currentSub to (existingList + trimmed).distinct())
                            orchestrator.detailText = trimmed
                        }
                        showAddDetailDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDetailDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add / Edit Unit Dialog
    if (showUnitDialog) {
        var unitNameInput by remember(editingUnitSize) { mutableStateOf(editingUnitSize?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showUnitDialog = false },
            title = { Text(if (editingUnitSize == null) "Add Unit" else "Edit Unit") },
            text = {
                OutlinedTextField(
                    value = unitNameInput,
                    onValueChange = { unitNameInput = it },
                    label = { Text("Unit Name (e.g. kg, L, Pack)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (unitNameInput.isNotBlank()) {
                        val trimmed = unitNameInput.trim()
                        if (editingUnitSize != null) {
                            viewModel.deleteUnitSize(editingUnitSize!!)
                        }
                        viewModel.addUnitSize(trimmed)
                        unitMeasureInput = trimmed
                        showUnitDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
