import re

with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'r') as f:
    content = f.read()

target = """                                OutlinedTextField(
                                    value = selectedAnswer,
                                    onValueChange = { selectedAnswer = it },
                                    label = { Text("Correct Answer (Exact Option Text)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )"""

replacement = """                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Correct Answer", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    val validOptions = listOf(optionA, optionB, optionC, optionD).filter { it.isNotBlank() }
                                    if (validOptions.isEmpty()) {
                                        Text("Please enter at least one option above first.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    } else {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(validOptions) { opt ->
                                                FilterChip(
                                                    selected = selectedAnswer == opt,
                                                    onClick = { selectedAnswer = opt },
                                                    label = { Text(opt) }
                                                )
                                            }
                                        }
                                    }
                                }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'w') as f:
    f.write(content)
