import re

with open('app/src/main/java/com/example/util/FtpManager.kt', 'r') as f:
    content = f.read()

target = """        client.controlEncoding = "UTF-8"
        client.connectTimeout = 15000
        client.defaultTimeout = 15000
        client.dataTimeout = 15000
        client.isRemoteVerificationEnabled = false"""

replacement = """        client.controlEncoding = "UTF-8"
        try { client.connectTimeout = 15000 } catch (e: Exception) {}
        try { client.defaultTimeout = 15000 } catch (e: Exception) {}
        try { client.dataTimeout = java.time.Duration.ofMillis(15000) } catch (e: Exception) {}
        if (client is FTPSClient) {
            client.isRemoteVerificationEnabled = false
        }"""

content = content.replace(target, replacement)

# also fix the other issue: `client.isRemoteVerificationEnabled = false` should only be called if client is FTPSClient, wait, or we can just use `client.setRemoteVerificationEnabled(false)`. But wait, FTPClient doesn't have it. FTPSClient has it.

with open('app/src/main/java/com/example/util/FtpManager.kt', 'w') as f:
    f.write(content)
