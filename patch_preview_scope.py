import re

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'r') as f:
    content = f.read()

target = """                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
                    val typeNames = mapOf("mcq" to "MULTIPLE CHOICE QUESTIONS (MCQ)", "tf" to "TRUE / FALSE", "fib" to "FILL IN THE BLANKS", "subjective" to "SUBJECTIVE QUESTIONS")
                    
                    val displayItems = remember(questions) {
                        val grouped = questions.groupBy { it.type }.toSortedMap(compareBy { typeOrder[it] ?: 5 })
                        val list = mutableListOf<Any>()
                        var counter = 0
                        for ((type, typeQuestions) in grouped) {
                            list.add(typeNames[type] ?: "OTHER")
                            for (q in typeQuestions) {
                                list.add(Pair(counter++, q))
                            }
                        }
                        list
                    }
                    
                    items(displayItems) {"""

replacement = """
                val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
                val typeNames = mapOf("mcq" to "MULTIPLE CHOICE QUESTIONS (MCQ)", "tf" to "TRUE / FALSE", "fib" to "FILL IN THE BLANKS", "subjective" to "SUBJECTIVE QUESTIONS")
                
                val displayItems = remember(questions) {
                    val grouped = questions.groupBy { it.type }.toSortedMap(compareBy { typeOrder[it] ?: 5 })
                    val list = mutableListOf<Any>()
                    var counter = 0
                    for ((type, typeQuestions) in grouped) {
                        list.add(typeNames[type] ?: "OTHER")
                        for (q in typeQuestions) {
                            list.add(Pair(counter++, q))
                        }
                    }
                    list
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayItems) {"""

content = content.replace(target, replacement)
content = content.replace("as com.example.data.models.QuestionEntity", "as com.example.data.model.QuestionEntity")

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'w') as f:
    f.write(content)
