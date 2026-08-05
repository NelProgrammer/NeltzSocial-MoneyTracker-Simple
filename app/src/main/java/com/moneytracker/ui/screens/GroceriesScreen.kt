package com.moneytracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneytracker.data.local.entity.GroceryBudgetItemEntity
import com.moneytracker.data.local.entity.ShoppingListEntity
import com.moneytracker.data.local.entity.UnitSizeEntity
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.ui.components.AppTopBar
import com.moneytracker.ui.components.ShoppingListPopupDialog
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.viewmodel.CategoriesViewModel
import com.moneytracker.ui.viewmodel.GroceriesViewModel
import com.moneytracker.ui.viewmodel.SubCategoriesViewModel
import com.moneytracker.ui.viewmodel.ViewModelFactory
import com.moneytracker.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceriesScreen(
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
            AppTopBar(screenTitle = "Monthly Grocery Budget")
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

            // Month Selector Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.setPayMonth(selectedPayMonthDate.minusMonths(1)) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }
                    Text(
                        text = formattedMonth,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.setPayMonth(selectedPayMonthDate.plusMonths(1)) }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }
            }

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
                val grouped = remember(budgetItems) { budgetItems.groupBy { it.category } }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    grouped.forEach { (category, items) ->
                        item {
                            Text(
                                text = category.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(items, key = { it.id }) { item ->
                            BudgetItemRow(
                                item = item,
                                isSelected = selectedIds.contains(item.id),
                                onToggleSelect = { viewModel.toggleBudgetItemSelection(item.id) },
                                onEdit = {
                                    editingItem = item
                                    showAddEditDialog = true
                                },
                                onDelete = { viewModel.deleteGroceryBudgetItem(item) }
                            )
                        }
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
        ShoppingListPopupDialog(
            shoppingList = activeShoppingList!!,
            items = activeShoppingListItems,
            onDismiss = { viewModel.openShoppingList(null) },
            onToggleItemChecked = { viewModel.toggleShoppingListItemChecked(it) },
            onUpdateActuals = { item, qty, price -> viewModel.updateShoppingListItemActuals(item, qty, price) },
            onConfirmAndClose = { createTxn -> viewModel.confirmAndCloseActiveShoppingList(createTxn) }
        )
    }

    // Generate Shopping List Title Dialog
    if (showGenerateListDialog) {
        var titleInput by remember { mutableStateOf("Shopping Trip (${selectedIds.size} items)") }
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
            onDismiss = { showAddEditDialog = false },
            onSave = { cat, subCat, detail, unitSize, note, qtyB, priceB, isRec ->
                viewModel.saveGroceryBudgetItem(
                    id = editingItem?.id ?: 0L,
                    category = cat,
                    subCategory = subCat,
                    itemDetail = detail,
                    unitSize = unitSize,
                    note = note,
                    quantityBudget = qtyB,
                    unitPriceBudget = priceB,
                    isRecurring = isRec
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

@Composable
private fun BudgetItemRow(
    item: GroceryBudgetItemEntity,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val dateStr = remember(item.date) { dateFormat.format(Date(item.date)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.itemDetail.ifBlank { item.subCategory },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // Recurrence Badge
                    val (recLabel, recColor) = when (item.isRecurring) {
                        1 -> "Monthly" to MaterialTheme.colorScheme.primary
                        2 -> "Planned" to MaterialTheme.colorScheme.tertiary
                        else -> "Once-off" to MaterialTheme.colorScheme.outline
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = recColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = recLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = recColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "${item.category} • ${item.subCategory} (${item.unitSize}) • Date: $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (item.note.isNotBlank()) {
                    Text(
                        text = "Note: ${item.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Budget vs Actual Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Budget: ${item.quantityBudget} x ${CurrencyUtils.formatZar(item.unitPriceBudget)} = ${CurrencyUtils.formatZar(item.costBudget)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Actual: ${item.quantityActual} x ${CurrencyUtils.formatZar(item.unitPriceActual)} = ${CurrencyUtils.formatZar(item.costActual)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.costActual > item.costBudget && item.costBudget > 0) ExpenseColor else IncomeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseColor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditBudgetItemDialog(
    item: GroceryBudgetItemEntity?,
    repository: TransactionRepository,
    viewModel: GroceriesViewModel,
    onDismiss: () -> Unit,
    onSave: (cat: String, subCat: String, detail: String, unitSize: String, note: String, qtyB: Int, priceB: Double, isRec: Int) -> Unit
) {
    var categoryInput by remember { mutableStateOf(item?.category ?: "Starch") }
    var subCategoryInput by remember { mutableStateOf(item?.subCategory ?: "Rice") }
    var itemDetailInput by remember { mutableStateOf(item?.itemDetail ?: "") }
    var unitSizeInput by remember { mutableStateOf(item?.unitSize ?: "pack") }
    var noteInput by remember { mutableStateOf(item?.note ?: "") }
    var qtyBudgetInput by remember { mutableStateOf(item?.quantityBudget?.toString() ?: "1") }
    var unitPriceBudgetInput by remember { mutableStateOf(if ((item?.unitPriceBudget ?: 0.0) > 0) item!!.unitPriceBudget.toString() else "") }
    var isRecurringInput by remember { mutableStateOf(item?.isRecurring ?: 0) } // 0 = Once-off, 1 = Monthly Recurring, 2 = Planned

    val unitSizes by viewModel.unitSizes.collectAsState()
    var showUnitSizeCrudDialog by remember { mutableStateOf(false) }

    // Editable Dropdown Expanded States
    var catExpanded by remember { mutableStateOf(false) }
    var subCatExpanded by remember { mutableStateOf(false) }
    var detailExpanded by remember { mutableStateOf(false) }
    var unitSizeExpanded by remember { mutableStateOf(false) }

    val categoriesViewModel: CategoriesViewModel = viewModel(factory = ViewModelFactory(repository))
    val categories by categoriesViewModel.categories.collectAsState()

    val defaultCategories = remember { listOf("Starch", "Beverages", "Meat", "Cold Meats", "Dairy", "Cleaning", "Toiletries", "Snacks") }
    val availableCategoryNames = remember(categories) {
        val names = categories.map { it.name }.toSet()
        (defaultCategories + names).distinct()
    }

    val subCategoriesViewModel: SubCategoriesViewModel = viewModel(factory = ViewModelFactory(repository))
    val dbSubCategories by subCategoriesViewModel.subCategories.collectAsState()
    val availableSubCatNames = remember(dbSubCategories) {
        val defaultSubs = listOf("Maize Meal", "Rice", "Bread", "Fizzy Drink", "Milk", "Beef", "Chicken", "Pork", "Sausage", "Cheese", "Butter")
        (defaultSubs + dbSubCategories.map { it.name }).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add Grocery Budget Item" else "Edit Grocery Budget Item") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Editable Dropdown
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it }
                ) {
                    OutlinedTextField(
                        value = categoryInput,
                        onValueChange = { categoryInput = it },
                        label = { Text("Category (e.g., Starch, Beverages, Meat)") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        availableCategoryNames.forEach { catName ->
                            DropdownMenuItem(
                                text = { Text(catName) },
                                onClick = {
                                    categoryInput = catName
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                // SubCategory Editable Dropdown
                ExposedDropdownMenuBox(
                    expanded = subCatExpanded,
                    onExpandedChange = { subCatExpanded = it }
                ) {
                    OutlinedTextField(
                        value = subCategoryInput,
                        onValueChange = { subCategoryInput = it },
                        label = { Text("Sub-Category (e.g., Maize Meal, Rice, Fizzy Drink)") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = subCatExpanded,
                        onDismissRequest = { subCatExpanded = false }
                    ) {
                        availableSubCatNames.forEach { subName ->
                            DropdownMenuItem(
                                text = { Text(subName) },
                                onClick = {
                                    subCategoryInput = subName
                                    subCatExpanded = false
                                }
                            )
                        }
                    }
                }

                // Item Detail Editable Dropdown
                OutlinedTextField(
                    value = itemDetailInput,
                    onValueChange = { itemDetailInput = it },
                    label = { Text("Item Detail (e.g., 2.5kg White Star, Coca-Cola 2L)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Unit Size Editable Dropdown with CRUD Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = unitSizeExpanded,
                        onExpandedChange = { unitSizeExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unitSizeInput,
                            onValueChange = { unitSizeInput = it },
                            label = { Text("Unit Size (e.g., 2.5kg, 2L, 500g, pack)") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true
                        )
                        DropdownMenu(
                            expanded = unitSizeExpanded,
                            onDismissRequest = { unitSizeExpanded = false }
                        ) {
                            unitSizes.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u.name) },
                                    onClick = {
                                        unitSizeInput = u.name
                                        unitSizeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(onClick = { showUnitSizeCrudDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Manage Unit Sizes")
                    }
                }

                // Note Field
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Budget Quantity & Unit Price Plain Numeric Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = qtyBudgetInput,
                        onValueChange = { qtyBudgetInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Budget Qty") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    // Plain numeric input field without 'R' inside field
                    OutlinedTextField(
                        value = unitPriceBudgetInput,
                        onValueChange = { unitPriceBudgetInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Budget Unit Price") },
                        prefix = { Text("R ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.3f)
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyBudgetInput.toIntOrNull() ?: 1
                    val price = unitPriceBudgetInput.toDoubleOrNull() ?: 0.0
                    onSave(
                        categoryInput,
                        subCategoryInput,
                        itemDetailInput,
                        unitSizeInput,
                        noteInput,
                        qty,
                        price,
                        isRecurringInput
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Unit Size CRUD Management Sub-Dialog
    if (showUnitSizeCrudDialog) {
        var newUnitInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showUnitSizeCrudDialog = false },
            title = { Text("Manage Unit Sizes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newUnitInput,
                            onValueChange = { newUnitInput = it },
                            label = { Text("New Unit Size (e.g. 5kg)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (newUnitInput.isNotBlank()) {
                                viewModel.addUnitSize(newUnitInput)
                                newUnitInput = ""
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                        }
                    }

                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        items(unitSizes) { u ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(u.name, style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { viewModel.deleteUnitSize(u) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Unit", tint = ExpenseColor)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUnitSizeCrudDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
