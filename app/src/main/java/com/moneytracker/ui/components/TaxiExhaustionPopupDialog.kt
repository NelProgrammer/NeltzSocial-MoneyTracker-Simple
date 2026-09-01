package com.moneytracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moneytracker.data.local.entity.TaxiExhaustionEntity
import com.moneytracker.data.local.entity.TaxiFareEntity
import com.moneytracker.ui.theme.ExpenseColor
import com.moneytracker.ui.theme.IncomeColor
import com.moneytracker.util.CurrencyUtils
import com.moneytracker.util.DateUtils
import com.moneytracker.util.TaxiExhaustionItemUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaxiExhaustionPopupDialog(
    route: TaxiFareEntity,
    exhaustionUiList: List<TaxiExhaustionItemUiModel>,
    rawExhaustions: List<TaxiExhaustionEntity>,
    monthLabel: String,
    onDismiss: () -> Unit,
    onQuickLogTrip: (timeOfDay: String, isMorning: Boolean) -> Unit,
    onSaveTrip: (TaxiExhaustionEntity) -> Unit,
    onDeleteTrip: (TaxiExhaustionEntity) -> Unit
) {
    var showAddEditOutlierDialog by remember { mutableStateOf(false) }
    var editingEntity by remember { mutableStateOf<TaxiExhaustionEntity?>(null) }
    var expandedWeekKeys by remember { mutableStateOf(setOf<Int>()) }

    // Summary calculations
    val totalBudgetCost = route.monthlyTotal
    val totalActualSpent = rawExhaustions.sumOf { it.totalCost }
    val remainingBalance = totalBudgetCost - totalActualSpent
    val isOver = totalActualSpent > totalBudgetCost && totalBudgetCost > 0.0
    val totalTripsLogged = rawExhaustions.sumOf { it.units }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 1. Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalTaxi,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Exhaust Budget: ${route.routeName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Monthly Tracking • $monthLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Budget Overview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Monthly Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                CurrencyUtils.formatZar(totalBudgetCost),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text("${route.workingDaysPerMonth * route.tripsPerDay} est. trips", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Actual Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                CurrencyUtils.formatZar(totalActualSpent),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOver) ExpenseColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text("$totalTripsLogged trips used", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (isOver) "Over Budget" else "Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                CurrencyUtils.formatZar(if (isOver) totalActualSpent - totalBudgetCost else remainingBalance),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOver) ExpenseColor else IncomeColor
                            )
                            Text(if (isOver) "Deficit" else "Available", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = if (isOver) ExpenseColor else IncomeColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Quick Action Buttons Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onQuickLogTrip("MORNING", true) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Brightness5, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+1 Morning", fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { onQuickLogTrip("EVENING", false) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.NightsStay, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+1 After-Hours", fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            editingEntity = null
                            showAddEditOutlierDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Uber/Other", fontSize = 11.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Monthly Exhaustion List (${rawExhaustions.size} entries)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 4. Single Unified Monthly List
                if (exhaustionUiList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.LocalTaxi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No trips logged for $monthLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap quick action buttons above to log trips",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(exhaustionUiList) { uiItem ->
                            when (uiItem) {
                                is TaxiExhaustionItemUiModel.WeeklyCollatedGroup -> {
                                    val isExpanded = expandedWeekKeys.contains(uiItem.weekNumber)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        expandedWeekKeys = if (isExpanded) {
                                                            expandedWeekKeys - uiItem.weekNumber
                                                        } else {
                                                            expandedWeekKeys + uiItem.weekNumber
                                                        }
                                                    }
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "W${uiItem.weekNumber}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontSize = 10.sp
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = uiItem.weekLabel,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "${uiItem.totalUnits} trips × ${CurrencyUtils.formatZar(uiItem.baseFare)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = CurrencyUtils.formatZar(uiItem.totalCost),
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Icon(
                                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                        contentDescription = null,
                                                        modifier = Modifier.padding(start = 4.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Expanded items breakdown inside the week
                                            AnimatedVisibility(visible = isExpanded) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                                    uiItem.tripItems.forEach { trip ->
                                                        val tripDateStr = SimpleDateFormat("EEE, d MMM • HH:mm", Locale.getDefault()).format(Date(trip.date))
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 2.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                val timeTitle = trip.timeOfDay.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                                                Text(
                                                                    text = "$timeTitle • ${trip.units} trip (${CurrencyUtils.formatZar(trip.farePerTrip)})",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                                Text(
                                                                    text = "$tripDateStr ${if (trip.note.isNotBlank()) "• ${trip.note}" else ""}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontSize = 9.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }

                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = CurrencyUtils.formatZar(trip.totalCost),
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                IconButton(
                                                                    onClick = {
                                                                        editingEntity = trip
                                                                        showAddEditOutlierDialog = true
                                                                    },
                                                                    modifier = Modifier.size(24.dp)
                                                                ) {
                                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(13.dp))
                                                                }
                                                                IconButton(
                                                                    onClick = { onDeleteTrip(trip) },
                                                                    modifier = Modifier.size(24.dp)
                                                                ) {
                                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseColor, modifier = Modifier.size(13.dp))
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                is TaxiExhaustionItemUiModel.OutlierTrip -> {
                                    val trip = uiItem.entity
                                    val tripDateStr = SimpleDateFormat("EEE, d MMM • HH:mm", Locale.getDefault()).format(Date(trip.date))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.DirectionsCar,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.tertiary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                                                        ) {
                                                            Text(
                                                                text = "OUTLIER / UBER",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                                                color = MaterialTheme.colorScheme.tertiary,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "${trip.units} unit(s) • ${CurrencyUtils.formatZar(trip.farePerTrip)}/trip",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        text = "$tripDateStr • ${trip.note.ifBlank { "Custom Transport Fare" }}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = CurrencyUtils.formatZar(trip.totalCost),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                                IconButton(
                                                    onClick = {
                                                        editingEntity = trip
                                                        showAddEditOutlierDialog = true
                                                    },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                                                }
                                                IconButton(
                                                    onClick = { onDeleteTrip(trip) },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseColor, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done")
                }
            }
        }
    }

    // 5. Add / Edit Outlier or Custom Trip Dialog
    if (showAddEditOutlierDialog) {
        val isEditing = editingEntity != null
        var fareInput by remember { mutableStateOf(editingEntity?.farePerTrip?.toString() ?: (if (route.farePerTrip > 0) (route.farePerTrip * 2).toString() else "150.0")) }
        var unitsInput by remember { mutableStateOf(editingEntity?.units?.toString() ?: "1") }
        var noteInput by remember { mutableStateOf(editingEntity?.note ?: "Uber trip from home") }
        var isOutlierFlag by remember { mutableStateOf(editingEntity?.isCustomOutlier ?: true) }

        AlertDialog(
            onDismissRequest = { showAddEditOutlierDialog = false },
            title = { Text(if (isEditing) "Edit Trip Entry" else "Log Outlier / Custom Trip") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Specify custom fare (e.g., R150 Uber) or adjust trip units:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = fareInput,
                        onValueChange = { fareInput = it },
                        label = { Text("Fare per Trip (R)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = unitsInput,
                        onValueChange = { unitsInput = it },
                        label = { Text("Units / Trips") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Note / Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fare = fareInput.toDoubleOrNull() ?: route.farePerTrip
                        val units = unitsInput.toIntOrNull() ?: 1
                        val total = fare * units
                        val entity = editingEntity?.copy(
                            units = units,
                            farePerTrip = fare,
                            totalCost = total,
                            note = noteInput.trim(),
                            isCustomOutlier = isOutlierFlag,
                            updatedAt = System.currentTimeMillis()
                        ) ?: TaxiExhaustionEntity(
                            profileId = route.profileId,
                            routeId = route.id,
                            payMonthDate = route.date,
                            date = System.currentTimeMillis(),
                            units = units,
                            farePerTrip = fare,
                            totalCost = total,
                            isAutoGenerated = false,
                            isCustomOutlier = true,
                            timeOfDay = "CUSTOM",
                            note = noteInput.trim()
                        )
                        onSaveTrip(entity)
                        showAddEditOutlierDialog = false
                    }
                ) {
                    Text(if (isEditing) "Save Changes" else "Log Outlier")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditOutlierDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
