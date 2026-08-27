import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

pattern = re.compile(r"// --- 8\. CLEAN UNINSTALL.*?// --- 9\. APP UPDATE ---", re.DOTALL)
content = pattern.sub("// --- 9. APP UPDATE ---", content)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)

