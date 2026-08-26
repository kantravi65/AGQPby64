with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

open_braces = content.count("{")
close_braces = content.count("}")
print(f"Open: {open_braces}, Close: {close_braces}")
