import re

with open('app/src/main/java/com/example/util/FtpManager.kt', 'r') as f:
    content = f.read()

target = """        try { client.connectTimeout = 15000 } catch (e: Exception) {}
        try { client.defaultTimeout = 15000 } catch (e: Exception) {}
        try { client.dataTimeout = java.time.Duration.ofMillis(15000) } catch (e: Exception) {}"""

replacement = """        client.connectTimeout = 15000
        client.defaultTimeout = 15000
        client.setDataTimeout(15000)"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/util/FtpManager.kt', 'w') as f:
    f.write(content)

