import re

with open('app/src/main/java/com/example/util/FtpManager.kt', 'r') as f:
    content = f.read()

target1 = """    private fun createClient(useFtps: Boolean = false): FTPClient {
        val client = if (useFtps) FTPSClient("TLS", false) else FTPClient()
        client.controlEncoding = "UTF-8"
        client.connectTimeout = 12000
        client.defaultTimeout = 12000
        return client
    }"""

replacement1 = """    private fun createClient(useFtps: Boolean = false): FTPClient {
        val client = if (useFtps) {
            val ftps = FTPSClient("TLS", false)
            ftps.trustManager = org.apache.commons.net.util.TrustManagerUtils.getAcceptAllTrustManager()
            ftps
        } else FTPClient()
        client.controlEncoding = "UTF-8"
        client.connectTimeout = 15000
        client.defaultTimeout = 15000
        client.dataTimeout = 15000
        client.isRemoteVerificationEnabled = false
        return client
    }"""

content = content.replace(target1, replacement1)

target2 = """            if (!loggedIn) {
                val errorMsg = "FTP login failed for user '${if (user.isBlank()) "anonymous" else user}'. Check credentials."
                try { ftpClient!!.logout(); ftpClient?.disconnect() } catch (_: Exception) {}
                return@withContext Result.failure(IOException(errorMsg))
            }

            configureTransferMode(ftpClient!!, usePassive)"""

replacement2 = """            if (!loggedIn) {
                val errorMsg = "FTP login failed for user '${if (user.isBlank()) "anonymous" else user}'. Check credentials."
                try { ftpClient!!.logout(); ftpClient?.disconnect() } catch (_: Exception) {}
                return@withContext Result.failure(IOException(errorMsg))
            }

            if (useFtps || port == 990) {
                (ftpClient as FTPSClient).execPBSZ(0)
                (ftpClient as FTPSClient).execPROT("P")
            }

            ftpClient!!.enterLocalPassiveMode()
            if (!usePassive) {
                ftpClient!!.enterLocalActiveMode()
            }"""

content = content.replace(target2, replacement2)

target3 = """            if (!loggedIn) {
                try { ftpClient?.disconnect() } catch (_: Exception) {}
                return@withContext Result.failure(IOException("Invalid FTP credentials"))
            }

            configureTransferMode(ftpClient!!, usePassive)"""

replacement3 = """            if (!loggedIn) {
                try { ftpClient?.disconnect() } catch (_: Exception) {}
                return@withContext Result.failure(IOException("Invalid FTP credentials"))
            }

            if (useFtps || port == 990) {
                (ftpClient as FTPSClient).execPBSZ(0)
                (ftpClient as FTPSClient).execPROT("P")
            }

            ftpClient!!.enterLocalPassiveMode()
            if (!usePassive) {
                ftpClient!!.enterLocalActiveMode()
            }"""

content = content.replace(target3, replacement3)

with open('app/src/main/java/com/example/util/FtpManager.kt', 'w') as f:
    f.write(content)

