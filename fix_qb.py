import re
import os

path = 'app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

mcq_old = '''                                  Text("MCQ Options", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                  OutlinedTextField(
                                      value = optionA,
                                      onValueChange = { optionA = it },
                                      label = { Text("Option A") },
                                      modifier = Modifier.fillMaxWidth(),
                                      singleLine = true
                                  )
                                  OutlinedTextField(
                                      value = optionB,
                                      onValueChange = { optionB = it },
                                      label = { Text("Option B") },
                                      modifier = Modifier.fillMaxWidth(),
                                      singleLine = true
                                  )
                                  OutlinedTextField(
                                      value = optionC,
                                      onValueChange = { optionC = it },
                                      label = { Text("Option C") },
                                      modifier = Modifier.fillMaxWidth(),
                                      singleLine = true
                                  )
                                  OutlinedTextField(
                                      value = optionD,
                                      onValueChange = { optionD = it },
                                      label = { Text("Option D") },
                                      modifier = Modifier.fillMaxWidth(),
                                      singleLine = true
                                  )
                                  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                  }'''

mcq_new = '''                                  Text("MCQ Options & Correct Answer (Tick the correct one)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                  Row(verticalAlignment = Alignment.CenterVertically) {
                                      RadioButton(
                                          selected = selectedAnswer == optionA && optionA.isNotBlank(),
                                          onClick = { if (optionA.isNotBlank()) selectedAnswer = optionA }
                                      )
                                      OutlinedTextField(
                                          value = optionA,
                                          onValueChange = { 
                                              optionA = it
                                              if (selectedAnswer == it) selectedAnswer = it // Update if already selected
                                          },
                                          label = { Text("Option A") },
                                          modifier = Modifier.weight(1f),
                                          singleLine = true
                                      )
                                  }
                                  Row(verticalAlignment = Alignment.CenterVertically) {
                                      RadioButton(
                                          selected = selectedAnswer == optionB && optionB.isNotBlank(),
                                          onClick = { if (optionB.isNotBlank()) selectedAnswer = optionB }
                                      )
                                      OutlinedTextField(
                                          value = optionB,
                                          onValueChange = { 
                                              optionB = it
                                              if (selectedAnswer == it) selectedAnswer = it 
                                          },
                                          label = { Text("Option B") },
                                          modifier = Modifier.weight(1f),
                                          singleLine = true
                                      )
                                  }
                                  Row(verticalAlignment = Alignment.CenterVertically) {
                                      RadioButton(
                                          selected = selectedAnswer == optionC && optionC.isNotBlank(),
                                          onClick = { if (optionC.isNotBlank()) selectedAnswer = optionC }
                                      )
                                      OutlinedTextField(
                                          value = optionC,
                                          onValueChange = { 
                                              optionC = it
                                              if (selectedAnswer == it) selectedAnswer = it 
                                          },
                                          label = { Text("Option C") },
                                          modifier = Modifier.weight(1f),
                                          singleLine = true
                                      )
                                  }
                                  Row(verticalAlignment = Alignment.CenterVertically) {
                                      RadioButton(
                                          selected = selectedAnswer == optionD && optionD.isNotBlank(),
                                          onClick = { if (optionD.isNotBlank()) selectedAnswer = optionD }
                                      )
                                      OutlinedTextField(
                                          value = optionD,
                                          onValueChange = { 
                                              optionD = it
                                              if (selectedAnswer == it) selectedAnswer = it 
                                          },
                                          label = { Text("Option D") },
                                          modifier = Modifier.weight(1f),
                                          singleLine = true
                                      )
                                  }'''

if mcq_old in content:
    content = content.replace(mcq_old, mcq_new)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("mcq_old not found")
