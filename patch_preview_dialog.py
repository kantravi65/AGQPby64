import re

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'r') as f:
    content = f.read()

target_ui = """                    val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
                    val sortedQuestions = questions.sortedBy { typeOrder[it.type] ?: 5 }
                    val typeNames = mapOf("mcq" to "MULTIPLE CHOICE QUESTIONS (MCQ)", "tf" to "TRUE / FALSE", "fib" to "FILL IN THE BLANKS", "subjective" to "SUBJECTIVE QUESTIONS")
                    
                    var currentType = ""
                    itemsIndexed(sortedQuestions) { idx, q ->
                        if (q.type != currentType) {
                            currentType = q.type
                            Text(
                                text = typeNames[currentType] ?: "OTHER",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp).fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }"""

replacement_ui = """                    val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
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

# But wait, using globalIdx like this in a LazyColumn is not recomposition safe because items() block doesn't update the var nicely for rendering. It's better to just flatten it or use itemsIndexed with a precomputed list of items. Let's create a flat list.

content = content.replace(target_ui, replacement_ui)

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'w') as f:
    f.write(content)
