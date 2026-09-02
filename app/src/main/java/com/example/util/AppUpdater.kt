package com.example.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdater {
    private const val DEFAULT_REPO = "kantravi65/AGQPby64"
    private const val USER_AGENT = "QPby64-Android-App"

    fun getUpdateRepo(context: Context): String {
        val custom = SettingsManager(context).githubUpdateRepo.trim()
        if (custom.isNotBlank()) return custom
        val buildConfigRepo = try { com.example.BuildConfig.APP_UPDATE_REPO.trim() } catch (e: Exception) { "" }
        if (buildConfigRepo.isNotBlank()) return buildConfigRepo
        return DEFAULT_REPO
    }

    fun getUpdateToken(context: Context): String {
        val custom = SettingsManager(context).githubUpdateToken.trim()
        if (custom.isNotBlank()) return custom
        return try { com.example.BuildConfig.APP_UPDATE_TOKEN.trim() } catch (e: Exception) { "" }
    }

    suspend fun checkForUpdatesAndPrompt(
        context: Context, 
        showToastIfNoUpdate: Boolean = false,
        onProgress: (String) -> Unit = {}
    ) {
        withContext(Dispatchers.IO) {
            try {
                if (showToastIfNoUpdate) {
                    withContext(Dispatchers.Main) { onProgress("Checking for updates...") }
                }
                
                val repo = getUpdateRepo(context)
                val token = getUpdateToken(context)
                val appGithubSha = try { com.example.BuildConfig.APP_GITHUB_SHA.trim() } catch (e: Exception) { "" }
                val buildTime = try { com.example.BuildConfig.BUILD_TIME } catch (e: Exception) { 0L }

                if (repo.isBlank()) {
                    if (showToastIfNoUpdate) {
                        withContext(Dispatchers.Main) { 
                            Toast.makeText(context, "GitHub repository is not configured.", Toast.LENGTH_LONG).show()
                            onProgress("")
                        }
                    }
                    return@withContext
                }

                // 1. Fetch Latest Release from GitHub API
                var releaseJson: JSONObject? = null
                val latestUrl = "https://api.github.com/repos/$repo/releases/latest"
                var connection = openGitHubConnection(latestUrl, token)
                var responseCode = connection.responseCode

                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    releaseJson = JSONObject(response)
                } else if (responseCode == 404) {
                    // Fallback to /releases list (in case /latest is not indexed or releases are prereleases/drafts)
                    val listUrl = "https://api.github.com/repos/$repo/releases?per_page=1"
                    val listConn = openGitHubConnection(listUrl, token)
                    if (listConn.responseCode == 200) {
                        val listResp = listConn.inputStream.bufferedReader().readText()
                        val listArr = JSONArray(listResp)
                        if (listArr.length() > 0) {
                            releaseJson = listArr.getJSONObject(0)
                        }
                    } else {
                        responseCode = listConn.responseCode
                    }
                }

                if (releaseJson == null) {
                    if (showToastIfNoUpdate) {
                        val message = when (responseCode) {
                            404 -> "No published releases found for '$repo'.\nPlease verify repository name."
                            403 -> "GitHub API rate limit reached. If this is a private repo, configure a token in Settings."
                            401 -> "GitHub authorization failed. Please check your GitHub token."
                            else -> "Failed to check updates (HTTP $responseCode from GitHub)."
                        }
                        Log.e("AppUpdater", "Failed to get release: HTTP $responseCode for repo '$repo'")
                        withContext(Dispatchers.Main) { 
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            onProgress("")
                        }
                    }
                    return@withContext
                }

                val tagName = releaseJson.getString("tag_name")
                val releaseName = releaseJson.optString("name", tagName)
                val releaseNotes = releaseJson.optString("body", "No release notes provided.")
                val publishedAtStr = releaseJson.optString("published_at", "")
                
                var isNewer = false
                
                // Try getting the commit SHA of the release tag if configured
                var latestReleaseSha = ""
                try {
                    val tagUrl = "https://api.github.com/repos/$repo/git/refs/tags/$tagName"
                    val tagConn = openGitHubConnection(tagUrl, token)
                    if (tagConn.responseCode == 200) {
                        val tagResp = tagConn.inputStream.bufferedReader().readText()
                        val tagJson = JSONObject(tagResp)
                        latestReleaseSha = tagJson.getJSONObject("object").getString("sha")
                    }
                } catch(e: Exception) {
                    Log.e("AppUpdater", "Error getting tag sha: ${e.message}")
                }

                if (latestReleaseSha.isNotBlank() && appGithubSha.isNotBlank()) {
                    isNewer = (latestReleaseSha != appGithubSha)
                } else if (publishedAtStr.isNotBlank() && buildTime > 0L) {
                    // Fallback to published_at vs BUILD_TIME + 10 mins (600,000 ms) buffer
                    val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    val publishedTime = format.parse(publishedAtStr)?.time ?: 0L
                    if (publishedTime > buildTime + 600_000L) {
                        isNewer = true
                    }
                } else {
                    // Compare tag names (e.g., v1.0.3 vs 1.0.2)
                    val appVersion = try { com.example.BuildConfig.VERSION_NAME } catch (e: Exception) { "" }
                    val cleanTag = tagName.removePrefix("v").trim()
                    if (cleanTag != "latest" && appVersion.isNotBlank() && cleanTag != appVersion) {
                        isNewer = isVersionHigher(cleanTag, appVersion)
                    } else {
                        // If tag is 'latest', treat as available if manually triggered
                        if (showToastIfNoUpdate) {
                            isNewer = true
                        }
                    }
                }
                
                val assets = releaseJson.optJSONArray("assets") ?: JSONArray()
                var downloadUrl = ""
                var directDownloadUrl = ""
                var apkName = "update.apk"

                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkName = name
                        downloadUrl = asset.optString("url", "")
                        directDownloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }

                // Choose the best URL: direct browser_download_url for public access, or API url when using token
                val finalDownloadUrl = when {
                    directDownloadUrl.isNotBlank() && token.isBlank() -> directDownloadUrl
                    downloadUrl.isNotBlank() -> downloadUrl
                    directDownloadUrl.isNotBlank() -> directDownloadUrl
                    else -> ""
                }

                withContext(Dispatchers.Main) {
                    onProgress("")
                    if (finalDownloadUrl.isEmpty()) {
                        if (showToastIfNoUpdate) {
                            Toast.makeText(context, "Release found ($tagName), but no APK asset is attached.", Toast.LENGTH_LONG).show()
                        }
                    } else if (isNewer) {
                        val displayTitle = if (releaseName.isNotBlank() && releaseName != tagName) "$releaseName ($tagName)" else tagName
                        AlertDialog.Builder(context)
                            .setTitle("Update Available")
                            .setMessage("A new update ($displayTitle) is available from $repo!\n\nDetails:\n$releaseNotes")
                            .setPositiveButton("Update Now") { _, _ ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    downloadAndInstall(context, finalDownloadUrl, token, tagName, apkName, onProgress)
                                }
                            }
                            .setNegativeButton("Later", null)
                            .show()
                    } else {
                        if (showToastIfNoUpdate) {
                            Toast.makeText(context, "You are already using the latest version.", Toast.LENGTH_LONG).show()
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("AppUpdater", "Update error", e)
                withContext(Dispatchers.Main) { 
                    if (showToastIfNoUpdate) {
                        Toast.makeText(context, "Update check failed: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
                    }
                    onProgress("")
                }
            }
        }
    }

    private fun openGitHubConnection(urlString: String, token: String): HttpURLConnection {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        if (token.isNotBlank()) {
            conn.setRequestProperty("Authorization", "Bearer $token")
        }
        return conn
    }

    private suspend fun downloadAndInstall(
        context: Context, 
        initialDownloadUrl: String, 
        token: String, 
        tagName: String,
        apkName: String,
        onProgress: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { onProgress("Connecting to download...") }
                
                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                val apkFile = File(downloadsDir, apkName.ifBlank { "update.apk" })
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                // Handle redirects up to 5 times (e.g., GitHub API -> AWS S3 redirect)
                var currentUrl = initialDownloadUrl
                var connection: HttpURLConnection? = null
                var redirectCount = 0
                val maxRedirects = 5
                var inputStream: InputStream? = null

                while (redirectCount < maxRedirects) {
                    val url = URL(currentUrl)
                    connection = url.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 30000
                    connection.readTimeout = 30000
                    connection.setRequestProperty("User-Agent", USER_AGENT)
                    
                    val isGitHubApiUrl = currentUrl.contains("api.github.com")
                    if (isGitHubApiUrl) {
                        connection.setRequestProperty("Accept", "application/octet-stream")
                        if (token.isNotBlank()) {
                            connection.setRequestProperty("Authorization", "Bearer $token")
                        }
                    }

                    val code = connection.responseCode
                    if (code == HttpURLConnection.HTTP_MOVED_TEMP || 
                        code == HttpURLConnection.HTTP_MOVED_PERM || 
                        code == HttpURLConnection.HTTP_SEE_OTHER ||
                        code == 307 || code == 308) {
                        val location = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (location.isNullOrBlank()) {
                            throw Exception("Received redirect without Location header.")
                        }
                        currentUrl = location
                        redirectCount++
                    } else if (code == HttpURLConnection.HTTP_OK) {
                        inputStream = connection.inputStream
                        break
                    } else {
                        val error = connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                        connection.disconnect()
                        throw Exception("Download server returned: $error")
                    }
                }

                if (inputStream == null || connection == null) {
                    throw Exception("Failed to establish download stream after redirects.")
                }

                val contentLength = connection.contentLength
                val outputStream = apkFile.outputStream()
                
                withContext(Dispatchers.Main) { onProgress("Downloading $tagName (0%)...") }

                inputStream.use { input ->
                    outputStream.use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var bytesRead: Int
                        var totalRead = 0L
                        var lastReportedProgress = -1
                        while (input.read(buffer).also { bytesRead = it } >= 0) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val progress = ((totalRead * 100) / contentLength).toInt()
                                if (progress != lastReportedProgress && progress % 5 == 0) {
                                    lastReportedProgress = progress
                                    withContext(Dispatchers.Main) { 
                                        onProgress("Downloading $tagName ($progress%)...") 
                                    }
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) { onProgress("Opening installer...") }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                
                withContext(Dispatchers.Main) { 
                    context.startActivity(intent)
                    onProgress("")
                }
            } catch (e: Exception) {
                Log.e("AppUpdater", "Install error", e)
                withContext(Dispatchers.Main) { 
                    Toast.makeText(context, "Download failed: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
                    onProgress("")
                }
            }
        }
    }

    private fun isVersionHigher(newVersion: String, currentVersion: String): Boolean {
        try {
            val newParts = newVersion.split('.').mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
            val currentParts = currentVersion.split('.').mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
            val maxLen = maxOf(newParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val n = newParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (n > c) return true
                if (n < c) return false
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return false
    }
}
