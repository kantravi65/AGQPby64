import re

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'r') as f:
    content = f.read()

target_text = """    questions.forEachIndexed { idx, q ->
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
    }"""

replacement_text = """    val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
    val typeNames = mapOf("mcq" to "MULTIPLE CHOICE QUESTIONS (MCQ)", "tf" to "TRUE / FALSE", "fib" to "FILL IN THE BLANKS", "subjective" to "SUBJECTIVE QUESTIONS")
    
    val grouped = questions.groupBy { it.type }.toSortedMap(compareBy { typeOrder[it] ?: 5 })
    
    var questionCounter = 1
    for ((type, list) in grouped) {
        sb.appendLine(typeNames[type] ?: "OTHER")
        sb.appendLine("------------------------------------------")
        list.forEach { q ->
            sb.appendLine("Q${questionCounter}. ${q.question}  [${q.marks} Marks]")
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
            questionCounter++
        }
        sb.appendLine()
    }"""

content = content.replace(target_text, replacement_text)

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'w') as f:
    f.write(content)
