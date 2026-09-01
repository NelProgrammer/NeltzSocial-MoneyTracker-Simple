package com.moneytracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

/**
 * Enterprise Reusable Generic Sortable & Pilled Grid Component
 *
 * Architecture:
 * 1. Independent Pills Container (with Combo mechanics: collapsible rows, horizontal scroll or wrap, gear config).
 * 2. Independent Column Header Container (positioned above table rows, synchronized horizontal scroll).
 * 3. Independent Table Body Container (data rows).
 * 4. Main Gear Settings Dialog:
 *    - Column Sort Priority (drag-and-drop priority ordering #1, #2, #3 + direction strategies).
 *    - Column Visibility Section (toggle which columns are visible in grid).
 *    - Filter Pills Configuration (pill rows 1-5, source column, A-Z sort).
 *    - Custom Values Priority Manager.
 * 5. Column Headers show Sort Order Badge (#1, #2, ⋮, ○), Direction Arrow (▲, ▼), and Column Gear (⚙).
 */
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
    // 1. Column List & Column Visibility State
    var columnList by remember(columns) { mutableStateOf(columns) }
    val columnVisibilityMap = remember(columns) {
        mutableStateMapOf<String, Boolean>().apply {
            columns.forEach { put(it.id, true) }
        }
    }

    // 2. Column Sort Priority Order & Strategies
    // The order in sortPriorityColumnIds defines Priority 1, Priority 2, etc.
    val sortPriorityColumnIds = remember(columns) {
        mutableStateListOf<String>().apply {
            addAll(columns.map { it.id })
        }
    }
    val columnSortStrategies = remember(columns) {
        mutableStateMapOf<String, ColumnSortStrategy>().apply {
            columns.forEach { put(it.id, it.defaultStrategy) }
        }
    }

    // 3. Dynamic Filter Pills Column Configuration
    var activePillColumnId by remember(columns, initialPillColumnId) {
        mutableStateOf(initialPillColumnId ?: columns.firstOrNull { it.id != "select" && it.id != "actions" }?.id ?: columns.first().id)
    }
    var selectedPillValue by remember { mutableStateOf<String?>(null) }
    var maxPillRows by remember { mutableIntStateOf(1) } // 1, 2, 3, 4, 5
    var isPillsExpanded by remember { mutableStateOf(false) }
    var sortPillsAlphabetical by remember { mutableStateOf(true) }

    // 4. Main Gear Settings Modal State
    var showMainGearDialog by remember { mutableStateOf(false) }
    var mainGearActiveTab by remember { mutableIntStateOf(0) } // 0 = Sort Priority, 1 = Visibility, 2 = Pills, 3 = Custom Values

    // 5. Custom Priority Orders State
    val localPriorityOrders = remember { mutableStateMapOf<String, List<String>>().apply { putAll(customPriorityOrders) } }
    var prioritySortTargetColumn by remember { mutableStateOf<GridColumnDefinition<T>?>(null) }

    // Synchronized Horizontal Scroll State
    val tableHorizontalScrollState = rememberScrollState()

    // Filter visible columns
    val visibleColumns = remember(columnList, columnVisibilityMap.toMap()) {
        columnList.filter { columnVisibilityMap[it.id] != false }
    }

    // Extract unique values for active pill column
    val activePillColumn = columnList.find { it.id == activePillColumnId }
    val distinctPillValues = remember(items, activePillColumn, sortPillsAlphabetical) {
        if (activePillColumn != null) {
            val list = items.map { activePillColumn.valueExtractor(it).trim() }
                .filter { it.isNotBlank() }
                .distinct()
            if (sortPillsAlphabetical) list.sortedBy { it.lowercase() } else list
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

    // Sort Items based on ordered Column Sort Priority and Strategies
    val activeSortedColumnIds = remember(sortPriorityColumnIds.toList(), columnSortStrategies.toMap()) {
        sortPriorityColumnIds.filter { (columnSortStrategies[it] ?: ColumnSortStrategy.NON_SORTING) != ColumnSortStrategy.NON_SORTING }
    }

    val sortedFilteredItems = remember(filteredItems, activeSortedColumnIds, columnSortStrategies.toMap(), localPriorityOrders.toMap(), columnList) {
        if (activeSortedColumnIds.isEmpty()) {
            filteredItems
        } else {
            filteredItems.sortedWith { a, b ->
                var result = 0
                for (colId in activeSortedColumnIds) {
                    val colDef = columnList.find { it.id == colId } ?: continue
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

    // 6. Pagination & Display Mode State
    var isPaginationEnabled by remember { mutableStateOf(true) }
    var pageSize by remember { mutableIntStateOf(10) } // 5, 10, 20, 50, 0 = All
    var currentPage by remember { mutableIntStateOf(1) }
    var showFooterSettingsDialog by remember { mutableStateOf(false) }

    val totalCount = sortedFilteredItems.size
    val totalPages = remember(totalCount, pageSize, isPaginationEnabled) {
        if (!isPaginationEnabled || pageSize <= 0 || totalCount == 0) 1
        else (totalCount + pageSize - 1) / pageSize
    }

    androidx.compose.runtime.LaunchedEffect(totalPages) {
        if (currentPage > totalPages) {
            currentPage = totalPages
        }
        if (currentPage < 1) {
            currentPage = 1
        }
    }

    val startItemIdx = if (totalCount == 0) 0 else if (!isPaginationEnabled || pageSize <= 0) 1 else (currentPage - 1) * pageSize + 1
    val endItemIdx = if (totalCount == 0) 0 else if (!isPaginationEnabled || pageSize <= 0) totalCount else kotlin.math.min(currentPage * pageSize, totalCount)

    val pagedItems = remember(sortedFilteredItems, currentPage, pageSize, isPaginationEnabled) {
        if (!isPaginationEnabled || pageSize <= 0 || totalCount == 0) sortedFilteredItems
        else {
            val from = (currentPage - 1) * pageSize
            val to = kotlin.math.min(from + pageSize, sortedFilteredItems.size)
            if (from < sortedFilteredItems.size) sortedFilteredItems.subList(from, to) else emptyList()
        }
    }

    // OVERARCHING PARENT CONTAINER
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // =================================================================
        // CONTAINER 1: INDEPENDENT FILTER PILLS CONTAINER CARD
        // =================================================================
        if (activePillColumn != null && distinctPillValues.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Pills Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Filter Column Title + Count Badge + Settings Gear
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { 
                                mainGearActiveTab = 2
                                showMainGearDialog = true 
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
                                    mainGearActiveTab = 2
                                    showMainGearDialog = true 
                                },
                                modifier = Modifier.size(24.dp).padding(start = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Pill Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Right: Expand / Collapse Toggle
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
                                contentDescription = if (isPillsExpanded) "Collapse Pills" else "Expand Pills",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pills Content
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

        // =================================================================
        // CONTAINER 2: INDEPENDENT COLUMN HEADER CONTAINER CARD
        // (Placed directly above the grid table body in its own container)
        // =================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tableHorizontalScrollState)
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                visibleColumns.forEach { colDef ->
                    var showColumnGearMenu by remember { mutableStateOf(false) }
                    val currentStrategy = columnSortStrategies[colDef.id] ?: ColumnSortStrategy.NON_SORTING
                    val activePriorityRank = activeSortedColumnIds.indexOf(colDef.id).let { if (it >= 0) it + 1 else null }

                    Row(
                        modifier = Modifier
                            .width(colDef.width)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Column Title Text
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

                        // Sort Icon Set + Column Gear
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // 1. Sort Order Badge (Priority Rank #1, #2, or 3-dots ⋮ for custom, or circle ○ for non-sorting)
                            if (activePriorityRank != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$activePriorityRank",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }

                            // 2. Sort Direction Arrow (▲, ▼) or 3 vertical dots (⋮) or small circle (○)
                            Icon(
                                imageVector = when (currentStrategy) {
                                    ColumnSortStrategy.ASCENDING -> Icons.Default.ArrowUpward
                                    ColumnSortStrategy.DESCENDING -> Icons.Default.ArrowDownward
                                    ColumnSortStrategy.CUSTOM_PRIORITY -> Icons.Default.MoreVert
                                    ColumnSortStrategy.NON_SORTING -> Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = "Sort Direction",
                                tint = if (currentStrategy != ColumnSortStrategy.NON_SORTING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(12.dp)
                            )

                            // 3. Column Gear (⚙) next to sort icon set
                            Box {
                                IconButton(
                                    onClick = { showColumnGearMenu = true },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Column Options",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(13.dp)
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
                                            showColumnGearMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Descending (Z → A / Max → Min)") },
                                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                                        onClick = {
                                            columnSortStrategies[colDef.id] = ColumnSortStrategy.DESCENDING
                                            showColumnGearMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Custom Priority (Rank Values)") },
                                        leadingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                                        onClick = {
                                            columnSortStrategies[colDef.id] = ColumnSortStrategy.CUSTOM_PRIORITY
                                            prioritySortTargetColumn = colDef
                                            showColumnGearMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Non-Sorting (Off)") },
                                        leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null) },
                                        onClick = {
                                            columnSortStrategies[colDef.id] = ColumnSortStrategy.NON_SORTING
                                            showColumnGearMenu = false
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Configure Multi-Column Sort...") },
                                        leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                                        onClick = {
                                            showColumnGearMenu = false
                                            mainGearActiveTab = 0
                                            showMainGearDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Manage Column Visibility...") },
                                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                                        onClick = {
                                            showColumnGearMenu = false
                                            mainGearActiveTab = 1
                                            showMainGearDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =================================================================
        // CONTAINER 3: INDEPENDENT TABLE BODY CONTAINER CARD
        // =================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (pagedItems.isEmpty()) {
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
                    pagedItems.forEachIndexed { rowIdx, item ->
                        val isSelected = selectedItemKeys.contains(itemKey(item))
                        if (rowIdx > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tableHorizontalScrollState)
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
                            visibleColumns.forEach { colDef ->
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

        // =================================================================
        // CONTAINER 4: INDEPENDENT FOOTER CARD (Pagination / Scroll Controls)
        // =================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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

                        // Quick Page Size Chips: 10 | 25 | 50 | All
                        listOf(10, 25, 50, 0).forEach { sizeOption ->
                            val isSelected = pageSize == sizeOption
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null,
                                modifier = Modifier.clickable { pageSize = sizeOption }
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
                        // First Page |◀
                        IconButton(
                            onClick = { currentPage = 1 },
                            enabled = currentPage > 1,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("«", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (currentPage > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        }

                        // Prev Page ◀
                        IconButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("‹", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (currentPage > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        }

                        // Page Indicator Text
                        Text(
                            text = "$currentPage / $totalPages",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        // Next Page ▶
                        IconButton(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = currentPage < totalPages,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("›", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (currentPage < totalPages) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        }

                        // Last Page ▶|
                        IconButton(
                            onClick = { currentPage = totalPages },
                            enabled = currentPage < totalPages,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Text("»", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (currentPage < totalPages) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        }

                        // Footer Settings Gear (⚙)
                        IconButton(
                            onClick = { showFooterSettingsDialog = true },
                            modifier = Modifier.size(26.dp).padding(start = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pagination & Scroll Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    // Continuous Scroll Mode Header / Summary
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
                                contentDescription = "Pagination & Scroll Settings",
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

                    // Option 1: Pagination Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isPaginationEnabled = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isPaginationEnabled,
                            onClick = { isPaginationEnabled = true }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Pagination Mode", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Fixed number of rows per page with page controls", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Option 2: Continuous Scroll Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isPaginationEnabled = false }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isPaginationEnabled,
                            onClick = { isPaginationEnabled = false }
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
                                    modifier = Modifier.clickable { pageSize = sizeOpt }
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

    // =================================================================
    // MAIN GEAR SETTINGS MODAL DIALOG
    // Contains: 1. Column Sort Priority | 2. Column Visibility | 3. Filter Pills
    // =================================================================
    if (showMainGearDialog) {
        Dialog(
            onDismissRequest = { showMainGearDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .height(580.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header
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
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Grid Table Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showMainGearDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tab Row
                    TabRow(selectedTabIndex = mainGearActiveTab) {
                        Tab(
                            selected = mainGearActiveTab == 0,
                            onClick = { mainGearActiveTab = 0 },
                            text = { Text("Sort Priority", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = mainGearActiveTab == 1,
                            onClick = { mainGearActiveTab = 1 },
                            text = { Text("Visibility", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = mainGearActiveTab == 2,
                            onClick = { mainGearActiveTab = 2 },
                            text = { Text("Pills", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // TAB 0: COLUMN SORT PRIORITY (Drag & Drop Column Hierarchy)
                    if (mainGearActiveTab == 0) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Drag columns up or down to set multi-level sort priority (#1, #2, ...). Tap the sort badge to cycle strategy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val lazyListState = rememberLazyListState()
                            val reorderableLazyColumnState = rememberReorderableLazyListState(lazyListState) { from, to ->
                                val item = sortPriorityColumnIds.removeAt(from.index)
                                sortPriorityColumnIds.add(to.index, item)
                            }

                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(sortPriorityColumnIds, key = { _, colId -> colId }) { index, colId ->
                                    val colDef = columnList.find { it.id == colId }
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
                                                        // Priority Rank Badge
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
                                                        // Strategy Selector Button
                                                        Button(
                                                            onClick = {
                                                                // Cycle strategy: NON_SORTING -> ASCENDING -> DESCENDING -> CUSTOM_PRIORITY -> NON_SORTING
                                                                columnSortStrategies[colId] = when (currentStrat) {
                                                                    ColumnSortStrategy.NON_SORTING -> ColumnSortStrategy.ASCENDING
                                                                    ColumnSortStrategy.ASCENDING -> ColumnSortStrategy.DESCENDING
                                                                    ColumnSortStrategy.DESCENDING -> ColumnSortStrategy.CUSTOM_PRIORITY
                                                                    ColumnSortStrategy.CUSTOM_PRIORITY -> ColumnSortStrategy.NON_SORTING
                                                                }
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

                                                        // Drag Grip
                                                        IconButton(
                                                            onClick = {},
                                                            modifier = Modifier.draggableHandle()
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.DragHandle,
                                                                contentDescription = "Drag to reorder priority",
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

                    // TAB 1: COLUMN VISIBILITY SECTION
                    if (mainGearActiveTab == 1) {
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
                                itemsIndexed(columnList, key = { _, col -> col.id }) { _, col ->
                                    val isVisible = columnVisibilityMap[col.id] != false
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { columnVisibilityMap[col.id] = !isVisible }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(col.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Switch(
                                                checked = isVisible,
                                                onCheckedChange = { columnVisibilityMap[col.id] = it }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: FILTER PILLS CONFIGURATION
                    if (mainGearActiveTab == 2) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Row Stepper
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Pill Rows (1–5)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
                                        onClick = { if (maxPillRows > 1) maxPillRows-- }
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
                                        onClick = { if (maxPillRows < 5) maxPillRows++ }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Add, contentDescription = "Increment", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Sort Pills A-Z
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sort Pills A-Z", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Switch(
                                    checked = sortPillsAlphabetical,
                                    onCheckedChange = { sortPillsAlphabetical = it }
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // Pill Column Driver
                            Text("Source Column for Pills:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(columnList) { _, col ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { 
                                                activePillColumnId = col.id
                                                selectedPillValue = null
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = activePillColumnId == col.id,
                                            onClick = {
                                                activePillColumnId = col.id
                                                selectedPillValue = null
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(col.title, style = MaterialTheme.typography.bodyMedium)
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
                        Button(onClick = { showMainGearDialog = false }) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }

    // =================================================================
    // EDITABLE PRIORITY SORTING POPUP DIALOG (For Custom Priority values)
    // =================================================================
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
                                text = "Custom Priority: ${targetCol.title}",
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
