import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

target = """        // --- 1. USER PROFILE & ACCOUNT ---
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
        }"""

replacement = """        // --- 1. USER PROFILE & ACCOUNT ---
        item {
            SettingsCategory(
                title = "User Profile & Institution Credentials",
                icon = {
                    if (settingsManager.isGoogleSignedIn && settingsManager.googlePhotoUrl.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = settingsManager.googlePhotoUrl,
                            contentDescription = "Profile Picture",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
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
                
                Button(
                    onClick = onGoogleSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout / Sign Out")
                }
            }
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
        f.write(content)
    print("Replaced section 1")
else:
    print("Section 1 target not found")
