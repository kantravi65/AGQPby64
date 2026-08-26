with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

lines = content.split('\n')
open_c = 0
close_c = 0
for i, line in enumerate(lines):
    open_c += line.count('{')
    close_c += line.count('}')
    if open_c - close_c < 0:
        print(f"Line {i+1}: {line}")
        break
print(f"Final Bal: {open_c - close_c}")
