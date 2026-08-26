package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricAuthManager
import com.example.util.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityVerificationDialog(
    title: String,
    subtitle: String,
    settingsManager: SettingsManager,
    showRecoverySnapshotOption: Boolean = false,
    recoverySnapshotChecked: Boolean = true,
    onRecoverySnapshotCheckedChange: (Boolean) -> Unit = {},
    lastBackupTime: Long = 0L,
    onRestoreBackup: (() -> Unit)? = null,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Format date for last snapshot
    val lastBackupFormatted = remember(lastBackupTime) {
        if (lastBackupTime == 0L) null
        else {
            try {
                java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(lastBackupTime))
            } catch (e: Exception) {
                null
            }
        }
    }

    val activity = context as? FragmentActivity

    fun triggerBiometric() {
        if (activity != null && BiometricAuthManager.isBiometricHardwareAvailable(context)) {
            BiometricAuthManager.promptBiometricAuth(
                activity = activity,
                title = "Security Verification",
                subtitle = subtitle,
                negativeButtonText = "Use PIN Code",
                onSuccess = {
                    onSuccess()
                    onDismiss()
                },
                onError = { err ->
                    errorMessage = "Fingerprint Auth Failed: $err"
                },
                onUsePinFallback = {
                    errorMessage = "Please authenticate using your 4-digit Security PIN"
                }
            )
        } else {
            errorMessage = "Fingerprint scan unavailable. Enter Security PIN."
        }
    }

    // Auto-trigger biometric verification on start if hardware is available
    LaunchedEffect(Unit) {
        triggerBiometric()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Optional Recovery Option Checkbox
                if (showRecoverySnapshotOption) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = recoverySnapshotChecked,
                                    onCheckedChange = onRecoverySnapshotCheckedChange
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Auto-create backup point before clearing",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Highly recommended. This saves a restorable JSON snapshot point of your questions locally so you can recover them later.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 36.dp)
                            )
                        }
                    }
                }

                // Restore/Recovery Button if backup point is available
                if (onRestoreBackup != null && lastBackupFormatted != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Local Recovery Point Found",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Created: $lastBackupFormatted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    onRestoreBackup()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Restore Data", fontSize = 10.sp)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // PIN Input Field
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            pinInput = it
                        }
                    },
                    label = { Text("Enter 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Scan Fingerprint Manual Retry Button
                if (BiometricAuthManager.isBiometricHardwareAvailable(context)) {
                    OutlinedButton(
                        onClick = { triggerBiometric() },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Fingerprint Scanner")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinInput == settingsManager.pinCode) {
                        errorMessage = null
                        onSuccess()
                        onDismiss()
                    } else {
                        errorMessage = "Incorrect PIN. Please try again."
                    }
                },
                enabled = pinInput.length == 4
            ) {
                Text("Confirm / Authorize")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
