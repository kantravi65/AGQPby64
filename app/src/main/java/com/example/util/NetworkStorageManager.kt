package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NetworkStorageManager {
    enum class Protocol { FTP, SMB }

    private fun detectProtocol(host: String): Protocol {
        return if (host.lowercase().startsWith("smb://")) {
            Protocol.SMB
        } else if (host.lowercase().startsWith("ftp://") || host.lowercase().startsWith("ftps://")) {
            Protocol.FTP
        } else {
            // Default to FTP for backward compatibility, or check for backslashes etc.
            if (host.contains("\\\\") || host.startsWith("\\\\")) {
                Protocol.SMB
            } else {
                Protocol.FTP
            }
        }
    }

    private fun cleanHost(host: String, protocol: Protocol): String {
        return if (protocol == Protocol.SMB) {
            host.removePrefix("smb://").removePrefix("SMB://")
        } else {
            host.removePrefix("ftp://").removePrefix("ftps://").removePrefix("FTP://").removePrefix("FTPS://")
        }
    }

    suspend fun testConnection(
        host: String,
        port: Int,
        user: String,
        pass: String,
        remoteDir: String = "",
        usePassive: Boolean = true,
        useFtps: Boolean = false
    ): Result<String> {
        val protocol = detectProtocol(host)
        val cleanHost = cleanHost(host, protocol)

        return if (protocol == Protocol.SMB) {
            SmbManager.testConnection(cleanHost, port, user, pass, remoteDir)
        } else {
            FtpManager.testConnection(cleanHost, port, user, pass, usePassive, useFtps)
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
    ): Result<String> {
        val protocol = detectProtocol(host)
        val cleanHost = cleanHost(host, protocol)

        return if (protocol == Protocol.SMB) {
            SmbManager.uploadJson(cleanHost, port, user, pass, remoteDir, fileName, jsonContent)
        } else {
            FtpManager.uploadJson(cleanHost, port, user, pass, remoteDir, fileName, jsonContent, usePassive, useFtps)
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
    ): Result<String> {
        val protocol = detectProtocol(host)
        val cleanHost = cleanHost(host, protocol)

        return if (protocol == Protocol.SMB) {
            SmbManager.downloadLatestJson(cleanHost, port, user, pass, remoteDir, fileName)
        } else {
            FtpManager.downloadLatestJson(cleanHost, port, user, pass, remoteDir, fileName, usePassive, useFtps)
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

