# Sorting, Reordering & Priority Engine Specification

## 1. Overview
This document specifies the algorithmic logic, state management, and interaction mechanics governing multi-column sort priority, column visibility management, and sort badge rendering in `SocialNeltz_Grid_Sortable_Pilled`.

---

## 2. Multi-Column Sort Priority Engine (Main Gear)

### Architecture:
- **Centralized Priority Management**: Column sorting priority is managed within the **Main Grid Gear** dialog (Tab 0: "Sort Priority").
- **Drag-and-Drop Hierarchy**: Users drag columns up or down to set multi-level priority (`#1`, `#2`, `#3`, ...).
- **Per-Column Strategy Selection**: Each column in the priority list can be toggled through:
  - 🔼 **`ASC`**: Ascending order (A → Z / Min → Max)
  - 🔽 **`DESC`**: Descending order (Z → A / Max → Min)
  - ⋮ **`CUSTOM`**: Custom Priority (order defined in Custom Values manager)
  - ○ **`OFF`**: Non-Sorting (excluded from active multi-column sorting)

### Multi-Level Sort Algorithm:
```kotlin
fun <T> multiColumnSort(
    items: List<T>,
    activePriorityColumnIds: List<String>,
    columnList: List<GridColumnDefinition<T>>,
    strategies: Map<String, ColumnSortStrategy>,
    customOrders: Map<String, List<String>>
): List<T> {
    if (activePriorityColumnIds.isEmpty()) return items

    return items.sortedWith { a, b ->
        var comparison = 0
        for (colId in activePriorityColumnIds) {
            val colDef = columnList.find { it.id == colId } ?: continue
            val strat = strategies[colId] ?: ColumnSortStrategy.NON_SORTING
            val valA = colDef.valueExtractor(a).trim()
            val valB = colDef.valueExtractor(b).trim()

            comparison = when (strat) {
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
                    val order = customOrders[colId] ?: emptyList()
                    val idxA = order.indexOfFirst { it.equals(valA, ignoreCase = true) }.let { if (it == -1) Int.MAX_VALUE else it }
                    val idxB = order.indexOfFirst { it.equals(valB, ignoreCase = true) }.let { if (it == -1) Int.MAX_VALUE else it }
                    idxA.compareTo(idxB)
                }
                ColumnSortStrategy.NON_SORTING -> 0
            }
            if (comparison != 0) break
        }
        comparison
    }
}
```

---

## 3. Column Header Badges & Indicator Layout

Each column header renders:
1. **Column Title Text**
2. **Sort Order Badge**:
   - Displays priority rank `#1`, `#2`, `#3` if active in multi-column sorting.
3. **Sort Direction Indicator**:
   - `▲` (Up Arrow) for Ascending
   - `▼` (Down Arrow) for Descending
   - `⋮` (3 Vertical Dots) for Custom Priority / Non-directional
   - `○` (Small Circle) for Non-Sorting
4. **Column Gear (⚙)**: Opens quick strategy dropdown and shortcuts to Main Gear settings.

---

## 4. Column Visibility Management (Main Gear Tab 1)
- Dedicated Visibility Tab in the Main Gear modal.
- Provides switches to show or hide any column dynamically without altering data structures.

---

## 5. Synchronized Horizontal Scroll Container
- The **Column Header Container** and **Table Body Container** share a single `horizontalScrollState`.
- Dragging / swiping horizontally across the Column Header Row moves table columns and data rows in perfect synchronization.
