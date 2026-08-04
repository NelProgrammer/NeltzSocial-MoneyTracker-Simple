package com.moneytracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moneytracker.ui.components.ProfileManagementDialog
import com.moneytracker.ui.viewmodel.ProfileViewModel
import com.moneytracker.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    profileViewModel: ProfileViewModel,
    contentPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    onSwitchProfile: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val activeProfile by profileViewModel.activeProfile.collectAsState()
    var showProfileDialog by remember { mutableStateOf(false) }

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

            // 2. Testing Controls Section Card (Hide Ryu Toggle)
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

            // 3. PayDate Day Setting Card
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

            // 4. Calculation Cutoff Day Setting Card
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
                    onError = { }
                )
            },
            onConvertGuest = { username, isPasswordProtected, password ->
                profileViewModel.convertGuestProfile(
                    context = context,
                    newUsername = username,
                    isPasswordProtected = isPasswordProtected,
                    passwordInput = password,
                    onSuccess = { showProfileDialog = false },
                    onError = { }
                )
            }
        )
    }
}
