import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

buttons_old = r'''Button\(
                        onClick = \{ viewModel\.startWebServer\(\) \},
                        modifier = Modifier\.fillMaxWidth\(\)
                    \) \{
                        Icon\(Icons\.Default\.PlayArrow, contentDescription = null\)
                        Spacer\(modifier = Modifier\.width\(8\.dp\)\)
                        Text\("Start Web Server"\)
                    \}'''

buttons_new = '''Button(
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
                    }'''

content = re.sub(buttons_old, buttons_new, content, count=1)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
