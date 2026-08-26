import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# Let's count } } }
matches = re.findall(r'\}\s*\}\s*\}\s*// ---', content)
print(f"Found {len(matches)} triple braces before comments")

