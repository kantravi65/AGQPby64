with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if i >= 409 and i <= 419: # lines 410 to 420 (0-indexed)
        continue
    new_lines.append(line)

# Wait, let's insert the correct closing braces at index 409
new_lines.insert(409, "                            }\n                        }\n                    }\n")

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'w') as f:
    f.writelines(new_lines)
