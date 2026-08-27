import re

with open("app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt", "r") as f:
    content = f.read()

lines = content.split('\n')
for i, line in enumerate(lines):
    if "instituteTitle" in line or "PdfPrintSettings(" in line:
        print(f"{i}: {line}")
