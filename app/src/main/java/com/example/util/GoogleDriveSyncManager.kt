package com.example.util

import android.content.Context
import com.example.ui.viewmodel.OtsViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

enum class SyncStrategy {
    MERGE_DEDUPLICATE,
    OVERWRITE_REMOTE,
    OVERWRITE_LOCAL
}

object GoogleDriveSyncManager {

    fun saveGoogleAccount(
        context: Context,
        settingsManager: SettingsManager,
        account: GoogleSignInAccount
    ) {
        val email = account.email ?: ""
        val name = account.displayName ?: account.givenName ?: "Google User"
        val photoUrl = account.photoUrl?.toString() ?: ""

        settingsManager.isGoogleSignedIn = true
        settingsManager.googleAccountEmail = email
        settingsManager.googleAccountName = name
        settingsManager.googlePhotoUrl = photoUrl
        if (email.isNotBlank()) settingsManager.userEmail = email
        if (name.isNotBlank()) settingsManager.userName = name
        settingsManager.isGoogleDriveSyncEnabled = true
    }

    fun connectDirectGoogleAccount(
        context: Context,
        settingsManager: SettingsManager,
        email: String,
        name: String = "Google User"
    ) {
        val cleanEmail = email.trim()
        val cleanName = if (name.isNotBlank()) name.trim() else "Google User"
        settingsManager.isGoogleSignedIn = true
        settingsManager.googleAccountEmail = cleanEmail
        settingsManager.googleAccountName = cleanName
        if (cleanEmail.isNotBlank()) settingsManager.userEmail = cleanEmail
        if (cleanName.isNotBlank()) settingsManager.userName = cleanName
        settingsManager.isGoogleDriveSyncEnabled = true
    }

    fun signOutGoogle(context: Context, settingsManager: SettingsManager) {
        settingsManager.isGoogleSignedIn = false
        settingsManager.googleAccountEmail = ""
        settingsManager.googleAccountName = ""
        settingsManager.googlePhotoUrl = ""
        settingsManager.isGoogleDriveSyncEnabled = false
        try {
            GoogleSignInHelper.getGoogleSignInClient(context).signOut()
        } catch (_: Exception) {}
    }

    fun backupToDrive(
        context: Context,
        viewModel: OtsViewModel,
        settingsManager: SettingsManager,
        strategy: SyncStrategy = SyncStrategy.MERGE_DEDUPLICATE,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (!settingsManager.isGoogleSignedIn) {
            val lastAcc = GoogleSignInHelper.getLastSignedInAccount(context)
            if (lastAcc != null) {
                saveGoogleAccount(context, settingsManager, lastAcc)
            } else {
                onComplete(false, "Please sign in with your Google Account first.")
                return
            }
        }

        viewModel.viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val lastAcc = GoogleSignInHelper.getLastSignedInAccount(context)
                if (lastAcc?.account == null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(false, "Failed to get Google account.")
                    }
                    return@launch
                }
                
                val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                    context,
                    lastAcc.account!!,
                    "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.readonly"
                )
                
                val jsonPayload = viewModel.exportQuestionsToJson()
                val backupPath = settingsManager.googleDriveBackupPath.ifBlank { "My Drive/QuestionBank_Backup/backup.json" }
                val filename = backupPath.substringAfterLast("/").ifBlank { "question_bank_backup.json" }

                // Search for existing file
                val query = java.net.URLEncoder.encode("name='$filename' and trashed=false", "UTF-8")
                val searchUrl = java.net.URL("https://www.googleapis.com/drive/v3/files?q=$query")
                val searchConn = searchUrl.openConnection() as java.net.HttpURLConnection
                searchConn.setRequestProperty("Authorization", "Bearer $token")
                searchConn.requestMethod = "GET"
                
                var fileId: String? = null
                if (searchConn.responseCode in 200..299) {
                    val searchResponse = searchConn.inputStream.bufferedReader().use { it.readText() }
                    val idMatch = "\"id\":\\s*\"([^\"]+)\"".toRegex().find(searchResponse)
                    fileId = idMatch?.groupValues?.get(1)
                }

                                val uploadUrl: java.net.URL
                val isPatch: Boolean
                if (fileId != null) {
                    uploadUrl = java.net.URL("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=multipart")
                    isPatch = true
                } else {
                    uploadUrl = java.net.URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                    isPatch = false
                }
                
                val conn = uploadUrl.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                if (isPatch) {
                    conn.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                }
                conn.setRequestProperty("Authorization", "Bearer $token")
                val boundary = "foo_bar_baz"
                conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                conn.doOutput = true

                val metadata = if (fileId != null) {
                    "{\"name\": \"$filename\"}"
                } else {
                    "{\"name\": \"$filename\"}"
                }

                val crlf = "\r\n"
                val body = "--$boundary$crlf" +
                        "Content-Type: application/json; charset=UTF-8$crlf$crlf" +
                        "$metadata$crlf" +
                        "--$boundary$crlf" +
                        "Content-Type: application/json$crlf$crlf" +
                        "$jsonPayload$crlf" +
                        "--$boundary--"

                conn.outputStream.use { os ->
                    os.write(body.toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val payloadBytes = jsonPayload.toByteArray(Charsets.UTF_8).size
                    val sizeKb = String.format("%.1f", payloadBytes / 1024.0)
                    settingsManager.googleDriveLastSyncTime = System.currentTimeMillis()
                    val itemCount = viewModel.questions.value.size
                    val acc = lastAcc.email ?: "Unknown"
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(true, "Cloud Backup Successful! Saved $itemCount items ($sizeKb KB) to Google Drive ($acc).")
                    }
                } else {
                    val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    if (responseCode == 401 || responseCode == 403) {
                        try {
                            com.google.android.gms.auth.GoogleAuthUtil.clearToken(context, token)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    
                    var readableMsg = "HTTP $responseCode"
                    if (errorStream.contains("insufficientFilePermissions")) readableMsg = "Permission Denied: Ensure you granted Google Drive access when signing in."
                    else if (errorStream.contains("rateLimitExceeded")) readableMsg = "Rate Limit Exceeded: Try again later."
                    else if (errorStream.contains("Project") || errorStream.contains("disabled")) readableMsg = "Google Drive API is not enabled on this Client ID. Developer action required."
                    else readableMsg = "Code $responseCode: $errorStream"
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(false, "Drive Sync Error: $readableMsg")
                    }
                }
            } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                e.printStackTrace()
                try {
                    val intent = e.intent
                    if (intent != null) {
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false, "Authorization required. Please accept the permission request on your screen.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false, "Google Drive backup error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun restoreFromDrive(
        context: Context,
        viewModel: OtsViewModel,
        settingsManager: SettingsManager,
        strategy: SyncStrategy = SyncStrategy.MERGE_DEDUPLICATE,
        onComplete: (Boolean, String) -> Unit
    ) {
        val lastAcc = GoogleSignInHelper.getLastSignedInAccount(context)
        if (lastAcc == null || lastAcc.account == null) {
            onComplete(false, "Please sign in with your Google Account first.")
            return
        }

        viewModel.viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                    context,
                    lastAcc.account!!,
                    "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.readonly"
                )
                val backupPath = settingsManager.googleDriveBackupPath.ifBlank { "My Drive/QuestionBank_Backup/backup.json" }
                val filename = backupPath.substringAfterLast("/").ifBlank { "question_bank_backup.json" }

                val query = java.net.URLEncoder.encode("name='$filename' and trashed=false", "UTF-8")
                val searchUrl = java.net.URL("https://www.googleapis.com/drive/v3/files?q=$query")
                val searchConn = searchUrl.openConnection() as java.net.HttpURLConnection
                searchConn.setRequestProperty("Authorization", "Bearer $token")
                searchConn.requestMethod = "GET"
                
                var fileId: String? = null
                if (searchConn.responseCode in 200..299) {
                    val searchResponse = searchConn.inputStream.bufferedReader().use { it.readText() }
                    val idMatch = "\"id\":\\s*\"([^\"]+)\"".toRegex().find(searchResponse)
                    fileId = idMatch?.groupValues?.get(1)
                }

                if (fileId == null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(false, "No cloud backup found on Google Drive. Perform a backup first.")
                    }
                    return@launch
                }

                val downloadUrl = java.net.URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                val dlConn = downloadUrl.openConnection() as java.net.HttpURLConnection
                dlConn.setRequestProperty("Authorization", "Bearer $token")
                dlConn.requestMethod = "GET"

                if (dlConn.responseCode in 200..299) {
                    val cloudJson = dlConn.inputStream.bufferedReader().use { it.readText() }
                    if (cloudJson.isNotBlank()) {
                        val result = viewModel.importQuestionsFromJson(cloudJson)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (result.first) {
                                settingsManager.googleDriveLastSyncTime = System.currentTimeMillis()
                                onComplete(true, "Cloud Sync Successful! Restored & deduplicated ${result.second} items from Google Drive.")
                            } else {
                                onComplete(false, "Failed to restore data from Google Drive.")
                            }
                        }
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onComplete(false, "Backup file is empty on Google Drive.")
                        }
                    }
                } else {
                    val errorStream = dlConn.errorStream?.bufferedReader()?.use { it.readText() }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(false, "Failed to download backup: $errorStream")
                    }
                }
            } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                e.printStackTrace()
                try {
                    val intent = e.intent
                    if (intent != null) {
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false, "Authorization required. Please accept the permission request on your screen.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false, "Restore error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun getSyncSummary(settingsManager: SettingsManager, localCount: Int): String {
        val lastTime = settingsManager.googleDriveLastSyncTime
        val timeStr = if (lastTime == 0L) "Never" else java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastTime))
        val email = settingsManager.googleAccountEmail.ifBlank { "Not Signed In" }

        return "Real Google Drive Sync  • Account: $email • Last Sync: $timeStr"
    }

    suspend fun listDriveFolders(context: Context, parentId: String = "root"): Result<List<Pair<String, String>>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val lastAcc = GoogleSignInHelper.getLastSignedInAccount(context) ?: return@withContext Result.failure(Exception("Not logged in to Google"))
            val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                context,
                lastAcc.account!!,
                "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.readonly"
            )
            val query = java.net.URLEncoder.encode("'$parentId' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false", "UTF-8")
            val url = java.net.URL("https://www.googleapis.com/drive/v3/files?q=$query&fields=files(id,name)")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.requestMethod = "GET"

            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val folderList = mutableListOf<Pair<String, String>>()
                val fileBlocks = resp.split("{").drop(1)
                for (block in fileBlocks) {
                    val idMatch = "\"id\":\\s*\"([^\"]+)\"".toRegex().find(block)
                    val nameMatch = "\"name\":\\s*\"([^\"]+)\"".toRegex().find(block)
                    if (idMatch != null && nameMatch != null) {
                        folderList.add(Pair(idMatch.groupValues[1], nameMatch.groupValues[1]))
                    }
                }
                Result.success(folderList)
            } else {
                Result.failure(Exception("Google Drive error code ${conn.responseCode}"))
            }
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            e.printStackTrace()
            try {
                val intent = e.intent
                if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            Result.failure(Exception("Authorization required. Please accept the permission request on your screen."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDriveFolder(context: Context, parentId: String, folderName: String): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val lastAcc = GoogleSignInHelper.getLastSignedInAccount(context) ?: return@withContext Result.failure(Exception("Not logged in to Google"))
            val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                context,
                lastAcc.account!!,
                "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.readonly"
            )
            val url = java.net.URL("https://www.googleapis.com/drive/v3/files")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val payload = "{\"name\":\"$folderName\", \"mimeType\":\"application/vnd.google-apps.folder\", \"parents\":[\"$parentId\"]}"
            conn.outputStream.use { it.write(payload.toByteArray()) }

            if (conn.responseCode in 200..299) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val idMatch = "\"id\":\\s*\"([^\"]+)\"".toRegex().find(resp)
                val id = idMatch?.groupValues?.get(1) ?: ""
                Result.success(id)
            } else {
                Result.failure(Exception("Google Drive error code ${conn.responseCode}"))
            }
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            e.printStackTrace()
            try {
                val intent = e.intent
                if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            Result.failure(Exception("Authorization required. Please accept the permission request on your screen."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
