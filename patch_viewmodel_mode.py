import re

with open("app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt", "r") as f:
    content = f.read()

start_func_old = r'fun startWebServer\(\) \{'
start_func_new = '''fun startWebServer(mode: String = "admin") {
        val intent = Intent(getApplication(), WebServerService::class.java).apply {
            putExtra("SERVER_MODE", mode)
        }'''
content = re.sub(start_func_old, start_func_new, content, count=1)

with open("app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt", "w") as f:
    f.write(content)
