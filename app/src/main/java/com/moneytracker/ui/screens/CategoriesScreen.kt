package com.moneytracker.ui.screens

import androidx.compose.ui.text.font.FontWeight
import com.moneytracker.util.sortedByPriority



import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.moneytracker.ui.components.AppTopBar
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.ui.viewmodel.CategoriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onNavigateBack: () -> Unit = {}
) {
    val categories by viewModel.categories.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var nameState by remember { mutableStateOf(TextFieldValue()) }
    var typeState by remember { mutableStateOf(TransactionType.EXPENSE) }
    var iconState by remember { mutableStateOf(TextFieldValue()) }

    Scaffold(
        modifier = Modifier,
        topBar = {
            AppTopBar(
                screenTitle = "Categories",
                showBack = true,
                onBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingCategory = null
                nameState = TextFieldValue()
                typeState = TransactionType.EXPENSE
                iconState = TextFieldValue()
                showDialog = true
            }) {
                Icon(painter = painterResource(id = android.R.drawable.ic_input_add), contentDescription = "Add Category")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories.sortedByPriority(), key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    onEdit = {
                        editingCategory = category
                        nameState = TextFieldValue(category.name)
                        typeState = category.type
                        iconState = TextFieldValue(category.iconName)
                        showDialog = true
                    },
                    onDelete = { categoryToDelete = category; showDeleteDialog = true }
                )
            }
        }
    }

    if (showDialog) {
    AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text(if (editingCategory == null) "Add Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = nameState,
                    onValueChange = { nameState = it },
                    label = { Text("Name") }
                )
                Text("Type: $typeState")
                androidx.compose.material3.OutlinedTextField(
                    value = iconState,
                    onValueChange = { iconState = it },
                    label = { Text("Icon Name (resource identifier)") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.startEdit(editingCategory)
                viewModel.updateName(nameState.text)
                viewModel.updateType(typeState)
                viewModel.updateIcon(iconState.text)
                viewModel.saveCategory { showDialog = false }
            }) { Text("Save") }
        },
        dismissButton = {
            Button(onClick = { showDialog = false }) { Text("Cancel") }
        }
    )
}

if (showDeleteDialog && categoryToDelete != null) {
    AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text("Delete Category") },
        text = { Text("Are you sure you want to delete \"${categoryToDelete!!.name}\"?") },
        confirmButton = {
            Button(onClick = {
                viewModel.deleteCategory(categoryToDelete!!)
                showDeleteDialog = false
            }) { Text("Delete") }
        },
        dismissButton = {
            Button(onClick = { showDeleteDialog = false }) { Text("Cancel") }
        }
    )
}


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryRow(
    category: CategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = category.name, fontWeight = FontWeight.Bold)
                Text(text = "${category.type}", color = Color.Gray)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(painter = painterResource(id = android.R.drawable.ic_menu_edit), contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(painter = painterResource(id = android.R.drawable.ic_menu_delete), contentDescription = "Delete")
                }
            }
        }
    }
}
