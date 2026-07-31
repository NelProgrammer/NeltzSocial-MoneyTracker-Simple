package com.moneytracker.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.RecurrenceFrequency
import com.moneytracker.data.local.entity.SubCategoryEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.ui.viewmodel.AddEditViewModel
import com.moneytracker.ui.viewmodel.CategoriesViewModel
import com.moneytracker.ui.viewmodel.SubCategoriesViewModel
import com.moneytracker.ui.viewmodel.ViewModelFactory
import com.moneytracker.util.DateUtils
import com.moneytracker.util.sortedByPriority
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditScreen(
    viewModel: AddEditViewModel,
    repository: TransactionRepository,
    title: String,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Category ViewModel & State
    val categoriesViewModel: CategoriesViewModel = viewModel(factory = ViewModelFactory(repository))
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    val categories by categoriesViewModel.categories.collectAsState()
    var showCategoryDialog by remember { mutableStateOf(false) }

    // SubCategory ViewModel & State (Cloned Category behaviors)
    val subCategoriesViewModel: SubCategoriesViewModel = viewModel(factory = ViewModelFactory(repository))
    var editingSubCategory by remember { mutableStateOf<SubCategoryEntity?>(null) }
    val dbSubCategories by subCategoriesViewModel.subCategories.collectAsState()
    var showSubCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1. Transaction Type filter chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = state.type == TransactionType.INCOME,
                    onClick = { viewModel.updateType(TransactionType.INCOME) },
                    label = { Text("Income") }
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = state.type == TransactionType.INVESTMENT,
                    onClick = { viewModel.updateType(TransactionType.INVESTMENT) },
                    label = { Text("Investment") }
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { viewModel.updateType(TransactionType.EXPENSE) },
                    label = { Text("Expense") }
                )
            }

            // Divider 1: After Transaction Type
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.5.dp
            )

            // 2. Category Section (Dropdown + Add Button + Commonly Used Category Chips in FlowRow)
            var categoryExpanded by remember { mutableStateOf(false) }
            val selectedCategory = categories.find { it.id == state.categoryId }
            val selectedCategoryText = selectedCategory?.name ?: ""
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedCategoryText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        for (cat in categories.filter { it.type == state.type }.sortedByPriority()) {
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { editingCategory = cat; showCategoryDialog = true }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                                        IconButton(onClick = { deletingCategory = cat; categoriesViewModel.deleteCategory(cat) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                                    }
                                },
                                onClick = {
                                    viewModel.updateCategory(cat.id)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { editingCategory = null; showCategoryDialog = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Category")
                }
            }

            // Commonly used category chips in FlowRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                for (cat in categories.filter { it.type == state.type }.sortedByPriority().take(6)) {
                    FilterChip(
                        selected = state.categoryId == cat.id,
                        onClick = { viewModel.updateCategory(cat.id) },
                        label = { Text(cat.name) }
                    )
                }
            }

            // Divider 2: After Category Section
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.5.dp
            )

            // 3. SubCategory Section (Exact clone of Category section: Dropdown + Add/Edit/Delete + FlowRow)
            var subCategoryExpanded by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = subCategoryExpanded,
                    onExpandedChange = { subCategoryExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = state.subCategory,
                        onValueChange = viewModel::updateSubCategory,
                        label = { Text("SubCategory (optional)") },
                        placeholder = { Text("e.g. Living Expenses, Debts, Insurances") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = subCategoryExpanded,
                        onDismissRequest = { subCategoryExpanded = false }
                    ) {
                        for (subCat in dbSubCategories) {
                            DropdownMenuItem(
                                text = { Text(subCat.name) },
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { editingSubCategory = subCat; showSubCategoryDialog = true }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                                        IconButton(onClick = { subCategoriesViewModel.deleteSubCategory(subCat) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                                    }
                                },
                                onClick = {
                                    viewModel.updateSubCategory(subCat.name)
                                    subCategoryExpanded = false
                                }
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { editingSubCategory = null; showSubCategoryDialog = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add SubCategory")
                }
            }

            // Commonly used subcategory chips in FlowRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                for (subCat in dbSubCategories.take(8)) {
                    FilterChip(
                        selected = state.subCategory == subCat.name,
                        onClick = { viewModel.updateSubCategory(subCat.name) },
                        label = { Text(subCat.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Divider 3: After SubCategory Section
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.5.dp
            )

            // 4. Amount & Date Fields
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = viewModel::updateAmount,
                    label = { Text("Amount") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    prefix = { Text("R") }
                )

                OutlinedTextField(
                    value = DateUtils.formatDate(DateUtils.toEpochMillis(state.date)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance().apply {
                                set(state.date.year, state.date.monthValue - 1, state.date.dayOfMonth)
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    viewModel.updateDate(java.time.LocalDate.of(year, month + 1, day))
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Filled.DateRange, contentDescription = "Pick Date")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Divider 4: Before Recurrence Section
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.5.dp
            )

            // Recurrence Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recurring Transaction",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Switch(
                    checked = state.isRecurring,
                    onCheckedChange = viewModel::updateIsRecurring
                )
            }

            if (state.isRecurring) {
                // Frequency Chips (Monthly / Weekly / FortNightly / Daily)
                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RecurrenceFrequency.values().forEach { freq ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = state.recurrenceFrequency == freq,
                            onClick = { viewModel.updateRecurrenceFrequency(freq) },
                            label = { Text(freq.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Recur Till Date & Recur Count
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tillText = state.recurTillDate?.let { DateUtils.formatDate(DateUtils.toEpochMillis(it)) } ?: ""
                    OutlinedTextField(
                        value = tillText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Recur Till Date") },
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = Calendar.getInstance()
                                if (state.recurTillDate != null) {
                                    cal.set(state.recurTillDate!!.year, state.recurTillDate!!.monthValue - 1, state.recurTillDate!!.dayOfMonth)
                                }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        viewModel.updateRecurTillDate(java.time.LocalDate.of(year, month + 1, day))
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Filled.DateRange, contentDescription = "Pick End Date")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = state.recurCount,
                        onValueChange = viewModel::updateRecurCount,
                        label = { Text("Recur Count") },
                        placeholder = { Text("e.g. 12") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Note Field
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::updateNote,
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 6. Save Button
            Button(
                onClick = { viewModel.save(onNavigateBack) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(if (state.isSaving) "Saving..." else "Save")
            }
        }

        // Add / Edit Category Dialog
        if (showCategoryDialog) {
            var name by remember(editingCategory) { mutableStateOf(editingCategory?.name ?: "") }
            var type by remember(editingCategory) { mutableStateOf(editingCategory?.type ?: state.type) }
            AlertDialog(
                onDismissRequest = { showCategoryDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text(if (editingCategory == null) "Add Category" else "Edit Category") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            TransactionType.values().forEach { t ->
                                FilterChip(
                                    selected = type == t,
                                    onClick = { type = t },
                                    label = { Text(t.name) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        categoriesViewModel.startEdit(editingCategory)
                        categoriesViewModel.updateName(name)
                        categoriesViewModel.updateType(type)
                        categoriesViewModel.saveCategory { showCategoryDialog = false }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCategoryDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Add / Edit SubCategory Dialog (Cloned from Category Dialog)
        if (showSubCategoryDialog) {
            var subName by remember(editingSubCategory) { mutableStateOf(editingSubCategory?.name ?: "") }
            AlertDialog(
                onDismissRequest = { showSubCategoryDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text(if (editingSubCategory == null) "Add SubCategory" else "Edit SubCategory") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = subName,
                            onValueChange = { subName = it },
                            label = { Text("SubCategory Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        subCategoriesViewModel.startEdit(editingSubCategory)
                        subCategoriesViewModel.updateName(subName)
                        subCategoriesViewModel.saveSubCategory {
                            viewModel.updateSubCategory(subName)
                            showSubCategoryDialog = false
                        }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSubCategoryDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
