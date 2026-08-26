#!/bin/bash
sed -i -z 's/        val ftpClient = createClient(useFtps || port == 990)\n        try {/        try {\n            val ftpClient = createClient(useFtps || port == 990)/g' app/src/main/java/com/example/util/FtpManager.kt
