package com.moneytracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.moneytracker.data.local.entity.ProfileEntity

@Composable
fun ProfileManagementDialog(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    onConfirmSave: (newUsername: String, isPasswordProtected: Boolean, newPassword: String?) -> Unit,
    onConvertGuest: ((newUsername: String, isPasswordProtected: Boolean, newPassword: String?) -> Unit)? = null
) {
    var username by remember { mutableStateOf(if (profile.isGuest) "" else profile.username) }
    var isPasswordProtected by remember { mutableStateOf(profile.isPasswordProtected) }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (profile.isGuest) "Convert Guest Profile to Permanent" else "Edit Profile Settings")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMessage = null
                    },
                    label = { Text("Username (4-20 chars, no spaces)") },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Password Protection",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = isPasswordProtected,
                        onCheckedChange = { isPasswordProtected = it }
                    )
                }

                if (isPasswordProtected) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Password (4-10 chars, no spaces)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (profile.isGuest && onConvertGuest != null) {
                        onConvertGuest(username, isPasswordProtected, if (isPasswordProtected) password else null)
                    } else {
                        onConfirmSave(username, isPasswordProtected, if (isPasswordProtected) password else null)
                    }
                }
            ) {
                Text(if (profile.isGuest) "Convert & Save" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
