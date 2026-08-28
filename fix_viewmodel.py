import re

with open("app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt", "r") as f:
    content = f.read()

bad_snippet = '''fun startWebServer() {
        val intent = Intent(getApplication(), WebServerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }
        webServerManager?.startServer { url ->
            _webServerUrl.value = url
        }
    }'''

good_snippet = '''fun startWebServer() {
        val intent = Intent(getApplication(), WebServerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }'''

content = content.replace(bad_snippet, good_snippet)

with open("app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt", "w") as f:
    f.write(content)
