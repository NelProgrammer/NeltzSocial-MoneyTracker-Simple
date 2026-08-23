# Master Prompt: Generate the Autonomous Managed Combobox with Pills in Any Language / Framework

Use the prompt below with any AI coding assistant or software engineer to generate the **Autonomous Managed Combobox with Pills Component** in any programming language or UI framework (such as React, SwiftUI, Flutter, Vue 3, Svelte, Angular, Jetpack Compose, or C#/.NET MAUI).

---

```markdown
# Role & Task
You are a Principal Software Architect and UI Component Engineer. Your task is to implement an enterprise-grade, autonomous, generic **Managed Combobox with Quick-Pick Pills Component** (`ManagedComboboxWithPills<T>`) and its parent orchestrator pattern in [INSERT TARGET LANGUAGE / FRAMEWORK HERE (e.g. React/TypeScript, SwiftUI, Flutter, Vue 3, Svelte, Angular, Jetpack Compose, C#/.NET MAUI)].

The component MUST adhere strictly to the behavioral contracts, state machines, and cascading algorithms defined in this specification.

---

## 1. Core Architectural Principles

1. **Autonomous Cascading Invalidation & Dynamic Entity Sourcing**:
   * **No Hardcoded Categories**: All categories (Income, Expense, Investment, Asset, etc.) are 100% dynamic domain entities loaded from and saved to the database. They can be created, edited/renamed, or deleted at runtime.
   * The component receives the full, raw domain entity dataset (`items: List<T>`), a parent identifier (`parentFilterKey`), and a filtering rule (`filterPredicate: (T) -> Boolean`).
   * The component **internally** computes its filtered subset (`parentFilteredItems = items.filter(filterPredicate)`).
   * If the parent context changes and makes the component's current `selectedValue` invalid, the component **autonomously clears itself** (`onValueChange("")`) without requiring manual reset logic in the screen controller.
   * On initial component mounting (e.g., in Edit Mode with pre-populated records), the component **must preserve** valid pre-populated selections without triggering premature wipeouts.

2. **Instant Overwrite & Smooth Text Editing**:
   * When an existing item is selected and the user focuses the text input, the component must automatically **select all text** (`[0..length]`).
   * Typing any character on the keyboard must **immediately replace/overwrite** the selected text without requiring manual backspacing.
   * An integrated **Clear (✕)** button must be available in the input field's trailing icons for one-tap clearing.

3. **Non-Blocking Continuous Typing & Live Dropdown Search**:
   * Typing into the text field must never drop soft-keyboard focus or recreate the popup window on every keystroke.
   * The dropdown menu popup must be configured as non-focusable (`focusable = false` / non-modal) so that the soft-keyboard connection remains active while the dropdown live-filters results matching the typed query.
   * An exact match in the text field displays the full list or matching pills; typing a partial query filters the dropdown in real-time.

4. **Dedicated Pills Card vs. CRUD Add Button Separation**:
   * The `(+)` button next to the input field is **strictly for CRUD item addition** (opening a creation dialog). It must NOT be used for expanding/collapsing lists.
   * Below the input field, render a dedicated **Quick-Pick Pills Card**:
     * **Card Header**: Displays `"Quick Pick [count]"` with an Expand/Collapse toggle button (`▲` / `▼`).
     * **Collapsed View (Default)**: Strictly constrains the pills to the configured row limit (e.g., 1 or 2 rows).
     * **Expanded View**: Wraps and displays all available pills in full multi-row layout.
     * **Pill Interactions**: Tapping a pill selects it (`onValueChange(itemText)`). Long-pressing or setting toggles show inline Edit `(✎)` and Delete `(✕)` buttons for direct management.

5. **Integrated Settings Management `(⚙)`**:
   * A gear icon in the input field header opens a personalization dialog allowing users to configure:
     * Number of visible pill rows (1 or 2).
     * Sorting mode (Alphabetical vs. Insertion / ID order).
     * Maximum visible dropdown items before scrolling.
     * Scroll step speed.

---

## 2. Generic Component Interface & Parameter Contract

Implement the generic component with the following signature:

```
ManagedComboboxWithPills<T>(
    label: String,                               // Display label (e.g. "Category", "SubCategory")
    selectedValue: String,                       // Current bound text value
    onValueChange: (String) -> Unit,             // Value update & reset callback
    items: List<T>,                              // Full raw entity collection
    filterPredicate: ((T) -> Boolean)? = null,   // Parent membership rule
    parentFilterKey: Any? = null,                // Parent tracker key (triggers filter re-eval)
    autoResetOnParentChange: Boolean = true,     // Autonomous reset flag
    itemToText: (T) -> String,                   // Entity to display string extractor
    onAddItem: (() -> Unit)? = null,             // CRUD: Add new entity callback
    onEditItem: ((T) -> Unit)? = null,           // CRUD: Edit existing entity callback
    onDeleteItem: ((T) -> Unit)? = null          // CRUD: Delete entity callback
)
```

---

## 3. Algorithmic State Machines to Implement

### A. Focus & Overwrite Engine:
```text
ON FocusGained:
    IF selectedValue is NOT EMPTY:
        Set TextSelection = [0 .. Length(selectedValue)]

ON TextInput(newChar):
    If TextSelection spanned entire text:
        Replace entire text with newChar
    Else:
        Append/insert newChar normally
    Emit onValueChange(newText)
```

### B. Autonomous Cascading Reset Engine:
```text
ON (items, filterPredicate, parentFilterKey) Changed:
    parentFilteredItems = (filterPredicate != null) ? items.filter(filterPredicate) : items
    
    IF isInitialMount == TRUE:
        isInitialMount = FALSE
        RETURN
        
    IF selectedValue is NOT EMPTY:
        IF parentFilteredItems does NOT contain any item where itemToText(item) == selectedValue:
            Emit onValueChange("") // Self-reset cascades down
```

---

## 4. Required Deliverables

1. **Component Source Code**: Complete, production-ready, typed component code in the target language/framework.
2. **State Management Hook / Controller**: Implementation of focus selection, dropdown visibility, search filtering, and pill row limiting.
3. **Usage Example**: A complete 3-tier cascading example (`Category` → `SubCategory` → `Detail`) demonstrating how the parent screen orchestrates the flow without writing manual reset boilerplate.
```
