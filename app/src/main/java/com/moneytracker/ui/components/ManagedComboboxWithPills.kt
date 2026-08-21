package com.moneytracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.data.local.ComponentStorageManager
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

data class ComboboxSettings(
    val pillRows: Int = 1,              // Default 1 row
    val maxVisibleItems: Int = 5,       // Default 5 items visible in dropdown
    val scrollStep: Int = 3,            // Default 3 items per scroll click
    val isAlphabeticalSort: Boolean = false
)

/**
 * Autonomous, Self-Persisting Managed Combobox with Quick-Pick Pills.
 * Automatically saves and loads from app-internal JSON storage using coordinates:
 * { screenId: { componentId: { label: { filterKey: [ items... ] } } } }
 */
@Composable
fun SelfStoringManagedCombobox(
    screenId: String,
    componentId: String,
    label: String,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    filterKey: String? = null,
    defaultItems: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageManager = remember { ComponentStorageManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var itemsList by remember(screenId, componentId, label, filterKey) {
        mutableStateOf<List<String>>(emptyList())
    }
    var initialSettings by remember(screenId, componentId) {
        mutableStateOf(ComboboxSettings())
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<String?>(null) }

    // Auto-load values and settings from internal files
    LaunchedEffect(screenId, componentId, label, filterKey) {
        itemsList = storageManager.getValues(screenId, componentId, label, filterKey, defaultItems)
        initialSettings = storageManager.getSettings(screenId, componentId)
    }

    ManagedComboboxWithPills(
        label = label,
        selectedValue = selectedValue,
        onValueChange = onValueChange,
        items = itemsList,
        initialSettings = initialSettings,
        onSettingsChange = { newSettings ->
            scope.launch {
                storageManager.saveSettings(screenId, componentId, newSettings)
            }
        },
        itemToText = { it },
        onAddItem = { showAddDialog = true },
        onEditItem = { editingItem = it },
        onDeleteItem = { itemToDelete ->
            scope.launch {
                itemsList = storageManager.deleteItem(screenId, componentId, label, filterKey, itemToDelete)
                if (selectedValue.equals(itemToDelete, ignoreCase = true)) {
                    onValueChange("")
                }
            }
        },
        modifier = modifier
    )

    // Add Item Dialog
    if (showAddDialog) {
        var newItemText by remember { mutableStateOf("") }
        val keyDisplay = if (!filterKey.isNullOrBlank()) " under '$filterKey'" else ""

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add $label$keyDisplay") },
            text = {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    label = { Text("$label Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newItemText.trim()
                        if (trimmed.isNotBlank()) {
                            scope.launch {
                                itemsList = storageManager.addItem(screenId, componentId, label, filterKey, trimmed)
                                onValueChange(trimmed)
                            }
                        }
                        showAddDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Item Dialog
    if (editingItem != null) {
        val original = editingItem!!
        var editedText by remember(original) { mutableStateOf(original) }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Edit $label") },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    label = { Text("$label Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editedText.trim()
                        if (trimmed.isNotBlank()) {
                            scope.launch {
                                itemsList = storageManager.editItem(screenId, componentId, label, filterKey, original, trimmed)
                                if (selectedValue.equals(original, ignoreCase = true)) {
                                    onValueChange(trimmed)
                                }
                            }
                        }
                        editingItem = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Standard Reusable Combobox with Up/Down buttons, Search, Quick-Pick Pills, and Gear Settings.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <T> ManagedComboboxWithPills(
    label: String,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    items: List<T>,
    filterPredicate: ((T) -> Boolean)? = null,
    initialSettings: ComboboxSettings = ComboboxSettings(),
    onSettingsChange: ((ComboboxSettings) -> Unit)? = null,
    itemToText: (T) -> String = { it.toString() },
    onAddItem: (() -> Unit)? = null,
    onEditItem: ((T) -> Unit)? = null,
    onDeleteItem: ((T) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var settings by remember(initialSettings) { mutableStateOf(initialSettings) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 1. Parent Filtering
    val parentFilteredItems = remember(items, filterPredicate) {
        if (filterPredicate != null) items.filter(filterPredicate) else items
    }

    // 2. Search / Live-Filtering as user types + Sort Order
    val displayItems = remember(parentFilteredItems, selectedValue, settings.isAlphabeticalSort) {
        val searchFiltered = if (selectedValue.isNotBlank()) {
            val query = selectedValue.trim()
            val matches = parentFilteredItems.filter { itemToText(it).contains(query, ignoreCase = true) }
            if (matches.isNotEmpty()) matches else parentFilteredItems
        } else {
            parentFilteredItems
        }

        if (settings.isAlphabeticalSort) {
            searchFiltered.sortedBy { itemToText(it).lowercase() }
        } else {
            searchFiltered
        }
    }

    // 3. Adaptive Dropdown Height based on `settings.maxVisibleItems`
    val maxDropdownHeight = remember(settings.maxVisibleItems) {
        (settings.maxVisibleItems * 48 + 76).dp
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // --- ROW 1: INPUT FIELD + ADD BUTTON (+) + GEAR SETTINGS (⚙) ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedValue,
                    onValueChange = {
                        onValueChange(it)
                        expanded = true
                    },
                    label = { Text(label) },
                    placeholder = { Text("Select a: $label item") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand $label"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true
                )

                // Dropdown Menu with Up/Down buttons and adaptive scrolling
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .heightIn(max = maxDropdownHeight)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // ▲ Up Scroll Button (at the START of the list)
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    val target = max(0, listState.firstVisibleItemIndex - settings.scrollStep)
                                    listState.animateScrollToItem(target)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Scroll Up",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Scroll Up (${settings.scrollStep})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Item 0: Default First Item: "Select a: $label item"
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Select a: $label item",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                onValueChange("")
                                expanded = false
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Scrollable List of Items
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        ) {
                            items(displayItems) { item ->
                                val itemText = itemToText(item)
                                val isSelected = selectedValue.equals(itemText, ignoreCase = true)

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = itemText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        onValueChange(itemText)
                                        expanded = false
                                    },
                                    trailingIcon = {
                                        if (onEditItem != null || onDeleteItem != null) {
                                            Row {
                                                if (onEditItem != null) {
                                                    IconButton(
                                                        onClick = { onEditItem(item) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit",
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                if (onDeleteItem != null) {
                                                    IconButton(
                                                        onClick = { onDeleteItem(item) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            modifier = Modifier.size(16.dp),
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // ▼ Down Scroll Button (at the END of the list)
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    val target = min(
                                        displayItems.size - 1,
                                        listState.firstVisibleItemIndex + settings.scrollStep
                                    )
                                    if (target >= 0) listState.animateScrollToItem(target)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Scroll Down",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Scroll Down (${settings.scrollStep})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Add Item Button (+)
            if (onAddItem != null) {
                IconButton(
                    onClick = onAddItem,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add $label",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Gear Settings Button (⚙)
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier.padding(start = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configure $label Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- ROW 2: QUICK-PICK PILLS ---
        if (displayItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))

            if (settings.pillRows <= 1) {
                // 1 Row (Default): Horizontal Scrollable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    displayItems.forEach { item ->
                        val itemText = itemToText(item)
                        FilterChip(
                            selected = selectedValue.equals(itemText, ignoreCase = true),
                            onClick = { onValueChange(itemText) },
                            label = { Text(itemText, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            } else {
                // Multi-Row Mode: FlowRow Wrap
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = Int.MAX_VALUE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    displayItems.forEach { item ->
                        val itemText = itemToText(item)
                        FilterChip(
                            selected = selectedValue.equals(itemText, ignoreCase = true),
                            onClick = { onValueChange(itemText) },
                            label = { Text(itemText, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }

    // --- GEAR SETTINGS MODAL ---
    if (showSettingsDialog) {
        var tempPillRows by remember { mutableIntStateOf(settings.pillRows) }
        var tempVisibleCount by remember { mutableIntStateOf(settings.maxVisibleItems) }
        var tempScrollStep by remember { mutableIntStateOf(settings.scrollStep) }
        var tempSortAlpha by remember { mutableStateOf(settings.isAlphabeticalSort) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("$label Settings", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Config 1: Quick-Pick Pill Rows Stepper
                    NumberStepperConfigRow(
                        title = "Pills Display Rows",
                        subtitle = if (tempPillRows == 1) "1 Row (Horizontal Scroll)" else "$tempPillRows Rows (Multi-line Wrap)",
                        value = tempPillRows,
                        min = 1,
                        max = 5,
                        onValueChange = { tempPillRows = it }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Config 2: Number of Visible List Items Stepper
                    NumberStepperConfigRow(
                        title = "Visible Dropdown Items",
                        subtitle = "$tempVisibleCount items in view before scrolling",
                        value = tempVisibleCount,
                        min = 3,
                        max = 15,
                        onValueChange = { tempVisibleCount = it }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Config 3: Scroll Step Amount Stepper (Default = 3)
                    NumberStepperConfigRow(
                        title = "Scroll Step Amount",
                        subtitle = "$tempScrollStep items per ▲ / ▼ button tap",
                        value = tempScrollStep,
                        min = 1,
                        max = 10,
                        onValueChange = { tempScrollStep = it }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Config 4: Sort Order (A-Z Alphabetical)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sort A-Z Alphabetically",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (tempSortAlpha) "Alphabetical order" else "Default / Custom order",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = tempSortAlpha,
                            onCheckedChange = { tempSortAlpha = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newSettings = ComboboxSettings(
                            pillRows = tempPillRows,
                            maxVisibleItems = tempVisibleCount,
                            scrollStep = tempScrollStep,
                            isAlphabeticalSort = tempSortAlpha
                        )
                        settings = newSettings
                        onSettingsChange?.invoke(newSettings)
                        showSettingsDialog = false
                    }
                ) {
                    Text("Apply Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Reusable Number Stepper Row with [-] and [+] buttons and bounds checking
 */
@Composable
private fun NumberStepperConfigRow(
    title: String,
    subtitle: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Stepper Control: [-] [ Number ] [+]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (value > min) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(32.dp),
                onClick = { if (value > min) onValueChange(value - 1) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrement",
                        modifier = Modifier.size(16.dp),
                        tint = if (value > min) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .width(36.dp)
                    .height(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$value",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = if (value < max) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(32.dp),
                onClick = { if (value < max) onValueChange(value + 1) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increment",
                        modifier = Modifier.size(16.dp),
                        tint = if (value < max) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
