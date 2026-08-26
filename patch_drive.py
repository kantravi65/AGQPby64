import re

with open('app/src/main/java/com/example/util/GoogleDriveSyncManager.kt', 'r') as f:
    content = f.read()

target = """                val jsonPayload = viewModel.exportQuestionsToJson()
                val backupPath = settingsManager.googleDriveBackupPath.ifBlank { "My Drive/QuestionBank_Backup/backup.json" }
                val filename = backupPath.substringAfterLast("/").ifBlank { "question_bank_backup.json" }

                // Search for existing file
                val searchUrl = java.net.URL("https://www.googleapis.com/drive/v3/files?q=name='$filename'")
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
                val method: String
                if (fileId != null) {
                    uploadUrl = java.net.URL("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=multipart")
                    method = "PATCH"
                } else {
                    uploadUrl = java.net.URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                    method = "POST"
                }

                val conn = uploadUrl.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = method
                conn.setRequestProperty("Authorization", "Bearer $token")
                val boundary = "foo_bar_baz"
                conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                conn.doOutput = true

                val metadata = if (fileId != null) {
                    "{\"name\": \"$filename\"}"
                } else {
                    "{\"name\": \"$filename\"}"
                }"""

replacement = """                val jsonPayload = viewModel.exportQuestionsToJson()
                val backupPath = settingsManager.googleDriveBackupPath.ifBlank { "QuestionBank_Backup/backup.json" }.replace("My Drive/", "")
                val filename = backupPath.substringAfterLast("/").ifBlank { "question_bank_backup.json" }
                var folderName = backupPath.substringBeforeLast("/")
                if (folderName == backupPath) folderName = "QuestionBank_Backup"
                folderName = folderName.substringAfterLast("/")
                
                var folderId: String? = null
                val folderSearchQ = "mimeType='application/vnd.google-apps.folder' and name='$folderName' and trashed=false"
                val fUrl = java.net.URL("https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(folderSearchQ, "UTF-8")}")
                val fConn = fUrl.openConnection() as java.net.HttpURLConnection
                fConn.setRequestProperty("Authorization", "Bearer $token")
                if (fConn.responseCode in 200..299) {
                    val fResp = fConn.inputStream.bufferedReader().use { it.readText() }
                    val idMatch = "\"id\":\\s*\"([^\"]+)\"".toRegex().find(fResp)
                    folderId = idMatch?.groupValues?.get(1)
                }

                if (folderId == null) {
                    val cConn = java.net.URL("https://www.googleapis.com/drive/v3/files").openConnection() as java.net.HttpURLConnection
                    cConn.requestMethod = "POST"
                    cConn.setRequestProperty("Authorization", "Bearer $token")
                    cConn.setRequestProperty("Content-Type", "application/json")
                    cConn.doOutput = true
                    cConn.outputStream.use {
                        it.write("{\\"name\\": \\"$folderName\\", \\"mimeType\\": \\"application/vnd.google-apps.folder\\"}".toByteArray())
                    }
                    if (cConn.responseCode in 200..299) {
                        val cResp = cConn.inputStream.bufferedReader().use { it.readText() }
                        val idMatch = "\"id\":\\s*\"([^\"]+)\"".toRegex().find(cResp)
                        folderId = idMatch?.groupValues?.get(1)
                    }
                }

                val searchQ = if (folderId != null) "name='$filename' and '$folderId' in parents and trashed=false" else "name='$filename' and trashed=false"
                val searchUrl = java.net.URL("https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(searchQ, "UTF-8")}")
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
                val method: String
                if (fileId != null) {
                    uploadUrl = java.net.URL("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=multipart")
                    method = "PATCH"
                } else {
                    uploadUrl = java.net.URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                    method = "POST"
                }

                val conn = uploadUrl.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = method
                conn.setRequestProperty("Authorization", "Bearer $token")
                val boundary = "foo_bar_baz"
                conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                conn.doOutput = true

                val metadata = if (fileId != null) {
                    "{\\"name\\": \\"$filename\\"}"
                } else if (folderId != null) {
                    "{\\"name\\": \\"$filename\\", \\"parents\\": [\\"$folderId\\"]}"
                } else {
                    "{\\"name\\": \\"$filename\\"}"
                }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/util/GoogleDriveSyncManager.kt', 'w') as f:
    f.write(content)
