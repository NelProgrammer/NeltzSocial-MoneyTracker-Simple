package com.moneytracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moneytracker.util.SettingsManager
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt

enum class ColumnSortStrategy {
    NON_SORTING,
    ASCENDING,
    DESCENDING,
    CUSTOM_PRIORITY
}

enum class PillSortMode {
    CUSTOM_PRIORITY,
    NUMERICAL,
    ALPHABETICAL
}

enum class RowSnapBehavior {
    ON_RELEASE,
    ON_FOCUS_OR_SCROLL
}

data class GridColumnDefinition<T>(
    val id: String,
    val title: String,
    val width: Dp = 100.dp,
    val isSortable: Boolean = true,
    val defaultStrategy: ColumnSortStrategy? = null,
    val valueExtractor: (T) -> String = { "" },
    val cellContent: @Composable (item: T, isSelected: Boolean, wrapText: Boolean) -> Unit
)

/**
 * Enterprise Reusable Generic Sortable & Pilled Grid Component
 *
 * Full Feature Matrix:
 * 1. Default Sort Strategy Rules:
 *    - Text / String columns -> CUSTOM_PRIORITY
 *    - Numerical columns (amount, count, etc.) -> DESCENDING
 *    - Date / Time columns -> ASCENDING
 * 2. Sticky Header & Footer: Table is bounded, only the data area scrolls vertically.
 * 3. Distinct Horizontal Swiping: Header swipe scrolls whole table; Row swipe moves individual row.
 * 4. Row Snap Behavior: Configurable snap-back on release vs on focus/scroll.
 * 5. Full Persistence: All settings (widths, visibility, priorities, strategies, pills, wraps, snap behavior)
 *    are persisted via SettingsManager.
 * 6. High-Contrast Border Lines: Crisp and clearly visible vertical and horizontal borders.
 * 7. Silent Hover-Activated Resize Handles.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun <T> SocialNeltz_Grid_Sortable_Pilled(
    items: List<T>,
    columns: List<GridColumnDefinition<T>>,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    tableName: String = "Transactions",
    initialPillColumnId: String? = null,
    selectedItemKeys: Set<Any> = emptySet(),
    onToggleSelectKey: ((Any) -> Unit)? = null,
    onRowClick: ((T) -> Unit)? = null,
    onRowDoubleClick: ((T) -> Unit)? = null,
    onRowLongClick: ((T) -> Unit)? = null,
    emptyMessage: String = "No items found",
    customPriorityOrders: Map<String, List<String>> = emptyMap(),
    onCustomPriorityOrderChanged: ((columnId: String, newOrder: List<String>) -> Unit)? = null,
    masterCategoryNames: List<String> = emptyList(),
    masterSubcategoriesByCategory: Map<String, List<String>> = emptyMap()
) {
    val coroutineScope = rememberCoroutineScope()
    val gridId = remember(tableName) { tableName.replace("\\s+".toRegex(), "_").lowercase() }

    // Helper: Determine intelligent default sort strategy
    fun inferDefaultStrategy(col: GridColumnDefinition<T>): ColumnSortStrategy {
        if (col.defaultStrategy != null) return col.defaultStrategy
        val id = col.id.lowercase()
        return when {
            id.contains("date") || id.contains("time") -> ColumnSortStrategy.ASCENDING
            id.contains("amount") || id.contains("spent") || id.contains("balance") || id.contains("cost") || id.contains("units") || id.contains("count") -> ColumnSortStrategy.DESCENDING
            id.contains("category") || id.contains("type") || id.contains("note") -> ColumnSortStrategy.CUSTOM_PRIORITY
            else -> ColumnSortStrategy.CUSTOM_PRIORITY
        }
    }

    // 1. Column Widths Map (Persisted)
    val columnWidthMap = remember(columns, gridId) {
        mutableStateMapOf<String, Dp>().apply {
            columns.forEach { col ->
                val savedWidth = SettingsManager.getGridInt(gridId, "width_${col.id}", -1)
                put(col.id, if (savedWidth > 0) savedWidth.dp else col.width)
            }
        }
    }

    // 2. Master Column Visibility Map (Persisted)
    val columnVisibilityMap = remember(columns, gridId) {
        mutableStateMapOf<String, Boolean>().apply {
            columns.forEach { col ->
                val savedVis = SettingsManager.getGridBoolean(gridId, "vis_${col.id}", true)
                put(col.id, savedVis)
            }
        }
    }

    // 3. Multi-Column Sort Priority & Strategies (Persisted)
    val sortPriorityColumnIds = remember(columns, gridId) {
        mutableStateListOf<String>().apply {
            val savedOrderStr = SettingsManager.getGridString(gridId, "sort_order", "")
            if (savedOrderStr.isNotBlank()) {
                val savedCols = savedOrderStr.split(",").filter { id -> columns.any { it.id == id } }
                addAll(savedCols)
                columns.filter { !savedCols.contains(it.id) }.forEach { add(it.id) }
            } else {
                addAll(columns.map { it.id })
            }
        }
    }

    val columnSortStrategies = remember(columns, gridId) {
        mutableStateMapOf<String, ColumnSortStrategy>().apply {
            columns.forEach { col ->
                val savedStratStr = SettingsManager.getGridString(gridId, "strat_${col.id}", "")
                val strat = if (savedStratStr.isNotBlank()) {
                    try { ColumnSortStrategy.valueOf(savedStratStr) } catch (e: Exception) { inferDefaultStrategy(col) }
                } else {
                    inferDefaultStrategy(col)
                }
                put(col.id, strat)
            }
        }
    }

    // 4. Line-Wrapping & Row Snap Settings (Persisted)
    var wrapHeaderLines by remember(gridId) {
        mutableStateOf(SettingsManager.getGridBoolean(gridId, "wrap_header", false))
    }
    var wrapDataLines by remember(gridId) {
        mutableStateOf(SettingsManager.getGridBoolean(gridId, "wrap_data", false))
    }
    var rowSnapBehavior by remember(gridId) {
        val savedSnap = SettingsManager.getGridString(gridId, "snap_behavior", RowSnapBehavior.ON_RELEASE.name)
        mutableStateOf(try { RowSnapBehavior.valueOf(savedSnap) } catch (e: Exception) { RowSnapBehavior.ON_RELEASE })
    }

    // 5. Dynamic Filter Pills Configuration (Persisted)
    var activePillColumnId by remember(columns, initialPillColumnId, gridId) {
        val savedPillCol = SettingsManager.getGridString(gridId, "pill_col", "")
        mutableStateOf(if (savedPillCol.isNotBlank()) savedPillCol else initialPillColumnId ?: columns.firstOrNull { it.id != "select" && it.id != "actions" }?.id ?: columns.first().id)
    }
    var selectedPillValue by remember { mutableStateOf<String?>(null) }
    var maxPillRows by remember(gridId) {
        mutableIntStateOf(SettingsManager.getGridInt(gridId, "max_pill_rows", 1).coerceIn(1, 5))
    }
    var isPillsExpanded by remember { mutableStateOf(false) }
    var pillSortMode by remember(gridId) {
        val savedPillSort = SettingsManager.getGridString(gridId, "pill_sort_mode", PillSortMode.CUSTOM_PRIORITY.name)
        mutableStateOf(try { PillSortMode.valueOf(savedPillSort) } catch (e: Exception) { PillSortMode.CUSTOM_PRIORITY })
    }

    // 6. Pagination & Display Mode State (Persisted)
    var isPaginationEnabled by remember(gridId) {
        mutableStateOf(SettingsManager.getGridBoolean(gridId, "is_pagination", true))
    }
    var pageSize by remember(gridId) {
        mutableIntStateOf(SettingsManager.getGridInt(gridId, "page_size", 10))
    }
    var currentPage by remember { mutableIntStateOf(1) }
    var showFooterSettingsDialog by remember { mutableStateOf(false) }

    // 7. Modals State
    var showPrimaryGridGearDialog by remember { mutableStateOf(false) }
    var primaryGearActiveTab by remember { mutableIntStateOf(0) }
    var showCustomPriorityDialog by remember { mutableStateOf(false) }
    var priorityTargetColumnId by remember { mutableStateOf("category") }

    // Custom Priority Ranks Map
    val localPriorityOrders = remember {
        mutableStateMapOf<String, List<String>>().apply {
            putAll(customPriorityOrders)
        }
    }

    // Track focused row for snap behavior
    var focusedRowKey by remember { mutableStateOf<Any?>(null) }

    // Shared Table Horizontal Scroll State for Header Table Swipe
    val tableHorizontalScrollState = rememberScrollState()
    val density = LocalDensity.current

    // Reset focused row offsets when table is scrolled horizontally
    LaunchedEffect(tableHorizontalScrollState.value) {
        if (rowSnapBehavior == RowSnapBehavior.ON_FOCUS_OR_SCROLL) {
            focusedRowKey = null
        }
    }

    // Visible columns list based on master definitions
    val visibleColumns = remember(columns, columnVisibilityMap.toMap()) {
        columns.filter { columnVisibilityMap[it.id] != false }
    }

    // Active pill column definition
    val activePillColumn = columns.find { it.id == activePillColumnId }

    // Extract unique values for active pill column using 3-mode sorting
    val distinctPillValues = remember(items, activePillColumn, pillSortMode, localPriorityOrders.toMap(), masterCategoryNames) {
        if (activePillColumn != null) {
            val list = items.map { activePillColumn.valueExtractor(it).trim() }
                .filter { it.isNotBlank() }
                .distinct()

            when (pillSortMode) {
                PillSortMode.CUSTOM_PRIORITY -> {
                    val customOrder = localPriorityOrders[activePillColumn.id] ?: masterCategoryNames
                    list.sortedWith { a, b ->
                        val idxA = customOrder.indexOfFirst { it.equals(a, ignoreCase = true) }.let { if (it == -1) Int.MAX_VALUE else it }
                        val idxB = customOrder.indexOfFirst { it.equals(b, ignoreCase = true) }.let { if (it == -1) Int.MAX_VALUE else it }
                        if (idxA != idxB) idxA.compareTo(idxB) else a.compareTo(b, ignoreCase = true)
                    }
                }
                PillSortMode.NUMERICAL -> {
                    list.sortedByDescending { pillVal ->
                        items.count { activePillColumn.valueExtractor(it).trim().equals(pillVal, ignoreCase = true) }
                    }
                }
                PillSortMode.ALPHABETICAL -> {
                    list.sortedBy { it.lowercase() }
                }
            }
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

    // Active sorted column IDs
    val activeSortedColumnIds = remember(sortPriorityColumnIds.toList(), columnSortStrategies.toMap()) {
        sortPriorityColumnIds.filter { (columnSortStrategies[it] ?: ColumnSortStrategy.NON_SORTING) != ColumnSortStrategy.NON_SORTING }
    }

    // Multi-Column Sorted Filtered Items
    val sortedFilteredItems = remember(filteredItems, activeSortedColumnIds, columnSortStrategies.toMap(), localPriorityOrders.toMap(), columns) {
        if (activeSortedColumnIds.isEmpty()) {
            filteredItems
        } else {
            filteredItems.sortedWith { a, b ->
                var result = 0
                for (colId in activeSortedColumnIds) {
                    val colDef = columns.find { it.id == colId } ?: continue
                    val strat = columnSortStrategies[colId] ?: ColumnSortStrategy.NON_SORTING
                    val valA = colDef.valueExtractor(a).trim()
                    val valB = colDef.valueExtractor(b).trim()

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
                            val order = localPriorityOrders[colId] ?: emptyList()
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

    // Total Count and Pages Calculation
    val totalCount = sortedFilteredItems.size
    val totalPages = remember(totalCount, pageSize, isPaginationEnabled) {
        if (!isPaginationEnabled || pageSize <= 0 || totalCount == 0) 1
        else (totalCount + pageSize - 1) / pageSize
    }

    LaunchedEffect(totalPages) {
        if (currentPage > totalPages) currentPage = totalPages
        if (currentPage < 1) currentPage = 1
    }

    val startItemIdx = if (totalCount == 0) 0 else if (!isPaginationEnabled || pageSize <= 0) 1 else (currentPage - 1) * pageSize + 1
    val endItemIdx = if (totalCount == 0) 0 else if (!isPaginationEnabled || pageSize <= 0) totalCount else kotlin.math.min(currentPage * pageSize, totalCount)

    // Paged Items slice
    val pagedItems = remember(sortedFilteredItems, currentPage, pageSize, isPaginationEnabled) {
        if (!isPaginationEnabled || pageSize <= 0 || totalCount == 0) sortedFilteredItems
        else {
            val from = (currentPage - 1) * pageSize
            val to = kotlin.math.min(from + pageSize, sortedFilteredItems.size)
            if (from < sortedFilteredItems.size) sortedFilteredItems.subList(from, to) else emptyList()
        }
    }

    // Crisp high-contrast cell borders
    val borderLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    // =================================================================
    // OVERARCHING BOUNDED GRID CONTAINER
    // =================================================================
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // -------------------------------------------------------------
        // CONTAINER 1: TABLE NAME & RECORD COUNT CARD (with Primary Gear ⚙)
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ),
            border = BorderStroke(1.2.dp, borderLineColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tableName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "$totalCount Items",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Primary Grid Gear (⚙)
                IconButton(
                    onClick = { 
                        primaryGearActiveTab = 0
                        showPrimaryGridGearDialog = true 
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Primary Grid Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // CONTAINER 2: INDEPENDENT FILTER PILLS CONTAINER CARD
        // -------------------------------------------------------------
        if (activePillColumn != null && distinctPillValues.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.2.dp, borderLineColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                primaryGearActiveTab = 2
                                showPrimaryGridGearDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${activePillColumn.title} Filter",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = "${distinctPillValues.size}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(
                                onClick = {
                                    primaryGearActiveTab = 2
                                    showPrimaryGridGearDialog = true
                                },
                                modifier = Modifier.size(24.dp).padding(start = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Pills Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Expand / Collapse Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isPillsExpanded = !isPillsExpanded }
                        ) {
                            Text(
                                text = if (isPillsExpanded) "Collapse" else "Expand (${maxPillRows} row${if (maxPillRows > 1) "s" else ""})",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = if (isPillsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pills Layout
                    if (isPillsExpanded) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            maxItemsInEachRow = Int.MAX_VALUE,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedPillValue == null,
                                onClick = { selectedPillValue = null },
                                label = { Text("All (${items.size})", style = MaterialTheme.typography.labelSmall) }
                            )
                            distinctPillValues.forEach { pillVal ->
                                val count = items.count { activePillColumn.valueExtractor(it).trim().equals(pillVal, ignoreCase = true) }
                                FilterChip(
                                    selected = selectedPillValue.equals(pillVal, ignoreCase = true),
                                    onClick = {
                                        selectedPillValue = if (selectedPillValue.equals(pillVal, ignoreCase = true)) null else pillVal
                                    },
                                    label = { Text("$pillVal ($count)", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    } else {
                        if (maxPillRows <= 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = selectedPillValue == null,
                                    onClick = { selectedPillValue = null },
                                    label = { Text("All (${items.size})", style = MaterialTheme.typography.labelSmall) }
                                )
                                distinctPillValues.forEach { pillVal ->
                                    val count = items.count { activePillColumn.valueExtractor(it).trim().equals(pillVal, ignoreCase = true) }
                                    FilterChip(
                                        selected = selectedPillValue.equals(pillVal, ignoreCase = true),
                                        onClick = {
                                            selectedPillValue = if (selectedPillValue.equals(pillVal, ignoreCase = true)) null else pillVal
                                        },
                                        label = { Text("$pillVal ($count)", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                maxLines = maxPillRows,
                                overflow = FlowRowOverflow.Clip,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = selectedPillValue == null,
                                    onClick = { selectedPillValue = null },
                                    label = { Text("All (${items.size})", style = MaterialTheme.typography.labelSmall) }
                                )
                                distinctPillValues.forEach { pillVal ->
                                    val count = items.count { activePillColumn.valueExtractor(it).trim().equals(pillVal, ignoreCase = true) }
                                    FilterChip(
                                        selected = selectedPillValue.equals(pillVal, ignoreCase = true),
                                        onClick = {
                                            selectedPillValue = if (selectedPillValue.equals(pillVal, ignoreCase = true)) null else pillVal
                                        },
                                        label = { Text("$pillVal ($count)", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // CONTAINER 3: BOUNDED TABLE CARD (Sticky Header + Scrollable Rows Area)
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, borderLineColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // =====================================================
                // 1. STICKY COLUMN HEADER ROW (Swiping horizontally scrolls whole table)
                // =====================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                        .horizontalScroll(tableHorizontalScrollState),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    visibleColumns.forEachIndexed { colIdx, colDef ->
                        var showColumnGearMenu by remember { mutableStateOf(false) }
                        var isBorderHovered by remember { mutableStateOf(false) }
                        val currentStrategy = columnSortStrategies[colDef.id] ?: ColumnSortStrategy.NON_SORTING
                        val activePriorityRank = activeSortedColumnIds.indexOf(colDef.id).let { if (it >= 0) it + 1 else null }
                        val colWidth = columnWidthMap[colDef.id] ?: colDef.width

                        // Cell Header Box
                        Box(
                            modifier = Modifier
                                .width(colWidth)
                                .heightIn(min = 42.dp)
                                .drawBehind {
                                    // Bottom cell border
                                    drawLine(
                                        color = borderLineColor,
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 1.2.dp.toPx()
                                    )
                                }
                                .padding(start = 6.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                    maxLines = if (wrapHeaderLines) 2 else 1,
                                    overflow = if (wrapHeaderLines) TextOverflow.Clip else TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(2.dp))

                                // Vertically Stacked Badge & Direction Arrow + Column Gear
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        if (activePriorityRank != null) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "$activePriorityRank",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                            }
                                        }

                                        Icon(
                                            imageVector = when (currentStrategy) {
                                                ColumnSortStrategy.ASCENDING -> Icons.Default.ArrowUpward
                                                ColumnSortStrategy.DESCENDING -> Icons.Default.ArrowDownward
                                                ColumnSortStrategy.CUSTOM_PRIORITY -> Icons.Default.MoreVert
                                                ColumnSortStrategy.NON_SORTING -> Icons.Default.RadioButtonUnchecked
                                            },
                                            contentDescription = "Sort Direction",
                                            tint = if (currentStrategy != ColumnSortStrategy.NON_SORTING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }

                                    // Column Gear (⚙)
                                    Box {
                                        IconButton(
                                            onClick = { showColumnGearMenu = true },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Column Options",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showColumnGearMenu,
                                            onDismissRequest = { showColumnGearMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Ascending (A → Z / Min → Max)") },
                                                leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                                                onClick = {
                                                    columnSortStrategies[colDef.id] = ColumnSortStrategy.ASCENDING
                                                    SettingsManager.saveGridString(gridId, "strat_${colDef.id}", ColumnSortStrategy.ASCENDING.name)
                                                    showColumnGearMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Descending (Z → A / Max → Min)") },
                                                leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                                                onClick = {
                                                    columnSortStrategies[colDef.id] = ColumnSortStrategy.DESCENDING
                                                    SettingsManager.saveGridString(gridId, "strat_${colDef.id}", ColumnSortStrategy.DESCENDING.name)
                                                    showColumnGearMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Custom Priority (Rank Order)") },
                                                leadingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                                                onClick = {
                                                    columnSortStrategies[colDef.id] = ColumnSortStrategy.CUSTOM_PRIORITY
                                                    SettingsManager.saveGridString(gridId, "strat_${colDef.id}", ColumnSortStrategy.CUSTOM_PRIORITY.name)
                                                    priorityTargetColumnId = colDef.id
                                                    showCustomPriorityDialog = true
                                                    showColumnGearMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Non-Sorting (Off)") },
                                                leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null) },
                                                onClick = {
                                                    columnSortStrategies[colDef.id] = ColumnSortStrategy.NON_SORTING
                                                    SettingsManager.saveGridString(gridId, "strat_${colDef.id}", ColumnSortStrategy.NON_SORTING.name)
                                                    showColumnGearMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Silent Inner Column Border with Drag Handle Dot
                        val handleAlpha by animateFloatAsState(targetValue = if (isBorderHovered) 1f else 0f, label = "handleAlpha")
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .heightIn(min = 42.dp)
                                .pointerInput(colDef.id) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { isBorderHovered = true },
                                        onDragEnd = { isBorderHovered = false },
                                        onDragCancel = { isBorderHovered = false }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        with(density) {
                                            val curW = columnWidthMap[colDef.id] ?: colDef.width
                                            val newW = (curW + dragAmount.toDp()).coerceIn(60.dp, 400.dp)
                                            columnWidthMap[colDef.id] = newW
                                            SettingsManager.saveGridInt(gridId, "width_${colDef.id}", newW.value.roundToInt())
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Vertical Divider Line (Crisp)
                            Box(
                                modifier = Modifier
                                    .width(1.2.dp)
                                    .fillMaxHeight()
                                    .background(borderLineColor)
                            )

                            // Silent Resize Handle Dot: Visible ONLY on hover/drag
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(10.dp)
                                    .alpha(handleAlpha)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onPrimary)
                                    )
                                }
                            }
                        }
                    }
                }

                // =====================================================
                // 2. BOUNDED SCROLLABLE DATA ROWS AREA (Only Data Scrolls Vertically)
                // =====================================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (pagedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emptyMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        pagedItems.forEachIndexed { rowIdx, item ->
                            val currentKey = itemKey(item)
                            val isSelected = selectedItemKeys.contains(currentKey)
                            val rowScrollOffset = remember { Animatable(0f) }

                            // Reset offset if focus shifted
                            LaunchedEffect(focusedRowKey) {
                                if (focusedRowKey != currentKey && rowScrollOffset.value != 0f) {
                                    rowScrollOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset { IntOffset(rowScrollOffset.value.roundToInt(), 0) }
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                                    .drawBehind {
                                        // Crisp horizontal bottom row border
                                        drawLine(
                                            color = borderLineColor,
                                            start = Offset(0f, size.height),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = 1.2.dp.toPx()
                                        )
                                    }
                                    .pointerInput(currentKey) {
                                        detectHorizontalDragGestures(
                                            onDragStart = { focusedRowKey = currentKey },
                                            onDragEnd = {
                                                if (rowSnapBehavior == RowSnapBehavior.ON_RELEASE) {
                                                    coroutineScope.launch {
                                                        rowScrollOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                coroutineScope.launch {
                                                    rowScrollOffset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                                }
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            coroutineScope.launch {
                                                rowScrollOffset.snapTo((rowScrollOffset.value + dragAmount).coerceIn(-150f, 150f))
                                            }
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                visibleColumns.forEach { colDef ->
                                    val colWidth = columnWidthMap[colDef.id] ?: colDef.width
                                    Box(
                                        modifier = Modifier
                                            .width(colWidth)
                                            .heightIn(min = 36.dp, max = 56.dp)
                                            .padding(horizontal = 6.dp, vertical = 6.dp)
                                            .clickable {
                                                focusedRowKey = currentKey
                                                if (onToggleSelectKey != null) onToggleSelectKey(currentKey)
                                                else onRowClick?.invoke(item)
                                            },
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        colDef.cellContent(item, isSelected, wrapDataLines)
                                    }

                                    // Crisp vertical divider line between cells
                                    Box(
                                        modifier = Modifier
                                            .width(12.dp)
                                            .heightIn(min = 36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.2.dp)
                                                .fillMaxHeight()
                                                .background(borderLineColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // CONTAINER 4: STICKY FOOTER CARD (Pagination & Mode Controls)
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ),
            border = BorderStroke(1.2.dp, borderLineColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isPaginationEnabled) {
                    // Left: Range Summary & Page Size Toggle Chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (totalCount == 0) "0 items" else "$startItemIdx–$endItemIdx of $totalCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        listOf(10, 25, 50, 0).forEach { sizeOption ->
                            val isSelected = pageSize == sizeOption
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null,
                                modifier = Modifier.clickable {
                                    pageSize = sizeOption
                                    SettingsManager.saveGridInt(gridId, "page_size", sizeOption)
                                }
                            ) {
                                Text(
                                    text = if (sizeOption == 0) "All" else "$sizeOption",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Right: Navigation Buttons + Footer Gear (⚙)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { currentPage = 1 },
                            enabled = currentPage > 1,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("«", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (currentPage > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        }

                        IconButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("‹", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (currentPage > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        }

                        Text(
                            text = "$currentPage / $totalPages",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = currentPage < totalPages,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("›", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (currentPage < totalPages) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        }

                        IconButton(
                            onClick = { currentPage = totalPages },
                            enabled = currentPage < totalPages,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("»", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (currentPage < totalPages) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        }

                        IconButton(
                            onClick = { showFooterSettingsDialog = true },
                            modifier = Modifier.size(26.dp).padding(start = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Footer Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total: $totalCount items • Continuous Scroll Mode",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(
                            onClick = { showFooterSettingsDialog = true },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Footer Settings",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // =================================================================
    // PRIMARY GRID GEAR SETTINGS DIALOG
    // Tabs: 0: Multi-Column Sort | 1: Visibility | 2: Pills | 3: Layout & Snap
    // =================================================================
    if (showPrimaryGridGearDialog) {
        Dialog(
            onDismissRequest = { showPrimaryGridGearDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .height(600.dp),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Grid Table Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showPrimaryGridGearDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TabRow(selectedTabIndex = primaryGearActiveTab) {
                        Tab(
                            selected = primaryGearActiveTab == 0,
                            onClick = { primaryGearActiveTab = 0 },
                            text = { Text("Sort Priority", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = primaryGearActiveTab == 1,
                            onClick = { primaryGearActiveTab = 1 },
                            text = { Text("Visibility", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = primaryGearActiveTab == 2,
                            onClick = { primaryGearActiveTab = 2 },
                            text = { Text("Pills", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = primaryGearActiveTab == 3,
                            onClick = { primaryGearActiveTab = 3 },
                            text = { Text("Layout & Snap", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // TAB 0: MULTI-COLUMN SORT PRIORITY
                    if (primaryGearActiveTab == 0) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Drag columns up or down to set multi-level sort priority (#1, #2, ...). Tap the button to cycle strategy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val lazyListState = rememberLazyListState()
                            val reorderableLazyColumnState = rememberReorderableLazyListState(lazyListState) { from, to ->
                                val item = sortPriorityColumnIds.removeAt(from.index)
                                sortPriorityColumnIds.add(to.index, item)
                                SettingsManager.saveGridString(gridId, "sort_order", sortPriorityColumnIds.joinToString(","))
                            }

                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(sortPriorityColumnIds, key = { _, colId -> colId }) { index, colId ->
                                    val colDef = columns.find { it.id == colId }
                                    val currentStrat = columnSortStrategies[colId] ?: ColumnSortStrategy.NON_SORTING
                                    val isVisible = columnVisibilityMap[colId] != false

                                    if (colDef != null && isVisible) {
                                        ReorderableItem(reorderableLazyColumnState, key = colId) { isDragging ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = if (currentStrat != ColumnSortStrategy.NON_SORTING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text(
                                                                    text = "${index + 1}",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                    color = if (currentStrat != ColumnSortStrategy.NON_SORTING) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                            text = colDef.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Button(
                                                            onClick = {
                                                                val nextStrat = when (currentStrat) {
                                                                    ColumnSortStrategy.NON_SORTING -> ColumnSortStrategy.ASCENDING
                                                                    ColumnSortStrategy.ASCENDING -> ColumnSortStrategy.DESCENDING
                                                                    ColumnSortStrategy.DESCENDING -> ColumnSortStrategy.CUSTOM_PRIORITY
                                                                    ColumnSortStrategy.CUSTOM_PRIORITY -> ColumnSortStrategy.NON_SORTING
                                                                }
                                                                columnSortStrategies[colId] = nextStrat
                                                                SettingsManager.saveGridString(gridId, "strat_$colId", nextStrat.name)
                                                            },
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(30.dp)
                                                        ) {
                                                            Text(
                                                                text = when (currentStrat) {
                                                                    ColumnSortStrategy.ASCENDING -> "ASC ▲"
                                                                    ColumnSortStrategy.DESCENDING -> "DESC ▼"
                                                                    ColumnSortStrategy.CUSTOM_PRIORITY -> "CUSTOM ⋮"
                                                                    ColumnSortStrategy.NON_SORTING -> "OFF ○"
                                                                },
                                                                fontSize = 11.sp
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.width(6.dp))

                                                        IconButton(
                                                            onClick = {},
                                                            modifier = Modifier.draggableHandle()
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
                                }
                            }
                        }
                    }

                    // TAB 1: COLUMN VISIBILITY
                    if (primaryGearActiveTab == 1) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Select which columns are visible in the grid table:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(columns, key = { _, col -> col.id }) { _, col ->
                                    val isVisible = columnVisibilityMap[col.id] != false
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val newVis = !isVisible
                                                    columnVisibilityMap[col.id] = newVis
                                                    SettingsManager.saveGridBoolean(gridId, "vis_${col.id}", newVis)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(col.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Switch(
                                                checked = isVisible,
                                                onCheckedChange = {
                                                    columnVisibilityMap[col.id] = it
                                                    SettingsManager.saveGridBoolean(gridId, "vis_${col.id}", it)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: FILTER PILLS
                    if (primaryGearActiveTab == 2) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Pill Rows Stepper
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Pill Display Rows", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (maxPillRows == 1) "1 Row (Horizontal Scroll)" else "$maxPillRows Rows (Multi-line Wrap)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (maxPillRows > 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(32.dp),
                                        onClick = {
                                            if (maxPillRows > 1) {
                                                maxPillRows--
                                                SettingsManager.saveGridInt(gridId, "max_pill_rows", maxPillRows)
                                            }
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrement", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.width(36.dp).height(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("$maxPillRows", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = if (maxPillRows < 5) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(32.dp),
                                        onClick = {
                                            if (maxPillRows < 5) {
                                                maxPillRows++
                                                SettingsManager.saveGridInt(gridId, "max_pill_rows", maxPillRows)
                                            }
                                        }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Add, contentDescription = "Increment", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Text("Pills Sorting Order:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(
                                    PillSortMode.CUSTOM_PRIORITY to "Custom Priority (Default)",
                                    PillSortMode.NUMERICAL to "Numerical (Highest to Lowest Count)",
                                    PillSortMode.ALPHABETICAL to "Alphabetical (A → Z)"
                                ).forEach { (mode, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                pillSortMode = mode
                                                SettingsManager.saveGridString(gridId, "pill_sort_mode", mode.name)
                                            }
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = pillSortMode == mode,
                                            onClick = {
                                                pillSortMode = mode
                                                SettingsManager.saveGridString(gridId, "pill_sort_mode", mode.name)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(label, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Text("Pills Source Column:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                itemsIndexed(columns) { _, col ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                activePillColumnId = col.id
                                                selectedPillValue = null
                                                SettingsManager.saveGridString(gridId, "pill_col", col.id)
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = activePillColumnId == col.id,
                                            onClick = {
                                                activePillColumnId = col.id
                                                selectedPillValue = null
                                                SettingsManager.saveGridString(gridId, "pill_col", col.id)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(col.title, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    // TAB 3: LAYOUT & ROW SNAP BEHAVIOR
                    if (primaryGearActiveTab == 3) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Row Swipe Snap Behavior Setting
                            Text("Row Swipe Snap Back Behavior:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            rowSnapBehavior = RowSnapBehavior.ON_RELEASE
                                            SettingsManager.saveGridString(gridId, "snap_behavior", RowSnapBehavior.ON_RELEASE.name)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = rowSnapBehavior == RowSnapBehavior.ON_RELEASE,
                                        onClick = {
                                            rowSnapBehavior = RowSnapBehavior.ON_RELEASE
                                            SettingsManager.saveGridString(gridId, "snap_behavior", RowSnapBehavior.ON_RELEASE.name)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Snap on Release (Default)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text("Snaps back immediately when drag gesture is released", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            rowSnapBehavior = RowSnapBehavior.ON_FOCUS_OR_SCROLL
                                            SettingsManager.saveGridString(gridId, "snap_behavior", RowSnapBehavior.ON_FOCUS_OR_SCROLL.name)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = rowSnapBehavior == RowSnapBehavior.ON_FOCUS_OR_SCROLL,
                                        onClick = {
                                            rowSnapBehavior = RowSnapBehavior.ON_FOCUS_OR_SCROLL
                                            SettingsManager.saveGridString(gridId, "snap_behavior", RowSnapBehavior.ON_FOCUS_OR_SCROLL.name)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Snap on Focus Shift or Table Scroll", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text("Holds position until another row is tapped or table is scrolled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Header Line Wrap Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Wrap Header Text", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (wrapHeaderLines) "Headers wrap up to 2 lines" else "Headers stay single-line with ellipsis",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = wrapHeaderLines,
                                    onCheckedChange = {
                                        wrapHeaderLines = it
                                        SettingsManager.saveGridBoolean(gridId, "wrap_header", it)
                                    }
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Data Line Wrap Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Wrap Data Cell Text", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (wrapDataLines) "Cell text wraps up to 2 lines max" else "Cell text is truncated with ellipsis",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = wrapDataLines,
                                    onCheckedChange = {
                                        wrapDataLines = it
                                        SettingsManager.saveGridBoolean(gridId, "wrap_data", it)
                                    }
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Reset Column Widths
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Reset Column Widths", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Restore default widths for all columns", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedButton(
                                    onClick = {
                                        columns.forEach {
                                            columnWidthMap[it.id] = it.width
                                            SettingsManager.saveGridInt(gridId, "width_${it.id}", it.width.value.roundToInt())
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = { showPrimaryGridGearDialog = false }) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }

    // =================================================================
    // CUSTOM PRIORITY MODAL DIALOG (With Parent Category Filter)
    // =================================================================
    if (showCustomPriorityDialog) {
        val targetColDef = columns.find { it.id == priorityTargetColumnId } ?: columns.first()
        var selectedParentCategory by remember { mutableStateOf<String?>(masterCategoryNames.firstOrNull()) }

        val distinctValues = remember(items, targetColDef, selectedParentCategory, masterSubcategoriesByCategory) {
            if (targetColDef.id == "subCategory" && selectedParentCategory != null) {
                val masterSub = masterSubcategoriesByCategory[selectedParentCategory] ?: emptyList()
                val itemSub = items.filter { (it as? com.moneytracker.data.local.entity.TransactionWithCategory)?.categoryName.equals(selectedParentCategory, ignoreCase = true) }
                    .map { targetColDef.valueExtractor(it).trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                (masterSub + itemSub).distinct()
            } else {
                val list = items.map { targetColDef.valueExtractor(it).trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                (if (masterCategoryNames.isNotEmpty()) masterCategoryNames + list else list).distinct()
            }
        }

        var editableList by remember(distinctValues) {
            val saved = localPriorityOrders[targetColDef.id] ?: emptyList()
            val ordered = saved.filter { distinctValues.contains(it) } + distinctValues.filter { !saved.contains(it) }
            mutableStateOf(ordered.toMutableList())
        }

        Dialog(
            onDismissRequest = { showCustomPriorityDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .height(560.dp),
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
                                text = "Custom Priority: ${targetColDef.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Drag items up or down to set custom sort rank order",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showCustomPriorityDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    if (targetColDef.id == "subCategory" && masterCategoryNames.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Category:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            masterCategoryNames.forEach { cat ->
                                FilterChip(
                                    selected = selectedParentCategory == cat,
                                    onClick = { selectedParentCategory = cat },
                                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
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
                        modifier = Modifier.fillMaxWidth().weight(1f),
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
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                            modifier = Modifier.draggableHandle()
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

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCustomPriorityDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                localPriorityOrders[targetColDef.id] = editableList
                                onCustomPriorityOrderChanged?.invoke(targetColDef.id, editableList)
                                showCustomPriorityDialog = false
                            }
                        ) {
                            Text("Save Priority Order")
                        }
                    }
                }
            }
        }
    }

    // =================================================================
    // FOOTER SETTINGS DIALOG (Pagination vs Continuous Scroll)
    // =================================================================
    if (showFooterSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showFooterSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Footer & Navigation Settings", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Grid Navigation Mode:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isPaginationEnabled = true
                                SettingsManager.saveGridBoolean(gridId, "is_pagination", true)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isPaginationEnabled,
                            onClick = {
                                isPaginationEnabled = true
                                SettingsManager.saveGridBoolean(gridId, "is_pagination", true)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Pagination Mode", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Fixed number of rows per page with page navigation buttons", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isPaginationEnabled = false
                                SettingsManager.saveGridBoolean(gridId, "is_pagination", false)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isPaginationEnabled,
                            onClick = {
                                isPaginationEnabled = false
                                SettingsManager.saveGridBoolean(gridId, "is_pagination", false)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Continuous Scroll Mode", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Render all data rows in a single scrollable container", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (isPaginationEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Text("Default Page Size:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(5, 10, 20, 50, 0).forEach { sizeOpt ->
                                val isSel = pageSize == sizeOpt
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable {
                                        pageSize = sizeOpt
                                        SettingsManager.saveGridInt(gridId, "page_size", sizeOpt)
                                    }
                                ) {
                                    Text(
                                        text = if (sizeOpt == 0) "All" else "$sizeOpt",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFooterSettingsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
