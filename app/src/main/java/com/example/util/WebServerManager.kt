package com.example.util

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import io.ktor.websocket.WebSocketSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.consumeEach


import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.data.model.BookEntity
import com.example.data.model.PaperEntity
import com.example.data.model.QuestionEntity
import com.example.data.repository.OtsRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import java.security.KeyStore
import java.io.File
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.response.respondFile
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.net.Inet4Address
import com.example.util.PdfPrintUtils
import com.example.util.FontSize
import com.example.util.MarginSize
import com.example.util.WatermarkPattern
import com.example.util.PdfPrintSettings
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.net.NetworkInterface

@Serializable
data class BookDto(val id: String, val title: String, val chapterCount: Int)

@Serializable
data class QuestionDto(
    val id: String, val bookId: String, val bookTitle: String, val chapter: String,
    val type: String, val difficulty: String, val question: String, val optionsJson: String,
    val answer: String, val explanation: String, val marks: Int, val isBookmarked: Boolean, val createdAt: Long
)

@Serializable
data class PaperDto(
    val id: String, val title: String, val subject: String, val durationMinutes: Int,
    val totalMarks: Int, val questionIdsJson: String, val createdAt: Long
)


object WsSignaling {
    val adminSessions = ConcurrentHashMap.newKeySet<WebSocketSession>()
    val candidateSessions = ConcurrentHashMap<String, WebSocketSession>()
    
    suspend fun sendToAdmins(message: String) {
        adminSessions.forEach {
            try { it.send(message) } catch (e: Exception) {}
        }
    }
    
    suspend fun sendToCandidate(rollNumber: String, message: String) {
        try { candidateSessions[rollNumber]?.send(message) } catch(e:Exception){}
    }
}

class WebServerManager(private val appContext: Context, private val repository: OtsRepository, private val mode: String = "admin") {

    private var server: NettyApplicationEngine? = null
    private var port = 8080

    private fun isPortAvailable(port: Int): Boolean {
        return try {
            java.net.ServerSocket(port).use { true }
        } catch (e: Exception) {
            false
        }
    }

    fun startServer(
        adminUser: String = "admin",
        adminPass: String = "1234",
        onStarted: (httpsUrl: String, httpUrl: String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (server != null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferredHttpPort = if (isPortAvailable(8080)) 8080 else 0
                val preferredHttpsPort = if (isPortAvailable(8443)) 8443 else 0

                val env = applicationEngineEnvironment {
                    val ksFile = File(appContext.filesDir, "test.p12")
                    if (!ksFile.exists()) {
                        appContext.assets.open("test.p12").use { input ->
                            ksFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                    val ks = KeyStore.getInstance("PKCS12")
                    ksFile.inputStream().use { ks.load(it, "android".toCharArray()) }

                    connector {
                        port = preferredHttpPort
                        host = "0.0.0.0"
                    }
                    sslConnector(
                        keyStore = ks,
                        keyAlias = "androiddebugkey",
                        keyStorePassword = { "android".toCharArray() },
                        privateKeyPassword = { "android".toCharArray() }
                    ) {
                        port = preferredHttpsPort
                        host = "0.0.0.0"
                        keyStorePath = ksFile
                    }
                    module {

                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(15)
                        timeout = Duration.ofSeconds(15)
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }
                    install(ContentNegotiation) {
                        json(kotlinx.serialization.json.Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })
                    }
                    install(CORS) {
                        anyHost()
                    }
                    routing {

                        val checkAuth: suspend (io.ktor.server.application.ApplicationCall) -> Boolean = { call ->
                            val auth = call.request.headers["Authorization"]
                            val expected = "Basic " + android.util.Base64.encodeToString("$adminUser:$adminPass".toByteArray(), android.util.Base64.NO_WRAP)
                            if (auth == expected) {
                                true
                            } else {
                                call.response.headers.append("WWW-Authenticate", "Basic realm=\"Admin Portal\"")
                                call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                                false
                            }
                        }

                        get("/admin") {
                            if (mode != "livetest" && mode != "all") {
                                call.respond(HttpStatusCode.Forbidden, "AV monitoring is only available on Live Test Server.")
                                return@get
                            }
                            if (!checkAuth(call)) return@get
                            val html = appContext.assets.open("web_admin.html").bufferedReader().use { it.readText() }
                            call.respondText(html, ContentType.Text.Html)
                        }
                        
                        get("/api/admin/status") {
                            if (mode != "livetest" && mode != "all") {
                                call.respond(HttpStatusCode.Forbidden, "AV monitoring is only available on Live Test Server.")
                                return@get
                            }
                            if (!checkAuth(call)) return@get
                            val candidates = LiveTestState.candidates.value
                            call.respond(candidates)
                        }
                        
                        post("/api/admin/warn") {
                            if (mode != "livetest" && mode != "all") {
                                call.respond(HttpStatusCode.Forbidden, "AV monitoring is only available on Live Test Server.")
                                return@post
                            }
                            if (!checkAuth(call)) return@post
                            val request = call.receive<Map<String, String>>()
                            val roll = request["rollNumber"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                            val msg = request["message"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                            LiveTestState.setWarning(roll, msg)
                            
                            val candidate = LiveTestState.candidates.value.find { it.rollNumber == roll }
                            if (candidate != null) {
                                if (candidate.warningCount >= 3) {
                                    WsSignaling.sendToCandidate(roll, "CMD:FORCE_SUBMIT")
                                } else {
                                    WsSignaling.sendToCandidate(roll, "CMD:WARNING:$msg")
                                }
                            }
                            call.respond(HttpStatusCode.OK, "Warning Set")
                        }
                        
                        post("/api/admin/dispatch") {
                            if (mode != "livetest" && mode != "all") {
                                call.respond(HttpStatusCode.Forbidden, "AV monitoring is only available on Live Test Server.")
                                return@post
                            }
                            if (!checkAuth(call)) return@post
                            val candidates = LiveTestState.candidates.value
                            val testing = candidates.filter { it.status == "Testing" }
                            if (testing.isNotEmpty()) {
                                call.respond(HttpStatusCode.BadRequest, "Cannot dispatch results while test is under progress (${testing.size} candidate(s) still testing).")
                                return@post
                            }
                            val request = call.receive<Map<String, String>>()
                            val roll = request["rollNumber"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                            LiveTestState.dispatchMarksheet(roll)
                            // Also send SMS using SmsManager if candidate has mobile
                            val candidate = LiveTestState.candidates.value.find { it.rollNumber == roll }
                            if (candidate != null && candidate.mobile.isNotEmpty()) {
                                try {
                                    val msg = "Exam Result: Dear ${candidate.name} (Roll: ${candidate.rollNumber}), score ${candidate.score}/${candidate.totalMarks}. Status: ${candidate.status}."
                                    val smsManager = android.telephony.SmsManager.getDefault()
                                    val parts = smsManager.divideMessage(msg)
                                    smsManager.sendMultipartTextMessage(candidate.mobile, null, parts, null, null)
                                } catch (e: Exception) {
                                    android.util.Log.e("WebServerManager", "Failed to send SMS", e)
                                }
                            }
                            call.respond(HttpStatusCode.OK, "Dispatched")
                        }

                        post("/api/admin/dispatch-all") {
                            if (mode != "livetest" && mode != "all") {
                                call.respond(HttpStatusCode.Forbidden, "AV monitoring is only available on Live Test Server.")
                                return@post
                            }
                            if (!checkAuth(call)) return@post
                            val candidates = LiveTestState.candidates.value
                            val testing = candidates.filter { it.status == "Testing" }
                            if (testing.isNotEmpty()) {
                                call.respond(HttpStatusCode.BadRequest, "Cannot dispatch results: ${testing.size} candidate(s) still testing.")
                                return@post
                            }
                            val toDispatch = candidates.filter { !it.isDispatched }
                            toDispatch.forEach { candidate ->
                                LiveTestState.dispatchMarksheet(candidate.rollNumber)
                                if (candidate.mobile.isNotEmpty()) {
                                    try {
                                        val msg = "Exam Result: Dear ${candidate.name} (Roll: ${candidate.rollNumber}), score ${candidate.score}/${candidate.totalMarks}. Status: ${candidate.status}."
                                        val smsManager = android.telephony.SmsManager.getDefault()
                                        val parts = smsManager.divideMessage(msg)
                                        smsManager.sendMultipartTextMessage(candidate.mobile, null, parts, null, null)
                                    } catch (e: Exception) {
                                        android.util.Log.e("WebServerManager", "Failed to send SMS to ${candidate.rollNumber}", e)
                                    }
                                }
                            }
                            WsSignaling.sendToAdmins("CANDIDATE_UPDATE")
                            call.respond(HttpStatusCode.OK, mapOf("dispatchedCount" to toDispatch.size))
                        }

                        get("/") {
                            if (mode == "admin" || mode == "expert") {
                                if (!checkAuth(call)) return@get
                            }
                            val filename = when(mode) {
                                "expert" -> "web_expert.html"
                                "admin" -> "web_dashboard.html"
                                else -> "web_livetest.html"
                            }
                            val html = appContext.assets.open(filename).bufferedReader().use { it.readText() }
                            call.respondText(html, ContentType.Text.Html)
                        }

                        get("/livetest") {
                            val html = appContext.assets.open("web_livetest.html").bufferedReader().use { it.readText() }
                            call.respondText(html, ContentType.Text.Html)
                        }

                        get("/dashboard") {
                            if (!checkAuth(call)) return@get
                            val html = appContext.assets.open("web_dashboard.html").bufferedReader().use { it.readText() }
                            call.respondText(html, ContentType.Text.Html)
                        }

                        get("/expert") {
                            if (!checkAuth(call)) return@get
                            val html = appContext.assets.open("web_expert.html").bufferedReader().use { it.readText() }
                            call.respondText(html, ContentType.Text.Html)
                        }

                        get("/results") {
                            val html = appContext.assets.open("web_results.html").bufferedReader().use { it.readText() }
                            call.respondText(html, ContentType.Text.Html)
                        }

                        // API: Books
                        get("/api/books") {
                            if (mode == "admin" || mode == "expert" || mode == "all") {
                                if (!checkAuth(call)) return@get
                            }
                            val books = repository.allBooks.first().map { BookDto(it.id, it.title, it.chapterCount) }
                            call.respond(books)
                        }
                        post("/api/books") {
                            if (mode == "admin" || mode == "expert" || mode == "all") {
                                if (!checkAuth(call)) return@post
                            }
                            val dto = call.receive<BookDto>()
                            repository.insertBook(BookEntity(dto.id, dto.title, dto.chapterCount))
                            call.respond(HttpStatusCode.OK)
                        }
                        delete("/api/books/{id}") {
                            if (mode == "admin" || mode == "expert" || mode == "all") {
                                if (!checkAuth(call)) return@delete
                            }
                            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                            val book = repository.allBooks.first().find { it.id == id }
                            if (book != null) repository.deleteBook(book)
                            call.respond(HttpStatusCode.OK)
                        }

                        // API: Questions
                        get("/api/questions") {
                            if (mode == "admin" || mode == "expert" || mode == "all") {
                                if (!checkAuth(call)) return@get
                            }
                            val activePaperId = LiveTestState.selectedExpertPaperId
                            val rawQuestions = if ((mode == "expert" || mode == "all") && activePaperId != null) {
                                val paper = repository.allPapers.first().find { it.id == activePaperId }
                                if (paper != null) {
                                    try {
                                        val arr = org.json.JSONArray(paper.questionIdsJson)
                                        val ids = mutableListOf<String>()
                                        for (i in 0 until arr.length()) {
                                            ids.add(arr.getString(i))
                                        }
                                        repository.allQuestions.first().filter { it.id in ids }
                                    } catch (e: Exception) {
                                        repository.allQuestions.first()
                                    }
                                } else {
                                    repository.allQuestions.first()
                                }
                            } else {
                                repository.allQuestions.first()
                            }
                            
                            val questions = rawQuestions.map {
                                QuestionDto(it.id, it.bookId, it.bookTitle, it.chapter, it.type, it.difficulty, it.question, it.optionsJson, it.answer, it.explanation, it.marks, it.isBookmarked, it.createdAt)
                            }
                            call.respond(questions)
                        }
                        
                        get("/api/expert/paper") {
                            if (mode == "admin" || mode == "expert" || mode == "all") {
                                if (!checkAuth(call)) return@get
                            }
                            val activePaperId = LiveTestState.selectedExpertPaperId
                            if (activePaperId != null) {
                                val paper = repository.allPapers.first().find { it.id == activePaperId }
                                if (paper != null) {
                                    call.respond(PaperDto(paper.id, paper.title, paper.subject, paper.durationMinutes, paper.totalMarks, paper.questionIdsJson, paper.createdAt))
                                } else {
                                    call.respond(HttpStatusCode.NotFound, "No paper found")
                                }
                            } else {
                                call.respond(HttpStatusCode.NotFound, "No paper selected")
                            }
                        }
                        post("/api/questions") {
                            if (mode == "admin" || mode == "expert" || mode == "all") {
                                if (!checkAuth(call)) return@post
                            }
                            val dto = call.receive<QuestionDto>()
                            val entity = QuestionEntity(dto.id, dto.bookId, dto.bookTitle, dto.chapter, dto.type, dto.difficulty, dto.question, dto.optionsJson, dto.answer, dto.explanation, dto.marks, dto.isBookmarked, dto.createdAt)
                            repository.insertQuestion(entity)
                            call.respond(HttpStatusCode.OK)
                        }
                        post("/api/questions/bulk") {
                            if (mode == "admin" || mode == "expert" || mode == "all") {
                                if (!checkAuth(call)) return@post
                            }
                            val dtos = call.receive<List<QuestionDto>>()
                            val entities = dtos.map { dto ->
                                QuestionEntity(dto.id, dto.bookId, dto.bookTitle, dto.chapter, dto.type, dto.difficulty, dto.question, dto.optionsJson, dto.answer, dto.explanation, dto.marks, dto.isBookmarked, dto.createdAt)
                            }
                            repository.insertAllQuestions(entities)
                            call.respond(HttpStatusCode.OK)
                        }
                        delete("/api/questions/{id}") {
                            if (mode == "admin" || mode == "expert" || mode == "all") {
                                if (!checkAuth(call)) return@delete
                            }
                            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                            repository.deleteQuestion(id)
                            call.respond(HttpStatusCode.OK)
                        }

                        // API: Papers
                        get("/api/papers") {
                            if (mode == "admin" || mode == "expert" || mode == "all") {
                                if (!checkAuth(call)) return@get
                            }
                            val papers = repository.allPapers.first().map {
                                PaperDto(it.id, it.title, it.subject, it.durationMinutes, it.totalMarks, it.questionIdsJson, it.createdAt)
                            }
                            call.respond(papers)
                        }
                        post("/api/papers") {
                            if (mode == "admin" || mode == "expert" || mode == "all") {
                                if (!checkAuth(call)) return@post
                            }
                            val dto = call.receive<PaperDto>()
                            val entity = PaperEntity(dto.id, dto.title, dto.subject, dto.durationMinutes, dto.totalMarks, dto.questionIdsJson, dto.createdAt)
                            repository.insertPaper(entity)
                            call.respond(HttpStatusCode.OK)
                        }
                        
                get("/api/papers/{id}/pdf") {
                    if (mode == "admin" || mode == "expert" || mode == "all") {
                        if (!checkAuth(call)) return@get
                    }
                    val id = call.parameters["id"]
                    val paper = id?.let { repository.allPapers.first().find { p -> p.id == it } }
                    if (paper != null) {
                        try {
                            val qIds = kotlinx.serialization.json.Json.decodeFromString<List<String>>(paper.questionIdsJson)
                            val questions = repository.allQuestions.first().filter { qIds.contains(it.id) }
                            
                            val wmPattern = call.request.queryParameters["wmPattern"]
                            val wmStyle = call.request.queryParameters["wmStyle"]
                            val watermarkText = call.request.queryParameters["watermarkText"] ?: ""
                            val watermarkEnabled = watermarkText.isNotEmpty()
                            val watermarkIsCursive = wmStyle == "cursive"
                            val wmp = when(wmPattern) {
                                "grid" -> WatermarkPattern.MULTIPLE_GRID
                                "center" -> WatermarkPattern.SINGLE_CENTER
                                "header" -> WatermarkPattern.HEADER_STAMP
                                else -> WatermarkPattern.SINGLE_CENTER
                            }
                            
                            val fontSizeStr = call.request.queryParameters["fontSize"]
                            val fSize = when(fontSizeStr) {
                                "12px" -> FontSize.COMPACT
                                "16px" -> FontSize.MEDIUM
                                "20px" -> FontSize.LARGE
                                else -> FontSize.MEDIUM
                            }
                            
                            val marginStr = call.request.queryParameters["margin"]
                            val mSize = when(marginStr) {
                                "0.5in" -> MarginSize.NARROW
                                "1in" -> MarginSize.NORMAL
                                "1.5in" -> MarginSize.WIDE
                                else -> MarginSize.NORMAL
                            }
                            
                            val showAns = call.request.queryParameters["showAns"] == "yes"
                            val showExp = call.request.queryParameters["showExp"] == "yes"
                            val showCandidate = call.request.queryParameters["showCandidate"] == "yes"

                            val settings = PdfPrintSettings(
                                mainTitle = paper.title,
                                subTitle = "Subject: ${paper.subject} | Duration: ${paper.durationMinutes} mins | Total Marks: ${paper.totalMarks}",
                                paperCode = "QP-${paper.id.takeLast(6)}",
                                watermarkEnabled = watermarkEnabled,
                                watermarkText = watermarkText,
                                watermarkIsCursive = watermarkIsCursive,
                                watermarkPattern = wmp,
                                marginPt = mSize.marginPt,
                                fontTitleSp = fSize.titleSp,
                                fontBodySp = fSize.bodySp,
                                showAnswerKey = showAns,
                                showExplanations = showExp,
                                showCandidateBox = showCandidate
                            )

                            val pdfFile = File(appContext.cacheDir, "paper_${paper.id}.pdf")
                            PdfPrintUtils.generatePdfFile(appContext, paper, questions, settings)?.let {
                                call.respondFile(it)
                            } ?: call.respond(HttpStatusCode.InternalServerError, "Failed to generate PDF")
                        } catch (e: Exception) {
                            Log.e("WebServerManager", "Error generating PDF: ${e.message}", e)
                            call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
                        }
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }

                // API: Live Test Portal
                get("/api/livetest/config") {
                    if (mode != "livetest" && mode != "all") {
                        call.respond(HttpStatusCode.Forbidden, "Live test portal is disabled on this server.")
                        return@get
                    }
                    call.respond(LiveTestState.config)
                }

                @Serializable
                data class LoginRequest(val name: String, val rollNumber: String, val email: String = "", val mobile: String = "", val portraitBase64: String = "")

                @Serializable
                data class LoginResponse(val success: Boolean, val questions: List<QuestionDto>, val durationMinutes: Int, val examName: String = "", val subjectName: String = "", val startTimeMillis: Long = 0L)

                post("/api/livetest/login") {
                    if (mode != "livetest" && mode != "all") {
                        call.respond(HttpStatusCode.Forbidden, "Live test portal is disabled on this server.")
                        return@post
                    }
                    try {
                        val req = call.receive<LoginRequest>()
                        
                        // Check if candidate has already completed/submitted/disqualified from the test
                        val existing = LiveTestState.candidates.value.find { it.rollNumber == req.rollNumber }
                        if (existing != null && (existing.status == "Submitted" || existing.status == "Disqualified")) {
                            return@post call.respond(HttpStatusCode.Forbidden, "You have already completed this exam and cannot enter again.")
                        }
                        
                        val allQuestions = repository.allQuestions.first()
                        val selectedQuestions = mutableListOf<QuestionEntity>()
                        
                        if (LiveTestState.config.paperId != null) {
                            val paper = repository.allPapers.first().find { it.id == LiveTestState.config.paperId }
                            if (paper != null) {
                                val qIds = try {
                                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(paper.questionIdsJson)
                                } catch (e: Exception) {
                                    emptyList<String>()
                                }
                                val paperQuestions = allQuestions.filter { qIds.contains(it.id) }
                                selectedQuestions.addAll(paperQuestions)
                            }
                        }
                        
                        if (selectedQuestions.isEmpty()) {
                            val subjectFiltered = if (LiveTestState.config.subject.isNotEmpty()) {
                                allQuestions.filter { it.bookTitle.equals(LiveTestState.config.subject, ignoreCase = true) }
                            } else {
                                allQuestions
                            }
                            
                            val mcqPool = subjectFiltered.filter { it.type == "mcq" }.shuffled()
                            val fibPool = subjectFiltered.filter { it.type == "fib" }.shuffled()
                            val tfPool = subjectFiltered.filter { it.type == "tf" }.shuffled()
                            
                            val mcqs = mcqPool.take(LiveTestState.config.mcqCount)
                            val fibs = fibPool.take(LiveTestState.config.fibCount)
                            val tfs = tfPool.take(LiveTestState.config.tfCount)
                            
                            selectedQuestions.addAll(mcqs)
                            selectedQuestions.addAll(fibs)
                            selectedQuestions.addAll(tfs)
                        }
                        
                        // Fallback: If still empty, grab any questions
                        if (selectedQuestions.isEmpty()) {
                            selectedQuestions.addAll(allQuestions.take(10))
                        }
                        
                        val questionDtos = selectedQuestions.map {
                            QuestionDto(it.id, it.bookId, it.bookTitle, it.chapter, it.type, it.difficulty, it.question, it.optionsJson, it.answer, it.explanation, it.marks, it.isBookmarked, it.createdAt)
                        }
                        
                        val questionsJsonStr = kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(QuestionDto.serializer()), questionDtos)
                        val session = CandidateSession(
                            id = java.util.UUID.randomUUID().toString(),
                            name = req.name,
                            rollNumber = req.rollNumber,
                            email = req.email,
                            mobile = req.mobile,
                            portraitBase64 = req.portraitBase64,
                            loginTime = System.currentTimeMillis(),
                            status = "Testing",
                            questionsJson = questionsJsonStr,
                            answersJson = "{}",
                            score = 0,
                            totalMarks = selectedQuestions.sumOf { it.marks },
                            isDispatched = false,
                            warningCount = 0
                        )
                        
                        LiveTestState.addCandidate(session)

                        // Immediately preserve candidate session in Room database!
                        try {
                            val existingSub = repository.getSubmissionByRollNumber(req.rollNumber)
                            val subId = existingSub?.id ?: session.id
                            val initialSubmission = com.example.data.model.TestSubmissionEntity(
                                id = subId,
                                paperId = LiveTestState.config.paperId,
                                paperTitle = "${LiveTestState.config.examName}${if (LiveTestState.config.subject.isNotBlank()) " - " + LiveTestState.config.subject else ""}",
                                candidateName = req.name,
                                candidateRollNumber = req.rollNumber,
                                candidateEmail = req.email,
                                candidateMobile = req.mobile,
                                portraitBase64 = req.portraitBase64,
                                status = "In-Progress",
                                questionsJson = questionsJsonStr,
                                answersJson = "{}",
                                score = 0,
                                maxMarks = selectedQuestions.sumOf { it.marks },
                                warningCount = 0,
                                violationsJson = "[]",
                                proctorRemarks = "",
                                disputeStatus = "None",
                                isResultDeclared = false,
                                rank = 0,
                                evaluatedBy = "",
                                loginTime = System.currentTimeMillis(),
                                submitTime = 0L
                            )
                            repository.insertSubmission(initialSubmission)
                        } catch (e: Exception) {
                            Log.e("WebServerManager", "Failed to preserve candidate in DB at login", e)
                        }

                        WsSignaling.sendToAdmins("CANDIDATE_UPDATE")
                        call.respond(LoginResponse(true, questionDtos, LiveTestState.config.durationMinutes, LiveTestState.config.examName, LiveTestState.config.subject, LiveTestState.config.startTimeMillis))
                    } catch (e: Exception) {
                        Log.e("WebServerManager", "Error in login: ${e.message}", e)
                        call.respond(HttpStatusCode.InternalServerError, e.message ?: "Internal Error")
                    }
                }

                @Serializable
                data class SubmitRequest(val rollNumber: String, val answers: Map<String, String>, val status: String)

                @Serializable
                data class WarningRequest(val rollNumber: String, val warnings: Int, val reason: String = "")

                @Serializable
                data class ResolveSubmissionRequest(val id: String, val score: Int, val remarks: String, val disputeStatus: String)

                post("/api/livetest/heartbeat") {
                    if (mode != "livetest" && mode != "all") {
                        call.respond(HttpStatusCode.Forbidden, "Live test portal is disabled on this server.")
                        return@post
                    }
                    val request = call.receive<Map<String, String>>()
                    val roll = request["rollNumber"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val frameBase64 = request["frameBase64"] ?: ""
                    val answersJson = request["answersJson"]

                    // Progressive Autosave
                    if (!answersJson.isNullOrBlank() && answersJson != "{}") {
                        LiveTestState.updateCandidateAnswers(roll, answersJson)
                        try {
                            val existing = repository.getSubmissionByRollNumber(roll)
                            if (existing != null && existing.status == "In-Progress") {
                                repository.updateSubmission(existing.copy(answersJson = answersJson))
                            }
                        } catch (e: Exception) {
                            Log.e("WebServerManager", "Autosave error in heartbeat", e)
                        }
                    }

                    val warningMsg = LiveTestState.updateFrame(roll, frameBase64)
                    WsSignaling.sendToAdmins("CANDIDATE_UPDATE")
                    call.respond(mapOf("warningMessage" to warningMsg))
                }

                post("/api/livetest/submit") {
                    if (mode != "livetest" && mode != "all") {
                        call.respond(HttpStatusCode.Forbidden, "Live test portal is disabled on this server.")
                        return@post
                    }
                    try {
                        val req = call.receive<SubmitRequest>()
                        val candidate = LiveTestState.candidates.value.find { it.rollNumber == req.rollNumber }
                        val existing = repository.getSubmissionByRollNumber(req.rollNumber)

                        if (candidate == null && existing == null) {
                            return@post call.respond(HttpStatusCode.NotFound, "Session not found")
                        }
                            
                        val qJson = candidate?.questionsJson ?: existing?.questionsJson ?: "[]"
                        val assignedQuestions = try {
                            kotlinx.serialization.json.Json.decodeFromString<List<QuestionDto>>(qJson)
                        } catch (e: Exception) { emptyList<QuestionDto>() }
                        
                        var score = 0
                        var totalMarks = 0
                        assignedQuestions.forEach { q ->
                            totalMarks += q.marks
                            val studentAns = req.answers[q.id]?.trim() ?: ""
                            if (studentAns.isNotEmpty() && studentAns.equals(q.answer.trim(), ignoreCase = true)) {
                                score += q.marks
                            }
                        }

                        val encodedAnswers = kotlinx.serialization.json.Json.encodeToString(req.answers)
                        val submissionEntity = com.example.data.model.TestSubmissionEntity(
                            id = existing?.id ?: candidate?.id ?: java.util.UUID.randomUUID().toString(),
                            paperId = LiveTestState.config.paperId,
                            paperTitle = existing?.paperTitle ?: LiveTestState.config.examName,
                            candidateName = candidate?.name ?: existing?.candidateName ?: "Unknown",
                            candidateRollNumber = req.rollNumber,
                            candidateEmail = candidate?.email ?: existing?.candidateEmail ?: "",
                            candidateMobile = candidate?.mobile ?: existing?.candidateMobile ?: "",
                            portraitBase64 = if (!candidate?.portraitBase64.isNullOrBlank()) candidate!!.portraitBase64 else (existing?.portraitBase64 ?: ""),
                            status = req.status, // "Submitted" or "Disqualified"
                            questionsJson = qJson,
                            answersJson = encodedAnswers,
                            score = score,
                            maxMarks = if (totalMarks > 0) totalMarks else (existing?.maxMarks ?: 0),
                            warningCount = candidate?.warningCount ?: existing?.warningCount ?: 0,
                            violationsJson = existing?.violationsJson ?: "[]",
                            proctorRemarks = existing?.proctorRemarks ?: "",
                            disputeStatus = existing?.disputeStatus ?: "None",
                            isResultDeclared = existing?.isResultDeclared ?: false,
                            rank = existing?.rank ?: 0,
                            evaluatedBy = existing?.evaluatedBy ?: "",
                            loginTime = candidate?.loginTime ?: existing?.loginTime ?: System.currentTimeMillis(),
                            submitTime = System.currentTimeMillis()
                        )
                        repository.insertSubmission(submissionEntity)

                        LiveTestState.updateCandidateStatus(
                            rollNumber = req.rollNumber,
                            status = req.status,
                            answersJson = encodedAnswers,
                            score = score,
                            totalMarks = totalMarks,
                            warnings = candidate?.warningCount ?: existing?.warningCount ?: 0
                        )
                        WsSignaling.sendToAdmins("CANDIDATE_UPDATE")
                        call.respond(HttpStatusCode.OK)
                    } catch (e: Exception) {
                        Log.e("WebServerManager", "Error in submit: ${e.message}", e)
                        call.respond(HttpStatusCode.InternalServerError, e.message ?: "Internal Error")
                    }
                }

                post("/api/livetest/warning") {
                    if (mode != "livetest" && mode != "all") {
                        call.respond(HttpStatusCode.Forbidden, "Live test portal is disabled on this server.")
                        return@post
                    }
                    try {
                        val req = call.receive<WarningRequest>()
                        LiveTestState.updateWarnings(req.rollNumber, req.warnings)
                        if (req.reason.isNotEmpty()) {
                            Log.w("WebServerManager", "Security violation reported for candidate ${req.rollNumber}: ${req.reason} (count: ${req.warnings})")
                        }

                        // Append violation log to database
                        try {
                            val existing = repository.getSubmissionByRollNumber(req.rollNumber)
                            if (existing != null) {
                                val newStatus = if (req.warnings >= 3) "Disqualified" else existing.status
                                val violations = try {
                                    val arr = org.json.JSONArray(existing.violationsJson)
                                    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                                    arr.put("${sdf.format(java.util.Date())}: ${req.reason.ifBlank { "Proctor Incident #${req.warnings}" }}")
                                    arr.toString()
                                } catch (e: Exception) {
                                    existing.violationsJson
                                }
                                repository.updateSubmission(existing.copy(
                                    warningCount = req.warnings,
                                    status = newStatus,
                                    violationsJson = violations
                                ))
                            }
                        } catch (e: Exception) {
                            Log.e("WebServerManager", "Error recording violation in DB", e)
                        }

                        WsSignaling.sendToAdmins("CANDIDATE_UPDATE")
                        call.respond(HttpStatusCode.OK)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
                    }
                }

                // Public Candidate Result Query Endpoint
                get("/api/livetest/result") {
                    val roll = call.request.queryParameters["rollNumber"]?.trim() ?: ""
                    if (roll.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Roll number is required"))
                        return@get
                    }
                    val sub = repository.getSubmissionByRollNumber(roll)
                    if (sub == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "No examination record found for roll number $roll"))
                        return@get
                    }
                    if (!sub.isResultDeclared) {
                        call.respond(HttpStatusCode.OK, mapOf(
                            "declared" to false,
                            "candidateName" to sub.candidateName,
                            "rollNumber" to sub.candidateRollNumber,
                            "paperTitle" to sub.paperTitle,
                            "status" to sub.status,
                            "message" to "Exam is under post-exam evaluation. Results will be published soon."
                        ))
                        return@get
                    }
                    val sm = SettingsManager(appContext)
                    val supName = sm.activeSupervisorName
                    val supRole = sm.activeSupervisorRole
                    val supInst = sm.activeSupervisorInstitution
                    val supEmail = sm.activeSupervisorEmail
                    val sigHash = "SIG-" + java.util.UUID.nameUUIDFromBytes("${sub.id}-${sub.candidateRollNumber}-${sub.score}-$supEmail".toByteArray()).toString().replace("-", "").take(12).uppercase()

                    call.respond(HttpStatusCode.OK, mapOf(
                        "declared" to true,
                        "candidateName" to sub.candidateName,
                        "rollNumber" to sub.candidateRollNumber,
                        "paperTitle" to sub.paperTitle,
                        "status" to sub.status,
                        "score" to sub.score,
                        "maxMarks" to sub.maxMarks,
                        "rank" to sub.rank,
                        "disputeStatus" to sub.disputeStatus,
                        "proctorRemarks" to sub.proctorRemarks,
                        "submitTime" to sub.submitTime,
                        "supervisorName" to supName,
                        "supervisorRole" to supRole,
                        "supervisorInstitution" to supInst,
                        "supervisorEmail" to supEmail,
                        "signatureToken" to sigHash
                    ))
                }

                // Admin Submissions List Endpoint
                get("/api/admin/submissions") {
                    if (mode != "livetest" && mode != "all") {
                        call.respond(HttpStatusCode.Forbidden, "Live test portal is disabled on this server.")
                        return@get
                    }
                    if (!checkAuth(call)) return@get
                    val subs = repository.allSubmissions.first()
                    call.respond(subs)
                }

                // Admin Dispute Resolution Endpoint
                post("/api/admin/submissions/resolve") {
                    if (mode != "livetest" && mode != "all") {
                        call.respond(HttpStatusCode.Forbidden, "Live test portal is disabled on this server.")
                        return@post
                    }
                    if (!checkAuth(call)) return@post
                    try {
                        val req = call.receive<ResolveSubmissionRequest>()
                        val sub = repository.getSubmissionById(req.id)
                        if (sub != null) {
                            val sm = SettingsManager(appContext)
                            val supervisorStamp = "${sm.activeSupervisorName} (${sm.activeSupervisorRole})"
                            repository.updateSubmission(sub.copy(
                                score = req.score,
                                proctorRemarks = req.remarks,
                                disputeStatus = req.disputeStatus,
                                evaluatedBy = supervisorStamp
                            ))
                            WsSignaling.sendToAdmins("CANDIDATE_UPDATE")
                            call.respond(HttpStatusCode.OK, "Resolved")
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Submission not found")
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
                    }
                }

                // Admin Result Declaration Endpoint
                post("/api/admin/declare-results") {
                    if (mode != "livetest" && mode != "all") {
                        call.respond(HttpStatusCode.Forbidden, "Live test portal is disabled on this server.")
                        return@post
                    }
                    if (!checkAuth(call)) return@post
                    try {
                        val submissions = repository.allSubmissions.first()
                        val toDeclare = submissions
                            .filter { it.status != "In-Progress" }
                            .sortedWith(
                                compareByDescending<com.example.data.model.TestSubmissionEntity> { it.score }
                                    .thenBy { it.warningCount }
                                    .thenBy { it.submitTime }
                            )
                        val sm = SettingsManager(appContext)
                        val supervisorStamp = "${sm.activeSupervisorName} (${sm.activeSupervisorRole})"
                        toDeclare.forEachIndexed { index, s ->
                            repository.updateSubmission(s.copy(
                                isResultDeclared = true,
                                rank = index + 1,
                                evaluatedBy = supervisorStamp
                            ))
                        }
                        WsSignaling.sendToAdmins("CANDIDATE_UPDATE")
                        call.respond(HttpStatusCode.OK, mapOf("declaredCount" to toDeclare.size))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
                    }
                }

                
                        
                        webSocket("/api/admin/ws") {
                            if (mode != "livetest" && mode != "all") {
                                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "AV monitoring is only available on Live Test Server."))
                                return@webSocket
                            }
                            WsSignaling.adminSessions.add(this)
                            try {
                                incoming.consumeEach { frame ->
                                    if (frame is Frame.Text) {
                                        val msg = frame.readText()
                                        // Expected: WEBRTC_OFFER:rollNumber:sdp, WEBRTC_ICE:rollNumber:ice
                                        val parts = msg.split(":", limit = 3)
                                        if (parts.size >= 3) {
                                            val cmd = parts[0]
                                            val rollNumber = parts[1]
                                            val payload = parts[2]
                                            if (cmd == "WEBRTC_OFFER") {
                                                WsSignaling.sendToCandidate(rollNumber, "WEBRTC_OFFER:$payload")
                                            } else if (cmd == "WEBRTC_ICE") {
                                                WsSignaling.sendToCandidate(rollNumber, "WEBRTC_ICE:$payload")
                                            }
                                        }
                                    }
                                }
                            } finally {
                                WsSignaling.adminSessions.remove(this)
                            }
                        }

                        webSocket("/api/livetest/ws") {
                            if (mode != "livetest" && mode != "all") {
                                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Live test portal is disabled on this server."))
                                return@webSocket
                            }
                            // Read initial setup message
                            val setupFrame = incoming.receive() as? Frame.Text ?: return@webSocket
                            val reqString = setupFrame.readText()
                            // expect something like "INIT:ROLLNUMBER"
                            if (!reqString.startsWith("INIT:")) return@webSocket
                            val rollNumber = reqString.removePrefix("INIT:")
                            
                            WsSignaling.candidateSessions[rollNumber] = this
                            WsSignaling.sendToAdmins("CANDIDATE_WS_READY:$rollNumber")
                            WsSignaling.sendToAdmins("CANDIDATE_UPDATE")
                            try {
                                val senderJob = this@webSocket.launch {
                                    while (true) {
                                        val candidate = LiveTestState.candidates.value.find { it.rollNumber == rollNumber }
                                        if (candidate != null) {
                                            if (candidate.forceSubmitRequested) {
                                                this@webSocket.send("CMD:FORCE_SUBMIT")
                                                LiveTestState.clearForceSubmit(rollNumber)
                                            }
                                            if (candidate.activeWarningMessage.isNotEmpty()) {
                                                this@webSocket.send("CMD:WARNING:" + candidate.activeWarningMessage)
                                                LiveTestState.updateFrame(rollNumber, candidate.latestFrameBase64 ?: "")
                                            }
                                        }
                                        kotlinx.coroutines.delay(1000)
                                    }
                                }

                                for (f in incoming) {
                                    if (f is Frame.Text) {
                                        val msg = f.readText()
                                        if (msg.startsWith("PROG:")) {
                                            val parts = msg.removePrefix("PROG:").split(":", limit = 2)
                                            if (parts.size == 2) {
                                                LiveTestState.updateProgress(rollNumber, parts[0], parts[1])
                                                WsSignaling.sendToAdmins("CANDIDATE_UPDATE")
                                            }
                                        } else if (msg.startsWith("FRAME:")) {
                                            val b64 = msg.removePrefix("FRAME:")
                                            LiveTestState.updateFrame(rollNumber, b64)
                                            WsSignaling.sendToAdmins("FRAME_UPDATE:$rollNumber:$b64")
                                        } else if (msg.startsWith("WEBRTC_ANSWER:")) {
                                            val sdp = msg.removePrefix("WEBRTC_ANSWER:")
                                            WsSignaling.sendToAdmins("WEBRTC_ANSWER:$rollNumber:$sdp")
                                        } else if (msg.startsWith("WEBRTC_ICE:")) {
                                            val ice = msg.removePrefix("WEBRTC_ICE:")
                                            WsSignaling.sendToAdmins("WEBRTC_ICE:$rollNumber:$ice")
                                        }
                                    }
                                }
                                senderJob.cancel()
                            } catch (e: Exception) {
                                Log.e("WS", "WebSocket error: ${e.message}")
                            } finally {
                                WsSignaling.candidateSessions.remove(rollNumber)
                            }
                        }

                        delete("/api/papers/{id}") {
                            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                            repository.deletePaper(id)
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                } // close module
                } // close env
                server = embeddedServer(Netty, env, configure = {
                    responseWriteTimeoutSeconds = 10
                    requestReadTimeoutSeconds = 10
                    connectionGroupSize = 16
                    workerGroupSize = 32
                    callGroupSize = 64
                }).start(wait = false)
                
                val httpsConnector = server?.resolvedConnectors()?.find { it.type == io.ktor.server.engine.ConnectorType.HTTPS }
                val httpConnector = server?.resolvedConnectors()?.find { it.type == io.ktor.server.engine.ConnectorType.HTTP }

                val httpsPort = httpsConnector?.port ?: preferredHttpsPort
                val httpPort = httpConnector?.port ?: preferredHttpPort
                port = httpsPort
                val ipAddress = getLocalIpAddress()
                val httpsUrl = "https://$ipAddress:$httpsPort"
                val httpUrl = "http://$ipAddress:$httpPort"
                Log.d("WebServer", "Server started at HTTPS: $httpsUrl | HTTP: $httpUrl")
                onStarted(httpsUrl, httpUrl)
            } catch (e: Exception) {
                Log.e("WebServer", "Failed to start server", e)
                onError(e.stackTraceToString())
            }
        }
    }

    fun startServer(
        adminUser: String = "admin",
        adminPass: String = "1234",
        onStarted: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) = startServer(adminUser, adminPass, { httpsUrl, _ -> onStarted(httpsUrl) }, onError)

    fun stopServer() {
        val s = server
        server = null
        if (s != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    s.stop(500, 1000)
                } catch (e: Throwable) {
                    Log.e("WebServer", "Error stopping Netty server: ${e.message}", e)
                }
            }
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            val preferred = interfaces.sortedByDescending { 
                val name = it.name.lowercase()
                when {
                    name.startsWith("wlan") || name.startsWith("ap") || name.startsWith("eth") -> 3
                    name.startsWith("rndis") || name.startsWith("usb") -> 2
                    !name.startsWith("rmnet") && !name.startsWith("dummy") && !name.startsWith("p2p") -> 1
                    else -> 0
                }
            }
            for (intf in preferred) {
                if (!intf.isUp) continue
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val inetAddress = addresses.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("WebServer", ex.toString())
        }
        return "127.0.0.1"
    }
}
