import re

with open("app/src/main/java/com/example/util/WebServerManager.kt", "r") as f:
    content = f.read()

# Add respondFile import if not present
if "import io.ktor.server.response.respondFile" not in content:
    content = content.replace("import io.ktor.server.response.respondText", "import io.ktor.server.response.respondText\nimport io.ktor.server.response.respondFile")

# Find the end of routing block and inject the PDF route
# I can just put it right after the POST /api/papers endpoint.
pdf_route = """
                get("/api/papers/{id}/pdf") {
                    val id = call.parameters["id"]
                    val paper = id?.let { repository.getPaper(it) }
                    if (paper != null) {
                        try {
                            val qIds = Json.decodeFromString<List<String>>(paper.questionIdsJson)
                            val questions = repository.getAllQuestions().first().filter { qIds.contains(it.id) }
                            
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
                                fontSize = fSize,
                                marginSize = mSize,
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
"""

# Let's just append it before `delete("/api/papers/{id}") {`
content = content.replace('delete("/api/papers/{id}") {', pdf_route + '\n                delete("/api/papers/{id}") {')

with open("app/src/main/java/com/example/util/WebServerManager.kt", "w") as f:
    f.write(content)
