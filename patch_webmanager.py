import re

with open('app/src/main/java/com/example/util/WebServerManager.kt', 'r') as f:
    wm = f.read()

# 1. Update startServer signature
old_sig = "fun startServer(onStarted: (String) -> Unit) {"
new_sig = 'fun startServer(adminUser: String = "admin", adminPass: String = "1234", onStarted: (String) -> Unit) {'
wm = wm.replace(old_sig, new_sig)

# 2. Add /admin routes and Basic Auth helper
# Find get("/")
get_slash = """                        get("/") {"""

admin_routes = """
                        val checkAuth: suspend (io.ktor.server.application.ApplicationCall) -> Boolean = { call ->
                            val auth = call.request.headers["Authorization"]
                            val expected = "Basic " + android.util.Base64.encodeToString("$adminUser:$adminPass".toByteArray(), android.util.Base64.NO_WRAP)
                            if (auth == expected) {
                                true
                            } else {
                                call.response.headers.append("WWW-Authenticate", "Basic realm=\\"Admin Portal\\"")
                                call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                                false
                            }
                        }

                        get("/admin") {
                            if (!checkAuth(call)) return@get
                            val html = appContext.assets.open("web_admin.html").bufferedReader().use { it.readText() }
                            call.respondText(html, ContentType.Text.Html)
                        }
                        
                        get("/api/admin/status") {
                            if (!checkAuth(call)) return@get
                            val candidates = LiveTestState.candidates.value
                            call.respond(candidates)
                        }
                        
                        post("/api/admin/warn") {
                            if (!checkAuth(call)) return@post
                            val request = call.receive<Map<String, String>>()
                            val roll = request["rollNumber"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                            val msg = request["message"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                            LiveTestState.setWarning(roll, msg)
                            call.respond(HttpStatusCode.OK, "Warning Set")
                        }
                        
                        post("/api/admin/dispatch") {
                            if (!checkAuth(call)) return@post
                            val request = call.receive<Map<String, String>>()
                            val roll = request["rollNumber"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                            LiveTestState.dispatchMarksheet(roll)
                            // Also send SMS using SmsManager if candidate has mobile
                            val candidate = LiveTestState.candidates.value.find { it.rollNumber == roll }
                            if (candidate != null && candidate.mobile.isNotEmpty()) {
                                try {
                                    val msg = "Exam Result: Dear ${candidate.name} (Roll: ${candidate.rollNumber}), score ${candidate.score}/${candidate.totalMarks}. Status: ${candidate.status}."
                                    android.telephony.SmsManager.getDefault().sendTextMessage(candidate.mobile, null, msg, null, null)
                                } catch (e: Exception) {}
                            }
                            call.respond(HttpStatusCode.OK, "Dispatched")
                        }
"""

wm = wm.replace(get_slash, admin_routes + '\n' + get_slash)

# Increase Netty worker group size to improve scalability
netty_block = """server = embeddedServer(Netty, port = port, host = "0.0.0.0") {"""
netty_scalable = """server = embeddedServer(Netty, port = port, host = "0.0.0.0", configure = {
                    responseWriteTimeoutSeconds = 10
                    requestReadTimeoutSeconds = 10
                    connectionGroupSize = 16
                    workerGroupSize = 32
                    callGroupSize = 64
                }) {"""
wm = wm.replace(netty_block, netty_scalable)

with open('app/src/main/java/com/example/util/WebServerManager.kt', 'w') as f:
    f.write(wm)

