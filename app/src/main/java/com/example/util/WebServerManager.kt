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

    fun startServer(onStarted: (String) -> Unit) {
        if (server != null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                server = embeddedServer(Netty, port = port) {
                    install(ContentNegotiation) {
                        json(kotlinx.serialization.json.Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })
                    }
                    install(CORS) {
                        anyHost()
                    }
                    routing {
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
