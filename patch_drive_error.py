import re

with open("app/src/main/java/com/example/util/GoogleDriveSyncManager.kt", "r") as f:
    content = f.read()

old_err = """                if (responseCode in 200..299) {
                    val payloadBytes = jsonPayload.toByteArray(Charsets.UTF_8).size
                    val sizeKb = String.format("%.1f", payloadBytes / 1024.0)
                    settingsManager.googleDriveLastSyncTime = System.currentTimeMillis()
                    val itemCount = viewModel.questions.value.size
                    val acc = lastAcc.email ?: "Unknown"
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(true, "Cloud Backup Successful! Saved $itemCount items ($sizeKb KB) to real Google Drive ($acc).")
                    }
                } else {
                    val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(false, "Google Drive backup failed with code $responseCode: $errorStream")
                    }
                }"""

new_err = """                if (responseCode in 200..299) {
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
                }"""

content = content.replace(old_err, new_err)

with open("app/src/main/java/com/example/util/GoogleDriveSyncManager.kt", "w") as f:
    f.write(content)

