import re

with open("app/src/main/java/com/example/util/GoogleDriveSyncManager.kt", "r") as f:
    content = f.read()

content = content.replace('"oauth2:https://www.googleapis.com/auth/drive.file"', 
                          '"oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.readonly"')

with open("app/src/main/java/com/example/util/GoogleDriveSyncManager.kt", "w") as f:
    f.write(content)

