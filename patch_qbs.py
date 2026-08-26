import re

with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("val selectedBookFilter by viewModel.selectedBookFilter.collectAsState()", "val selectedBookFilter by viewModel.selectedBookFilter.collectAsState()\n    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()")

content = content.replace("questions, searchQuery, selectedBookFilter, selectedBookTitle, selectedDifficultyFilter, showBookmarkedOnly, showNoAnswerOnly", "questions, searchQuery, selectedBookFilter, selectedBookTitle, selectedTypeFilter, selectedDifficultyFilter, showBookmarkedOnly, showNoAnswerOnly")

content = content.replace("val matchesBook = selectedBookFilter == null || q.bookId == selectedBookFilter || q.bookTitle.equals(selectedBookTitle, ignoreCase = true)", "val matchesBook = selectedBookFilter == null || q.bookId == selectedBookFilter || q.bookTitle.equals(selectedBookTitle, ignoreCase = true)\n            val matchesType = selectedTypeFilter == null || q.type == selectedTypeFilter")

content = content.replace("matchesBook && matchesDifficulty", "matchesBook && matchesType && matchesDifficulty")

content = content.replace("val activeFiltersCount = remember(selectedBookFilter, selectedDifficultyFilter, showBookmarkedOnly, showNoAnswerOnly) {\n                    (if (selectedBookFilter != null) 1 else 0) +\n", "val activeFiltersCount = remember(selectedBookFilter, selectedTypeFilter, selectedDifficultyFilter, showBookmarkedOnly, showNoAnswerOnly) {\n                    (if (selectedBookFilter != null) 1 else 0) +\n                    (if (selectedTypeFilter != null) 1 else 0) +\n")

content = content.replace("selectedSubjectFilter = selectedBookFilter,\n                    allQuestionsCount = questions.size,\n                    allQuestionsList = questions,\n                    onSelectSubjectFilter = {\n                        viewModel.setBookFilter(it)\n                        viewModel.startPracticeMode()\n                    },", "selectedSubjectFilter = selectedBookFilter,\n                    selectedTypeFilter = selectedTypeFilter,\n                    allQuestionsCount = questions.size,\n                    allQuestionsList = questions,\n                    onSelectSubjectFilter = {\n                        viewModel.setBookFilter(it)\n                        viewModel.startPracticeMode()\n                    },\n                    onSelectTypeFilter = {\n                        viewModel.setTypeFilter(it)\n                        viewModel.startPracticeMode()\n                    },")

content = content.replace("selectedSubjectFilter: String? = null,", "selectedSubjectFilter: String? = null,\n    selectedTypeFilter: String? = null,")
content = content.replace("onSelectSubjectFilter: (String?) -> Unit = {},", "onSelectSubjectFilter: (String?) -> Unit = {},\n    onSelectTypeFilter: (String?) -> Unit = {},")

with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'w') as f:
    f.write(content)
