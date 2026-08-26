import re

with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'r') as f:
    content = f.read()

target = """                // Questions List inside paper
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(questions) { idx, q ->
                        val optionsList = remember(q.optionsJson) {
                            try {
                                val arr = JSONArray(q.optionsJson)
                                val list = mutableListOf<String>()
                                for (i in 0 until arr.length()) list.add(arr.getString(i))
                                list
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Q${idx + 1}. ${q.question}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "[${q.marks} Marks]",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                            }

                            if (optionsList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(
                                    modifier = Modifier.padding(start = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    optionsList.forEachIndexed { optIdx, opt ->
                                        Text(
                                            text = "(${('A' + optIdx)}) $opt",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Black,
                                            fontFamily = FontFamily.Serif
                                        )
                                    }
                                }
                            }
                        }
                    }"""

replacement = """                // Questions List inside paper
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
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
                        }

                        val optionsList = remember(q.optionsJson) {
                            try {
                                val arr = JSONArray(q.optionsJson)
                                val list = mutableListOf<String>()
                                for (i in 0 until arr.length()) list.add(arr.getString(i))
                                list
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }

                        val content = @Composable {
                            Column(modifier = Modifier.fillMaxWidth().padding(if (q.type == "mcq") 8.dp else 0.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Q${idx + 1}. ${q.question}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "[${q.marks} Marks]",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray
                                    )
                                }

                                if (q.type == "mcq" && optionsList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(
                                        modifier = Modifier.padding(start = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        optionsList.forEachIndexed { optIdx, opt ->
                                            Text(
                                                text = "(${('A' + optIdx)}) $opt",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Black,
                                                fontFamily = FontFamily.Serif
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (q.type == "mcq") {
                            Box(
                                modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                            ) {
                                content()
                            }
                        } else {
                            content()
                        }
                    }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt', 'w') as f:
    f.write(content)
