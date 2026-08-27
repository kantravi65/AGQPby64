import re
with open("app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt", "r") as f:
    content = f.read()

# Remove variable declarations
content = re.sub(r'        val initialPaperCode = remember \{.*?\}\n    var paperCode by remember \{ mutableStateOf\(initialPaperCode\) \}\n', '', content, flags=re.DOTALL)
content = re.sub(r'    var sectionHeading by remember \{ mutableStateOf\("MULTIPLE CHOICE QUESTIONS \(MCQ\)"\) \}\n', '', content)

# Remove UI for paperCode and sectionHeading
# We'll just replace the specific text block.
target_ui = """                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = subTitle,
                                onValueChange = { subTitle = it },
                                label = { Text("Subtitle / Branch") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = paperCode,
                                onValueChange = { paperCode = it },
                                label = { Text("Paper Code") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = { dateStr = it },
                                label = { Text("Exam Date") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = totalMarksText,
                                onValueChange = { totalMarksText = it },
                                label = { Text("Total Marks Text") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = sectionHeading,
                            onValueChange = { sectionHeading = it },
                            label = { Text("Section Heading Banner") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )"""

replacement_ui = """                        OutlinedTextField(
                            value = subTitle,
                            onValueChange = { subTitle = it },
                            label = { Text("Subtitle / Branch") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = { dateStr = it },
                                label = { Text("Exam Date") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = totalMarksText,
                                onValueChange = { totalMarksText = it },
                                label = { Text("Total Marks Text") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }"""

content = content.replace(target_ui, replacement_ui)

# Remove the arguments in PdfPrintSettings
content = re.sub(r'                            paperCode = paperCode,\n', '', content)
content = re.sub(r'                            sectionHeading = sectionHeading,\n', '', content)

with open("app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt", "w") as f:
    f.write(content)

print("Done")
