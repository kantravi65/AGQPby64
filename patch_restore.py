import re

with open('app/src/main/java/com/example/util/GoogleDriveSyncManager.kt', 'r') as f:
    content = f.read()

target = """                val backupPath = settingsManager.googleDriveBackupPath.ifBlank { "My Drive/QuestionBank_Backup/backup.json" }
                val filename = backupPath.substringAfterLast("/").ifBlank { "question_bank_backup.json" }
                val searchUrl = java.net.URL("https://www.googleapis.com/drive/v3/files?q=name='$filename'")"""

replacement = """                val backupPath = settingsManager.googleDriveBackupPath.ifBlank { "QuestionBank_Backup/backup.json" }.replace("My Drive/", "")
                val filename = backupPath.substringAfterLast("/").ifBlank { "question_bank_backup.json" }
                val searchQ = "name='$filename' and trashed=false"
                val searchUrl = java.net.URL("https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(searchQ, "UTF-8")}")"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/util/GoogleDriveSyncManager.kt', 'w') as f:
    f.write(content)
