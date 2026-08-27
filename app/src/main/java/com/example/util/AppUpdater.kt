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
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppUpdater {
    // Read from BuildConfig
    val APP_UPDATE_REPO = com.example.BuildConfig.APP_UPDATE_REPO
    val APP_UPDATE_TOKEN = com.example.BuildConfig.APP_UPDATE_TOKEN
    val APP_GITHUB_SHA = com.example.BuildConfig.APP_GITHUB_SHA

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
                
                if (APP_UPDATE_REPO.isBlank()) {
                    if (showToastIfNoUpdate) {
                        withContext(Dispatchers.Main) { 
                            Toast.makeText(context, "GitHub Repo not configured.", Toast.LENGTH_LONG).show()
                            onProgress("")
                        }
                    }
                    return@withContext
                }

                // 1. Get Latest Release
                val apiUrl = "https://api.github.com/repos/$APP_UPDATE_REPO/releases/latest"
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                if (APP_UPDATE_TOKEN.isNotBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $APP_UPDATE_TOKEN")
                }
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                if (connection.responseCode != 200) {
                    if (showToastIfNoUpdate) {
                        val error = connection.errorStream?.bufferedReader()?.readText()
                        Log.e("AppUpdater", "Failed to get release: ${connection.responseCode} - $error")
                        withContext(Dispatchers.Main) { 
                            Toast.makeText(context, "Failed to find update: HTTP ${connection.responseCode}", Toast.LENGTH_LONG).show()
                            onProgress("")
                        }
                    }
                    return@withContext
                }

                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val tagName = json.getString("tag_name")
                val releaseNotes = json.optString("body", "No release notes provided.")
                val publishedAtStr = json.optString("published_at", "")
                
                var isNewer = false
                
                // Try getting the commit SHA of the release tag
                var latestReleaseSha = ""
                try {
                    val tagUrl = "https://api.github.com/repos/$APP_UPDATE_REPO/git/refs/tags/$tagName"
                    val tagConn = URL(tagUrl).openConnection() as HttpURLConnection
                    tagConn.requestMethod = "GET"
                    if (APP_UPDATE_TOKEN.isNotBlank()) {
                        tagConn.setRequestProperty("Authorization", "Bearer $APP_UPDATE_TOKEN")
                    }
                    tagConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    if (tagConn.responseCode == 200) {
                        val tagResp = tagConn.inputStream.bufferedReader().readText()
                        val tagJson = JSONObject(tagResp)
                        latestReleaseSha = tagJson.getJSONObject("object").getString("sha")
                    }
                } catch(e: Exception) {
                    Log.e("AppUpdater", "Error getting tag sha", e)
                }

                if (latestReleaseSha.isNotBlank() && APP_GITHUB_SHA.isNotBlank()) {
                    isNewer = (latestReleaseSha != APP_GITHUB_SHA)
                } else {
                    // Fallback to published_at vs BUILD_TIME + 10 mins (600,000 ms) buffer
                    if (publishedAtStr.isNotBlank()) {
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        val publishedTime = format.parse(publishedAtStr)?.time ?: 0L
                        val buildTime = com.example.BuildConfig.BUILD_TIME
                        if (publishedTime > buildTime + 600_000L) {
                            isNewer = true
                        }
                    }
                }
                
                val assets = json.getJSONArray("assets")
                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        downloadUrl = asset.getString("url")
                        break
                    }
                }

                withContext(Dispatchers.Main) {
                    onProgress("")
                    if (downloadUrl.isEmpty()) {
                        if (showToastIfNoUpdate) {
                            Toast.makeText(context, "No APK found in the latest release.", Toast.LENGTH_LONG).show()
                        }
                    } else if (isNewer) {
                        AlertDialog.Builder(context)
                            .setTitle("Update Available")
                            .setMessage("A new update ($tagName) is available!\n\nDetails:\n$releaseNotes")
                            .setPositiveButton("Update") { _, _ ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    downloadAndInstall(context, downloadUrl, APP_UPDATE_TOKEN, tagName, onProgress)
                                }
                            }
                            .setNegativeButton("Later", null)
                            .show()
                    } else {
                        if (showToastIfNoUpdate) {
                            Toast.makeText(context, "You are already on the latest version.", Toast.LENGTH_LONG).show()
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("AppUpdater", "Update error", e)
                withContext(Dispatchers.Main) { 
                    if (showToastIfNoUpdate) {
                        Toast.makeText(context, "Update check failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    onProgress("")
                }
            }
        }
    }

    private suspend fun downloadAndInstall(
        context: Context, 
        downloadUrl: String, 
        token: String, 
        tagName: String,
        onProgress: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { onProgress("Downloading version $tagName...") }
                
                val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val downloadConnection = URL(downloadUrl).openConnection() as HttpURLConnection
                downloadConnection.requestMethod = "GET"
                if (token.isNotBlank()) {
                    downloadConnection.setRequestProperty("Authorization", "Bearer $token")
                }
                downloadConnection.setRequestProperty("Accept", "application/octet-stream")

                if (downloadConnection.responseCode != 200) {
                    val error = downloadConnection.errorStream?.bufferedReader()?.readText()
                    Log.e("AppUpdater", "Failed to download apk: ${downloadConnection.responseCode} - $error")
                    withContext(Dispatchers.Main) { 
                        Toast.makeText(context, "Failed to download update: HTTP ${downloadConnection.responseCode}", Toast.LENGTH_LONG).show()
                        onProgress("")
                    }
                    return@withContext
                }

                val contentLength = downloadConnection.contentLength
                val inputStream = downloadConnection.inputStream
                val outputStream = apkFile.outputStream()
                
                inputStream.use { input ->
                    outputStream.use { output ->
                        if (contentLength > 0) {
                            val buffer = ByteArray(8 * 1024)
                            var bytesRead: Int
                            var totalRead = 0L
                            var lastProgress = -1
                            while (input.read(buffer).also { bytesRead = it } >= 0) {
                                output.write(buffer, 0, bytesRead)
                                totalRead += bytesRead
                                val progress = ((totalRead * 100) / contentLength).toInt()
                                if (progress != lastProgress && progress % 2 == 0) {
                                    lastProgress = progress
                                    withContext(Dispatchers.Main) { onProgress("Downloading version $tagName... $progress%") }
                                }
                            }
                        } else {
                            input.copyTo(output)
                        }
                    }
                }

                withContext(Dispatchers.Main) { onProgress("Installing update...") }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )

                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                
                withContext(Dispatchers.Main) { 
                    context.startActivity(intent)
                    onProgress("")
                }
            } catch (e: Exception) {
                Log.e("AppUpdater", "Install error", e)
                withContext(Dispatchers.Main) { 
                    Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
                    onProgress("")
                }
            }
        }
    }
}
