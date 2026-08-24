package com.moneytracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
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
    parentFilterKey: Any? = null,
    autoResetOnParentChange: Boolean = true,
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
    val scrollState = rememberScrollState()

    // 1. Parent Filtering (Encapsulated in Component)
    val parentFilteredItems = remember(items, filterPredicate, parentFilterKey) {
        if (filterPredicate != null) items.filter(filterPredicate) else items
    }

    // Auto-Reset inside Component ONLY when parent filter key ACTUALLY changes & current selection is invalid
    if (autoResetOnParentChange) {
        var hasInitialized by remember { mutableStateOf(false) }
        var lastKnownParentKey by remember { mutableStateOf<Any?>(null) }

        LaunchedEffect(parentFilterKey, parentFilteredItems) {
            if (!hasInitialized) {
                if (items.isNotEmpty() || parentFilterKey != null) {
                    hasInitialized = true
                    lastKnownParentKey = parentFilterKey
                }
                return@LaunchedEffect
            }

            // Only trigger reset when parent filter key has actually changed
            if (lastKnownParentKey != parentFilterKey) {
                lastKnownParentKey = parentFilterKey
                if (items.isNotEmpty() && selectedValue.isNotEmpty() &&
                    parentFilteredItems.none { itemToText(it).equals(selectedValue.trim(), ignoreCase = true) }) {
                    onValueChange("")
                }
            }
        }
    }

    // 2. All Available Pill Items (respects alphabetical sort)
    val allPillItems = remember(parentFilteredItems, settings.isAlphabeticalSort) {
        if (settings.isAlphabeticalSort) {
            parentFilteredItems.sortedBy { itemToText(it).lowercase() }
        } else {
            parentFilteredItems
        }
    }

    // 3. Search / Live-Filtering as user types in dropdown + Sort Order
    val displayItems = remember(allPillItems, selectedValue) {
        val isExactMatch = allPillItems.any { itemToText(it).equals(selectedValue.trim(), ignoreCase = true) }
        if (selectedValue.isNotBlank() && !isExactMatch) {
            val query = selectedValue.trim()
            val matches = allPillItems.filter { itemToText(it).contains(query, ignoreCase = true) }
            if (matches.isNotEmpty()) matches else allPillItems
        } else {
            allPillItems
        }
    }

    // 4. Adaptive Dropdown Height based on `settings.maxVisibleItems`
    val maxDropdownHeight = remember(settings.maxVisibleItems) {
        (settings.maxVisibleItems * 48 + 120).dp
    }
    val maxListHeight = remember(settings.maxVisibleItems) {
        (settings.maxVisibleItems * 48).dp
    }

    var textFieldValue by remember(selectedValue) {
        mutableStateOf(
            TextFieldValue(
                text = selectedValue,
                selection = TextRange(selectedValue.length)
            )
        )
    }
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
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
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            if (newValue.text != selectedValue) {
                                onValueChange(newValue.text)
                            }
                            if (!expanded) {
                                expanded = true
                            }
                        },
                        label = { Text(label) },
                        placeholder = { Text("Select a: $label item") },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedValue.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            textFieldValue = TextFieldValue("")
                                            onValueChange("")
                                            expanded = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear $label"
                                        )
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && !isFocused) {
                                    expanded = true
                                }
                                isFocused = focusState.isFocused
                            },
                        singleLine = true
                    )

                    // Dropdown Menu with Up/Down buttons and adaptive scrolling
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxDropdownHeight)
                    ) {
                        // ▲ Up Scroll Button (at the START of the list)
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    val target = max(0, scrollState.value - (settings.scrollStep * 140))
                                    scrollState.animateScrollTo(target)
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxListHeight)
                                .verticalScroll(scrollState)
                        ) {
                            displayItems.forEach { item ->
                                val itemText = itemToText(item)
                                val isSelected = selectedValue.equals(itemText, ignoreCase = true)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            onValueChange(itemText)
                                            expanded = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = itemText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (onEditItem != null || onDeleteItem != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            if (onEditItem != null) {
                                                IconButton(
                                                    onClick = { onEditItem(item) },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        modifier = Modifier.size(15.dp),
                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                                    )
                                                }
                                            }
                                            if (onDeleteItem != null) {
                                                IconButton(
                                                    onClick = { onDeleteItem(item) },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        modifier = Modifier.size(15.dp),
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // ▼ Down Scroll Button (at the END of the list)
                        Surface(
                            onClick = {
                                coroutineScope.launch {
                                    val target = min(scrollState.maxValue, scrollState.value + (settings.scrollStep * 140))
                                    scrollState.animateScrollTo(target)
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

            // --- ROW 2: DEDICATED EXPANDABLE PILLS CARD ---
            if (allPillItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                var isPillsExpanded by remember { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        // Pills Card Header with Expand/Collapse toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPillsExpanded = !isPillsExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Quick Pick",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = "${allPillItems.size}",
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isPillsExpanded) "Collapse" else "Expand (${settings.pillRows} row${if (settings.pillRows > 1) "s" else ""})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = if (isPillsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isPillsExpanded) "Collapse Pills" else "Expand Pills",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Pills Content:
                        if (isPillsExpanded) {
                            // Fully expanded: Multi-row wrap of all pills
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                maxItemsInEachRow = Int.MAX_VALUE,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                allPillItems.forEach { item ->
                                    val itemText = itemToText(item)
                                    FilterChip(
                                        selected = selectedValue.equals(itemText, ignoreCase = true),
                                        onClick = { onValueChange(itemText) },
                                        label = { Text(itemText, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        } else {
                            // Collapsed: Display default number of lines (1 or 2 as configured)
                            if (settings.pillRows <= 1) {
                                // 1 Row: Horizontal Scrollable
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    allPillItems.forEach { item ->
                                        val itemText = itemToText(item)
                                        FilterChip(
                                            selected = selectedValue.equals(itemText, ignoreCase = true),
                                            onClick = { onValueChange(itemText) },
                                            label = { Text(itemText, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            } else {
                                // 2+ Rows: FlowRow strictly limited by maxLines = settings.pillRows
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    maxLines = settings.pillRows,
                                    overflow = FlowRowOverflow.Clip,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    allPillItems.forEach { item ->
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
