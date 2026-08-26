with open('app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt', 'r') as f:
    content = f.read()

target = """    fun createPaper(
        title: String,
        subject: String,
        selectedQuestions: List<QuestionEntity>,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            val jsonArray = JSONArray()
            selectedQuestions.forEach { jsonArray.put(it.id) }"""

replacement = """    fun createPaper(
        title: String,
        subject: String,
        selectedQuestions: List<QuestionEntity>,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
            val sortedQuestions = selectedQuestions.sortedBy { typeOrder[it.type] ?: 5 }
            val jsonArray = JSONArray()
            sortedQuestions.forEach { jsonArray.put(it.id) }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt', 'w') as f:
    f.write(content)
