# Architecture Specification: SocialNeltz_Grid_Sortable_Pilled

## 1. Overview & Purpose
`SocialNeltz_Grid_Sortable_Pilled<T>` is a reusable, enterprise-grade, generic data table component designed for rich interactive data presentation, agile multi-column sorting, column reordering, quick-pick pill filtering, and user-defined priority sorting.

---

## 2. Key Architecture Pillars

### A. Quad Independent Container Architecture
- **Overarching Container**: Large parent container that encapsulates four independent child containers:
  1. **Container 1: Filter Pills Card**:
     - Dedicated styled Card with rounded corners and border.
     - Header displays Active Filter Column Title, count badge, Settings Gear (⚙), and Expand/Collapse toggle (`▲` / `▼`).
     - Implements **Combo Pill Mechanics**:
       - Stepper configuration for display rows (1 to 5 rows).
       - Single-row horizontal scrolling when configured to 1 row.
       - Multi-line wrap (`FlowRow` with `maxLines`) when configured to 2+ rows.
       - Fully expanded multi-row wrap when toggled open.
  2. **Container 2: Column Header Card**:
     - Placed above the grid rows in its own container card.
     - Synchronized horizontal scrolling with data rows.
     - Every column header shows Title, Sort Order Badge (`#1`, `#2`, `⋮`, `○`), Sort Direction Indicator (`▲`, `▼`), and Column Gear (⚙).
  3. **Container 3: Table Body Card**:
     - Houses the scrollable paged data rows.
  4. **Container 4: Footer Controls Card**:
     - **Footer Settings Gear (⚙)**: Opens dialog to switch between **Pagination Mode** and **Continuous Scroll Mode**, and select default page size.
     - **In Pagination Mode**: Displays item range summary (`Showing 1–10 of 45 items`), quick page size toggle chips (`10`, `25`, `50`, `All`), and navigation controls (`«`, `‹`, `Page 1 / 5`, `›`, `»`).
     - **In Continuous Scroll Mode**: Displays total item summary (`Total: 45 items • Continuous Scroll Mode`) and renders all rows in a single scrollable view.

### B. Main Gear Centralized Sort Priority & Visibility Management
- **Tab 0: Sort Priority**: Drag-and-drop hierarchy to establish multi-level priority (`#1`, `#2`, `#3`, ...) and strategy per column (`ASC`, `DESC`, `CUSTOM`, `OFF`).
- **Tab 1: Visibility**: Toggle switches to show/hide any column dynamically.
- **Tab 2: Filter Pills**: Configure pill rows (1–5), sort pills A-Z, and select source column.

### C. Row Action & Deletion Safety Policy
- **No Long-Press Row Deletion**: Clicking and holding table rows does NOT delete table items.
- **AddEditScreen Only**: Table item deletion is strictly confined to the dedicated edit screen (`AddEditTransactionsScreen`), preventing accidental deletions.

### D. Editable Priority Sorting Popup Tab
- A dedicated modal dialog providing a reorderable list of distinct values (e.g., Categories, Subcategories, or Details).
- Supports drag handles / up-down repositioning to establish hard-coded rank priorities (`Priority 1`, `Priority 2`, ...).

---

## 3. Data Models & Component State

```kotlin
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
```
