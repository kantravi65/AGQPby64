with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if line.startswith("    val selectedBookTitle = remember(allSubjects"):
        new_lines.append("    val selectedBookTitle = remember(allSubjects, selectedBookFilter) { allSubjects.find { it.first == selectedBookFilter }?.second }\n")
        skip = True
    elif skip and line.strip() == "}":
        skip = False
    elif not skip:
        new_lines.append(line)

with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'w') as f:
    f.writelines(new_lines)
