import re

with open("app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt", "r") as f:
    content = f.read()

bad = '''fun startWebServer(mode: String = "admin") {
        val intent = Intent(getApplication(), WebServerService::class.java).apply {
            putExtra("SERVER_MODE", mode)
        }
        val intent = Intent(getApplication(), WebServerService::class.java)'''
good = '''fun startWebServer(mode: String = "admin") {
        val intent = Intent(getApplication(), WebServerService::class.java).apply {
            putExtra("SERVER_MODE", mode)
        }'''
content = content.replace(bad, good)

with open("app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt", "w") as f:
    f.write(content)
