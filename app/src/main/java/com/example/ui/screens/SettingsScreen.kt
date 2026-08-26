package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.OtsViewModel
import com.example.util.NetworkStorageManager
import com.example.util.GoogleDriveSyncManager
import com.example.util.GoogleSignInHelper
import com.example.util.SettingsManager
import kotlinx.coroutines.launch
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: OtsViewModel,
    settingsManager: SettingsManager,
    onLockAppNow: () -> Unit,
    onGoogleSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showRecycleBinDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings & Backup",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // --- 1. USER PROFILE & ACCOUNT ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (settingsManager.isGoogleSignedIn && settingsManager.googlePhotoUrl.isNotBlank()) {
                            coil.compose.AsyncImage(
                                model = settingsManager.googlePhotoUrl,
                                contentDescription = "Profile Picture",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Text(
                            text = "User Profile & Institution Credentials",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()

                    var name by remember { mutableStateOf(settingsManager.userName) }
                    var role by remember { mutableStateOf(settingsManager.userRole) }
                    var inst by remember { mutableStateOf(settingsManager.userInstitution) }
                    var email by remember { mutableStateOf(settingsManager.userEmail) }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; settingsManager.userName = it },
                        label = { Text("Examiner / Author Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it; settingsManager.userRole = it },
                        label = { Text("Designation / Role") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inst,
                        onValueChange = { inst = it; settingsManager.userInstitution = it },
                        label = { Text("Institution / Department") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; settingsManager.userEmail = it },
                        label = { Text("Contact Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // --- 2. APP LOCK & BIOMETRIC SECURITY ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "App Lock & Security Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()

                    var isAppLockEnabled by remember { mutableStateOf(settingsManager.isAppLockEnabled) }
                    var isBiometricEnabled by remember { mutableStateOf(settingsManager.isBiometricEnabled) }
                    var lockOnBg by remember { mutableStateOf(settingsManager.lockOnBackground) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable App PIN Lock", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Protect question banks with security PIN code",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = {
                                isAppLockEnabled = it
                                settingsManager.isAppLockEnabled = it
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric / Fingerprint Unlock", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Unlock quickly using device fingerprint or face recognition",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = {
                                isBiometricEnabled = it
                                settingsManager.isBiometricEnabled = it
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lock Immediately on Background", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Require PIN code when resuming application",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = lockOnBg,
                            onCheckedChange = {
                                lockOnBg = it
                                settingsManager.lockOnBackground = it
                            }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showChangePinDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change PIN Code")
                        }

                        Button(
                            onClick = onLockAppNow,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lock App Now")
                        }
                    }
                }
            }
        }

        // --- 3. GOOGLE DRIVE SYNC & BACKUP ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Google Drive Cloud Backup & Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()

                    var drivePath by remember { mutableStateOf(settingsManager.googleDriveBackupPath) }
                    var isSyncing by remember { mutableStateOf(false) }
                    var showDriveFolderPicker by remember { mutableStateOf(false) }

                    Text(
                        text = "Backup your entire question bank securely to Google Drive. Specify your preferred backup folder location below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = drivePath,
                            onValueChange = {
                                drivePath = it
                                settingsManager.googleDriveBackupPath = it
                            },
                            label = { Text("Google Drive Backup Location") },
                            placeholder = { Text("My Drive/QuestionBank_Backup/backup.json") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Folder, contentDescription = null)
                            }
                        )
                        IconButton(onClick = {
                            if (settingsManager.isGoogleSignedIn) {
                                showDriveFolderPicker = true
                            } else {
                                Toast.makeText(context, "Please sign in with Google first", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Browse Google Drive")
                        }
                    }

                    if (showDriveFolderPicker) {
                        GoogleDriveFolderPickerDialog(
                            onDismiss = { showDriveFolderPicker = false },
                            onSelect = { fullPath, folderId ->
                                drivePath = fullPath
                                settingsManager.googleDriveBackupPath = fullPath
                                showDriveFolderPicker = false
                            },
                            coroutineScope = scope
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isSyncing = true
                                GoogleDriveSyncManager.backupToDrive(context, viewModel, settingsManager) { success, msg ->
                                    isSyncing = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Backup to Drive")
                        }

                        OutlinedButton(
                            onClick = {
                                isSyncing = true
                                GoogleDriveSyncManager.restoreFromDrive(context, viewModel, settingsManager) { success, msg ->
                                    isSyncing = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore")
                        }
                    }
                }
            }
        }

        // --- 3.5. OWNER WHITELIST MANAGEMENT ---
        if (com.example.util.WhitelistManager.isOwner(settingsManager.googleAccountEmail)) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Owner Whitelist Management",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider()

                        var whitelist by remember { mutableStateOf<List<String>>(emptyList()) }
                        var newEmailInput by remember { mutableStateOf("") }
                        var isLoadingWhitelist by remember { mutableStateOf(false) }
                        var isAddingEmail by remember { mutableStateOf(false) }

                        // Fetch Whitelist Function
                        val refreshWhitelist: () -> Unit = {
                            scope.launch {
                                isLoadingWhitelist = true
                                whitelist = com.example.util.WhitelistManager.getWhitelist()
                                isLoadingWhitelist = false
                            }
                        }

                        // Load Whitelist Initially
                        LaunchedEffect(Unit) {
                            refreshWhitelist()
                        }

                        Text(
                            text = "Add and manage whitelisted users. Only emails added below can log in to the application.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newEmailInput,
                                onValueChange = { newEmailInput = it },
                                label = { Text("User Email Address") },
                                placeholder = { Text("user@gmail.com") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                }
                            )

                            Button(
                                onClick = {
                                    if (newEmailInput.isNotBlank()) {
                                        scope.launch {
                                            isAddingEmail = true
                                            val success = com.example.util.WhitelistManager.addEmailToWhitelist(newEmailInput.trim())
                                            isAddingEmail = false
                                            if (success) {
                                                Toast.makeText(context, "Added successfully", Toast.LENGTH_SHORT).show()
                                                newEmailInput = ""
                                                refreshWhitelist()
                                            } else {
                                                Toast.makeText(context, "Failed to add email", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isAddingEmail && newEmailInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isAddingEmail) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Add")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Whitelisted Users:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (isLoadingWhitelist) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (whitelist.isEmpty()) {
                            Text(
                                text = "No external users whitelisted yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                whitelist.forEach { email ->
                                    var isDeletingUser by remember { mutableStateOf(false) }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = email,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    isDeletingUser = true
                                                    val success = com.example.util.WhitelistManager.removeEmailFromWhitelist(email)
                                                    isDeletingUser = false
                                                    if (success) {
                                                        Toast.makeText(context, "Removed successfully", Toast.LENGTH_SHORT).show()
                                                        refreshWhitelist()
                                                    } else {
                                                        Toast.makeText(context, "Failed to remove email", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            enabled = !isDeletingUser,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            if (isDeletingUser) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.error)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove from Whitelist",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. EXAM & PRINT DEFAULTS ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Exam Paper & Watermark Defaults",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()

                    var defaultInst by remember { mutableStateOf(settingsManager.defaultInstitute) }
                    var defaultCode by remember { mutableStateOf(settingsManager.defaultPaperCode) }

                    OutlinedTextField(
                        value = defaultInst,
                        onValueChange = { defaultInst = it; settingsManager.defaultInstitute = it },
                        label = { Text("Default Header / Institution Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = defaultCode,
                        onValueChange = { defaultCode = it; settingsManager.defaultPaperCode = it },
                        label = { Text("Default Paper Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    HorizontalDivider()

                    var watermarkEnabled by remember { mutableStateOf(settingsManager.watermarkEnabled) }
                    var watermarkText by remember { mutableStateOf(settingsManager.watermarkText) }
                    var isCursive by remember { mutableStateOf(settingsManager.watermarkIsCursive) }
                    var size by remember { mutableStateOf(settingsManager.watermarkSize) }
                    var opacity by remember { mutableStateOf(settingsManager.watermarkOpacity) }
                    var angle by remember { mutableStateOf(settingsManager.watermarkAngle) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Permanent PDF Security Watermark", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Embeds non-removable security watermarks on generated question paper PDFs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = watermarkEnabled,
                            onCheckedChange = {
                                watermarkEnabled = it
                                settingsManager.watermarkEnabled = it
                            }
                        )
                    }

                    if (watermarkEnabled) {
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it; settingsManager.watermarkText = it },
                            label = { Text("Watermark Text") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cursive / Calligraphy Font Style", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isCursive,
                                onCheckedChange = {
                                    isCursive = it
                                    settingsManager.watermarkIsCursive = it
                                }
                            )
                        }

                        Text("Watermark Size: ${size.toInt()} pt", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = size,
                            onValueChange = { size = it; settingsManager.watermarkSize = it },
                            valueRange = 10f..60f
                        )

                        Text("Opacity: ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = opacity,
                            onValueChange = { opacity = it; settingsManager.watermarkOpacity = it },
                            valueRange = 0.05f..0.8f
                        )

                        Text("Rotation Angle: ${angle.toInt()}°", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = angle,
                            onValueChange = { angle = it; settingsManager.watermarkAngle = it },
                            valueRange = -90f..90f
                        )
                    }
                }
            }
        }

        // --- 5. FTP REMOTE STORAGE INTEGRATION ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var isFtpConnectionValid by remember { mutableStateOf(settingsManager.isFtpConnectionValid) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "FTP Remote Storage Integration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (isFtpConnectionValid) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid Connection",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider()

                    var ftpHost by remember { mutableStateOf(settingsManager.ftpHost) }
                    var ftpPort by remember { mutableStateOf(settingsManager.ftpPort.toString()) }
                    var ftpUser by remember { mutableStateOf(settingsManager.ftpUser) }
                    var ftpPass by remember { mutableStateOf(settingsManager.ftpPass) }
                    var ftpRemoteDir by remember { mutableStateOf(settingsManager.ftpRemoteDir) }
                    var ftpUsePassive by remember { mutableStateOf(settingsManager.ftpUsePassive) }
                    var ftpUseFtps by remember { mutableStateOf(settingsManager.ftpUseFtps) }

                    var isFtpOperating by remember { mutableStateOf(false) }
                    var ftpStatusMessage by remember { mutableStateOf("") }

                    Text(
                        text = "Link a remote FTP/FTPS or SMB server for centralized backup (Auto-detects smb:// or ftp:// prefixes):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ftpHost,
                            onValueChange = { 
                                ftpHost = it
                                settingsManager.ftpHost = it
                                settingsManager.isFtpConnectionValid = false
                                isFtpConnectionValid = false
                            },
                            label = { Text("Host/IP (prefix smb:// or ftp://)") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ftpPort,
                            onValueChange = {
                                ftpPort = it
                                settingsManager.ftpPort = it.toIntOrNull() ?: 21
                                settingsManager.isFtpConnectionValid = false
                                isFtpConnectionValid = false
                            },
                            label = { Text("Port") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ftpUser,
                            onValueChange = { 
                                ftpUser = it
                                settingsManager.ftpUser = it
                                settingsManager.isFtpConnectionValid = false
                                isFtpConnectionValid = false
                            },
                            label = { Text("Username") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ftpPass,
                            onValueChange = { 
                                ftpPass = it
                                settingsManager.ftpPass = it
                                settingsManager.isFtpConnectionValid = false
                                isFtpConnectionValid = false
                            },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    var showFolderPicker by remember { mutableStateOf(false) }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ftpRemoteDir,
                            onValueChange = { ftpRemoteDir = it; settingsManager.ftpRemoteDir = it },
                            label = { Text("Remote Server Directory Folder") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = { 
                            if (ftpHost.isNotBlank()) {
                                showFolderPicker = true 
                            } else {
                                Toast.makeText(context, "Please enter Server Host first", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Browse")
                        }
                    }
                    
                    if (showFolderPicker) {
                        RemoteFolderPickerDialog(
                            initialDir = ftpRemoteDir,
                            onDismiss = { showFolderPicker = false },
                            onSelect = { 
                                ftpRemoteDir = it
                                settingsManager.ftpRemoteDir = it
                                showFolderPicker = false 
                            },
                            host = ftpHost,
                            port = ftpPort.toIntOrNull() ?: 21,
                            user = ftpUser,
                            pass = ftpPass,
                            usePassive = ftpUsePassive,
                            useFtps = ftpUseFtps,
                            coroutineScope = scope
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = ftpUsePassive,
                            onClick = {
                                val newVal = !ftpUsePassive
                                ftpUsePassive = newVal
                                settingsManager.ftpUsePassive = newVal
                            },
                            label = { Text(if (ftpUsePassive) "Passive Mode (PASV)" else "Active Mode (PORT)") },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = ftpUseFtps,
                            onClick = {
                                val newFtps = !ftpUseFtps
                                ftpUseFtps = newFtps
                                settingsManager.ftpUseFtps = newFtps
                                if (newFtps && ftpPort == "21") {
                                    ftpPort = "990"
                                    settingsManager.ftpPort = 990
                                }
                                settingsManager.isFtpConnectionValid = false
                                isFtpConnectionValid = false
                            },
                            label = { Text(if (ftpUseFtps) "FTPS (TLS)" else "Standard FTP") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // FTP Auto Sync Settings
                    var ftpAutoSync by remember { mutableStateOf<Boolean>(settingsManager.ftpAutoSync) }
                    var autoSyncIntervalMins by remember { mutableStateOf<Int>(settingsManager.autoSyncIntervalMins) }
                    var showIntervalDropdown by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FTP/Cloud Auto Backup", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Automatically sync database in background",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = ftpAutoSync,
                            onCheckedChange = {
                                ftpAutoSync = it
                                settingsManager.ftpAutoSync = it
                                viewModel.startAutoSyncLoop()
                            },
                            enabled = isFtpConnectionValid
                        )
                    }

                    if (ftpAutoSync) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sync Interval", fontWeight = FontWeight.Medium)
                            Box {
                                OutlinedButton(onClick = { showIntervalDropdown = true }) {
                                    Text("$autoSyncIntervalMins Minutes")
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = showIntervalDropdown,
                                    onDismissRequest = { showIntervalDropdown = false }
                                ) {
                                    listOf(1, 5, 15, 30, 60, 120).forEach { mins ->
                                        DropdownMenuItem(
                                            text = { Text("$mins Minutes") },
                                            onClick = {
                                                autoSyncIntervalMins = mins
                                                settingsManager.autoSyncIntervalMins = mins
                                                showIntervalDropdown = false
                                                viewModel.startAutoSyncLoop()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isFtpOperating || ftpStatusMessage.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            if (isFtpOperating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = ftpStatusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (ftpHost.isBlank()) {
                                    Toast.makeText(context, "Please enter Server Host IP or domain", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                isFtpOperating = true
                                ftpStatusMessage = "Testing connection..."
                                scope.launch {
                                    val res = NetworkStorageManager.testConnection(
                                        host = ftpHost.trim(),
                                        port = ftpPort.toIntOrNull() ?: 21,
                                        user = ftpUser.trim(),
                                        pass = ftpPass,
                                        remoteDir = ftpRemoteDir.trim(),
                                        usePassive = ftpUsePassive,
                                        useFtps = ftpUseFtps
                                    )
                                    isFtpOperating = false
                                    res.fold(
                                        onSuccess = { msg ->
                                            ftpStatusMessage = "Connected to $ftpHost"
                                            settingsManager.isFtpConnectionValid = true
                                            isFtpConnectionValid = true
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        },
                                        onFailure = { err ->
                                            ftpStatusMessage = "Connection Error: ${err.message}"
                                            settingsManager.isFtpConnectionValid = false
                                            isFtpConnectionValid = false
                                            Toast.makeText(context, err.message ?: "Connection Error", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isFtpOperating,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Test Link", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (ftpHost.isBlank()) {
                                    Toast.makeText(context, "Please enter Server Host", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isFtpOperating = true
                                ftpStatusMessage = "Uploading database..."
                                scope.launch {
                                    val jsonPayload = viewModel.exportQuestionsToJson()
                                    val res = NetworkStorageManager.uploadJson(
                                        host = ftpHost.trim(),
                                        port = ftpPort.toIntOrNull() ?: 21,
                                        user = ftpUser.trim(),
                                        pass = ftpPass,
                                        remoteDir = ftpRemoteDir.trim(),
                                        fileName = "ots_question_bank_backup.json",
                                        jsonContent = jsonPayload,
                                        usePassive = ftpUsePassive,
                                        useFtps = ftpUseFtps
                                    )
                                    isFtpOperating = false
                                    res.fold(
                                        onSuccess = { msg ->
                                            settingsManager.ftpLastSyncTime = System.currentTimeMillis()
                                            ftpStatusMessage = "Synced to remote folder: $ftpRemoteDir"
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        },
                                        onFailure = { err ->
                                            ftpStatusMessage = "Upload Failed: ${err.message}"
                                            Toast.makeText(context, err.message ?: "Sync Failed", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1.1f),
                            enabled = !isFtpOperating,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Sync To Remote", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (ftpHost.isBlank()) {
                                    Toast.makeText(context, "Please enter Server Host", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                isFtpOperating = true
                                ftpStatusMessage = "Downloading backup..."
                                scope.launch {
                                    val res = NetworkStorageManager.downloadLatestJson(
                                        host = ftpHost.trim(),
                                        port = ftpPort.toIntOrNull() ?: 21,
                                        user = ftpUser.trim(),
                                        pass = ftpPass,
                                        remoteDir = ftpRemoteDir.trim(),
                                        fileName = "ots_question_bank_backup.json",
                                        usePassive = ftpUsePassive,
                                        useFtps = ftpUseFtps
                                    )
                                    isFtpOperating = false
                                    res.fold(
                                        onSuccess = { jsonContent ->
                                            val importRes = viewModel.importQuestionsFromJson(jsonContent)
                                            if (importRes.first) {
                                                ftpStatusMessage = "Restored ${importRes.second} items"
                                                Toast.makeText(context, "Restored ${importRes.second} items from remote server!", Toast.LENGTH_LONG).show()
                                            } else {
                                                ftpStatusMessage = "Parse Error"
                                                Toast.makeText(context, "Invalid JSON data on remote server", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        onFailure = { err ->
                                            ftpStatusMessage = "Download Failed: ${err.message}"
                                            Toast.makeText(context, err.message ?: "Download Failed", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isFtpOperating,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Restore", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- 6. LOCAL DEVICE STORAGE EXPORT / IMPORT ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Local Device Export & Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()

                    Text(
                        text = "Export your question bank to a standard JSON file on your local device storage (e.g. Downloads folder) or import an existing backup file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val exportLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument("application/json")
                    ) { uri: Uri? ->
                        if (uri != null) {
                            scope.launch {
                                try {
                                    val json = viewModel.exportQuestionsToJson()
                                    context.contentResolver.openOutputStream(uri)?.use { os ->
                                        os.write(json.toByteArray(Charsets.UTF_8))
                                    }
                                    Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }

                    val importLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            scope.launch {
                                try {
                                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                                    if (json != null && json.isNotBlank()) {
                                        val res = viewModel.importQuestionsFromJson(json)
                                        if (res.first) {
                                            Toast.makeText(context, "Successfully imported ${res.second} items!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Failed to parse backup file", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportLauncher.launch("ots_question_bank_backup.json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Backup")
                        }

                        Button(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Backup")
                        }
                    }
                }
            }
        }

        // --- 7. DATA RECOVERY & TRASH / RECYCLE BIN ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Data Recovery & Undo Trash",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()

                    val recycleBinCount = remember(settingsManager.recycleBinJson) {
                        try {
                            JSONArray(settingsManager.recycleBinJson).length()
                        } catch (_: Exception) {
                            0
                        }
                    }

                    Text(
                        text = "If questions or test papers were deleted by mistake, use the options below to restore deleted items or revert to the last auto-snapshot point.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.restoreSnapshotPoint(settingsManager) { success, count ->
                                    if (success) {
                                        Toast.makeText(context, "Restored $count questions from auto-snapshot point!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "No snapshot point available.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore Snapshot")
                        }

                        Button(
                            onClick = { showRecycleBinDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Trash ($recycleBinCount)")
                        }
                    }
                }
            }
        }

        // --- 8. CLEAN UNINSTALL & REINSTALL DATA PRESERVATION ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Clean Uninstall & Cloud Preservation Policy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()

                    Text(
                        text = "Clean Uninstall Policy:\n" +
                                "• Local App Data Cleanup: When you uninstall this application, Android completely cleans and removes local app databases and internal cached files from the device storage.\n\n" +
                                "• Google Drive Preservation: Your cloud backups stored in Google Drive remain 100% intact and untouched in your cloud account.\n\n" +
                                "• Next Install Recovery: When you reinstall the app on any Android device, simply sign into Google Drive and tap 'Restore' under Google Drive Cloud Sync to immediately recover your entire question bank and configuration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- 9. APP UPDATE ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "App Update",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()

                    var updateStatus by remember { mutableStateOf("") }
                    
                    Text(
                        text = "Check for and download the latest application update from the configured GitHub repository.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (updateStatus.isNotBlank()) {
                        Text(
                            text = updateStatus,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                com.example.util.AppUpdater.checkForUpdatesAndInstall(context) { status ->
                                    updateStatus = status
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = updateStatus.isBlank()
                    ) {
                        if (updateStatus.isNotBlank()) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (updateStatus.isNotBlank()) "Downloading..." else "Check for Updates")
                    }
                }
            }
        }

        // --- 10. ABOUT DEVELOPER ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()

                    Text(
                        text = "developed by Ravikant, email - myslv409@gmail.com, contact me for any suggestion or feedback",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            currentPin = settingsManager.pinCode,
            onDismiss = { showChangePinDialog = false },
            onPinChanged = { newPin ->
                settingsManager.pinCode = newPin
                settingsManager.isAppLockEnabled = true
                showChangePinDialog = false
                Toast.makeText(context, "PIN code updated successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showRecycleBinDialog) {
        RecycleBinDialog(
            settingsManager = settingsManager,
            viewModel = viewModel,
            onDismiss = { showRecycleBinDialog = false }
        )
    }
}

@Composable
fun ChangePinDialog(
    currentPin: String,
    onDismiss: () -> Unit,
    onPinChanged: (String) -> Unit
) {
    var oldPinInput by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change App Lock PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentPin.isNotBlank()) {
                    OutlinedTextField(
                        value = oldPinInput,
                        onValueChange = { oldPinInput = it },
                        label = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("New 4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText.isNotBlank()) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentPin.isNotBlank() && oldPinInput != currentPin) {
                        errorText = "Current PIN is incorrect."
                    } else if (newPin.length < 4) {
                        errorText = "New PIN must be 4 digits long."
                    } else if (newPin != confirmPin) {
                        errorText = "New PIN and confirmation PIN do not match."
                    } else {
                        onPinChanged(newPin)
                    }
                }
            ) {
                Text("Save PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RecycleBinDialog(
    settingsManager: SettingsManager,
    viewModel: OtsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val binItems = remember(settingsManager.recycleBinJson) {
        val list = mutableListOf<Triple<String, String, String>>() // ID, Title, Subject
        try {
            val jsonArr = JSONArray(settingsManager.recycleBinJson)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                val id = obj.optString("id", "")
                val text = obj.optString("questionText", "Question Item").take(60)
                val subject = obj.optString("subject", "General")
                list.add(Triple(id, text, subject))
            }
        } catch (_: Exception) {}
        list
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recycle Bin / Deleted Items (${binItems.size})") },
        text = {
            if (binItems.isEmpty()) {
                Text("Trash bin is empty. No deleted questions stored.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(binItems) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.second,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Subject: ${item.third}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.restoreSingleRecycleBinQuestion(item.first, settingsManager) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Restored item to question bank!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = "Restore Item")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (binItems.isNotEmpty()) {
                Button(
                    onClick = {
                        viewModel.restoreRecycleBinItems(settingsManager) { count ->
                            Toast.makeText(context, "Restored $count items from recycle bin!", Toast.LENGTH_LONG).show()
                            onDismiss()
                        }
                    }
                ) {
                    Text("Restore All")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    settingsManager.recycleBinJson = "[]"
                    Toast.makeText(context, "Recycle bin emptied", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Text(if (binItems.isNotEmpty()) "Empty Bin" else "Close")
            }
        }
    )
}



@Composable
fun RemoteFolderPickerDialog(
    initialDir: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    host: String,
    port: Int,
    user: String,
    pass: String,
    usePassive: Boolean,
    useFtps: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    var currentDir by remember { mutableStateOf(initialDir.removeSuffix("/")) }
    var directories by remember { mutableStateOf<List<String>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newDirName by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    fun loadDirs() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = NetworkStorageManager.listDirectories(host, port, user, pass, currentDir, usePassive, useFtps)
            res.fold(
                onSuccess = { dirs ->
                    directories = dirs.sorted()
                    isLoading = false
                },
                onFailure = { err ->
                    errorMessage = err.message
                    isLoading = false
                }
            )
        }
    }

    LaunchedEffect(currentDir) {
        loadDirs()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Remote Folder") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Current: /${currentDir.ifBlank { "" }}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (currentDir.isNotBlank()) {
                    TextButton(onClick = {
                        val parts = currentDir.split("/")
                        currentDir = if (parts.size > 1) parts.dropLast(1).joinToString("/") else ""
                    }) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Up")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(".. (Up)")
                    }
                    HorizontalDivider()
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (errorMessage != null) {
                    Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(directories ?: emptyList()) { dir ->
                            TextButton(
                                onClick = {
                                    currentDir = if (currentDir.isEmpty()) dir else "$currentDir/$dir"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = "Folder", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(dir, textAlign = TextAlign.Start)
                                }
                            }
                        }
                        if (directories?.isEmpty() == true) {
                            item {
                                Text("No subdirectories found.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { showCreateDialog = true }) {
                    Text("New Folder")
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { onSelect(currentDir) }) {
                    Text("Select This")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = newDirName,
                    onValueChange = { newDirName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newDirName.isNotBlank()) {
                        coroutineScope.launch {
                            val res = NetworkStorageManager.createDirectory(host, port, user, pass, currentDir, newDirName.trim(), usePassive, useFtps)
                            res.fold(
                                onSuccess = {
                                    showCreateDialog = false
                                    newDirName = ""
                                    loadDirs()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "Error creating folder: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun GoogleDriveFolderPickerDialog(
    initialDirId: String = "root",
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    var currentFolderId by remember { mutableStateOf(initialDirId) }
    var currentFolderName by remember { mutableStateOf("My Drive") }
    val historyStack = remember { mutableStateListOf<Pair<String, String>>() }
    var folders by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    fun loadFolders() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = GoogleDriveSyncManager.listDriveFolders(context, currentFolderId)
            res.fold(
                onSuccess = { folderPairs ->
                    folders = folderPairs.sortedBy { it.second }
                    isLoading = false
                },
                onFailure = { err ->
                    errorMessage = err.localizedMessage ?: err.message
                    isLoading = false
                }
            )
        }
    }

    LaunchedEffect(currentFolderId) {
        loadFolders()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Google Drive Folder") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Current: $currentFolderName", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (currentFolderId != "root" || historyStack.isNotEmpty()) {
                    TextButton(onClick = {
                        if (historyStack.isNotEmpty()) {
                            val prev = historyStack.removeAt(historyStack.lastIndex)
                            currentFolderId = prev.first
                            currentFolderName = prev.second
                        } else {
                            currentFolderId = "root"
                            currentFolderName = "My Drive"
                        }
                    }) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Up")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(".. (Up)")
                    }
                    HorizontalDivider()
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (errorMessage != null) {
                    Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(folders ?: emptyList()) { folder ->
                            TextButton(
                                onClick = {
                                    historyStack.add(Pair(currentFolderId, currentFolderName))
                                    currentFolderId = folder.first
                                    currentFolderName = folder.second
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = "Folder", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(folder.second, textAlign = TextAlign.Start)
                                }
                            }
                        }
                        if (folders?.isEmpty() == true) {
                            item {
                                Text("No subfolders found.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { showCreateDialog = true }) {
                    Text("New Folder")
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { 
                    val pathBuilder = historyStack.map { it.second }.toMutableList()
                    pathBuilder.add(currentFolderName)
                    val fullPath = pathBuilder.joinToString("/") + "/backup.json"
                    onSelect(fullPath, currentFolderId) 
                }) {
                    Text("Select This")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFolderName.isNotBlank()) {
                        coroutineScope.launch {
                            val res = GoogleDriveSyncManager.createDriveFolder(context, currentFolderId, newFolderName.trim())
                            res.fold(
                                onSuccess = { newId ->
                                    showCreateDialog = false
                                    newFolderName = ""
                                    loadFolders()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}
