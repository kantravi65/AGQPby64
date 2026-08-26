import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('text = "Link a remote FTP server folder for centralized backup, question sharing, and remote synchronization:"', 'text = "Link a remote FTP/FTPS or SMB server for centralized backup (Auto-detects smb:// or ftp:// prefixes):"')
content = content.replace('label = { Text("FTP Host / IP") }', 'label = { Text("Host/IP (prefix smb:// or ftp://)") }')
content = content.replace('Toast.makeText(context, "Please enter FTP Server Host IP or domain"', 'Toast.makeText(context, "Please enter Server Host IP or domain"')
content = content.replace('ftpStatusMessage = "Testing FTP connection..."', 'ftpStatusMessage = "Testing connection..."')
content = content.replace('ftpStatusMessage = "FTP Error: ${err.message}"', 'ftpStatusMessage = "Connection Error: ${err.message}"')
content = content.replace('Toast.makeText(context, err.message ?: "FTP Error"', 'Toast.makeText(context, err.message ?: "Connection Error"')
content = content.replace('Toast.makeText(context, "Please enter FTP Server Host"', 'Toast.makeText(context, "Please enter Server Host"')
content = content.replace('ftpStatusMessage = "Uploading database to FTP..."', 'ftpStatusMessage = "Uploading database..."')
content = content.replace('ftpStatusMessage = "Synced to FTP folder: $ftpRemoteDir"', 'ftpStatusMessage = "Synced to remote folder: $ftpRemoteDir"')
content = content.replace('ftpStatusMessage = "FTP Upload Failed: ${err.message}"', 'ftpStatusMessage = "Upload Failed: ${err.message}"')
content = content.replace('Toast.makeText(context, err.message ?: "FTP Sync Failed"', 'Toast.makeText(context, err.message ?: "Sync Failed"')
content = content.replace('Text("Sync FTP"', 'Text("Sync To Remote"')
content = content.replace('ftpStatusMessage = "Downloading backup from FTP..."', 'ftpStatusMessage = "Downloading backup..."')
content = content.replace('ftpStatusMessage = "Restored ${importRes.second} items from FTP"', 'ftpStatusMessage = "Restored ${importRes.second} items"')
content = content.replace('Toast.makeText(context, "Restored ${importRes.second} items from FTP server!"', 'Toast.makeText(context, "Restored ${importRes.second} items from remote server!"')
content = content.replace('Toast.makeText(context, "Invalid JSON data on FTP server"', 'Toast.makeText(context, "Invalid JSON data on remote server"')
content = content.replace('ftpStatusMessage = "FTP Download Failed: ${err.message}"', 'ftpStatusMessage = "Download Failed: ${err.message}"')
content = content.replace('Toast.makeText(context, err.message ?: "FTP Download Failed"', 'Toast.makeText(context, err.message ?: "Download Failed"')

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
