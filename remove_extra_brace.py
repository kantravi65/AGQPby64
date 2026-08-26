with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    lines = f.readlines()

# Line 1217 is index 1216
if "}" in lines[1216]:
    lines[1216] = lines[1216].replace("}", "", 1)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.writelines(lines)

