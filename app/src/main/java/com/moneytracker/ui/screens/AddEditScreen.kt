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
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.DetailEntity
import com.moneytracker.data.local.entity.RecurrenceFrequency
import com.moneytracker.data.local.entity.SubCategoryEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.ui.viewmodel.AddEditViewModel
import com.moneytracker.ui.viewmodel.CategoriesViewModel
import com.moneytracker.ui.viewmodel.DetailsViewModel
import com.moneytracker.ui.viewmodel.SubCategoriesViewModel
import com.moneytracker.ui.viewmodel.ViewModelFactory
import com.moneytracker.util.DateUtils
import com.moneytracker.util.sortedByPriority
import java.time.LocalDate

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

    // Category (SubCategoryEntity) ViewModel & State
    val categoriesViewModel: CategoriesViewModel = viewModel(factory = ViewModelFactory(repository))
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    val categories by categoriesViewModel.categories.collectAsState()
    var showCategoryDialog by remember { mutableStateOf(false) }

    // SubCategory ViewModel & State
    val subCategoriesViewModel: SubCategoriesViewModel = viewModel(factory = ViewModelFactory(repository))
    var editingSubCategory by remember { mutableStateOf<SubCategoryEntity?>(null) }
    val dbSubCategories by subCategoriesViewModel.subCategories.collectAsState()
    var showSubCategoryDialog by remember { mutableStateOf(false) }

    // Detail ViewModel & State
    val detailsViewModel: DetailsViewModel = viewModel(factory = ViewModelFactory(repository))
    var editingDetail by remember { mutableStateOf<DetailEntity?>(null) }
    val dbDetails by detailsViewModel.details.collectAsState()
    var showDetailDialog by remember { mutableStateOf(false) }

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
            // 1. Category Section (Displays Income, Investment, Expense dropdown & chips)
            var categoryDropdownExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = when (state.type) {
                        TransactionType.INCOME -> "Income"
                        TransactionType.INVESTMENT -> "Investment"
                        TransactionType.EXPENSE -> "Expense"
                    },
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
                        .menuAnchor(),
                    singleLine = true
                )
                DropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Income") },
                        onClick = {
                            viewModel.updateType(TransactionType.INCOME)
                            categoryDropdownExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Investment") },
                        onClick = {
                            viewModel.updateType(TransactionType.INVESTMENT)
                            categoryDropdownExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Expense") },
                        onClick = {
                            viewModel.updateType(TransactionType.EXPENSE)
                            categoryDropdownExpanded = false
                        }
                    )
                }
            }

            // Category filter chips (Income, Investment, Expense)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
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

            // Divider 1: After Category Section
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.5.dp
            )

            // 2. SubCategory Section (Category items like Salary, Utilities, Food, etc.)
            var subCategoryDropdownExpanded by remember { mutableStateOf(false) }
            val availableSubCategories = categories.filter { it.type == state.type }.sortedByPriority()
            val selectedSubCategory = availableSubCategories.find { it.id == state.categoryId }
            val selectedSubCategoryText = selectedSubCategory?.name ?: ""

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = subCategoryDropdownExpanded,
                    onExpandedChange = { subCategoryDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedSubCategoryText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("SubCategory") },
                        placeholder = { Text("Select SubCategory") },
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
                        expanded = subCategoryDropdownExpanded,
                        onDismissRequest = { subCategoryDropdownExpanded = false }
                    ) {
                        for (cat in availableSubCategories) {
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { editingCategory = cat; showCategoryDialog = true }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                                        IconButton(onClick = {
                                            deletingCategory = cat
                                            categoriesViewModel.deleteCategory(cat)
                                            if (state.categoryId == cat.id) {
                                                val remaining = availableSubCategories.filter { it.id != cat.id }
                                                remaining.firstOrNull()?.let { viewModel.updateCategory(it.id) }
                                            }
                                        }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                                    }
                                },
                                onClick = {
                                    viewModel.updateCategory(cat.id)
                                    subCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { editingCategory = null; showCategoryDialog = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add SubCategory")
                }
            }

            // SubCategory chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                for (cat in availableSubCategories.take(8)) {
                    FilterChip(
                        selected = state.categoryId == cat.id,
                        onClick = { viewModel.updateCategory(cat.id) },
                        label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Divider 2: After SubCategory Section
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.5.dp
            )

            // 3. Detail Section (Singular Detail dropdown, chips & text input)
            val categoryTypeMap = remember(categories) { categories.associate { it.id to it.type } }
            val filteredDetails = remember(dbSubCategories, dbDetails, state.type, state.categoryId, categoryTypeMap) {
                val fromSubCats = dbSubCategories.map { subCat ->
                    DetailEntity(id = subCat.id, name = subCat.name, categoryId = subCat.categoryId, type = subCat.type)
                }
                (fromSubCats + dbDetails).distinctBy { it.name }.filter { detail ->
                    val catType = detail.categoryId?.let { categoryTypeMap[it] }
                    when {
                        detail.categoryId != null -> detail.categoryId == state.categoryId || catType == state.type
                        detail.type != null -> detail.type == state.type
                        else -> true
                    }
                }
            }

            var detailExpanded by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = detailExpanded,
                    onExpandedChange = { detailExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = state.subCategory,
                        onValueChange = viewModel::updateSubCategory,
                        label = { Text("Detail (optional)") },
                        placeholder = { Text("e.g. Specific item or detail") },
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
                        expanded = detailExpanded,
                        onDismissRequest = { detailExpanded = false }
                    ) {
                        for (detail in filteredDetails) {
                            DropdownMenuItem(
                                text = { Text(detail.name) },
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { editingDetail = detail; showDetailDialog = true }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                                        IconButton(onClick = { detailsViewModel.deleteDetail(detail) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                                    }
                                },
                                onClick = {
                                    viewModel.updateSubCategory(detail.name)
                                    detailExpanded = false
                                }
                            )
                        }
                    }
                }
                IconButton(
                    onClick = { editingDetail = null; showDetailDialog = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Detail")
                }
            }

            // Detail chips in FlowRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                for (detail in filteredDetails.take(8)) {
                    FilterChip(
                        selected = state.subCategory == detail.name,
                        onClick = { viewModel.updateSubCategory(detail.name) },
                        label = { Text(detail.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Divider 3: After Detail Section
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                val datePicker = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                        viewModel.updateDate(selectedDate)
                    },
                    state.date.year,
                    state.date.monthValue - 1,
                    state.date.dayOfMonth
                )

                OutlinedTextField(
                    value = DateUtils.formatDate(DateUtils.toEpochMillis(state.date)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        IconButton(onClick = { datePicker.show() }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = "Pick Date")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Divider 4: After Amount & Date Fields
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.5.dp
            )

            // 5. Recurrence Section
            var recurrenceExpanded by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = recurrenceExpanded,
                    onExpandedChange = { recurrenceExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (state.isRecurring && state.recurrenceFrequency != null) {
                            state.recurrenceFrequency!!.name.lowercase().replaceFirstChar { it.uppercase() }
                        } else {
                            "One-time (No Recurrence)"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Recurrence") },
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
                        expanded = recurrenceExpanded,
                        onDismissRequest = { recurrenceExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("One-time (No Recurrence)") },
                            onClick = {
                                viewModel.updateIsRecurring(false)
                                recurrenceExpanded = false
                            }
                        )
                        RecurrenceFrequency.values().forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    viewModel.updateIsRecurring(true)
                                    viewModel.updateRecurrenceFrequency(freq)
                                    recurrenceExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (state.isRecurring) {
                val tillDateVal = state.recurTillDate ?: state.date
                val tillDatePicker = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedTill = LocalDate.of(year, month + 1, dayOfMonth)
                        viewModel.updateRecurTillDate(selectedTill)
                    },
                    tillDateVal.year,
                    tillDateVal.monthValue - 1,
                    tillDateVal.dayOfMonth
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = state.recurCount,
                        onValueChange = viewModel::updateRecurCount,
                        label = { Text("Limit (count)") },
                        placeholder = { Text("e.g. 12") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = state.recurTillDate?.let { DateUtils.formatDate(DateUtils.toEpochMillis(it)) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End Date") },
                        placeholder = { Text("Till Date") },
                        trailingIcon = {
                            IconButton(onClick = { tillDatePicker.show() }) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = "Pick End Date")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 6. Note Field
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

            // 7. Save Button
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

        // Add / Edit SubCategory Dialog
        if (showCategoryDialog) {
            var name by remember(editingCategory) { mutableStateOf(editingCategory?.name ?: "") }
            var type by remember(editingCategory) { mutableStateOf(editingCategory?.type ?: state.type) }
            AlertDialog(
                onDismissRequest = { showCategoryDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text(if (editingCategory == null) "Add SubCategory" else "Edit SubCategory") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("SubCategory Name") },
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

        // Add / Edit Detail Dialog
        if (showDetailDialog) {
            var detailName by remember(editingDetail) { mutableStateOf(editingDetail?.name ?: "") }
            AlertDialog(
                onDismissRequest = { showDetailDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text(if (editingDetail == null) "Add Detail" else "Edit Detail") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = detailName,
                            onValueChange = { detailName = it },
                            label = { Text("Detail Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        detailsViewModel.startEdit(editingDetail)
                        detailsViewModel.updateName(detailName)
                        detailsViewModel.updateType(state.type)
                        detailsViewModel.updateCategoryId(state.categoryId)
                        detailsViewModel.saveDetail {
                            viewModel.updateSubCategory(detailName)
                            showDetailDialog = false
                        }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDetailDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
