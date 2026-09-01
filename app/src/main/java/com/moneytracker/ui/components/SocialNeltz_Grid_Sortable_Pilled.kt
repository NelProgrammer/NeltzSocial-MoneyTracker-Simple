package com.moneytracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

enum class ColumnSortStrategy {
    NON_SORTING,
    ASCENDING,
    DESCENDING,
    CUSTOM_PRIORITY
}

data class GridColumnDefinition<T>(
    val id: String,
    val title: String,
    val width: Dp = 100.dp,
    val isSortable: Boolean = true,
    val defaultStrategy: ColumnSortStrategy = ColumnSortStrategy.NON_SORTING,
    val valueExtractor: (T) -> String = { "" },
    val cellContent: @Composable (item: T, isSelected: Boolean) -> Unit
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun <T> SocialNeltz_Grid_Sortable_Pilled(
    items: List<T>,
    columns: List<GridColumnDefinition<T>>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    initialPillColumnId: String? = null,
    selectedItemKeys: Set<Any> = emptySet(),
    onToggleSelectKey: ((Any) -> Unit)? = null,
    onRowClick: ((T) -> Unit)? = null,
    onRowDoubleClick: ((T) -> Unit)? = null,
    onRowLongClick: ((T) -> Unit)? = null,
    emptyMessage: String = "No items found",
    customPriorityOrders: Map<String, List<String>> = emptyMap(),
    onCustomPriorityOrderChanged: ((columnId: String, newOrder: List<String>) -> Unit)? = null
) {
    // 1. Column Order State (supports drag-and-drop reordering via Click & Hold on column gear)
    var columnList by remember(columns) { mutableStateOf(columns) }

    // 2. Active Sort Strategies per Column
    val columnSortStrategies = remember {
        mutableStateMapOf<String, ColumnSortStrategy>().apply {
            columns.forEach { put(it.id, it.defaultStrategy) }
        }
    }

    // 3. Dynamic Filter Pills Column Configuration
    var activePillColumnId by remember(columns, initialPillColumnId) {
        mutableStateOf(initialPillColumnId ?: columns.firstOrNull { it.id != "select" && it.id != "actions" }?.id ?: columns.first().id)
    }
    var selectedPillValue by remember { mutableStateOf<String?>(null) }
    var maxPillRows by remember { mutableStateOf(2) } // 1, 2, 3, or Int.MAX_VALUE
    var showPillConfigDialog by remember { mutableStateOf(false) }

    // 4. Priority Sorting Popup Dialog State
    var prioritySortTargetColumn by remember { mutableStateOf<GridColumnDefinition<T>?>(null) }
    val localPriorityOrders = remember { mutableStateMapOf<String, List<String>>().apply { putAll(customPriorityOrders) } }

    // Extract unique values for active pill column
    val activePillColumn = columnList.find { it.id == activePillColumnId }
    val distinctPillValues = remember(items, activePillColumn) {
        if (activePillColumn != null) {
            items.map { activePillColumn.valueExtractor(it).trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        } else {
            emptyList()
        }
    }

    // Filter Items by Selected Pill
    val filteredItems = remember(items, activePillColumn, selectedPillValue) {
        if (activePillColumn == null || selectedPillValue == null) {
            items
        } else {
            items.filter { activePillColumn.valueExtractor(it).trim().equals(selectedPillValue, ignoreCase = true) }
        }
    }

    // Sort Items based on ordered Column Sort Strategies
    val sortedFilteredItems = remember(filteredItems, columnList, columnSortStrategies.toMap(), localPriorityOrders.toMap()) {
        val activeSortingColumns = columnList.filter {
            val strat = columnSortStrategies[it.id] ?: ColumnSortStrategy.NON_SORTING
            strat != ColumnSortStrategy.NON_SORTING
        }

        if (activeSortingColumns.isEmpty()) {
            filteredItems
        } else {
            filteredItems.sortedWith { a, b ->
                var result = 0
                for (col in activeSortingColumns) {
                    val strat = columnSortStrategies[col.id] ?: ColumnSortStrategy.NON_SORTING
                    val valA = col.valueExtractor(a).trim()
                    val valB = col.valueExtractor(b).trim()

                    val comparison = when (strat) {
                        ColumnSortStrategy.ASCENDING -> {
                            val numA = valA.toDoubleOrNull()
                            val numB = valB.toDoubleOrNull()
                            if (numA != null && numB != null) numA.compareTo(numB) else valA.compareTo(valB, ignoreCase = true)
                        }
                        ColumnSortStrategy.DESCENDING -> {
                            val numA = valA.toDoubleOrNull()
                            val numB = valB.toDoubleOrNull()
                            if (numA != null && numB != null) numB.compareTo(numA) else valB.compareTo(valA, ignoreCase = true)
                        }
                        ColumnSortStrategy.CUSTOM_PRIORITY -> {
                            val order = localPriorityOrders[col.id] ?: emptyList()
                            val idxA = order.indexOfFirst { it.equals(valA, ignoreCase = true) }.let { if (it == -1) Int.MAX_VALUE else it }
                            val idxB = order.indexOfFirst { it.equals(valB, ignoreCase = true) }.let { if (it == -1) Int.MAX_VALUE else it }
                            idxA.compareTo(idxB)
                        }
                        ColumnSortStrategy.NON_SORTING -> 0
                    }
                    if (comparison != 0) {
                        result = comparison
                        break
                    }
                }
                result
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // -------------------------------------------------------------
            // A. DYNAMIC FILTER PILLS CONTAINER (with Gear ⚙)
            // -------------------------------------------------------------
            if (activePillColumn != null && distinctPillValues.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filter Pills Header with Gear
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showPillConfigDialog = true }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activePillColumn.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configure Pills",
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Filter Pills Horizontal / Multi-row Layout
                        val horizontalScroll = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalScroll),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "All" Pill
                            FilterChip(
                                selected = selectedPillValue == null,
                                onClick = { selectedPillValue = null },
                                label = { Text("All (${items.size})", style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            )

                            // Column Value Pills
                            distinctPillValues.forEach { pillVal ->
                                val count = items.count { activePillColumn.valueExtractor(it).trim().equals(pillVal, ignoreCase = true) }
                                FilterChip(
                                    selected = selectedPillValue.equals(pillVal, ignoreCase = true),
                                    onClick = {
                                        selectedPillValue = if (selectedPillValue.equals(pillVal, ignoreCase = true)) null else pillVal
                                    },
                                    label = { Text("$pillVal ($count)", style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }

            // -------------------------------------------------------------
            // B. GRID TABLE HEADER (with Column Gear ⚙ & Click&Hold Reorder)
            // -------------------------------------------------------------
            val tableHorizontalScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tableHorizontalScroll)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                columnList.forEachIndexed { colIdx, colDef ->
                    var showColumnGearMenu by remember { mutableStateOf(false) }
                    val currentStrategy = columnSortStrategies[colDef.id] ?: ColumnSortStrategy.NON_SORTING

                    Row(
                        modifier = Modifier
                            .width(colDef.width)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = colDef.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (currentStrategy != ColumnSortStrategy.NON_SORTING) FontWeight.Bold else FontWeight.SemiBold
                            ),
                            color = if (currentStrategy != ColumnSortStrategy.NON_SORTING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Column Gear Icon (Click = Strategy Selection, Click&Hold = Column Reordering)
                        if (colDef.isSortable) {
                            Box {
                                IconButton(
                                    onClick = { showColumnGearMenu = true },
                                    modifier = Modifier
                                        .size(22.dp)
                                        .pointerInput(colIdx) {
                                            detectDragGesturesAfterLongPress(
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    if (dragAmount.x > 30 && colIdx < columnList.size - 1) {
                                                        val mutable = columnList.toMutableList()
                                                        val temp = mutable[colIdx]
                                                        mutable[colIdx] = mutable[colIdx + 1]
                                                        mutable[colIdx + 1] = temp
                                                        columnList = mutable
                                                    } else if (dragAmount.x < -30 && colIdx > 0) {
                                                        val mutable = columnList.toMutableList()
                                                        val temp = mutable[colIdx]
                                                        mutable[colIdx] = mutable[colIdx - 1]
                                                        mutable[colIdx - 1] = temp
                                                        columnList = mutable
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    Icon(
                                        imageVector = when (currentStrategy) {
                                            ColumnSortStrategy.ASCENDING -> Icons.Default.ArrowUpward
                                            ColumnSortStrategy.DESCENDING -> Icons.Default.ArrowDownward
                                            ColumnSortStrategy.CUSTOM_PRIORITY -> Icons.Default.FormatListNumbered
                                            ColumnSortStrategy.NON_SORTING -> Icons.Default.Settings
                                        },
                                        contentDescription = "Column Sort & Options",
                                        tint = if (currentStrategy != ColumnSortStrategy.NON_SORTING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showColumnGearMenu,
                                    onDismissRequest = { showColumnGearMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Normal Ascending (A-Z / Min-Max)") },
                                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                                        onClick = {
                                            columnSortStrategies[colDef.id] = ColumnSortStrategy.ASCENDING
                                            showColumnGearMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Normal Descending (Z-A / Max-Min)") },
                                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                                        onClick = {
                                            columnSortStrategies[colDef.id] = ColumnSortStrategy.DESCENDING
                                            showColumnGearMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Custom Priority (Drag & Drop Tab)") },
                                        leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                                        onClick = {
                                            columnSortStrategies[colDef.id] = ColumnSortStrategy.CUSTOM_PRIORITY
                                            prioritySortTargetColumn = colDef
                                            showColumnGearMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Non-Sorting (Off)") },
                                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                                        onClick = {
                                            columnSortStrategies[colDef.id] = ColumnSortStrategy.NON_SORTING
                                            showColumnGearMenu = false
                                        }
                                    )
                                    HorizontalDivider()
                                    if (colIdx > 0) {
                                        DropdownMenuItem(
                                            text = { Text("Move Column Left") },
                                            onClick = {
                                                val mutable = columnList.toMutableList()
                                                val temp = mutable[colIdx]
                                                mutable[colIdx] = mutable[colIdx - 1]
                                                mutable[colIdx - 1] = temp
                                                columnList = mutable
                                                showColumnGearMenu = false
                                            }
                                        )
                                    }
                                    if (colIdx < columnList.size - 1) {
                                        DropdownMenuItem(
                                            text = { Text("Move Column Right") },
                                            onClick = {
                                                val mutable = columnList.toMutableList()
                                                val temp = mutable[colIdx]
                                                mutable[colIdx] = mutable[colIdx + 1]
                                                mutable[colIdx + 1] = temp
                                                columnList = mutable
                                                showColumnGearMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // -------------------------------------------------------------
            // C. GRID TABLE ROWS
            // -------------------------------------------------------------
            if (sortedFilteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                sortedFilteredItems.forEachIndexed { rowIdx, item ->
                    val isSelected = selectedItemKeys.contains(itemKey(item))
                    if (rowIdx > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .combinedClickable(
                                onClick = {
                                    if (onToggleSelectKey != null) onToggleSelectKey(itemKey(item))
                                    else onRowClick?.invoke(item)
                                },
                                onDoubleClick = { onRowDoubleClick?.invoke(item) },
                                onLongClick = { onRowLongClick?.invoke(item) }
                            )
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        columnList.forEach { colDef ->
                            Box(
                                modifier = Modifier
                                    .width(colDef.width)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                colDef.cellContent(item, isSelected)
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // D. PILL CONFIGURATION MODAL DIALOG
    // -------------------------------------------------------------
    if (showPillConfigDialog) {
        AlertDialog(
            onDismissRequest = { showPillConfigDialog = false },
            title = { Text("Configure Filter Pills") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select which table column drives the quick-filter pills:")
                    columnList.filter { it.id != "select" && it.id != "actions" }.forEach { col ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    activePillColumnId = col.id
                                    selectedPillValue = null
                                    showPillConfigDialog = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = activePillColumnId == col.id,
                                onClick = {
                                    activePillColumnId = col.id
                                    selectedPillValue = null
                                    showPillConfigDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(col.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPillConfigDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // E. EDITABLE PRIORITY SORTING POPUP TAB / DIALOG
    // -------------------------------------------------------------
    if (prioritySortTargetColumn != null) {
        val targetCol = prioritySortTargetColumn!!
        val distinctItems = remember(items, targetCol) {
            val list = items.map { targetCol.valueExtractor(it).trim() }
                .filter { it.isNotBlank() }
                .distinct()
            val existingSaved = localPriorityOrders[targetCol.id] ?: emptyList()
            val ordered = existingSaved.filter { list.contains(it) } + list.filter { !existingSaved.contains(it) }
            ordered.toMutableList()
        }
        var editableList by remember(targetCol) { mutableStateOf(distinctItems) }

        Dialog(
            onDismissRequest = { prioritySortTargetColumn = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(520.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Priority Sort: ${targetCol.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Drag items up or down to set custom sorting priority",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { prioritySortTargetColumn = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    val lazyListState = rememberLazyListState()
                    val reorderableLazyColumnState = rememberReorderableLazyListState(lazyListState) { from, to ->
                        val updated = editableList.toMutableList()
                        val item = updated.removeAt(from.index)
                        updated.add(to.index, item)
                        editableList = updated
                    }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(editableList, key = { _, item -> item }) { index, itemValue ->
                            ReorderableItem(reorderableLazyColumnState, key = itemValue) { isDragging ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = itemValue,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        IconButton(
                                            onClick = {},
                                            modifier = Modifier.draggableHandle(
                                                onDragStarted = {},
                                                onDragStopped = {}
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "Drag to reorder",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { prioritySortTargetColumn = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                localPriorityOrders[targetCol.id] = editableList
                                onCustomPriorityOrderChanged?.invoke(targetCol.id, editableList)
                                prioritySortTargetColumn = null
                            }
                        ) {
                            Text("Save Priority Order")
                        }
                    }
                }
            }
        }
    }
}
