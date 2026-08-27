import re

with open("app/src/main/java/com/example/util/GoogleDriveSyncManager.kt", "r") as f:
    content = f.read()

old_query = """val searchUrl = java.net.URL("https://www.googleapis.com/drive/v3/files?q=name='$filename'")"""
new_query = """val query = java.net.URLEncoder.encode("name='$filename' and trashed=false", "UTF-8")
                val searchUrl = java.net.URL("https://www.googleapis.com/drive/v3/files?q=$query")"""

content = content.replace(old_query, new_query)

with open("app/src/main/java/com/example/util/GoogleDriveSyncManager.kt", "w") as f:
    f.write(content)

