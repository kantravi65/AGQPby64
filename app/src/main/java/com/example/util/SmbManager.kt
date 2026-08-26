package com.example.util

import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Properties
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext

object SmbManager {
    private fun getContext(user: String, pass: String): jcifs.CIFSContext {
        val props = Properties()
        props.setProperty("jcifs.smb.client.enableSMB2", "true")
        props.setProperty("jcifs.smb.client.useSMB2Negotiation", "true")
        props.setProperty("jcifs.smb.client.disableSMB1", "false")
        
        val config = PropertyConfiguration(props)
        val baseContext = BaseContext(config)
        
        return if (user.isNotBlank()) {
            // Split domain and user if provided like DOMAIN\user
            val parts = user.split("\\\\")
            val domain = if (parts.size > 1) parts[0] else ""
            val actualUser = if (parts.size > 1) parts[1] else user
            val auth = NtlmPasswordAuthenticator(domain, actualUser, pass)
            baseContext.withCredentials(auth)
        } else {
            baseContext.withAnonymousCredentials()
        }
    }

    private fun buildSmbUrl(host: String, port: Int, remoteDir: String, fileName: String? = null): String {
        var cleanHost = host.replace("smb://", "").removeSuffix("/")
        val cleanDir = remoteDir.removePrefix("/").removeSuffix("/")
        
        val portStr = if (port > 0 && port != 445 && port != 139) ":$port" else ""
        
        var url = "smb://$cleanHost$portStr/"
        if (cleanDir.isNotBlank()) {
            url += "$cleanDir/"
        }
        if (fileName != null) {
            url += fileName
        }
        return url
    }

    suspend fun testConnection(host: String, port: Int, user: String, pass: String, remoteDir: String = ""): Result<String> = withContext(Dispatchers.IO) {
        try {
            val context = getContext(user, pass)
            val url = buildSmbUrl(host, port, remoteDir)
            val file = SmbFile(url, context)
            if (file.exists() && file.isDirectory) {
                Result.success("Connected to SMB server successfully!")
            } else {
                Result.success("Connected to SMB server, but directory does not exist or access denied.")
            }
        } catch (e: Throwable) {
            Result.failure(Exception("SMB Connection Failed: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun uploadJson(
        host: String,
        port: Int,
        user: String,
        pass: String,
        remoteDir: String,
        fileName: String,
        jsonContent: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val context = getContext(user, pass)
            val dirUrl = buildSmbUrl(host, port, remoteDir)
            val dirFile = SmbFile(dirUrl, context)
            if (!dirFile.exists()) {
                dirFile.mkdirs()
            }
            
            val fileUrl = buildSmbUrl(host, port, remoteDir, fileName)
            val smbFile = SmbFile(fileUrl, context)
            val outputStream = smbFile.openOutputStream()
            val bytes = jsonContent.toByteArray(Charsets.UTF_8)
            outputStream.write(bytes)
            outputStream.close()
            
            Result.success("Uploaded snapshot ($fileName) to SMB server successfully!")
        } catch (e: Throwable) {
            Result.failure(Exception("SMB Upload Error: ${e.localizedMessage ?: e.message}"))
        }
    }

    suspend fun downloadLatestJson(
        host: String,
        port: Int,
        user: String,
        pass: String,
        remoteDir: String,
        fileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val context = getContext(user, pass)
            val fileUrl = buildSmbUrl(host, port, remoteDir, fileName)
            val smbFile = SmbFile(fileUrl, context)
            
            if (!smbFile.exists()) {
                return@withContext Result.failure(Exception("File '$fileName' not found on SMB server."))
            }
            
            val inputStream = smbFile.openInputStream()
            val outputStream = ByteArrayOutputStream()
            inputStream.copyTo(outputStream)
            val content = outputStream.toString("UTF-8")
            inputStream.close()
            outputStream.close()
            
            if (content.isNotBlank()) {
                Result.success(content)
            } else {
                Result.failure(Exception("File '$fileName' on SMB server is empty."))
            }
        } catch (e: Throwable) {
            Result.failure(Exception("SMB Download Error: ${e.localizedMessage ?: e.message}"))
        }
    }

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

