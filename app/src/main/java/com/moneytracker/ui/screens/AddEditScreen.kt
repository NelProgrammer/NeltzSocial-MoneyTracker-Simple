package com.moneytracker.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.ui.viewmodel.AddEditViewModel
import com.moneytracker.util.DateUtils
import com.moneytracker.util.sortedByPriority
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneytracker.data.local.entity.CategoryEntity
import java.util.Calendar
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.ui.viewmodel.ViewModelFactory
import com.moneytracker.ui.viewmodel.CategoriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    viewModel: AddEditViewModel,
    repository: TransactionRepository,
    title: String,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val categoriesViewModel: CategoriesViewModel = viewModel(factory = ViewModelFactory(repository))
    var editingCategory by remember { mutableStateOf<com.moneytracker.data.local.entity.CategoryEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<com.moneytracker.data.local.entity.CategoryEntity?>(null) }
    val categories by categoriesViewModel.categories.collectAsState()
    var showCategoryDialog by remember { mutableStateOf(false) }

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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { viewModel.updateType(TransactionType.INCOME) },
                    label = { Text("Income") }
                )
                FilterChip(
                    selected = state.type == TransactionType.INVESTMENT,
                    onClick = { viewModel.updateType(TransactionType.INVESTMENT) },
                    label = { Text("Investment") }
                )
                FilterChip(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { viewModel.updateType(TransactionType.EXPENSE) },
                    label = { Text("Expense") }
                )
            }

            // Category selection UI
            var categoryExpanded by remember { mutableStateOf(false) }
            val selectedCategory = categories.find { it.id == state.categoryId }
            val selectedText = selectedCategory?.name ?: ""
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedText,
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
                            .weight(1f)
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
            // Commonly used category chips (top 5 for current type)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                for (cat in categories.filter { it.type == state.type }.sortedByPriority().take(5)) {
                    FilterChip(
                        selected = state.categoryId == cat.id,
                        onClick = { viewModel.updateCategory(cat.id) },
                        label = { Text(cat.name) },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                thickness = 1.5.dp
            )

            // Amount field
            OutlinedTextField(
                value = state.amount,
                onValueChange = viewModel::updateAmount,
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                prefix = { Text("$") }
            )

            OutlinedTextField(
                value = DateUtils.formatDate(DateUtils.toEpochMillis(state.date)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        Modifier.fillMaxWidth()
                    ),
                enabled = true
            )

            Button(
                onClick = {
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
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pick Date")
            }

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
    }
}

