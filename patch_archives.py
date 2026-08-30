import re

with open('app/src/main/java/com/example/ui/screens/ArchivesScreen.kt', 'r') as f:
    text = f.read()

# Add liveExamName variable
text = text.replace(
    "val liveSubject by viewModel.liveTestSubject.collectAsState()",
    "val liveExamName by viewModel.liveTestExamName.collectAsState()\n            val liveSubject by viewModel.liveTestSubject.collectAsState()"
)

# Fix updateLiveTestConfig calls
text = text.replace(
    "viewModel.updateLiveTestConfig(s, liveMcqCount, liveFibCount, liveTfCount, liveDuration)",
    "viewModel.updateLiveTestConfig(liveExamName, s, liveMcqCount, liveFibCount, liveTfCount, liveDuration)"
)
text = text.replace(
    "viewModel.updateLiveTestConfig(liveSubject, count, liveFibCount, liveTfCount, liveDuration)",
    "viewModel.updateLiveTestConfig(liveExamName, liveSubject, count, liveFibCount, liveTfCount, liveDuration)"
)
text = text.replace(
    "viewModel.updateLiveTestConfig(liveSubject, liveMcqCount, count, liveTfCount, liveDuration)",
    "viewModel.updateLiveTestConfig(liveExamName, liveSubject, liveMcqCount, count, liveTfCount, liveDuration)"
)
text = text.replace(
    "viewModel.updateLiveTestConfig(liveSubject, liveMcqCount, liveFibCount, count, liveDuration)",
    "viewModel.updateLiveTestConfig(liveExamName, liveSubject, liveMcqCount, liveFibCount, count, liveDuration)"
)
text = text.replace(
    "viewModel.updateLiveTestConfig(liveSubject, liveMcqCount, liveFibCount, liveTfCount, mins)",
    "viewModel.updateLiveTestConfig(liveExamName, liveSubject, liveMcqCount, liveFibCount, liveTfCount, mins)"
)

# Add Exam Name Text Field
exam_name_tf = """                            OutlinedTextField(
                                value = liveExamName,
                                onValueChange = { viewModel.updateLiveTestConfig(it, liveSubject, liveMcqCount, liveFibCount, liveTfCount, liveDuration) },
                                label = { Text("Exam Name (For Candidate Portal)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // 1. Subject Select Dropdown"""

text = text.replace("// 1. Subject Select Dropdown", exam_name_tf)

# Add "End Exam & Generate PDF" button near "Stop Web Server"
end_exam_btn = """                    Button(
                        onClick = {
                            viewModel.stopWebServer()
                            viewModel.generateMeritListPdf(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("End Exam & Generate Merit List")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button("""

text = text.replace("Button(\n                        onClick = { viewModel.stopWebServer() }", end_exam_btn + "onClick = { viewModel.stopWebServer() }")

with open('app/src/main/java/com/example/ui/screens/ArchivesScreen.kt', 'w') as f:
    f.write(text)

