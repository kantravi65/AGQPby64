package com.example.util

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
import java.io.File
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

class WebServerManager(private val appContext: Context, private val repository: OtsRepository, private val mode: String = "admin") {

    private var server: NettyApplicationEngine? = null
    private val port = 8080

    fun startServer(adminUser: String = "admin", adminPass: String = "1234", onStarted: (String) -> Unit) {
        if (server != null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                server = embeddedServer(Netty, port = port, host = "0.0.0.0", configure = {
                    responseWriteTimeoutSeconds = 10
                    requestReadTimeoutSeconds = 10
                    connectionGroupSize = 16
                    workerGroupSize = 32
                    callGroupSize = 64
                }) {
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

                        get("/") {
                            val filename = when(mode) {
                                "expert" -> "web_expert.html"
                                "livetest" -> "web_livetest.html"
                                else -> "web_dashboard.html"
                            }
                            val html = appContext.assets.open(filename).bufferedReader().use { it.readText() }
                            call.respondText(html, ContentType.Text.Html)
                        }

                        // API: Books
                        get("/api/books") {
                            val books = repository.allBooks.first().map { BookDto(it.id, it.title, it.chapterCount) }
                            call.respond(books)
                        }
                        post("/api/books") {
                            val dto = call.receive<BookDto>()
                            repository.insertBook(BookEntity(dto.id, dto.title, dto.chapterCount))
                            call.respond(HttpStatusCode.OK)
                        }
                        delete("/api/books/{id}") {
                            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                            val book = repository.allBooks.first().find { it.id == id }
                            if (book != null) repository.deleteBook(book)
                            call.respond(HttpStatusCode.OK)
                        }

                        // API: Questions
                        get("/api/questions") {
                            val questions = repository.allQuestions.first().map {
                                QuestionDto(it.id, it.bookId, it.bookTitle, it.chapter, it.type, it.difficulty, it.question, it.optionsJson, it.answer, it.explanation, it.marks, it.isBookmarked, it.createdAt)
                            }
                            call.respond(questions)
                        }
                        post("/api/questions") {
                            val dto = call.receive<QuestionDto>()
                            val entity = QuestionEntity(dto.id, dto.bookId, dto.bookTitle, dto.chapter, dto.type, dto.difficulty, dto.question, dto.optionsJson, dto.answer, dto.explanation, dto.marks, dto.isBookmarked, dto.createdAt)
                            repository.insertQuestion(entity)
                            call.respond(HttpStatusCode.OK)
                        }
                        post("/api/questions/bulk") {
                            val dtos = call.receive<List<QuestionDto>>()
                            val entities = dtos.map { dto ->
                                QuestionEntity(dto.id, dto.bookId, dto.bookTitle, dto.chapter, dto.type, dto.difficulty, dto.question, dto.optionsJson, dto.answer, dto.explanation, dto.marks, dto.isBookmarked, dto.createdAt)
                            }
                            repository.insertAllQuestions(entities)
                            call.respond(HttpStatusCode.OK)
                        }
                        delete("/api/questions/{id}") {
                            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                            repository.deleteQuestion(id)
                            call.respond(HttpStatusCode.OK)
                        }

                        // API: Papers
                        get("/api/papers") {
                            val papers = repository.allPapers.first().map {
                                PaperDto(it.id, it.title, it.subject, it.durationMinutes, it.totalMarks, it.questionIdsJson, it.createdAt)
                            }
                            call.respond(papers)
                        }
                        post("/api/papers") {
                            val dto = call.receive<PaperDto>()
                            val entity = PaperEntity(dto.id, dto.title, dto.subject, dto.durationMinutes, dto.totalMarks, dto.questionIdsJson, dto.createdAt)
                            repository.insertPaper(entity)
                            call.respond(HttpStatusCode.OK)
                        }
                        
                get("/api/papers/{id}/pdf") {
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
                    call.respond(LiveTestState.config)
                }

                @Serializable
                data class LoginRequest(val name: String, val rollNumber: String)

                @Serializable
                data class LoginResponse(val success: Boolean, val questions: List<QuestionDto>, val durationMinutes: Int)

                post("/api/livetest/login") {
                    try {
                        val req = call.receive<LoginRequest>()
                        
                        // Check if candidate has already completed/submitted/disqualified from the test
                        val existing = LiveTestState.candidates.value.find { it.rollNumber == req.rollNumber }
                        if (existing != null && (existing.status == "Submitted" || existing.status == "Disqualified")) {
                            return@post call.respond(HttpStatusCode.Forbidden, "You have already completed this exam and cannot enter again.")
                        }
                        
                        val allQuestions = repository.allQuestions.first()
                        
                        val subjectFiltered = if (LiveTestState.config.subject.isNotEmpty()) {
                            allQuestions.filter { it.bookTitle.equals(LiveTestState.config.subject, ignoreCase = true) }
                        } else {
                            allQuestions
                        }
                        
                        val mcqPool = subjectFiltered.filter { it.type == "mcq" }.shuffled()
                        val fibPool = subjectFiltered.filter { it.type == "fib" }.shuffled()
                        val tfPool = subjectFiltered.filter { it.type == "tf" }.shuffled()
                        
                        val selectedQuestions = mutableListOf<QuestionEntity>()
                        selectedQuestions.addAll(mcqPool.take(LiveTestState.config.mcqCount))
                        selectedQuestions.addAll(fibPool.take(LiveTestState.config.fibCount))
                        selectedQuestions.addAll(tfPool.take(LiveTestState.config.tfCount))
                        
                        if (selectedQuestions.isEmpty()) {
                            selectedQuestions.addAll(subjectFiltered.shuffled().take(10))
                        }
                        
                        val questionDtos = selectedQuestions.map {
                            QuestionDto(it.id, it.bookId, it.bookTitle, it.chapter, it.type, it.difficulty, it.question, it.optionsJson, it.answer, it.explanation, it.marks, it.isBookmarked, it.createdAt)
                        }
                        
                        val session = CandidateSession(
                            id = java.util.UUID.randomUUID().toString(),
                            name = req.name,
                            rollNumber = req.rollNumber,
                            loginTime = System.currentTimeMillis(),
                            status = "Testing",
                            questionsJson = kotlinx.serialization.json.Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(QuestionDto.serializer()), questionDtos),
                            answersJson = "{}",
                            score = 0,
                            totalMarks = selectedQuestions.sumOf { it.marks },
                            isDispatched = false,
                            warningCount = 0
                        )
                        
                        LiveTestState.addCandidate(session)
                        call.respond(LoginResponse(true, questionDtos, LiveTestState.config.durationMinutes))
                    } catch (e: Exception) {
                        Log.e("WebServerManager", "Error in login: ${e.message}", e)
                        call.respond(HttpStatusCode.InternalServerError, e.message ?: "Internal Error")
                    }
                }

                @Serializable
                data class SubmitRequest(val rollNumber: String, val answers: Map<String, String>, val status: String)

                                        post("/api/livetest/heartbeat") {
                            val request = call.receive<Map<String, String>>()
                            val roll = request["rollNumber"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                            val frameBase64 = request["frameBase64"] ?: ""
                            
                            val warningMsg = LiveTestState.updateFrame(roll, frameBase64)
                            call.respond(mapOf("warningMessage" to warningMsg))
                        }

                        post("/api/livetest/submit") {
                    try {
                        val req = call.receive<SubmitRequest>()
                        val candidate = LiveTestState.candidates.value.find { it.rollNumber == req.rollNumber }
                            ?: return@post call.respond(HttpStatusCode.NotFound, "Session not found")
                            
                        val assignedQuestions = kotlinx.serialization.json.Json.decodeFromString<List<QuestionDto>>(candidate.questionsJson)
                        
                        var score = 0
                        var totalMarks = 0
                        assignedQuestions.forEach { q ->
                            totalMarks += q.marks
                            val studentAns = req.answers[q.id]?.trim() ?: ""
                            if (studentAns.isNotEmpty() && studentAns.equals(q.answer.trim(), ignoreCase = true)) {
                                score += q.marks
                            }
                        }
                        
                        LiveTestState.updateCandidateStatus(
                            rollNumber = req.rollNumber,
                            status = req.status,
                            answersJson = kotlinx.serialization.json.Json.encodeToString(req.answers),
                            score = score,
                            totalMarks = totalMarks,
                            warnings = candidate.warningCount
                        )
                        call.respond(HttpStatusCode.OK)
                    } catch (e: Exception) {
                        Log.e("WebServerManager", "Error in submit: ${e.message}", e)
                        call.respond(HttpStatusCode.InternalServerError, e.message ?: "Internal Error")
                    }
                }

                @Serializable
                data class WarningRequest(val rollNumber: String, val warnings: Int)

                post("/api/livetest/warning") {
                    try {
                        val req = call.receive<WarningRequest>()
                        LiveTestState.updateWarnings(req.rollNumber, req.warnings)
                        call.respond(HttpStatusCode.OK)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
                    }
                }

                delete("/api/papers/{id}") {
                            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                            repository.deletePaper(id)
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }.start(wait = false)
                
                val ipAddress = getLocalIpAddress()
                val url = "http://$ipAddress:$port"
                Log.d("WebServer", "Server started at $url")
                onStarted(url)
            } catch (e: Exception) {
                Log.e("WebServer", "Failed to start server", e)
            }
        }
    }

    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
    }

    private fun getLocalIpAddress(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
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
