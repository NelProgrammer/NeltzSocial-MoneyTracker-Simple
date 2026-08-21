package com.moneytracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moneytracker.data.local.entity.TaxiFareEntity
import com.moneytracker.ui.components.AppTopBar
import com.moneytracker.ui.components.EmptyState
import com.moneytracker.ui.components.PayMonthFilterHeader
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.ui.viewmodel.TaxiFareViewModel
import com.moneytracker.util.CurrencyUtils
import com.moneytracker.util.DateUtils
import com.moneytracker.util.SettingsManager
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxiFareScreen(
    viewModel: TaxiFareViewModel,
    contentPadding: PaddingValues
) {
    val routes by viewModel.routes.collectAsState()
    val budgetSummary by viewModel.budgetSummary.collectAsState()
    val selectedPayMonthDate by viewModel.selectedPayMonthDate.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRoute by remember { mutableStateOf<TaxiFareEntity?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                screenTitle = "Taxi Fare & Commute Calculator",
                showBack = false
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                editingRoute = null
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Commute Route")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PayMonthFilterHeader(
                selectedPayMonthDate = selectedPayMonthDate,
                onPayMonthSelected = { viewModel.setPayMonth(it) }
            )
            // 1. Commute Budget vs Total Estimated Fare Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Taxi Commute Budget Target",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Over / Under Budget Status Badge
                        Surface(
                            color = if (budgetSummary.isOverBudget) ExpenseColor else IncomeColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (budgetSummary.isOverBudget) {
                                    "OVER BUDGET by ${CurrencyUtils.format(budgetSummary.overBudgetAmount)}"
                                } else {
                                    "UNDER BUDGET by ${CurrencyUtils.format(budgetSummary.remainingBudget)}"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Main Table Taxi Budget",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = CurrencyUtils.format(budgetSummary.mainBudget),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Estimated Commute Cost",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = CurrencyUtils.format(budgetSummary.totalEstimatedFare),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (budgetSummary.isOverBudget) ExpenseColor else IncomeColor
                            )
                        }
                    }
                }
            }

            // 2. Saved Commute Routes Section Header
            Text(
                text = "Saved Commute Routes (${routes.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // 3. Saved Routes List
            if (routes.isEmpty()) {
                EmptyState("No taxi routes saved yet. Tap + to add a commute route.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(routes, key = { it.id }) { route ->
                        TaxiRouteCard(
                            route = route,
                            onClick = {
                                editingRoute = route
                                showAddDialog = true
                            },
                            onDelete = { viewModel.deleteRoute(route) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditTaxiRouteDialog(
            route = editingRoute,
            selectedPayMonthDate = selectedPayMonthDate,
            onDismiss = { 
                showAddDialog = false
                editingRoute = null
            },
            onDelete = if (editingRoute != null) {
                {
                    viewModel.deleteRoute(editingRoute!!)
                    showAddDialog = false
                    editingRoute = null
                }
            } else null,
            onConfirm = { id, routeName, fare, trips, workDays, startTs ->
                viewModel.saveRoute(
                    id = id,
                    routeName = routeName,
                    farePerTrip = fare,
                    tripsPerDay = trips,
                    workingDaysPerMonth = workDays,
                    startDate = startTs
                )
                showAddDialog = false
                editingRoute = null
            }
        )
    }
}

@Composable
private fun TaxiRouteCard(
    route: TaxiFareEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.routeName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${CurrencyUtils.format(route.farePerTrip)} / trip  •  ${route.tripsPerDay} trips/day  •  ${route.workingDaysPerMonth} days/mo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CurrencyUtils.format(route.monthlyTotal),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 4.dp)
                )
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Route",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Route",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditTaxiRouteDialog(
    route: TaxiFareEntity?,
    selectedPayMonthDate: LocalDate,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onConfirm: (id: Long, routeName: String, farePerTrip: Double, tripsPerDay: Int, workingDaysPerMonth: Int, startTs: Long) -> Unit
) {
    val payDateDay = remember { SettingsManager.getPayDateDay() }
    val currentPayMonth = selectedPayMonthDate
    val candidateMonths = remember(currentPayMonth) {
        (-3..12).map { currentPayMonth.plusMonths(it.toLong()) }
    }
    var startMonthInput by remember {
        mutableStateOf(
            route?.let { DateUtils.toLocalDate(it.date) } ?: currentPayMonth
        )
    }
    var startMonthExpanded by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(route?.routeName ?: "") }
    var fareInput by remember { mutableStateOf(if ((route?.farePerTrip ?: 0.0) > 0) route!!.farePerTrip.toString() else "") }
    var tripsInput by remember { mutableStateOf(route?.tripsPerDay?.toString() ?: "2") }
    var daysInput by remember { mutableStateOf(route?.workingDaysPerMonth?.toString() ?: "20") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (route == null) "Add Taxi Route" else "Edit Taxi Route",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (route != null && onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Route",
                            tint = ExpenseColor
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Start Month Dropdown
                ExposedDropdownMenuBox(
                    expanded = startMonthExpanded,
                    onExpandedChange = { startMonthExpanded = it }
                ) {
                    OutlinedTextField(
                        value = DateUtils.formatPayMonth(startMonthInput, payDateDay),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Month") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = startMonthExpanded,
                        onDismissRequest = { startMonthExpanded = false }
                    ) {
                        candidateMonths.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(DateUtils.formatPayMonth(m, payDateDay)) },
                                onClick = {
                                    startMonthInput = m
                                    startMonthExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Route Name (e.g. Work Commute)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fareInput,
                    onValueChange = { fareInput = it },
                    label = { Text("Fare Per Trip (R)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tripsInput,
                        onValueChange = { tripsInput = it },
                        label = { Text("Trips / Day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = daysInput,
                        onValueChange = { daysInput = it },
                        label = { Text("Work Days / Mo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (route != null && route.updatedAt > 0L) {
                    var showDebugInfo by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                        Text(
                            text = if (showDebugInfo) "System Info ▲" else "System Info ▼",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable { showDebugInfo = !showDebugInfo }
                                .padding(vertical = 2.dp)
                        )
                        if (showDebugInfo) {
                            val formattedTime = remember(route.updatedAt) {
                                java.time.Instant.ofEpochMilli(route.updatedAt)
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fare = fareInput.toDoubleOrNull() ?: 0.0
                    val trips = tripsInput.toIntOrNull() ?: 2
                    val days = daysInput.toIntOrNull() ?: 20
                    val startTs = DateUtils.startOfPayMonth(startMonthInput, payDateDay)
                    if (name.isNotBlank() && fare > 0.0) {
                        onConfirm(route?.id ?: 0L, name, fare, trips, days, startTs)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (route == null) "Add" else "Save")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (route != null && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = ExpenseColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                        Text("Delete")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
