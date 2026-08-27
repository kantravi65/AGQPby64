import re

with open("app/src/main/java/com/example/util/GoogleDriveSyncManager.kt", "r") as f:
    content = f.read()

old_search = """                var fileId: String? = null
                if (searchConn.responseCode in 200..299) {
                    val searchResponse = searchConn.inputStream.bufferedReader().use { it.readText() }
                    val idMatch = "\"id\":\\s*\"([^\"]+)\"".toRegex().find(searchResponse)
                    fileId = idMatch?.groupValues?.get(1)
                }"""

new_search = """                var fileId: String? = null
                if (searchConn.responseCode in 200..299) {
                    val searchResponse = searchConn.inputStream.bufferedReader().use { it.readText() }
                    val idMatch = "\"id\":\\s*\"([^\"]+)\"".toRegex().find(searchResponse)
                    fileId = idMatch?.groupValues?.get(1)
                } else if (searchConn.responseCode == 401 || searchConn.responseCode == 403) {
                    val errorStream = searchConn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    try {
                        com.google.android.gms.auth.GoogleAuthUtil.clearToken(context, token)
                    } catch (e: Exception) {}
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(false, "Drive Sync Error: Session expired or permission denied. Please try again. Code: ${searchConn.responseCode}")
                    }
                    return@launch
                }"""

content = content.replace(old_search, new_search)

with open("app/src/main/java/com/example/util/GoogleDriveSyncManager.kt", "w") as f:
    f.write(content)

