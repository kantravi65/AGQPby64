import re

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'r') as f:
    content = f.read()

target = """    val availableQuestions = remember(questions, selectedBookFilter) {
        if (selectedBookFilter == null) questions
        else questions.filter { it.bookId == selectedBookFilter || it.bookTitle.equals(selectedBookFilter, ignoreCase = true) }
    }"""

replacement = """    val availableQuestions = remember(questions, selectedBookFilter, allSubjects) {
        if (selectedBookFilter == null) questions
        else {
            val filterTitle = allSubjects.find { it.first == selectedBookFilter }?.second ?: selectedBookFilter
            questions.filter { it.bookId == selectedBookFilter || it.bookTitle.equals(filterTitle, ignoreCase = true) }
        }
    }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'w') as f:
    f.write(content)
