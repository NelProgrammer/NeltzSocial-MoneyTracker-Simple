# Master Prompt: Generate NeltzSocial_Combo_PilledFilteredCruded & Cascading Orchestrator in Any Language / Framework

Use this master prompt with any AI coding assistant, LLM, or software engineering team to implement the production-grade **Autonomous Managed Combobox with Pills Component** (`NeltzSocial_Combo_PilledFilteredCruded<T>`) and its **Cascading Orchestrator Tool** in any programming language or UI framework (such as React / TypeScript, SwiftUI, Flutter, Vue 3, Svelte 5, Angular, Jetpack Compose, C# / .NET MAUI, or Python / PyQt).

---

```markdown
# Role & Task
You are a Principal Software Architect and UI Framework Engineer. Your task is to implement an enterprise-grade, generic **Managed Combobox with Quick-Pick Pills Component** (`NeltzSocial_Combo_PilledFilteredCruded<T>`) and its accompanying **Cascading Combobox Orchestrator Tool** (`CascadingComboboxOrchestrator<TCat, TSub, TDet>`) in:
👉 **TARGET LANGUAGE / FRAMEWORK**: [INSERT TARGET FRAMEWORK HERE, e.g., React + TypeScript / SwiftUI / Flutter / Jetpack Compose / Vue 3 + TypeScript / Svelte 5 / C# .NET MAUI]

The implementation MUST adhere strictly to the decoupled architecture, state machines, behavioral contracts, and algorithms detailed below.

---

## 1. Architectural Model & Separation of Concerns

The architecture strictly separates UI rendering from hierarchical state orchestration:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        SCREEN / VIEW CONTROLLER                        │
│  - Host Container: Instantiates Orchestrator with domain datasets      │
│  - Mounts UI Comboboxes and connects them to the Orchestrator          │
└──────────────────┬─────────────────────────────────┬───────────────────┘
                   │                                 │
                   ▼ (1. Initializes)                ▼ (2. Mounts)
┌────────────────────────────────────┐ ┌──────────────────────────────────────────┐
│     CascadingComboboxOrchestrator  │ │   NeltzSocial_Combo_PilledFilteredCruded │
│   (State & Cascading Engine Tool)  │ │      (Pure Generic UI Widget)            │
│                                    │ │                                          │
│  - Coordinates the Combos' updates │◄┼── (3. Emits onValueChange events         │
│  - Resolves active matched parents │ │       when user selects/types)           │
│  - Handles cascade resets & rules  │─┼─► (4. Feeds state, items &               │
│  - Computes dynamic predicates     │ │       active predicates)                 │
└────────────────────────────────────┘ └──────────────────────────────────────────┘
```

1. **`NeltzSocial_Combo_PilledFilteredCruded<T>` (UI Primitive)**:
   * 100% generic (`<T>`), dumb, and reusable in single-tier or multi-tier contexts.
   * Completely unaware of the Orchestrator or sibling comboboxes.
   * Receives `items: List<T>`, `selectedValue: String`, `parentFilterKey: Any?`, and `filterPredicate: ((T) -> Boolean)?`.
2. **`CascadingComboboxOrchestrator` (Screen Tool)**:
   * Reusable state holder and predicate evaluator instantiated by the screen.
   * Completely unaware of UI widgets or rendering elements.
   * Coordinates parent entity matching, active selection text, and cascading validation rules.
3. **Screen / View Controller (Glue)**:
   * Instantiates the Orchestrator with domain data and binds its properties to the dumb Combobox instances.

---

## 2. Core Functional Requirements for `NeltzSocial_Combo_PilledFilteredCruded<T>`

### A. Autonomous Cascading Invalidation
* The component receives the full raw domain entity dataset (`items: List<T>`), an optional parent tracker (`parentFilterKey`), and a filtering rule (`filterPredicate: (T) -> Boolean`).
* Internally computes `parentFilteredItems = (filterPredicate != null) ? items.filter(filterPredicate) : items`.
* If `parentFilterKey` changes and makes the current `selectedValue` invalid within `parentFilteredItems`, the component **autonomously resets itself** (`onValueChange("")`).
* On initial mounting / edit-mode loading with pre-existing records, pre-populated values **must be preserved** without premature wipeouts.

### B. Instant Overwrite & Text Editing
* When focusing the input field containing an existing value, the component selects the entire text range `[0..length]`.
* Typing any new character immediately replaces/overwrites the selected text without requiring manual backspacing.
* A clear **(✕)** trailing icon provides one-tap clearing and immediately opens the dropdown for a new selection.

### C. Live Search & Non-Blocking Typing
* Typing into the input field filters dropdown suggestions in real-time.
* The dropdown popup must be non-modal / non-focus-stealing so soft-keyboard focus is never lost while typing.
* The dropdown list displays a default first option: `"Select a: [label] item"` (which clears the selection), followed by the scrollable list of filtered items.

### D. Dedicated Quick-Pick Pills Card vs. CRUD Add Button
* **CRUD Add Button `(+)`**: Located next to the input field, strictly for opening a modal/dialog to create a new entity in the database.
* **Settings Gear `(⚙)`**: Opens a personalization dialog (pill row count, alphabetical vs. ID sort, visible item count, scroll step).
* **Dedicated Pills Card**:
  * Located below the input field.
  * Header shows `"Quick Pick [count]"` with an Expand/Collapse toggle (`▲` / `▼`).
  * Collapsed View (Default): Constrains pills to the configured row count (1 or 2 rows).
  * Expanded View: Wraps and shows all available items.
  * Tapping a pill selects it. Long-pressing or settings mode exposes inline Edit `(✎)` and Delete `(✕)` actions on the pill chip.

### E. Smooth Dual-Expansion ("Either / Or")
* The dropdown menu MUST expand smoothly when:
  1. Tapping / clicking anywhere on the text field or focusing it.
  2. Clicking the trailing drop-down arrow icon.
  3. Typing any character in the input field.
* Clicking the trailing dropdown arrow must NOT double-toggle or cancel expansion.

---

## 3. Core Functional Requirements for `CascadingComboboxOrchestrator`

The Orchestrator provides:
1. **Multi-Tier Reactive State**: Holds bound string values (`categoryText`, `subCategoryText`, `detailText`).
2. **Active Parent Entity Resolution**: Computes `matchedCategory: TCat?`, `matchedSubCategory: TSub?`, and `matchedDetail: TDet?` by matching text and parent IDs.
3. **Dynamic Filtering Predicates**:
   * `isSubCategoryValid(sub: TSub): Boolean` -> Checks if child entity belongs to `matchedCategory`.
   * `isDetailValid(det: TDet): Boolean` -> Checks if grandchild entity belongs to `matchedSubCategory`.
4. **Lightweight String-Based Variant**:
   * `SimpleStringComboboxOrchestrator` for screens operating directly on plain strings and map-based lookup tables.

---

## 4. API Contract Signatures

### 4.1 `NeltzSocial_Combo_PilledFilteredCruded<T>` Signature

```typescript
interface ComboboxSettings {
  pillRows: number;            // Default 1
  maxVisibleItems: number;     // Default 5
  scrollStep: number;          // Default 3
  isAlphabeticalSort: boolean; // Default false
}

interface NeltzSocialComboPilledFilteredCrudedProps<T> {
  label: string;
  selectedValue: string;
  onValueChange: (value: string) => void;
  items: T[];
  filterPredicate?: (item: T) => boolean;
  parentFilterKey?: any;
  autoResetOnParentChange?: boolean; // Default true
  itemToText?: (item: T) => string;
  initialSettings?: ComboboxSettings;
  onSettingsChange?: (settings: ComboboxSettings) => void;
  onAddItem?: () => void;
  onEditItem?: (item: T) => void;
  onDeleteItem?: (item: T) => void;
}
```
```
