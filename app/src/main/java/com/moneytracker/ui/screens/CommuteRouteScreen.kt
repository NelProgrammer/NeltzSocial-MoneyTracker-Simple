package com.moneytracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moneytracker.data.local.entity.CommuteJourneyEntity
import com.moneytracker.data.local.entity.CommuteLegEntity
import com.moneytracker.data.local.entity.JourneyWithLegs
import com.moneytracker.data.local.entity.TransportMode
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.ui.components.AppTopBar
import com.moneytracker.util.CurrencyUtils
import kotlinx.coroutines.launch

fun getTrafficColor(density: String): Color {
    return when (density.uppercase()) {
        "LOW" -> Color(0xFF2E7D32)      // Green (Low / Free flow)
        "MODERATE" -> Color(0xFFF9A825) // Yellow (Moderate)
        "HEAVY" -> Color(0xFFE65100)    // Orange (Heavy)
        "SEVERE" -> Color(0xFFC62828)   // Red (Severe Delay)
        else -> Color(0xFF2E7D32)
    }
}

fun getTransportIcon(mode: TransportMode): ImageVector {
    return when (mode) {
        TransportMode.TAXI -> Icons.Default.LocalTaxi
        TransportMode.UBER, TransportMode.BOLT -> Icons.Default.DirectionsCar
        TransportMode.BUS -> Icons.Default.DirectionsBus
        TransportMode.TRAIN, TransportMode.METRO -> Icons.Default.DirectionsTransit
        TransportMode.WALK -> Icons.Default.DirectionsWalk
        TransportMode.OTHER -> Icons.Default.Traffic
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommuteRouteScreen(
    repository: TransactionRepository,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val journeys by repository.observeCommuteJourneys().collectAsState(initial = emptyList())
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingJourneyWithLegs by remember { mutableStateOf<JourneyWithLegs?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                screenTitle = "Commute Routes & Traffic",
                showBack = false
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingJourneyWithLegs = null
                    showAddEditDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Commute Journey")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---------------------------------------------------------
            // 1. TRAVERSE TIME & TRAFFIC DENSITY LEGEND CARD
            // ---------------------------------------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Traffic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Traffic Density & Traverse Time Legend",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Green: Free Flow
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text("Low (Free Flow)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), fontWeight = FontWeight.Bold)
                                    Text("0-10 min (On Time)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Yellow: Moderate
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF9A825)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text("Moderate", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), fontWeight = FontWeight.Bold)
                                    Text("+3–5 min delay", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Orange: Heavy
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFE65100)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text("Heavy", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), fontWeight = FontWeight.Bold)
                                    Text("+10–15 min", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Red: Severe Delay
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFC62828)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text("Severe", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), fontWeight = FontWeight.Bold)
                                    Text("+20+ min delay", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------
            // 2. COMMUTE JOURNEYS LIST (With Multi-Leg Segment Visualizer)
            // ---------------------------------------------------------
            if (journeys.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsTransit,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "No Multi-Modal Commute Journeys",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap the + button to build a multi-leg daily commute (e.g. Local Uber -> Bus Hub -> City Taxi -> Last Mile).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(journeys, key = { it.journey.id }) { journeyWithLegs ->
                    CommuteJourneyCard(
                        journeyWithLegs = journeyWithLegs,
                        onEdit = {
                            editingJourneyWithLegs = journeyWithLegs
                            showAddEditDialog = true
                        },
                        onDelete = {
                            scope.launch {
                                repository.deleteCommuteJourney(journeyWithLegs.journey)
                            }
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Multi-Leg Commute Journey Modal
    if (showAddEditDialog) {
        AddEditCommuteJourneyDialog(
            journeyWithLegs = editingJourneyWithLegs,
            onDismiss = { showAddEditDialog = false },
            onSave = { journey, legs ->
                scope.launch {
                    repository.saveCommuteJourney(journey, legs)
                    showAddEditDialog = false
                }
            }
        )
    }
}

@Composable
fun CommuteJourneyCard(
    journeyWithLegs: JourneyWithLegs,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val journey = journeyWithLegs.journey
    val legs = journeyWithLegs.legs.sortedBy { it.legOrder }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Journey Name, Cost Badges, Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = journey.journeyName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (journey.isDefaultWorkday) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Default Workday",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${legs.size} Connecting Legs • Total Est. Time: ~${journeyWithLegs.totalTraverseTimeMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Journey", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Journey", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Cost Summary Strip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Cost: ${CurrencyUtils.format(journeyWithLegs.totalDailyCost)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Monthly Budget: ${CurrencyUtils.format(journeyWithLegs.totalMonthlyBudget)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Multi-Leg Route Visualization Strip
            Text("Route Segments & Traffic Conditions:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

            legs.forEachIndexed { index, leg ->
                val trafficColor = getTrafficColor(leg.trafficDensity)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, trafficColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = trafficColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = getTransportIcon(leg.mode),
                                        contentDescription = leg.mode.displayName,
                                        tint = trafficColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = "Leg ${index + 1}: ${leg.legName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${leg.mode.displayName} • ${CurrencyUtils.format(leg.farePerTrip)}/trip",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (leg.estimatedDelayMinutes > 0) {
                                        Text(
                                            text = "+${leg.estimatedDelayMinutes}m delay",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = trafficColor
                                        )
                                    }
                                }
                            }
                        }

                        // Traffic Density Badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = trafficColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${leg.trafficDensity} (~${leg.traverseTimeMinutes + leg.estimatedDelayMinutes}m)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = trafficColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditCommuteJourneyDialog(
    journeyWithLegs: JourneyWithLegs?,
    onDismiss: () -> Unit,
    onSave: (journey: CommuteJourneyEntity, legs: List<CommuteLegEntity>) -> Unit
) {
    var journeyName by remember { mutableStateOf(journeyWithLegs?.journey?.journeyName ?: "") }
    var isDefaultWorkday by remember { mutableStateOf(journeyWithLegs?.journey?.isDefaultWorkday ?: true) }

    val legsState = remember {
        mutableStateListOf<CommuteLegEntity>().apply {
            if (journeyWithLegs != null && journeyWithLegs.legs.isNotEmpty()) {
                addAll(journeyWithLegs.legs.sortedBy { it.legOrder })
            } else {
                // Default 3 sample connecting legs: Local Uber -> Bus -> City Taxi
                add(CommuteLegEntity(legName = "Home to Node", mode = TransportMode.UBER, farePerTrip = 5.00, tripsPerDay = 2, workingDaysPerMonth = 20, trafficDensity = "LOW", traverseTimeMinutes = 10))
                add(CommuteLegEntity(legName = "Node to Station", mode = TransportMode.BUS, farePerTrip = 2.50, tripsPerDay = 2, workingDaysPerMonth = 20, trafficDensity = "MODERATE", estimatedDelayMinutes = 5, traverseTimeMinutes = 20))
                add(CommuteLegEntity(legName = "Station to Office", mode = TransportMode.TAXI, farePerTrip = 4.00, tripsPerDay = 2, workingDaysPerMonth = 20, trafficDensity = "HEAVY", estimatedDelayMinutes = 10, traverseTimeMinutes = 15))
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(640.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (journeyWithLegs == null) "New Multi-Modal Commute" else "Edit Commute Journey",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Journey Name Input
                OutlinedTextField(
                    value = journeyName,
                    onValueChange = { journeyName = it },
                    label = { Text("Journey / Route Name (e.g. Work Commute)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Multi-Legs Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Commute Connecting Legs (${legsState.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            legsState.add(
                                CommuteLegEntity(
                                    legName = "Next Leg",
                                    mode = TransportMode.TAXI,
                                    farePerTrip = 0.0,
                                    tripsPerDay = 2,
                                    workingDaysPerMonth = 20,
                                    trafficDensity = "LOW"
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Leg")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(legsState) { index, leg ->
                        var legName by remember { mutableStateOf(leg.legName) }
                        var mode by remember { mutableStateOf(leg.mode) }
                        var fareText by remember { mutableStateOf(if (leg.farePerTrip > 0) leg.farePerTrip.toString() else "") }
                        var trafficDensity by remember { mutableStateOf(leg.trafficDensity) }
                        var showModeMenu by remember { mutableStateOf(false) }
                        var showTrafficMenu by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Leg ${index + 1}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    if (legsState.size > 1) {
                                        IconButton(
                                            onClick = { legsState.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove Leg", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = legName,
                                        onValueChange = {
                                            legName = it
                                            legsState[index] = legsState[index].copy(legName = it)
                                        },
                                        label = { Text("Leg Name") },
                                        modifier = Modifier.weight(1.3f),
                                        singleLine = true
                                    )

                                    // Mode Selector
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { showModeMenu = true },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(mode.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
                                        }
                                        DropdownMenu(
                                            expanded = showModeMenu,
                                            onDismissRequest = { showModeMenu = false }
                                        ) {
                                            TransportMode.values().forEach { m ->
                                                DropdownMenuItem(
                                                    text = { Text(m.displayName) },
                                                    onClick = {
                                                        mode = m
                                                        legsState[index] = legsState[index].copy(mode = m)
                                                        showModeMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = fareText,
                                        onValueChange = {
                                            fareText = it
                                            val fare = it.toDoubleOrNull() ?: 0.0
                                            legsState[index] = legsState[index].copy(farePerTrip = fare)
                                        },
                                        label = { Text("Fare/Trip") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    // Traffic Density Selector
                                    Box(modifier = Modifier.weight(1.3f)) {
                                        OutlinedButton(
                                            onClick = { showTrafficMenu = true },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("Traffic: $trafficDensity", fontSize = 11.sp, maxLines = 1)
                                        }
                                        DropdownMenu(
                                            expanded = showTrafficMenu,
                                            onDismissRequest = { showTrafficMenu = false }
                                        ) {
                                            listOf("LOW", "MODERATE", "HEAVY", "SEVERE").forEach { d ->
                                                DropdownMenuItem(
                                                    text = { Text(d) },
                                                    onClick = {
                                                        trafficDensity = d
                                                        legsState[index] = legsState[index].copy(trafficDensity = d)
                                                        showTrafficMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (journeyName.isNotBlank() && legsState.isNotEmpty()) {
                                val journey = (journeyWithLegs?.journey ?: CommuteJourneyEntity(journeyName = journeyName))
                                    .copy(journeyName = journeyName, isDefaultWorkday = isDefaultWorkday)
                                onSave(journey, legsState.toList())
                            }
                        }
                    ) {
                        Text("Save Journey")
                    }
                }
            }
        }
    }
}
