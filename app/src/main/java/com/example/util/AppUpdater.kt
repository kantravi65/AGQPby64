package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppUpdater {
    // Read from BuildConfig
    val APP_UPDATE_REPO = com.example.BuildConfig.APP_UPDATE_REPO
    val APP_UPDATE_TOKEN = com.example.BuildConfig.APP_UPDATE_TOKEN

    suspend fun checkForUpdatesAndInstall(context: Context, onProgress: (String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { onProgress("Checking for updates...") }
                
                if (APP_UPDATE_REPO.isBlank()) {
                    withContext(Dispatchers.Main) { 
                        Toast.makeText(context, "GitHub Repo not configured.", Toast.LENGTH_LONG).show()
                        onProgress("")
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
                    val error = connection.errorStream?.bufferedReader()?.readText()
                    Log.e("AppUpdater", "Failed to get release: ${connection.responseCode} - $error")
                    withContext(Dispatchers.Main) { 
                        Toast.makeText(context, "Failed to find update: HTTP ${connection.responseCode}", Toast.LENGTH_LONG).show()
                        onProgress("")
                    }
                    return@withContext
                }

                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val tagName = json.getString("tag_name")
                val assets = json.getJSONArray("assets")
                
                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        downloadUrl = asset.getString("url")
                        break
                    }
                }

                if (downloadUrl.isEmpty()) {
                    withContext(Dispatchers.Main) { 
                        Toast.makeText(context, "No APK found in the latest release ($tagName).", Toast.LENGTH_LONG).show()
                        onProgress("")
                    }
                    return@withContext
                }

                withContext(Dispatchers.Main) { onProgress("Downloading version $tagName...") }

                // 2. Download the APK
                val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val downloadConnection = URL(downloadUrl).openConnection() as HttpURLConnection
                downloadConnection.requestMethod = "GET"
                if (APP_UPDATE_TOKEN.isNotBlank()) {
                    downloadConnection.setRequestProperty("Authorization", "Bearer $APP_UPDATE_TOKEN")
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

                val inputStream = downloadConnection.inputStream
                val outputStream = apkFile.outputStream()
                
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) { onProgress("Installing update...") }

                // 3. Install the APK
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
                Log.e("AppUpdater", "Update error", e)
                withContext(Dispatchers.Main) { 
                    Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                    onProgress("")
                }
            }
        }
    }
}
