import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# Replace the three closing braces at the end of sections 2 to 9
content = re.sub(
    r'\n\s*\}\n\s*\}\n\s*\}\n(\s*// --- (3|3\.5|4|5|6|7|8|9|10)\.)',
    r'\n            }\n        }\n\1',
    content
)

# Replace the three closing braces for section 10, which is followed by the end of LazyColumn
content = re.sub(
    r'\n\s*\}\n\s*\}\n\s*\}\n\s*\}\n\s*(if \(showChangePinDialog\))',
    r'\n            }\n        }\n    }\n\n    \1',
    content
)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
