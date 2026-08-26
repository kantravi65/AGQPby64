import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

target_browse_button = """                    OutlinedTextField(
                        value = ftpRemoteDir,
                        onValueChange = { ftpRemoteDir = it; settingsManager.ftpRemoteDir = it },
                        label = { Text("Remote Server Directory Folder") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )"""

replacement_browse_button = """                    var showFolderPicker by remember { mutableStateOf(false) }
                    
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
                    }"""

content = content.replace(target_browse_button, replacement_browse_button)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
