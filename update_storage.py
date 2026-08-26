import re

# Update FtpManager
with open('app/src/main/java/com/example/util/FtpManager.kt', 'r') as f:
    ftp_content = f.read()

ftp_new_methods = """
    suspend fun listDirectories(
        host: String,
        port: Int,
        user: String,
        pass: String,
        remoteDir: String,
        usePassive: Boolean = true,
        useFtps: Boolean = false
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = createClient(useFtps || port == 990)
            val targetPort = if (port > 0) port else (if (useFtps || port == 990) 990 else 21)
            ftpClient.connect(host, targetPort)
            if (!FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
                ftpClient.disconnect()
                return@withContext Result.failure(IOException("Server refused connection"))
            }
            val loggedIn = if (user.isNotBlank()) ftpClient.login(user, pass) else ftpClient.login("anonymous", "")
            if (!loggedIn) {
                try { ftpClient.disconnect() } catch (_: Exception) {}
                return@withContext Result.failure(IOException("Invalid FTP credentials"))
            }
            if (useFtps || port == 990) {
                (ftpClient as FTPSClient).execPBSZ(0)
                (ftpClient as FTPSClient).execPROT("P")
            }
            ftpClient.enterLocalPassiveMode()
            if (!usePassive) {
                ftpClient.enterLocalActiveMode()
            }
            if (remoteDir.isNotBlank()) {
                val cleanDir = remoteDir.trim().removePrefix("/").removeSuffix("/")
                if (cleanDir.isNotEmpty()) {
                    ftpClient.changeWorkingDirectory(cleanDir)
                }
            }
            val files = ftpClient.listDirectories()
            val dirs = files?.map { it.name } ?: emptyList()
            try { ftpClient.logout(); ftpClient.disconnect() } catch (_: Exception) {}
            Result.success(dirs)
        } catch (e: Throwable) {
            try { ftpClient?.disconnect() } catch (_: Exception) {}
            Result.failure(Exception("FTP List Error: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun createDirectory(
        host: String,
        port: Int,
        user: String,
        pass: String,
        baseDir: String,
        newDir: String,
        usePassive: Boolean = true,
        useFtps: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        var ftpClient: FTPClient? = null
        try {
            ftpClient = createClient(useFtps || port == 990)
            val targetPort = if (port > 0) port else (if (useFtps || port == 990) 990 else 21)
            ftpClient.connect(host, targetPort)
            if (!FTPReply.isPositiveCompletion(ftpClient.replyCode)) {
                ftpClient.disconnect()
                return@withContext Result.failure(IOException("Server refused connection"))
            }
            val loggedIn = if (user.isNotBlank()) ftpClient.login(user, pass) else ftpClient.login("anonymous", "")
            if (!loggedIn) {
                try { ftpClient.disconnect() } catch (_: Exception) {}
                return@withContext Result.failure(IOException("Invalid FTP credentials"))
            }
            if (useFtps || port == 990) {
                (ftpClient as FTPSClient).execPBSZ(0)
                (ftpClient as FTPSClient).execPROT("P")
            }
            ftpClient.enterLocalPassiveMode()
            if (!usePassive) {
                ftpClient.enterLocalActiveMode()
            }
            if (baseDir.isNotBlank()) {
                val cleanDir = baseDir.trim().removePrefix("/").removeSuffix("/")
                if (cleanDir.isNotEmpty()) {
                    ftpClient.changeWorkingDirectory(cleanDir)
                }
            }
            val success = ftpClient.makeDirectory(newDir)
            try { ftpClient.logout(); ftpClient.disconnect() } catch (_: Exception) {}
            if (success) Result.success(newDir)
            else Result.failure(IOException("Could not create directory"))
        } catch (e: Throwable) {
            try { ftpClient?.disconnect() } catch (_: Exception) {}
            Result.failure(Exception("FTP Mkdir Error: ${e.localizedMessage ?: e.message}"))
        }
    }
}
"""

ftp_content = ftp_content.replace("}\n}", "}\n" + ftp_new_methods)

with open('app/src/main/java/com/example/util/FtpManager.kt', 'w') as f:
    f.write(ftp_content)


# Update SmbManager
with open('app/src/main/java/com/example/util/SmbManager.kt', 'r') as f:
    smb_content = f.read()

smb_new_methods = """
    suspend fun listDirectories(
        host: String,
        port: Int,
        user: String,
        pass: String,
        remoteDir: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val context = getContext(user, pass)
            val url = buildSmbUrl(host, port, remoteDir)
            val dirFile = SmbFile(url, context)
            if (!dirFile.exists() || !dirFile.isDirectory) {
                return@withContext Result.failure(Exception("Directory does not exist"))
            }
            val files = dirFile.listFiles()
            val dirs = files?.filter { it.isDirectory }?.map { it.name.removeSuffix("/") } ?: emptyList()
            Result.success(dirs)
        } catch (e: Throwable) {
            Result.failure(Exception("SMB List Error: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun createDirectory(
        host: String,
        port: Int,
        user: String,
        pass: String,
        baseDir: String,
        newDir: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val context = getContext(user, pass)
            val base = buildSmbUrl(host, port, baseDir)
            var baseStr = base
            if (!baseStr.endsWith("/")) baseStr += "/"
            val newDirUrl = baseStr + newDir
            val newDirFile = SmbFile(newDirUrl, context)
            newDirFile.mkdir()
            Result.success(newDir)
        } catch (e: Throwable) {
            Result.failure(Exception("SMB Mkdir Error: ${e.localizedMessage ?: e.message}"))
        }
    }
}
"""

smb_content = smb_content.replace("}\n}", "}\n" + smb_new_methods)

with open('app/src/main/java/com/example/util/SmbManager.kt', 'w') as f:
    f.write(smb_content)


# Update NetworkStorageManager
with open('app/src/main/java/com/example/util/NetworkStorageManager.kt', 'r') as f:
    nsm_content = f.read()

nsm_new_methods = """
    suspend fun listDirectories(
        host: String,
        port: Int,
        user: String,
        pass: String,
        remoteDir: String,
        usePassive: Boolean = true,
        useFtps: Boolean = false
    ): Result<List<String>> {
        val protocol = detectProtocol(host)
        val cleanHost = cleanHost(host, protocol)

        return if (protocol == Protocol.SMB) {
            SmbManager.listDirectories(cleanHost, port, user, pass, remoteDir)
        } else {
            FtpManager.listDirectories(cleanHost, port, user, pass, remoteDir, usePassive, useFtps)
        }
    }

    suspend fun createDirectory(
        host: String,
        port: Int,
        user: String,
        pass: String,
        baseDir: String,
        newDir: String,
        usePassive: Boolean = true,
        useFtps: Boolean = false
    ): Result<String> {
        val protocol = detectProtocol(host)
        val cleanHost = cleanHost(host, protocol)

        return if (protocol == Protocol.SMB) {
            SmbManager.createDirectory(cleanHost, port, user, pass, baseDir, newDir)
        } else {
            FtpManager.createDirectory(cleanHost, port, user, pass, baseDir, newDir, usePassive, useFtps)
        }
    }
}
"""

nsm_content = nsm_content.replace("}\n}", "}\n" + nsm_new_methods)

with open('app/src/main/java/com/example/util/NetworkStorageManager.kt', 'w') as f:
    f.write(nsm_content)

print("Updated storage managers")
