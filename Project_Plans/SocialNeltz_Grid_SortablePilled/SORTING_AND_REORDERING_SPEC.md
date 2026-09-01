# Sorting, Reordering & Priority Engine Specification

## 1. Overview
This document specifies the algorithmic logic, state management, and gesture mechanics governing multi-column sorting, drag-and-drop column reordering, and priority ranking in `SocialNeltz_Grid_Sortable_Pilled`.

---

## 2. Priority Sorting Engine

### Algorithm:
When a column is sorted with `CUSTOM_PRIORITY`:
1. Retrieve the `priorityMap: Map<String, Int>` for that column's target attribute.
2. For each row item `T`:
   - Extract string key: `val key = column.valueExtractor(item).trim().lowercase()`
   - Lookup rank: `val rank = priorityMap[key] ?: Int.MAX_VALUE`
3. Sort items primarily by `rank ASC`.
4. Secondary sort items with identical ranks by their natural alphanumeric or insertion order.

```kotlin
fun <T> sortDataByPriority(
    items: List<T>,
    column: GridColumnConfig<T>,
    priorityMap: Map<String, Int>,
    isAscending: Boolean = true
): List<T> {
    return items.sortedWith(Comparator { a, b ->
        val keyA = column.valueExtractor(a).trim().lowercase()
        val keyB = column.valueExtractor(b).trim().lowercase()
        val rankA = priorityMap[keyA] ?: (Int.MAX_VALUE - 1)
        val rankB = priorityMap[keyB] ?: (Int.MAX_VALUE - 1)

        val rankComp = rankA.compareTo(rankB)
        if (rankComp != 0) {
            if (isAscending) rankComp else -rankComp
        } else {
            keyA.compareTo(keyB)
        }
    })
}
```

---

## 3. Long-Press Column Drag & Drop Reordering

### Gesture Mechanics:
- **Long-Press Detector**: Detected via pointer input / long-press gesture on the main gear icon (`combinedClickable(onLongClick = { ... })`).
- **Visual Feedback**:
  - Grid enters `isReorderMode = true`.
  - Column headers display subtle drag grip handles `⋮⋮` and elevation changes.
- **Reordering Handler**:
  - `onMoveColumn(fromIndex: Int, toIndex: Int)` updates the observable column order list.
  - Persists column order indices to local state / settings.

---

## 4. Multi-Strategy Header Dialog

### Header Gear Click Workflow:
1. User clicks the mini gear icon located on any column header `[Category ⚙]`.
2. A popup modal presents 4 selectable options:
   - 🔼 **Ascending (A → Z / 0 → 9)**
   - 🔽 **Descending (Z → A / 9 → 0)**
   - ⏸️ **Non-Sorting (Reset / Default)**
   - ⭐ **Custom Priority Order (Use Ranked Order)**
3. If "Custom Priority" is selected without prior configured ranks, prompt to open the Priority Tab editor.
