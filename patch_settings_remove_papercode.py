import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

target = """                    OutlinedTextField(
                        value = defaultCode,
                        onValueChange = { defaultCode = it; settingsManager.defaultPaperCode = it },
                        label = { Text("Default Paper Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )"""

if target in content:
    content = content.replace(target, "")
    content = content.replace("var defaultCode by remember { mutableStateOf(settingsManager.defaultPaperCode) }", "")
    with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
        f.write(content)
    print("Paper code setting removed")
else:
    print("Target not found")

