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
import com.moneytracker.ui.components.AppTopBar
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
            AppTopBar(
                screenTitle = title,
                showBack = true,
                onBack = onNavigateBack
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
            // 1. Category Section (Editable Dropdown: Income, Investment, Education, Expense)
            var categoryTypeDropdownExpanded by remember { mutableStateOf(false) }
            val categoryTypeNames = remember {
                listOf(
                    TransactionType.INCOME to "Income",
                    TransactionType.INVESTMENT to "Investment",
                    TransactionType.EDUCATION to "Education",
                    TransactionType.EXPENSE to "Expense"
                )
            }
            var categoryInputText by remember(state.type) {
                mutableStateOf(categoryTypeNames.find { it.first == state.type }?.second ?: state.type.name)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = categoryTypeDropdownExpanded,
                    onExpandedChange = { categoryTypeDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = categoryInputText,
                        onValueChange = { input ->
                            categoryInputText = input
                            val matched = categoryTypeNames.find { it.second.equals(input, ignoreCase = true) }
                            if (matched != null) {
                                viewModel.updateType(matched.first)
                            }
                        },
                        label = { Text("Category") },
                        placeholder = { Text("Select or type Category") },
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
                        expanded = categoryTypeDropdownExpanded,
                        onDismissRequest = { categoryTypeDropdownExpanded = false }
                    ) {
                        for ((type, name) in categoryTypeNames) {
                            DropdownMenuItem(
                                text = { Text(name) },
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = {
                                            val catEntity = categories.find { it.name.equals(name, ignoreCase = true) }
                                            editingCategory = catEntity ?: CategoryEntity(id = 0, name = name, type = type)
                                            showCategoryDialog = true
                                        }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit Category")
                                        }
                                        IconButton(onClick = {
                                            val catEntity = categories.find { it.name.equals(name, ignoreCase = true) }
                                            if (catEntity != null) {
                                                deletingCategory = catEntity
                                                categoriesViewModel.deleteCategory(catEntity)
                                            }
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete Category")
                                        }
                                    }
                                },
                                onClick = {
                                    categoryInputText = name
                                    viewModel.updateType(type)
                                    categoryTypeDropdownExpanded = false
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

            // Category Chips for quick 1-tap selection
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                for ((type, name) in categoryTypeNames) {
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = state.type == type,
                        onClick = {
                            categoryInputText = name
                            viewModel.updateType(type)
                        },
                        label = { Text(if (name == "Investment") "Invest" else name, maxLines = 1) }
                    )
                }
            }

            // Divider 1: After Category Section
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.5.dp
            )

            // 2. SubCategory Section (Salary, Credit & Bank Charges, Utilities, University, Certificate, School, etc. from seed data)
            val availableSubCategories = remember(dbSubCategories, state.type) {
                dbSubCategories.filter { it.type == state.type }.distinctBy { it.name }
            }

            var subCategoryDropdownExpanded by remember { mutableStateOf(false) }

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
                        value = state.subCategory,
                        onValueChange = viewModel::updateSubCategory,
                        label = { Text("SubCategory") },
                        placeholder = { Text("Select or type SubCategory") },
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
                        for (subCat in availableSubCategories) {
                            DropdownMenuItem(
                                text = { Text(subCat.name) },
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { editingSubCategory = subCat; showSubCategoryDialog = true }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit SubCategory")
                                        }
                                        IconButton(onClick = {
                                            subCategoriesViewModel.deleteSubCategory(subCat)
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete SubCategory")
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.updateSubCategory(subCat.name)
                                    subCategoryDropdownExpanded = false
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

            // SubCategory chips for quick 1-tap selection
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                for (subCat in availableSubCategories.take(8)) {
                    FilterChip(
                        selected = state.subCategory == subCat.name,
                        onClick = { viewModel.updateSubCategory(subCat.name) },
                        label = { Text(subCat.name, style = MaterialTheme.typography.labelSmall) }
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

            // 3. Detail Section (Details matching selected SubCategory / Category from seed data)
            val selectedSubCatObj = dbSubCategories.find { it.name.equals(state.subCategory, ignoreCase = true) }
            val availableDetails = remember(dbDetails, state.type, state.subCategory, selectedSubCatObj) {
                if (selectedSubCatObj != null) {
                    dbDetails.filter { it.subCategoryId == selectedSubCatObj.id || it.type == state.type }
                } else {
                    dbDetails.filter { it.type == state.type }
                }.distinctBy { it.name }
            }

            var detailDropdownExpanded by remember { mutableStateOf(false) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = detailDropdownExpanded,
                    onExpandedChange = { detailDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = state.detail,
                        onValueChange = viewModel::updateDetail,
                        label = { Text("Detail (optional)") },
                        placeholder = { Text("Select or type Detail") },
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
                        expanded = detailDropdownExpanded,
                        onDismissRequest = { detailDropdownExpanded = false }
                    ) {
                        for (detail in availableDetails) {
                            DropdownMenuItem(
                                text = { Text(detail.name) },
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { editingDetail = detail; showDetailDialog = true }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit Detail")
                                        }
                                        IconButton(onClick = {
                                            detailsViewModel.deleteDetail(detail)
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete Detail")
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.updateDetail(detail.name)
                                    detailDropdownExpanded = false
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

            // Detail chips for quick 1-tap selection
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                for (detail in availableDetails.take(8)) {
                    FilterChip(
                        selected = state.detail == detail.name,
                        onClick = { viewModel.updateDetail(detail.name) },
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
