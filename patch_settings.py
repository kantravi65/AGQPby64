import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("import com.example.util.FtpManager", "import com.example.util.NetworkStorageManager")

target1 = """                                    val res = FtpManager.testConnection(
                                        host = ftpHost.trim(),
                                        port = ftpPort.toIntOrNull() ?: 21,
                                        user = ftpUser.trim(),
                                        pass = ftpPass,
                                        usePassive = ftpUsePassive,
                                        useFtps = ftpUseFtps
                                    )"""
replacement1 = """                                    val res = NetworkStorageManager.testConnection(
                                        host = ftpHost.trim(),
                                        port = ftpPort.toIntOrNull() ?: 21,
                                        user = ftpUser.trim(),
                                        pass = ftpPass,
                                        remoteDir = ftpRemoteDir.trim(),
                                        usePassive = ftpUsePassive,
                                        useFtps = ftpUseFtps
                                    )"""
content = content.replace(target1, replacement1)

target2 = """                                    val res = FtpManager.uploadJson(
                                        host = ftpHost.trim(),
                                        port = ftpPort.toIntOrNull() ?: 21,
                                        user = ftpUser.trim(),
                                        pass = ftpPass,
                                        remoteDir = ftpRemoteDir.trim(),
                                        fileName = "ots_question_bank_backup.json",
                                        jsonContent = jsonPayload,
                                        usePassive = ftpUsePassive,
                                        useFtps = ftpUseFtps
                                    )"""
replacement2 = """                                    val res = NetworkStorageManager.uploadJson(
                                        host = ftpHost.trim(),
                                        port = ftpPort.toIntOrNull() ?: 21,
                                        user = ftpUser.trim(),
                                        pass = ftpPass,
                                        remoteDir = ftpRemoteDir.trim(),
                                        fileName = "ots_question_bank_backup.json",
                                        jsonContent = jsonPayload,
                                        usePassive = ftpUsePassive,
                                        useFtps = ftpUseFtps
                                    )"""
content = content.replace(target2, replacement2)

target3 = """                                    val res = FtpManager.downloadLatestJson(
                                        host = ftpHost.trim(),
                                        port = ftpPort.toIntOrNull() ?: 21,
                                        user = ftpUser.trim(),
                                        pass = ftpPass,
                                        remoteDir = ftpRemoteDir.trim(),
                                        fileName = "ots_question_bank_backup.json",
                                        usePassive = ftpUsePassive,
                                        useFtps = ftpUseFtps
                                    )"""
replacement3 = """                                    val res = NetworkStorageManager.downloadLatestJson(
                                        host = ftpHost.trim(),
                                        port = ftpPort.toIntOrNull() ?: 21,
                                        user = ftpUser.trim(),
                                        pass = ftpPass,
                                        remoteDir = ftpRemoteDir.trim(),
                                        fileName = "ots_question_bank_backup.json",
                                        usePassive = ftpUsePassive,
                                        useFtps = ftpUseFtps
                                    )"""
content = content.replace(target3, replacement3)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
