import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Add a RemoteFolderPickerDialog composable at the bottom
dialog_code = """
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
"""

if "fun RemoteFolderPickerDialog" not in content:
    content = content + "\n\n" + dialog_code

target_browse_button = """                        OutlinedTextField(
                            value = ftpRemoteDir,
                            onValueChange = { ftpRemoteDir = it; settingsManager.ftpRemoteDir = it },
                            label = { Text("Remote Folder Path (e.g., /ots_backup)") },
                            modifier = Modifier.fillMaxWidth()
                        )"""
replacement_browse_button = """                        var showFolderPicker by remember { mutableStateOf(false) }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ftpRemoteDir,
                                onValueChange = { ftpRemoteDir = it; settingsManager.ftpRemoteDir = it },
                                label = { Text("Remote Folder Path (e.g., /ots_backup)") },
                                modifier = Modifier.weight(1f)
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
                        }"""

content = content.replace(target_browse_button, replacement_browse_button)

# Also need to add Icons import if missing
if "import androidx.compose.material.icons.filled.ArrowUpward" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Settings", "import androidx.compose.material.icons.filled.Settings\nimport androidx.compose.material.icons.filled.ArrowUpward\nimport androidx.compose.material.icons.filled.Folder\nimport androidx.compose.material.icons.filled.FolderOpen")

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
