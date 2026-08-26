with open('app/src/main/java/com/example/util/FtpManager.kt', 'r') as f:
    content = f.read()

content = content.replace("            ftpClient.soTimeout = 12000\n", "")

with open('app/src/main/java/com/example/util/FtpManager.kt', 'w') as f:
    f.write(content)
