import re

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'r') as f:
    content = f.read()

target = """fun buildPaperTextString(paper: PaperEntity, questions: List<QuestionEntity>): String {
    val sb = StringBuilder()
    sb.appendLine("==========================================")
    sb.appendLine("          ${paper.title.uppercase()}")
    sb.appendLine("Subject: ${paper.subject}")
    sb.appendLine("Time Allowed: ${paper.durationMinutes} Mins | Max Marks: ${paper.totalMarks}")
    sb.appendLine("==========================================")
    sb.appendLine()

    questions.forEachIndexed { idx, q ->
        sb.appendLine("Q${idx + 1}. ${q.question}  [${q.marks} Marks]")
        try {
            val arr = JSONArray(q.optionsJson)
            for (i in 0 until arr.length()) {
                val optChar = ('A' + i)
                sb.appendLine("   ($optChar) ${arr.getString(i)}")
            }
        } catch (e: Exception) {
            // Ignore options formatting error
        }
        sb.appendLine()
    }

    sb.appendLine("\n\n------------------------------------------")
    sb.appendLine(" PAGE BREAK: ANSWER KEY (SEPARATE PAGE)   ")
    sb.appendLine("------------------------------------------")
    sb.appendLine("==========================================")
    sb.appendLine("               ANSWER KEY                 ")
    sb.appendLine("==========================================")
    questions.forEachIndexed { idx, q ->
        sb.appendLine("Q${idx + 1}: ${q.answer}")
    }

    return sb.toString()
}"""

replacement = """fun buildPaperTextString(paper: PaperEntity, questions: List<QuestionEntity>): String {
    val sb = StringBuilder()
    sb.appendLine("==========================================")
    sb.appendLine("          ${paper.title.uppercase()}")
    sb.appendLine("Subject: ${paper.subject}")
    sb.appendLine("Time Allowed: ${paper.durationMinutes} Mins | Max Marks: ${paper.totalMarks}")
    sb.appendLine("==========================================")
    sb.appendLine()

    val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
    val sortedQuestions = questions.sortedBy { typeOrder[it.type] ?: 5 }
    val typeNames = mapOf("mcq" to "MULTIPLE CHOICE QUESTIONS (MCQ)", "tf" to "TRUE / FALSE", "fib" to "FILL IN THE BLANKS", "subjective" to "SUBJECTIVE QUESTIONS")

    var currentType = ""
    sortedQuestions.forEachIndexed { idx, q ->
        if (q.type != currentType) {
            currentType = q.type
            sb.appendLine("--- ${typeNames[currentType] ?: "OTHER"} ---")
            sb.appendLine()
        }
        sb.appendLine("Q${idx + 1}. ${q.question}  [${q.marks} Marks]")
        if (q.type == "mcq") {
            try {
                val arr = JSONArray(q.optionsJson)
                for (i in 0 until arr.length()) {
                    val optChar = ('A' + i)
                    sb.appendLine("   ($optChar) ${arr.getString(i)}")
                }
            } catch (e: Exception) {
                // Ignore options formatting error
            }
        }
        sb.appendLine()
    }

    sb.appendLine("\n\n------------------------------------------")
    sb.appendLine(" PAGE BREAK: ANSWER KEY (SEPARATE PAGE)   ")
    sb.appendLine("------------------------------------------")
    sb.appendLine("==========================================")
    sb.appendLine("               ANSWER KEY                 ")
    sb.appendLine("==========================================")
    sortedQuestions.forEachIndexed { idx, q ->
        sb.appendLine("Q${idx + 1}: ${q.answer}")
    }

    return sb.toString()
}"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'w') as f:
    f.write(content)
