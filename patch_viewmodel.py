import re

with open('app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt', 'r') as f:
    vm = f.read()

old_start = """    fun startWebServer(mode: String = "admin") {
        val intent = Intent(getApplication(), WebServerService::class.java).apply {
            putExtra("SERVER_MODE", mode)
        }"""
new_start = """    fun startWebServer(mode: String = "admin", adminUser: String = "admin", adminPass: String = "1234") {
        val intent = Intent(getApplication(), WebServerService::class.java).apply {
            putExtra("SERVER_MODE", mode)
            putExtra("ADMIN_USER", adminUser)
            putExtra("ADMIN_PASS", adminPass)
        }"""

vm = vm.replace(old_start, new_start)

with open('app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt', 'w') as f:
    f.write(vm)

with open('app/src/main/java/com/example/service/WebServerService.kt', 'r') as f:
    svc = f.read()

old_svc_mode = """        val mode = intent?.getStringExtra("SERVER_MODE") ?: "admin"
        val notification = createNotification(mode)"""
new_svc_mode = """        val mode = intent?.getStringExtra("SERVER_MODE") ?: "admin"
        val adminUser = intent?.getStringExtra("ADMIN_USER") ?: "admin"
        val adminPass = intent?.getStringExtra("ADMIN_PASS") ?: "1234"
        val notification = createNotification(mode)"""

svc = svc.replace(old_svc_mode, new_svc_mode)

old_start_server = """            webServerManager = WebServerManager(applicationContext, repository, mode)
            webServerManager?.startServer { url ->"""
new_start_server = """            webServerManager = WebServerManager(applicationContext, repository, mode)
            webServerManager?.startServer(adminUser, adminPass) { url ->"""
svc = svc.replace(old_start_server, new_start_server)

with open('app/src/main/java/com/example/service/WebServerService.kt', 'w') as f:
    f.write(svc)

