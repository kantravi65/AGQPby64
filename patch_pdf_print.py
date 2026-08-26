import re

with open('app/src/main/java/com/example/util/PdfPrintUtils.kt', 'r') as f:
    content = f.read()

target = """            var currentY = marginPt + 24f
            
            questions.forEachIndexed { index, q ->
                val qNumStr = "${index + 1}."
                val qText = q.question.trim()"""

replacement = """            var currentY = marginPt + 24f
            
            val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
            val typeNames = mapOf("mcq" to "MULTIPLE CHOICE QUESTIONS (MCQ)", "tf" to "TRUE / FALSE", "fib" to "FILL IN THE BLANKS", "subjective" to "SUBJECTIVE QUESTIONS")
            val groupedQuestions = questions.groupBy { it.type }.toSortedMap(compareBy { typeOrder[it] ?: 5 })
            
            var globalQuestionIdx = 1
            for ((type, typeQuestions) in groupedQuestions) {
                // Draw group heading
                val headingText = typeNames[type] ?: "OTHER"
                if (currentY + settings.fontBodySp * 3 > heightPt - marginPt) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(widthPt.toInt(), heightPt.toInt(), pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawWatermarkLayer(canvas)
                    currentY = marginPt
                }
                
                currentY += 8f
                canvas.drawText(headingText, widthPt / 2f, currentY, subTitlePaint)
                currentY += settings.fontBodySp + 8f
                
                for (q in typeQuestions) {
                    val index = globalQuestionIdx - 1
                    val qNumStr = "${globalQuestionIdx}."
                    val qText = q.question.trim()
                    globalQuestionIdx++"""

content = content.replace(target, replacement)

# We replaced `questions.forEachIndexed { index, q ->` with `for (q in typeQuestions) {`, so the closing bracket is fine.

with open('app/src/main/java/com/example/util/PdfPrintUtils.kt', 'w') as f:
    f.write(content)
