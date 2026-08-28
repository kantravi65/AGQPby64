import re

with open("app/src/main/java/com/example/service/WebServerService.kt", "r") as f:
    content = f.read()

content = content.replace("webServerManager = WebServerManager(applicationContext, repository)", """val mode = intent?.getStringExtra("SERVER_MODE") ?: "admin"
            webServerManager = WebServerManager(applicationContext, repository, mode)""")

with open("app/src/main/java/com/example/service/WebServerService.kt", "w") as f:
    f.write(content)
