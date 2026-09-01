package com.moneytracker.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
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
fun AddEditTransactionsScreen(
    viewModel: AddEditViewModel,
    repository: TransactionRepository,
    title: String,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 1. ViewModels & State Collection
    val categoriesViewModel: CategoriesViewModel = viewModel(factory = ViewModelFactory(repository))
    val subCategoriesViewModel: SubCategoriesViewModel = viewModel(factory = ViewModelFactory(repository))
    val detailsViewModel: DetailsViewModel = viewModel(factory = ViewModelFactory(repository))

    val categories by categoriesViewModel.categories.collectAsState()
    val dbSubCategories by subCategoriesViewModel.subCategories.collectAsState()
    val dbDetails by detailsViewModel.details.collectAsState()

    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    var editingSubCategory by remember { mutableStateOf<SubCategoryEntity?>(null) }
    var showSubCategoryDialog by remember { mutableStateOf(false) }

    var editingDetail by remember { mutableStateOf<DetailEntity?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var showDeleteTxnDialog by remember { mutableStateOf(false) }

    // 2. Smooth, continuous category input text
    var categoryInputText by remember { mutableStateOf("") }

    LaunchedEffect(state.categoryId, state.type, categories) {
        if (categories.isNotEmpty()) {
            val foundCat = categories.find { it.id == state.categoryId }
            if (foundCat != null) {
                categoryInputText = foundCat.name
            } else if (state.categoryId == null) {
                val matchByType = categories.find { it.type == state.type }
                if (matchByType != null) {
                    categoryInputText = matchByType.name
                    viewModel.updateCategory(matchByType.id)
                } else {
                    categoryInputText = state.type.name.lowercase().replaceFirstChar { it.uppercase() }
                }
            }
        } else if (categoryInputText.isBlank()) {
            categoryInputText = state.type.name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    // 3. Matched Category Entity / Type from categoryInputText
    val matchedCategory = remember(categoryInputText, categories) {
        categories.find { it.name.equals(categoryInputText.trim(), ignoreCase = true) }
    }
    val isCategoryUnmatched = remember(categoryInputText, matchedCategory) {
        categoryInputText.isNotBlank() && matchedCategory == null
    }

    val matchedSubCat = remember(state.subCategory, dbSubCategories, matchedCategory) {
        dbSubCategories.find {
            it.name.equals(state.subCategory.trim(), ignoreCase = true) &&
            (matchedCategory == null || it.categoryId == matchedCategory.id || (it.categoryId == null && it.type == matchedCategory.type))
        }
    }
    val isSubCategoryUnmatched = remember(state.subCategory, matchedSubCat) {
        state.subCategory.isNotBlank() && matchedSubCat == null
    }

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
            // 1. Category Section (NeltzSocial_Combo_PilledFilteredCruded - 100% Database Driven)
            com.moneytracker.ui.components.NeltzSocial_Combo_PilledFilteredCruded(
                label = "Category",
                selectedValue = categoryInputText,
                onValueChange = { input ->
                    categoryInputText = input
                    val matchedCat = categories.find { it.name.equals(input.trim(), ignoreCase = true) }
                    if (matchedCat != null) {
                        viewModel.updateType(matchedCat.type)
                        viewModel.updateCategory(matchedCat.id)
                    }
                },
                items = categories,
                itemToText = { it.name },
                onAddItem = { editingCategory = null; showCategoryDialog = true },
                onEditItem = { cat ->
                    editingCategory = cat
                    showCategoryDialog = true
                },
                onDeleteItem = { cat ->
                    deletingCategory = cat
                    categoriesViewModel.deleteCategory(cat)
                }
            )

            // Divider 1: After Category Section
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.5.dp
            )

            // 2. SubCategory Section (NeltzSocial_Combo_PilledFilteredCruded - with Component Parent Filtering & Auto-Reset)
            com.moneytracker.ui.components.NeltzSocial_Combo_PilledFilteredCruded(
                label = "SubCategory",
                selectedValue = state.subCategory,
                onValueChange = viewModel::updateSubCategory,
                items = dbSubCategories,
                parentFilterKey = categoryInputText,
                filterPredicate = { subCat ->
                    if (isCategoryUnmatched) {
                        false
                    } else if (matchedCategory != null) {
                        subCat.categoryId == matchedCategory.id || (subCat.categoryId == null && subCat.type == matchedCategory.type)
                    } else {
                        subCat.type == state.type
                    }
                },
                itemToText = { it.name },
                onAddItem = { editingSubCategory = null; showSubCategoryDialog = true },
                onEditItem = { subCat -> editingSubCategory = subCat; showSubCategoryDialog = true },
                onDeleteItem = { subCat -> subCategoriesViewModel.deleteSubCategory(subCat) }
            )

            // Divider 2: After SubCategory Section
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.5.dp
            )

            // 3. Detail Section (NeltzSocial_Combo_PilledFilteredCruded - with Component Parent Filtering & Auto-Reset)
            com.moneytracker.ui.components.NeltzSocial_Combo_PilledFilteredCruded(
                label = "Detail (optional)",
                selectedValue = state.detail,
                onValueChange = viewModel::updateDetail,
                items = dbDetails,
                parentFilterKey = Pair(categoryInputText, state.subCategory),
                filterPredicate = { detail ->
                    if (isCategoryUnmatched || isSubCategoryUnmatched) {
                        false
                    } else if (matchedSubCat != null) {
                        detail.subCategoryId == matchedSubCat.id
                    } else if (matchedCategory != null) {
                        detail.categoryId == matchedCategory.id && detail.subCategoryId == null
                    } else {
                        false
                    }
                },
                itemToText = { it.name },
                onAddItem = { editingDetail = null; showDetailDialog = true },
                onEditItem = { detail -> editingDetail = detail; showDetailDialog = true },
                onDeleteItem = { detail -> detailsViewModel.deleteDetail(detail) }
            )

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
                val autoPrefix = if (state.type == TransactionType.INCOME) "+ " else "- "
                val autoColor = if (state.type == TransactionType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                OutlinedTextField(
                    value = state.amount,
                    onValueChange = viewModel::updateAmount,
                    label = { Text("Amount") },
                    prefix = {
                        Text(
                            text = autoPrefix,
                            color = autoColor,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
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
                    val currentRecurrenceLabel = if (state.isRecurring && state.recurrenceFrequency != null) {
                        state.recurrenceFrequency!!.label
                    } else {
                        "One-time (No Recurrence)"
                    }

                    OutlinedTextField(
                        value = currentRecurrenceLabel,
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
                        listOf(
                            RecurrenceFrequency.ONCE_OFF,
                            RecurrenceFrequency.MONTHLY,
                            RecurrenceFrequency.PLAN_FUTURE
                        ).forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.label) },
                                onClick = {
                                    viewModel.updateRecurrenceFrequency(freq)
                                    recurrenceExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (state.isRecurring && state.recurrenceFrequency == RecurrenceFrequency.MONTHLY) {
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
                        placeholder = { Text("Optional limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = state.recurTillDate?.let { DateUtils.formatDate(DateUtils.toEpochMillis(it)) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End Date") },
                        placeholder = { Text("Optional date") },
                        trailingIcon = {
                            IconButton(onClick = { tillDatePicker.show() }) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = "Pick End Date")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = if (state.recurCount.isBlank() && state.recurTillDate == null) 
                        "Repeats continuously every month indefinitely" 
                        else "Repeats monthly until limit/end date is reached",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            } else if (state.isRecurring && state.recurrenceFrequency == RecurrenceFrequency.PLAN_FUTURE) {
                Text(
                    text = "Visible in roadmap every month (no effect on balances or summations)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
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

            if (state.updatedAt > 0L) {
                var showDebugInfo by remember { mutableStateOf(false) }
                Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp)) {
                    Text(
                        text = if (showDebugInfo) "System Info ▲" else "System Info ▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .clickable { showDebugInfo = !showDebugInfo }
                            .padding(vertical = 2.dp)
                    )
                    if (showDebugInfo) {
                        val formattedTime = remember(state.updatedAt) {
                            java.time.Instant.ofEpochMilli(state.updatedAt)
                                .atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        }
                        Text(
                            text = "Last updated: $formattedTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // 7. Action Buttons (Save & Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (title.contains("Edit", ignoreCase = true)) {
                    Button(
                        onClick = { showDeleteTxnDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Delete")
                    }
                }

                Button(
                    onClick = { viewModel.save(onNavigateBack) },
                    modifier = Modifier.weight(if (title.contains("Edit", ignoreCase = true)) 1.5f else 1f),
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
        }

        if (state.duplicateCandidate != null && state.pendingNewItem != null) {
            com.moneytracker.ui.components.DuplicateResolutionDialog(
                existingItem = state.duplicateCandidate!!,
                newItem = state.pendingNewItem!!,
                onKeepNew = { viewModel.save(onNavigateBack, forceSave = true) },
                onSwapToExisting = { viewModel.swapToExistingDuplicate() },
                onDismiss = { viewModel.dismissDuplicateDialog() }
            )
        }

        if (showDeleteTxnDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteTxnDialog = false },
                title = { Text("Delete Transaction?") },
                text = { Text("Are you sure you want to delete this transaction entry? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteTxnDialog = false
                            viewModel.delete(onNavigateBack)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteTxnDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Category Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text("Category Type", style = MaterialTheme.typography.labelMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TransactionType.values().forEach { t ->
                                FilterChip(
                                    selected = type == t,
                                    onClick = { type = t },
                                    label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (name.isNotBlank()) {
                            val trimmed = name.trim()
                            categoriesViewModel.startEdit(editingCategory)
                            categoriesViewModel.updateName(trimmed)
                            categoriesViewModel.updateType(type)
                            categoriesViewModel.saveCategory { savedCategoryId ->
                                categoryInputText = trimmed
                                viewModel.updateType(type)
                                viewModel.updateCategory(savedCategoryId)
                                showCategoryDialog = false
                            }
                        }
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

        // Add / Edit SubCategory Dialog
        if (showSubCategoryDialog) {
            var subCatName by remember(editingSubCategory) { mutableStateOf(editingSubCategory?.name ?: "") }
            var subCatType by remember(editingSubCategory) { mutableStateOf(editingSubCategory?.type ?: state.type) }
            AlertDialog(
                onDismissRequest = { showSubCategoryDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                title = { Text(if (editingSubCategory == null) "Add SubCategory" else "Edit SubCategory") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = subCatName,
                            onValueChange = { subCatName = it },
                            label = { Text("SubCategory Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text("Category Type", style = MaterialTheme.typography.labelMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TransactionType.values().forEach { t ->
                                FilterChip(
                                    selected = subCatType == t,
                                    onClick = { subCatType = t },
                                    label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (subCatName.isNotBlank()) {
                            val trimmed = subCatName.trim()
                            subCategoriesViewModel.startEdit(editingSubCategory)
                            subCategoriesViewModel.updateName(trimmed)
                            subCategoriesViewModel.updateCategoryId(matchedCategory?.id)
                            subCategoriesViewModel.updateType(subCatType)
                            subCategoriesViewModel.saveSubCategory {
                                viewModel.updateSubCategory(trimmed)
                                viewModel.updateType(subCatType)
                                showSubCategoryDialog = false
                            }
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
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (detailName.isNotBlank()) {
                            val trimmed = detailName.trim()
                            detailsViewModel.startEdit(editingDetail)
                            detailsViewModel.updateName(trimmed)
                            detailsViewModel.updateSubCategoryId(matchedSubCat?.id)
                            detailsViewModel.updateCategoryId(matchedCategory?.id)
                            detailsViewModel.updateType(matchedCategory?.type ?: state.type)
                            detailsViewModel.saveDetail {
                                viewModel.updateDetail(trimmed)
                                showDetailDialog = false
                            }
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
