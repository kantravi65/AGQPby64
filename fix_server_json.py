import re

with open("app/src/main/java/com/example/util/WebServerManager.kt", "r") as f:
    content = f.read()

# I will just use kotlinx.serialization.json.Json fully qualified in the code
content = content.replace("Json.decodeFromString<List<String>>(paper.questionIdsJson)", "kotlinx.serialization.json.Json.decodeFromString<List<String>>(paper.questionIdsJson)")

# Remove the import kotlinx.serialization.json.Json
content = content.replace("import kotlinx.serialization.json.Json\n", "")

with open("app/src/main/java/com/example/util/WebServerManager.kt", "w") as f:
    f.write(content)
