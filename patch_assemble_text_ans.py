import re

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'r') as f:
    content = f.read()

target = """    sb.appendLine("               ANSWER KEY                 ")
    sb.appendLine("==========================================")
    questions.forEachIndexed { idx, q ->
        sb.appendLine("Q${idx + 1}: ${q.answer}")
    }"""

replacement = """    sb.appendLine("               ANSWER KEY                 ")
    sb.appendLine("==========================================")
    val sortedQuestionsForAns = questions.sortedBy { typeOrder[it.type] ?: 5 }
    sortedQuestionsForAns.forEachIndexed { idx, q ->
        sb.appendLine("Q${idx + 1}: ${q.answer}")
    }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'w') as f:
    f.write(content)
