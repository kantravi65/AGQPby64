import re

with open("app/src/main/java/com/example/util/WebServerManager.kt", "r") as f:
    content = f.read()

# Update class signature
content = content.replace("class WebServerManager(private val appContext: Context, private val repository: OtsRepository) {", "class WebServerManager(private val appContext: Context, private val repository: OtsRepository, private val mode: String = \"admin\") {")

# Add imports for PdfPrintUtils and File
imports_add = """import java.io.File
import com.example.util.PdfPrintUtils
import com.example.util.PdfPrintSettings
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
"""
content = content.replace("import java.net.NetworkInterface", imports_add + "import java.net.NetworkInterface")

# Serve the correct HTML based on mode
# Look for: val htmlContent = appContext.assets.open("web_dashboard.html").bufferedReader().use { it.readText() }
# And change to mode logic
old_html_logic = """val htmlContent = appContext.assets.open("web_dashboard.html").bufferedReader().use { it.readText() }
                        call.respondText(htmlContent, ContentType.Text.Html)"""
new_html_logic = """val filename = when(mode) {
                            "expert" -> "web_expert.html"
                            "livetest" -> "web_livetest.html"
                            else -> "web_dashboard.html"
                        }
                        val htmlContent = appContext.assets.open(filename).bufferedReader().use { it.readText() }
                        call.respondText(htmlContent, ContentType.Text.Html)"""
content = content.replace(old_html_logic, new_html_logic)

with open("app/src/main/java/com/example/util/WebServerManager.kt", "w") as f:
    f.write(content)
