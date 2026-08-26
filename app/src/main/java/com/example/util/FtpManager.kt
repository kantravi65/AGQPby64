package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import org.apache.commons.net.ftp.FTPReply
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

object FtpManager {

    private fun createClient(useFtps: Boolean = false): FTPClient {
        val client = if (useFtps) {
            val ftps = FTPSClient("TLS", false)
            ftps.trustManager = org.apache.commons.net.util.TrustManagerUtils.getAcceptAllTrustManager()
            ftps
        } else FTPClient()
        client.controlEncoding = "UTF-8"
        client.connectTimeout = 15000
        client.defaultTimeout = 15000
        client.setDataTimeout(15000)
        if (client is FTPSClient) {
            client.isRemoteVerificationEnabled = false
        }
        return client
    }

    private fun configureTransferMode(client: FTPClient, usePassive: Boolean) {
        if (usePassive) {
            client.enterLocalPassiveMode()
        } else {
            client.enterLocalActiveMode()
        }
    }

    private fun navigateToDirectory(client: FTPClient, remoteDir: String) {
        val cleanDir = remoteDir.trim().removePrefix("/").removeSuffix("/")
        if (cleanDir.isEmpty()) return

        val segments = cleanDir.split("/")
        for (segment in segments) {
            if (segment.isNotEmpty()) {
                val changed = client.changeWorkingDirectory(segment)
                if (!changed) {
                    client.makeDirectory(segment)
                    client.changeWorkingDirectory(segment)
                }
            }
        }
    }

    suspend fun testConnection(
        host: String,
        port: Int,
        user: String,
        pass: String,
        usePassive: Boolean = true,
        useFtps: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        if (host.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("FTP Host IP or address cannot be empty."))
        }

        var ftpClient: FTPClient? = null
        try {
            ftpClient = createClient(useFtps || port == 990)
            val targetPort = if (port > 0) port else (if (useFtps || port == 990) 990 else 21)
            ftpClient!!.connect(host, targetPort)

            val reply = ftpClient!!.replyCode
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient?.disconnect()
                return@withContext Result.failure(IOException("FTP server refused connection. Server Response Code: $reply"))
            }

            val loggedIn = if (user.isNotBlank()) {
                ftpClient!!.login(user, pass)
            } else {
                ftpClient!!.login("anonymous", "")
            }

            if (!loggedIn) {
                val errorMsg = "FTP login failed for user '${if (user.isBlank()) "anonymous" else user}'. Check credentials."
                try { ftpClient!!.logout(); ftpClient?.disconnect() } catch (_: Exception) {}
                return@withContext Result.failure(IOException(errorMsg))
            }

            if (useFtps || port == 990) {
                (ftpClient as FTPSClient).execPBSZ(0)
                (ftpClient as FTPSClient).execPROT("P")
            }

            ftpClient!!.enterLocalPassiveMode()
            if (!usePassive) {
                ftpClient!!.enterLocalActiveMode()
            }
            val systemType = try { ftpClient!!.systemType } catch (_: Exception) { "FTP" }

            try {
                ftpClient!!.logout()
                ftpClient?.disconnect()
            } catch (_: Exception) {}

            Result.success("Connected successfully to FTP server ($systemType) at $host:$targetPort!")
        } catch (e: Throwable) {
            try {
                if (ftpClient?.isConnected == true) {
                    ftpClient?.disconnect()
                }
            } catch (_: Exception) {}
            Result.failure(Exception("FTP Connection Failed: ${e.localizedMessage ?: e.message}. Ensure IP/Port is reachable from device."))
        }
    }

    suspend fun uploadJson(
        host: String,
        port: Int,
        user: String,
        pass: String,
        remoteDir: String,
        fileName: String,
        jsonContent: String,
        usePassive: Boolean = true,
        useFtps: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        if (host.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("FTP Host is empty."))
        }

        var ftpClient: FTPClient? = null
        try {
            ftpClient = createClient(useFtps || port == 990)
            val targetPort = if (port > 0) port else (if (useFtps || port == 990) 990 else 21)
            ftpClient!!.connect(host, targetPort)

            if (!FTPReply.isPositiveCompletion(ftpClient!!.replyCode)) {
                ftpClient?.disconnect()
                return@withContext Result.failure(IOException("Server refused connection"))
            }

            val loggedIn = if (user.isNotBlank()) ftpClient!!.login(user, pass) else ftpClient!!.login("anonymous", "")
            if (!loggedIn) {
                try { ftpClient?.disconnect() } catch (_: Exception) {}
                return@withContext Result.failure(IOException("Invalid FTP credentials"))
            }

            if (useFtps || port == 990) {
                (ftpClient as FTPSClient).execPBSZ(0)
                (ftpClient as FTPSClient).execPROT("P")
            }

            ftpClient!!.enterLocalPassiveMode()
            if (!usePassive) {
                ftpClient!!.enterLocalActiveMode()
            }
            ftpClient!!.setFileType(FTP.BINARY_FILE_TYPE)

            navigateToDirectory(ftpClient!!, remoteDir)

            val bytes = jsonContent.toByteArray(Charsets.UTF_8)
            val inputStream = ByteArrayInputStream(bytes)
            val stored = ftpClient!!.storeFile(fileName, inputStream)
            inputStream.close()

            try {
                ftpClient!!.logout()
                ftpClient?.disconnect()
            } catch (_: Exception) {}

            if (stored) {
                Result.success("Uploaded snapshot ($fileName) to FTP server successfully!")
            } else {
                Result.failure(IOException("FTP server denied write operation. Verify target folder permissions."))
            }
        } catch (e: Throwable) {
            try { if (ftpClient?.isConnected == true) ftpClient?.disconnect() } catch (_: Exception) {}
            Result.failure(Exception("FTP Upload Error: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun downloadLatestJson(
        host: String,
        port: Int,
        user: String,
        pass: String,
        remoteDir: String,
        fileName: String,
        usePassive: Boolean = true,
        useFtps: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        if (host.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("FTP Host is empty."))
        }

        var ftpClient: FTPClient? = null
        try {
            ftpClient = createClient(useFtps || port == 990)
            val targetPort = if (port > 0) port else (if (useFtps || port == 990) 990 else 21)
            ftpClient!!.connect(host, targetPort)

            if (!FTPReply.isPositiveCompletion(ftpClient!!.replyCode)) {
                ftpClient?.disconnect()
                return@withContext Result.failure(IOException("Server refused connection"))
            }

            val loggedIn = if (user.isNotBlank()) ftpClient!!.login(user, pass) else ftpClient!!.login("anonymous", "")
            if (!loggedIn) {
                try { ftpClient?.disconnect() } catch (_: Exception) {}
                return@withContext Result.failure(IOException("Invalid FTP credentials"))
            }

            if (useFtps || port == 990) {
                (ftpClient as FTPSClient).execPBSZ(0)
                (ftpClient as FTPSClient).execPROT("P")
            }

            ftpClient!!.enterLocalPassiveMode()
            if (!usePassive) {
                ftpClient!!.enterLocalActiveMode()
            }
            ftpClient!!.setFileType(FTP.BINARY_FILE_TYPE)

            navigateToDirectory(ftpClient!!, remoteDir)

            val outputStream = ByteArrayOutputStream()
            val retrieved = ftpClient!!.retrieveFile(fileName, outputStream)
            val content = outputStream.toString("UTF-8")
            outputStream.close()

            try {
                ftpClient!!.logout()
                ftpClient?.disconnect()
            } catch (_: Exception) {}

            if (retrieved && content.isNotBlank()) {
                Result.success(content)
            } else {
                Result.failure(IOException("File '$fileName' not found in FTP directory or is empty."))
            }
        } catch (e: Throwable) {
            try { if (ftpClient?.isConnected == true) ftpClient?.disconnect() } catch (_: Exception) {}
            Result.failure(Exception("FTP Download Error: ${e.localizedMessage ?: e.message}"))
        }
    }

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

