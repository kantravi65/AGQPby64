import re

# 1. Update LiveTestState.kt
with open('app/src/main/java/com/example/util/LiveTestState.kt', 'r') as f:
    state_code = f.read()

state_code = state_code.replace("    val rollNumber: String,", "    val rollNumber: String,\n    val email: String = \"\",\n    val mobile: String = \"\",\n    var portraitBase64: String = \"\",\n    var latestFrameBase64: String = \"\",\n    var activeWarningMessage: String = \"\",")

state_code = state_code.replace("""object LiveTestState {""", """object LiveTestState {
    fun updateFrame(rollNumber: String, frameBase64: String): String {
        var msg = ""
        val list = _candidates.value.map {
            if (it.rollNumber == rollNumber) {
                msg = it.activeWarningMessage
                it.copy(latestFrameBase64 = frameBase64, activeWarningMessage = "")
            } else {
                it
            }
        }
        _candidates.value = list
        return msg
    }
    
    fun setWarning(rollNumber: String, message: String) {
        val list = _candidates.value.map {
            if (it.rollNumber == rollNumber) it.copy(activeWarningMessage = message) else it
        }
        _candidates.value = list
    }
""")

with open('app/src/main/java/com/example/util/LiveTestState.kt', 'w') as f:
    f.write(state_code)


# 2. Update WebServerManager.kt
with open('app/src/main/java/com/example/util/WebServerManager.kt', 'r') as f:
    ktor_code = f.read()

# Login route
login_old = """                        post("/api/livetest/login") {
                            val request = call.receive<Map<String, String>>()
                            val name = request["name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                            val roll = request["rollNumber"] ?: return@post call.respond(HttpStatusCode.BadRequest)"""

login_new = """                        post("/api/livetest/login") {
                            val request = call.receive<Map<String, String>>()
                            val name = request["name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                            val roll = request["rollNumber"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                            val email = request["email"] ?: ""
                            val mobile = request["mobile"] ?: ""
                            val portrait = request["portraitBase64"] ?: \"\""""
ktor_code = ktor_code.replace(login_old, login_new)

# Session creation
sess_old = """                            val session = CandidateSession(
                                id = java.util.UUID.randomUUID().toString(),
                                name = name,
                                rollNumber = roll,
                                loginTime = System.currentTimeMillis(),
                                questionsJson = kotlinx.serialization.json.Json.encodeToString(selectedQuestions)
                            )"""
sess_new = """                            val session = CandidateSession(
                                id = java.util.UUID.randomUUID().toString(),
                                name = name,
                                rollNumber = roll,
                                email = email,
                                mobile = mobile,
                                portraitBase64 = portrait,
                                loginTime = System.currentTimeMillis(),
                                questionsJson = kotlinx.serialization.json.Json.encodeToString(selectedQuestions)
                            )"""
ktor_code = ktor_code.replace(sess_old, sess_new)

# Add heartbeat route
hb_route = """                        post("/api/livetest/heartbeat") {
                            val request = call.receive<Map<String, String>>()
                            val roll = request["rollNumber"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                            val frameBase64 = request["frameBase64"] ?: ""
                            
                            val warningMsg = LiveTestState.updateFrame(roll, frameBase64)
                            call.respond(mapOf("warningMessage" to warningMsg))
                        }"""
                        
ktor_code = ktor_code.replace('post("/api/livetest/submit") {', hb_route + '\n\n                        post("/api/livetest/submit") {')

with open('app/src/main/java/com/example/util/WebServerManager.kt', 'w') as f:
    f.write(ktor_code)

