import re

with open('app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace("    private val _liveTestSubject = MutableStateFlow(\"\")", "    private val _liveTestExamName = MutableStateFlow(\"Online Secured Exam\")\n    val liveTestExamName: StateFlow<String> = _liveTestExamName.asStateFlow()\n    private val _liveTestSubject = MutableStateFlow(\"\")")

with open('app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt', 'w') as f:
    f.write(text)
