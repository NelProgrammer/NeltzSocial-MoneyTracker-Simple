package com.moneytracker.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moneytracker.data.repository.TransactionRepository
import com.moneytracker.ui.components.AppTopBar
import com.moneytracker.ui.components.ProfileManagementDialog
import com.moneytracker.ui.viewmodel.ProfileViewModel
import com.moneytracker.ui.viewmodel.SettingsViewModel
import com.moneytracker.util.AppThemeMode
import com.moneytracker.util.AppThemePalette
import com.moneytracker.util.DataExportImportManager
import com.moneytracker.util.ExportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    profileViewModel: ProfileViewModel,
    repository: TransactionRepository,
    contentPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    onSwitchProfile: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()
    val activeProfile by profileViewModel.activeProfile.collectAsState()
    var showProfileDialog by remember { mutableStateOf(false) }

    // Multi-Format Export/Import State
    var selectedExportFormat by remember { mutableStateOf(ExportFormat.EXCEL) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    // SAF Document Launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(selectedExportFormat.mimeType)
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isExporting = true
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            DataExportImportManager.exportData(repository, selectedExportFormat, outputStream)
                        }
                    }
                    Toast.makeText(context, "Export completed successfully (${selectedExportFormat.extension.uppercase()})", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    isExporting = false
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isImporting = true
                try {
                    val result = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            DataExportImportManager.importData(repository, selectedExportFormat, inputStream)
                        }
                    }
                    if (result != null && result.success) {
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, result?.message ?: "Import failed", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    isImporting = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                screenTitle = "Settings",
                showBack = true,
                onBack = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profile Management Section Card
            if (activeProfile != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Active Profile: ${activeProfile!!.username}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (activeProfile!!.isGuest) "Currently signed in as Guest (Data isolated to this session)."
                            else if (activeProfile!!.isPasswordProtected) "Permanent Profile (Password Protected)."
                            else "Permanent Profile (No Password Protection).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showProfileDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (activeProfile!!.isGuest) "Convert Guest Profile" else "Edit Profile")
                            }
                            OutlinedButton(
                                onClick = {
                                    profileViewModel.logout(context)
                                    onSwitchProfile()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Switch Profile")
                            }
                        }
                    }
                }
            }

            // 2. Data Backup, Export & Import Card (Multi-Format: JSON, XML, CSV, Excel)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Data Backup & Export",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Data Backup, Export & Import",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Export or import your complete financial data (Transactions, Groceries, Shopping Lists, Categories) in multiple standard formats.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Format Selection Chips
                    Text(
                        text = "Select Format:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExportFormat.values().forEach { fmt ->
                            FilterChip(
                                selected = selectedExportFormat == fmt,
                                onClick = { selectedExportFormat = fmt },
                                label = {
                                    Text(
                                        text = fmt.extension.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selectedExportFormat == fmt) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Export / Import Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = java.time.LocalDate.now().toString()
                                val defaultName = "MoneyTracker_Backup_${timestamp}.${selectedExportFormat.extension}"
                                exportLauncher.launch(defaultName)
                            },
                            enabled = !isExporting && !isImporting,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf(selectedExportFormat.mimeType, "*/*"))
                            },
                            enabled = !isExporting && !isImporting,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import")
                            }
                        }
                    }
                }
            }

            // 3. Appearance & Themes Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme Palette",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Appearance & Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Theme Mode Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.values().forEach { mode ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.updateThemeMode(mode) },
                                label = {
                                    Text(
                                        text = when (mode) {
                                            AppThemeMode.SYSTEM -> "System"
                                            AppThemeMode.LIGHT -> "Light"
                                            AppThemeMode.DARK -> "Dark"
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (settings.themeMode == mode) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(
                        text = "Color Palette",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 4 Palette Options
                    val palettes = listOf(
                        Triple(AppThemePalette.EMERALD_GREEN, "Emerald Green", Color(0xFF2E7D32)),
                        Triple(AppThemePalette.OCEAN_BLUE, "Ocean Blue", Color(0xFF1565C0)),
                        Triple(AppThemePalette.ROYAL_VIOLET, "Royal Violet", Color(0xFF7B1FA2)),
                        Triple(AppThemePalette.SUNSET_AMBER, "Sunset Amber", Color(0xFFD84315))
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        palettes.chunked(2).forEach { rowPalettes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPalettes.forEach { (palette, label, swatchColor) ->
                                    val isSelected = settings.themePalette == palette
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { viewModel.updateThemePalette(palette) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) swatchColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) swatchColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(swatchColor)
                                            )
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) swatchColor else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Testing Controls Section Card (Hide Ryu Toggle)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Testing Mode",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Hide Ryu Profile (Testing Mode)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = settings.isRyuHidden,
                            onCheckedChange = { viewModel.updateIsRyuHidden(it) }
                        )
                    }

                    Text(
                        text = "When enabled, hides profile 'Ryu' from startup checks and profile selection, allowing you to test the scenario where no permanent local profile exists.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Recurrence & PayDate Cycle Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 5. PayDate Day Setting Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "PayDate Day",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "PayDate Day of Month",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Determines the start day of your monthly financial cycle and the date generated recurring child instances are fixed to (Default: 20th).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = settings.payDateDay.toString(),
                        onValueChange = { input ->
                            val num = input.filter { it.isDigit() }.toIntOrNull()
                            if (num != null && num in 1..31) {
                                viewModel.updatePayDateDay(num)
                            }
                        },
                        label = { Text("PayDate Day (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 6. Calculation Cutoff Day Setting Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Cutoff Day",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Calculation Cutoff Day",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Determines the monthly milestone date when automatic recurrence calculations run (Default: 18th).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = settings.cutoffDay.toString(),
                        onValueChange = { input ->
                            val num = input.filter { it.isDigit() }.toIntOrNull()
                            if (num != null && num in 1..31) {
                                viewModel.updateCutoffDay(num)
                            }
                        },
                        label = { Text("Cutoff Day (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 7. Morning Slot Cutoff Hour Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Morning Slot Cutoff",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Taxi Morning Slot Cutoff Hour",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val hourLabel = when {
                        settings.morningCutoffHour == 12 -> "12:00 PM (Noon)"
                        settings.morningCutoffHour < 12 -> "${settings.morningCutoffHour}:00 AM"
                        else -> "${settings.morningCutoffHour - 12}:00 PM"
                    }

                    Text(
                        text = "Current cutoff: $hourLabel. Trips logged before this hour are classified as Morning trips, and trips logged after are classified as After-Hours / Evening trips (Default: 12:00 PM).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = settings.morningCutoffHour.toString(),
                        onValueChange = { input ->
                            val num = input.filter { it.isDigit() }.toIntOrNull()
                            if (num != null && num in 1..23) {
                                viewModel.updateMorningCutoffHour(num)
                            }
                        },
                        label = { Text("Cutoff Hour (1-23, e.g. 12 for 12:00 PM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showProfileDialog && activeProfile != null) {
        ProfileManagementDialog(
            profile = activeProfile!!,
            onDismiss = { showProfileDialog = false },
            onConfirmSave = { username, isPasswordProtected, password ->
                profileViewModel.updateProfileSettings(
                    context = context,
                    profile = activeProfile!!,
                    newUsername = username,
                    isPasswordProtected = isPasswordProtected,
                    newPasswordInput = password,
                    onSuccess = { showProfileDialog = false },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            },
            onConvertGuest = { username, isPasswordProtected, password ->
                profileViewModel.convertGuestProfile(
                    context = context,
                    newUsername = username,
                    isPasswordProtected = isPasswordProtected,
                    passwordInput = password,
                    onSuccess = { showProfileDialog = false },
                    onError = { error ->
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}
