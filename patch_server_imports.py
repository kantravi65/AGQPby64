import re

with open("app/src/main/java/com/example/util/WebServerManager.kt", "r") as f:
    content = f.read()

imports = """import com.example.util.FontSize
import com.example.util.MarginSize
import com.example.util.WatermarkPattern
"""
content = content.replace("import com.example.util.PdfPrintSettings", imports + "import com.example.util.PdfPrintSettings")

with open("app/src/main/java/com/example/util/WebServerManager.kt", "w") as f:
    f.write(content)
