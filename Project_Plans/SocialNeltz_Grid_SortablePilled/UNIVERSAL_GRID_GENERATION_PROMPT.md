# Master Prompt: Generate the Universal Sortable Pilled Grid Component in Any Language / Framework

Use this master prompt with any AI coding assistant, LLM, or engineering team to implement the production-grade **Universal Sortable Pilled Grid Component** (`SocialNeltz_Grid_Sortable_Pilled<T>`) in any programming language or UI framework (such as React / TypeScript, SwiftUI, Flutter, Vue 3, Svelte 5, Angular, Jetpack Compose, C# / .NET MAUI, or Python / PyQt).

---

```markdown
# Role & Task
You are a Principal UI/UX Architect and Software Engineer. Your task is to implement an enterprise-grade, generic **Sortable Pilled Grid Component** (`SocialNeltz_Grid_Sortable_Pilled<T>`) in [TARGET LANGUAGE / FRAMEWORK].

---

## 1. Core Feature Requirements

### A. Dynamic Pill Filter Container with Dual-Action Gear
- **Pill Container**: Displays horizontal scrollable pills at the top of the grid for quick-filtering distinct dataset values.
- **Top Gear Trigger (⚙️)**:
  - **Single Click**: Opens the **Editable Priority Sorting Popup Tab** where users can drag/reposition items to establish manual sort priorities.
  - **Click & Hold (Long-Press)**: Activates **Column Drag-and-Drop Reordering Mode**, allowing column repositioning.

### B. Multi-Strategy Column Header Sorting
Each column header must provide an interactive sort toggle and a mini-gear strategy selector:
1. **Ascending (`ASC`)**: Standard alphanumeric or numeric ascending order.
2. **Descending (`DESC`)**: Standard alphanumeric or numeric descending order.
3. **Non-Sorting (`NONE`)**: Raw natural order without sorting overhead.
4. **Custom Priority (`CUSTOM_PRIORITY`)**: Sorts according to the manual rank hierarchy established in the Priority Sorting Tab.

### C. Editable Priority Sorting Popup Tab
- Modal tab providing an interactive, reorderable list of distinct values (e.g. Categories, Subcategories).
- Users drag or move items up and down to define `Priority 1`, `Priority 2`, etc.
- These priority ranks immediately govern sorting whenever `CUSTOM_PRIORITY` is selected for that column.

### D. Visual Date Column Omission with Preserved Data Integrity
- The Date column is omitted from the grid table body visually (since global date pills provide temporal context).
- Underlying row item objects (`T`) preserve all date timestamps for filtering, grouping, and exports.

---

## 2. Generic TypeScript / Kotlin API Contract

```typescript
export type ColumnSortStrategy = 'ASC' | 'DESC' | 'NONE' | 'CUSTOM_PRIORITY';

export interface GridColumnConfig<T> {
  key: string;
  headerTitle: string;
  widthFraction?: number;
  isSortable?: boolean;
  defaultSortStrategy?: ColumnSortStrategy;
  valueExtractor: (item: T) => string;
  numericExtractor?: (item: T) => number;
  renderCell: (item: T) => ReactNode; // or framework-equivalent view
}

export interface SocialNeltzGridSortablePilledProps<T> {
  items: T[];
  columns: GridColumnConfig<T>[];
  pillExtractor?: (item: T) => string;
  onItemClick?: (item: T) => void;
  priorityRanks?: Record<string, number>;
  onSavePriorityRanks?: (ranks: Record<string, number>) => void;
}
```

---

## 3. Implementation Instructions
1. Ensure full accessibility, smooth keyboard navigation, and responsive touch/mouse gestures.
2. Support animated column reordering and sleek glassmorphism / modern Material 3 / Tailwind CSS design aesthetics.
3. Write clean, modular, zero-leak code with stateful and stateless separation.
```
