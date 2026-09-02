import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if 'WEB DASHBOARD MANAGER' not in content:
    dashboard_code = '''
        // --- WEB DASHBOARD MANAGER ---
        item {
            val webServerUrl by viewModel.webServerUrl.collectAsState()
            
            SettingsCategory(
                title = "Remote Web Dashboard",
                icon = { Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                Text(
                    text = "Manage your database (Questions, Papers, Books) directly from your PC browser on the same Wi-Fi network.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (webServerUrl != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Server is running!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Open this URL on your PC:", style = MaterialTheme.typography.bodySmall)
                            Text(webServerUrl ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.stopWebServer() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Web Server")
                    }
                } else {
                    Button(
                        onClick = { viewModel.startWebServer("admin") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Admin Server")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.startWebServer("expert") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Expert Review Server")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.startWebServer("livetest") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Live Test Server")
                    }
                }
            }
        }
'''
    content = content.replace('        // --- 10. ABOUT DEVELOPER ---', dashboard_code + '\n        // --- 10. ABOUT DEVELOPER ---')
    with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w', encoding='utf-8') as f:
        f.write(content)
