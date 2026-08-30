import re

with open('app/src/main/java/com/example/util/WebServerManager.kt', 'r') as f:
    text = f.read()

old_login_req = "data class LoginRequest(val name: String, val rollNumber: String)"
new_login_req = "data class LoginRequest(val name: String, val rollNumber: String, val email: String = \"\", val mobile: String = \"\", val portraitBase64: String = \"\")"

text = text.replace(old_login_req, new_login_req)

old_session = """                        val session = CandidateSession(
                            id = java.util.UUID.randomUUID().toString(),
                            name = req.name,
                            rollNumber = req.rollNumber,
                            loginTime = System.currentTimeMillis(),
                            status = "Testing","""
new_session = """                        val session = CandidateSession(
                            id = java.util.UUID.randomUUID().toString(),
                            name = req.name,
                            rollNumber = req.rollNumber,
                            email = req.email,
                            mobile = req.mobile,
                            portraitBase64 = req.portraitBase64,
                            loginTime = System.currentTimeMillis(),
                            status = "Testing","""

text = text.replace(old_session, new_session)

with open('app/src/main/java/com/example/util/WebServerManager.kt', 'w') as f:
    f.write(text)
