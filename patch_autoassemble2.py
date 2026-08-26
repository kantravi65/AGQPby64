import re

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'r') as f:
    content = f.read()

# Replace the states
content = re.sub(r'var autoQuestionCount by remember \{ mutableStateOf\("5"\) \}', 
"""var autoMcqCount by remember { mutableStateOf("5") }
    var autoTfCount by remember { mutableStateOf("5") }
    var autoFibCount by remember { mutableStateOf("5") }
    var autoSubjectiveCount by remember { mutableStateOf("5") }""", content)

# Replace the UI
old_ui_regex = r'if \(isAutoAssemble\) \{[\s\S]*?\}'
new_ui = """if (isAutoAssemble) {
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
                                                label = { Text("Subject") },
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

# Find the location of "if (isAutoAssemble) {" and replace the block
# Since we have another `if (isAutoAssemble)` in `Row`, wait, no, we just have one `if (isAutoAssemble)` under `Row(...) { Text("Auto Random") }`
start_idx = content.find("if (isAutoAssemble) {")
if start_idx != -1:
    end_idx = content.find("                            }", start_idx) # End of the column inside the card
    
    content = content[:start_idx] + new_ui + "\n" + content[end_idx:]
    
with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'w') as f:
    f.write(content)
