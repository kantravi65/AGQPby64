import re

with open('app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace("com.example.util.PdfPrintUtils.generateLiveTestMeritList(context, config.subject, config.durationMinutes, candidates)", "com.example.util.PdfPrintUtils.generateLiveTestReportPdf(context, candidates, config.subject, config.durationMinutes)")

with open('app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt', 'w') as f:
    f.write(text)
