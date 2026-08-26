import re

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'r') as f:
    content = f.read()

# Replace the states
content = content.replace("var autoQuestionCount by remember { mutableStateOf(\"5\") }", 
"""    var autoMcqCount by remember { mutableStateOf("5") }
    var autoTfCount by remember { mutableStateOf("5") }
    var autoFibCount by remember { mutableStateOf("5") }
    var autoSubjectiveCount by remember { mutableStateOf("5") }""")

# Replace the UI
old_ui = """                                if (isAutoAssemble) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = autoQuestionCount,
                                            onValueChange = { autoQuestionCount = it },
                                            label = { Text("Number of Questions") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                val count = autoQuestionCount.toIntOrNull() ?: 5
                                                val shuffled = availableQuestions.shuffled().take(count)
                                                selectedQuestionIds.clear()
                                                selectedQuestionIds.addAll(shuffled.map { it.id })
                                                Toast.makeText(context, "Auto-selected ${shuffled.size} questions!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Casino, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Generate Selection")
                                        }
                                    }
                                }"""

new_ui = """                                if (isAutoAssemble) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = autoMcqCount,
                                                onValueChange = { autoMcqCount = it },
                                                label = { Text("MCQs") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = autoTfCount,
                                                onValueChange = { autoTfCount = it },
                                                label = { Text("T/F") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = autoFibCount,
                                                onValueChange = { autoFibCount = it },
                                                label = { Text("FIB") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = autoSubjectiveCount,
                                                onValueChange = { autoSubjectiveCount = it },
                                                label = { Text("Subjective") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                val mcqCount = autoMcqCount.toIntOrNull() ?: 0
                                                val tfCount = autoTfCount.toIntOrNull() ?: 0
                                                val fibCount = autoFibCount.toIntOrNull() ?: 0
                                                val subjCount = autoSubjectiveCount.toIntOrNull() ?: 0
                                                
                                                val mcqs = availableQuestions.filter { it.type == "mcq" }.shuffled().take(mcqCount)
                                                val tfs = availableQuestions.filter { it.type == "tf" }.shuffled().take(tfCount)
                                                val fibs = availableQuestions.filter { it.type == "fib" }.shuffled().take(fibCount)
                                                val subjs = availableQuestions.filter { it.type == "subjective" }.shuffled().take(subjCount)
                                                
                                                val allSelected = mcqs + tfs + fibs + subjs
                                                selectedQuestionIds.clear()
                                                selectedQuestionIds.addAll(allSelected.map { it.id })
                                                Toast.makeText(context, "Auto-selected ${allSelected.size} questions!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Casino, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Generate Category-Wise Selection")
                                        }
                                    }
                                }"""

content = content.replace(old_ui, new_ui)
with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'w') as f:
    f.write(content)
