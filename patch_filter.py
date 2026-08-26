import re

with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'r') as f:
    content = f.read()

target = """            val matchesDiff = selectedDifficultyFilter == null || q.difficulty.equals(selectedDifficultyFilter, ignoreCase = true)
            val matchesBookmark = !showBookmarkedOnly || q.isBookmarked
            val matchesNoAnswer = !showNoAnswerOnly || q.answer.trim().isEmpty()

            matchesQuery && matchesBook && matchesDiff && matchesBookmark && matchesNoAnswer"""

replacement = """            val matchesType = selectedTypeFilter == null || q.type.equals(selectedTypeFilter, ignoreCase = true)
            val matchesDiff = selectedDifficultyFilter == null || q.difficulty.equals(selectedDifficultyFilter, ignoreCase = true)
            val matchesBookmark = !showBookmarkedOnly || q.isBookmarked
            val matchesNoAnswer = !showNoAnswerOnly || q.answer.trim().isEmpty()

            matchesQuery && matchesBook && matchesType && matchesDiff && matchesBookmark && matchesNoAnswer"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'w') as f:
        f.write(content)
    print("Patched filter successfully")
else:
    print("Target not found")
