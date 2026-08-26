#!/bin/bash
sed -i 's/ftpClient = createClient/ftpClient = createClient/g' app/src/main/java/com/example/util/FtpManager.kt
sed -i 's/ftpClient\./ftpClient!!./g' app/src/main/java/com/example/util/FtpManager.kt
