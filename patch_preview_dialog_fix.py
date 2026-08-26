import re

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'r') as f:
    content = f.read()

target_ui = """                    val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
                    val groupedQuestions = questions.groupBy { it.type }.toSortedMap(compareBy { typeOrder[it] ?: 5 })
                    val typeNames = mapOf("mcq" to "MULTIPLE CHOICE QUESTIONS (MCQ)", "tf" to "TRUE / FALSE", "fib" to "FILL IN THE BLANKS", "subjective" to "SUBJECTIVE QUESTIONS")
                    
                    var globalIdx = 0
                    groupedQuestions.forEach { (type, typeQuestions) ->
                        item {
                            Text(
                                text = typeNames[type] ?: "OTHER",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp).fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        
                        items(typeQuestions) { q ->
                            val idx = globalIdx++"""

replacement_ui = """                    val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
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
                    
                    items(displayItems) { item ->
                        if (item is String) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp).fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else if (item is Pair<*, *>) {
                            val idx = item.first as Int
                            val q = item.second as com.example.data.models.QuestionEntity"""

content = content.replace(target_ui, replacement_ui)

# Need to fix the closing braces because we replaced `items(typeQuestions) { q ->` with `items(displayItems) { item ->` ... `else if (item is Pair<*, *>) {`

target_close = """                        } else {
                            content()
                        }
                    }"""

replacement_close = """                        } else {
                            content()
                        }
                        } // end of else if
                    }"""

content = content.replace(target_close, replacement_close)

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'w') as f:
    f.write(content)
