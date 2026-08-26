with open('app/src/main/java/com/example/util/FtpManager.kt', 'r') as f:
    content = f.read()

target = "ftpClient!!.connect(host, targetPort)"
replacement = "ftpClient!!.connect(host, targetPort)\n            ftpClient.soTimeout = 12000"

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/util/FtpManager.kt', 'w') as f:
    f.write(content)
