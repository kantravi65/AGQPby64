import re

with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'r') as f:
    content = f.read()

target1 = """                    onSelectOption = { viewModel.selectPracticeOption(it) },
                    onJumpToQuestion = { viewModel.jumpToPracticeQuestion(it, filteredQuestions.size) },
                    onNext = { viewModel.nextPracticeQuestion(filteredQuestions.size) },
                    onPrev = { viewModel.prevPracticeQuestion() },
                    onToggleBookmark = { viewModel.toggleBookmark(it) },
                    onExit = { viewModel.stopPracticeMode() }"""

replacement1 = """                    onSelectOption = { viewModel.selectPracticeOption(it) },
                    onJumpToQuestion = { viewModel.jumpToPracticeQuestion(it, filteredQuestions.size) },
                    onNext = { viewModel.nextPracticeQuestion(filteredQuestions.size) },
                    onPrev = { viewModel.prevPracticeQuestion() },
                    onToggleBookmark = { viewModel.toggleBookmark(it) },
                    onExit = { viewModel.stopPracticeMode() },
                    onEdit = { editingQuestion = it }"""

content = content.replace(target1, replacement1)

target2 = """    onJumpToQuestion: (Int) -> Unit = {},
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleBookmark: (QuestionEntity) -> Unit,
    onExit: () -> Unit
) {"""

replacement2 = """    onJumpToQuestion: (Int) -> Unit = {},
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleBookmark: (QuestionEntity) -> Unit,
    onEdit: (QuestionEntity) -> Unit = {},
    onExit: () -> Unit
) {"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'w') as f:
    f.write(content)
