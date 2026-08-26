#!/bin/bash
sed -i -z 's/        try {\n            val ftpClient = createClient(useFtps || port == 990)/        var ftpClient: FTPClient? = null\n        try {\n            ftpClient = createClient(useFtps || port == 990)/g' app/src/main/java/com/example/util/FtpManager.kt
sed -i 's/if (ftpClient.isConnected)/if (ftpClient?.isConnected == true)/g' app/src/main/java/com/example/util/FtpManager.kt
sed -i 's/ftpClient.disconnect()/ftpClient?.disconnect()/g' app/src/main/java/com/example/util/FtpManager.kt
