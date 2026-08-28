import re

with open("app/src/main/java/com/example/util/WebServerManager.kt", "r") as f:
    content = f.read()

bad = """                            val settings = PdfPrintSettings(
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
                            )"""

good = """                            val settings = PdfPrintSettings(
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
                            )"""

content = content.replace(bad, good)

with open("app/src/main/java/com/example/util/WebServerManager.kt", "w") as f:
    f.write(content)
