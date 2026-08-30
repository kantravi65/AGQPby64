import re

with open('app/src/main/java/com/example/util/PdfPrintUtils.kt', 'r') as f:
    content = f.read()

old_code = """            val file = File(context.cacheDir, "live_test_merit_list.pdf")
            pdfDocument.writeTo(FileOutputStream(file))"""

new_code = """            val archiveDir = File(context.filesDir, "ExamArchives")
            if (!archiveDir.exists()) archiveDir.mkdirs()
            val safeSubject = subject.replace(" ", "_").ifEmpty { "All_Subjects" }
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
            val file = File(archiveDir, "Merit_List_${safeSubject}_$timestamp.pdf")
            pdfDocument.writeTo(FileOutputStream(file))"""

content = content.replace(old_code, new_code)

with open('app/src/main/java/com/example/util/PdfPrintUtils.kt', 'w') as f:
    f.write(content)
