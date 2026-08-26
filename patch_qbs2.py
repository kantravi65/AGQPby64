import re

with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'r') as f:
    content = f.read()

# Replace selectedBookTitle definition
target = "    val selectedBookTitle = remember(books, selectedBookFilter) {\n        books.find { it.id == selectedBookFilter }?.title\n    }"
# If the exact multiline matches, great. Otherwise I'll use regex.
import re
content = re.sub(r'val selectedBookTitle = remember\(books, selectedBookFilter\) \{[^}]*\}', 
                 'val selectedBookTitle = remember(allSubjects, selectedBookFilter) { allSubjects.find { it.first == selectedBookFilter }?.second }', 
                 content)

with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'w') as f:
    f.write(content)
