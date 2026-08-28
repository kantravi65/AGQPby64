import re

with open("app/src/main/java/com/example/util/WebServerManager.kt", "r") as f:
    content = f.read()

content = content.replace("json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })", 
                          "json(kotlinx.serialization.json.Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })")

with open("app/src/main/java/com/example/util/WebServerManager.kt", "w") as f:
    f.write(content)
