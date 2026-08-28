import re

with open("app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt", "r") as f:
    content = f.read()

# Replace import
content = content.replace("import com.example.util.WebServerManager", "import com.example.util.WebServerManager\nimport com.example.util.WebServerState\nimport com.example.service.WebServerService\nimport android.content.Intent\nimport android.os.Build")

# Replace webServerUrl property
content = re.sub(r'private var webServerManager: WebServerManager\? = null\s*private val _webServerUrl = MutableStateFlow<String\?>\(null\)\s*val webServerUrl: StateFlow<String\?> = _webServerUrl.asStateFlow\(\)', 'val webServerUrl: StateFlow<String?> = WebServerState.url', content)

# Replace startWebServer
start_func_pattern = r'fun startWebServer\(\) \{[\s\S]*?\}'
new_start_func = '''fun startWebServer() {
        val intent = Intent(getApplication(), WebServerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }'''
content = re.sub(start_func_pattern, new_start_func, content, count=1)

# Replace stopWebServer
stop_func_pattern = r'fun stopWebServer\(\) \{[\s\S]*?\}'
new_stop_func = '''fun stopWebServer() {
        val intent = Intent(getApplication(), WebServerService::class.java)
        getApplication<Application>().stopService(intent)
    }'''
content = re.sub(stop_func_pattern, new_stop_func, content, count=1)

# Remove stopWebServer from onCleared
on_cleared_pattern = r'override fun onCleared\(\) \{\s*super\.onCleared\(\)\s*stopWebServer\(\)\s*\}'
new_on_cleared = '''override fun onCleared() {
        super.onCleared()
    }'''
content = re.sub(on_cleared_pattern, new_on_cleared, content)

with open("app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt", "w") as f:
    f.write(content)
