import re

with open("app/src/main/java/com/example/util/GoogleDriveSyncManager.kt", "r") as f:
    content = f.read()

old_patch = """                val uploadUrl: java.net.URL
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
                conn.setRequestProperty("Authorization", "Bearer $token")"""

new_patch = """                val uploadUrl: java.net.URL
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
                conn.setRequestProperty("Authorization", "Bearer $token")"""

# Let's use regex to be safe about whitespace
pattern = r"val uploadUrl: java\.net\.URL\s+val method: String\s+if \(fileId != null\) \{\s+uploadUrl = java\.net\.URL\(\"https://www\.googleapis\.com/upload/drive/v3/files/\$fileId\?uploadType=multipart\"\)\s+method = \"PATCH\"\s+\} else \{\s+uploadUrl = java\.net\.URL\(\"https://www\.googleapis\.com/upload/drive/v3/files\?uploadType=multipart\"\)\s+method = \"POST\"\s+\}\s+val conn = uploadUrl\.openConnection\(\) as java\.net\.HttpURLConnection\s+conn\.requestMethod = method\s+conn\.setRequestProperty\(\"Authorization\", \"Bearer \$token\"\)"

match = re.search(pattern, content)
if match:
    content = content[:match.start()] + new_patch + content[match.end():]
    with open("app/src/main/java/com/example/util/GoogleDriveSyncManager.kt", "w") as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Match not found")

