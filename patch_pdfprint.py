import re

with open('app/src/main/java/com/example/util/PdfPrintUtils.kt', 'r') as f:
    content = f.read()

target = """            questions.forEachIndexed { index, q ->
                val qNumStr = "${index + 1}."
                val qText = q.question.trim()

                // Parse options
                val optionsList = mutableListOf<String>()
                try {
                    val arr = JSONArray(q.optionsJson)
                    for (i in 0 until arr.length()) optionsList.add(arr.getString(i))
                } catch (e: Exception) {}

                val qLines = wrapText(qText, bodyPaint, contentWidth - 12f)
                val lineH = settings.fontBodySp + settings.lineSpacingExtra

                // Calculate options layout
                var optionsHeight = 0f
                val optRowLines = mutableListOf<Pair<String, String?>>() // Pair for 2-column or single

                if (optionsList.isNotEmpty()) {
                    if (settings.twoColumnOptions && optionsList.size == 4) {
                        val optA = "(A) ${optionsList[0]}"
                        val optB = "(B) ${optionsList[1]}"
                        val optC = "(C) ${optionsList[2]}"
                        val optD = "(D) ${optionsList[3]}"
                        optRowLines.add(Pair(optA, optB))
                        optRowLines.add(Pair(optC, optD))
                        optionsHeight = optRowLines.size * lineH + 4f
                    } else {
                        optionsList.forEachIndexed { i, opt ->
                            val optChar = ('A' + i)
                            optRowLines.add(Pair("($optChar) $opt", null))
                        }
                        optionsHeight = optRowLines.size * lineH + 4f
                    }
                }

                var expHeight = 0f
                var expLines = emptyList<String>()
                if (settings.showExplanations && q.explanation.isNotBlank()) {
                    expLines = wrapText("Explanation: ${q.explanation}", labelPaint, contentWidth - 12f)
                    expHeight = expLines.size * lineH + 4f
                }

                val rowContentHeight = (qLines.size * lineH) + optionsHeight + expHeight + 14f

                // Check if this row fits on current page
                checkNewPage(rowContentHeight)

                val rowStartY = currentY
                val rowEndY = currentY + rowContentHeight

                if (settings.showGridBorders) {
                    canvas.drawRect(marginPt, rowStartY, pageWidth - marginPt, rowEndY, gridPaint)
                    canvas.drawLine(marginPt + 30f, rowStartY, marginPt + 30f, rowEndY, gridPaint) // Num col separator
                    canvas.drawLine(pageWidth - marginPt - 40f, rowStartY, pageWidth - marginPt - 40f, rowEndY, gridPaint) // Marks col separator
                }

                // Draw Q Number
                canvas.drawText(qNumStr, marginPt + 4f, rowStartY + settings.fontBodySp + 4f, boldBodyPaint)

                // Draw Marks
                val marksStr = "[${q.marks}]"
                canvas.drawText(marksStr, pageWidth - marginPt - 36f, rowStartY + settings.fontBodySp + 4f, labelPaint)

                // Draw Question text
                var textY = rowStartY + settings.fontBodySp + 4f
                qLines.forEach { l ->
                    canvas.drawText(l, marginPt + 34f, textY, bodyPaint)
                    textY += lineH
                }
                textY += 4f

                // Draw Options
                optRowLines.forEach { row ->
                    canvas.drawText(row.first, marginPt + 34f, textY, bodyPaint)
                    row.second?.let {
                        canvas.drawText(it, marginPt + 34f + (contentWidth / 2f), textY, bodyPaint)
                    }
                    textY += lineH
                }

                if (expLines.isNotEmpty()) {
                    textY += 4f
                    expLines.forEach { l ->
                        canvas.drawText(l, marginPt + 34f, textY, labelPaint)
                        textY += lineH
                    }
                }

                currentY = rowEndY
            }"""

replacement = """
            val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
            val sortedQuestions = questions.sortedBy { typeOrder[it.type] ?: 5 }
            val typeNames = mapOf("mcq" to "MULTIPLE CHOICE QUESTIONS (MCQ)", "tf" to "TRUE / FALSE", "fib" to "FILL IN THE BLANKS", "subjective" to "SUBJECTIVE QUESTIONS")

            var currentType = ""

            sortedQuestions.forEachIndexed { index, q ->
                val qNumStr = "${index + 1}."
                val qText = q.question.trim()

                // Check if type changed
                if (q.type != currentType) {
                    currentType = q.type
                    val heading = typeNames[currentType] ?: "OTHER"
                    val headingLines = wrapText(heading, boldBodyPaint, printableWidth)
                    val headingH = headingLines.size * (settings.fontBodySp + settings.lineSpacingExtra) + 12f
                    checkNewPage(headingH)
                    var hy = currentY + settings.fontBodySp + 8f
                    headingLines.forEach { l ->
                        canvas.drawText(l, pageWidth / 2f - boldBodyPaint.measureText(l) / 2f, hy, boldBodyPaint)
                        hy += (settings.fontBodySp + settings.lineSpacingExtra)
                    }
                    currentY += headingH
                }

                // Parse options (only for MCQ)
                val optionsList = mutableListOf<String>()
                if (q.type == "mcq") {
                    try {
                        val arr = JSONArray(q.optionsJson)
                        for (i in 0 until arr.length()) optionsList.add(arr.getString(i))
                    } catch (e: Exception) {}
                }

                val qLines = wrapText(qText, bodyPaint, contentWidth - 12f)
                val lineH = settings.fontBodySp + settings.lineSpacingExtra

                // Calculate options layout
                var optionsHeight = 0f
                val optRowLines = mutableListOf<Pair<String, String?>>() // Pair for 2-column or single

                if (optionsList.isNotEmpty()) {
                    if (settings.twoColumnOptions && optionsList.size == 4) {
                        val optA = "(A) ${optionsList[0]}"
                        val optB = "(B) ${optionsList[1]}"
                        val optC = "(C) ${optionsList[2]}"
                        val optD = "(D) ${optionsList[3]}"
                        optRowLines.add(Pair(optA, optB))
                        optRowLines.add(Pair(optC, optD))
                        optionsHeight = optRowLines.size * lineH + 4f
                    } else {
                        optionsList.forEachIndexed { i, opt ->
                            val optChar = ('A' + i)
                            optRowLines.add(Pair("($optChar) $opt", null))
                        }
                        optionsHeight = optRowLines.size * lineH + 4f
                    }
                }

                var expHeight = 0f
                var expLines = emptyList<String>()
                if (settings.showExplanations && q.explanation.isNotBlank()) {
                    expLines = wrapText("Explanation: ${q.explanation}", labelPaint, contentWidth - 12f)
                    expHeight = expLines.size * lineH + 4f
                }

                val rowContentHeight = (qLines.size * lineH) + optionsHeight + expHeight + 14f

                // Check if this row fits on current page
                checkNewPage(rowContentHeight)

                val rowStartY = currentY
                val rowEndY = currentY + rowContentHeight

                if (settings.showGridBorders && q.type == "mcq") {
                    canvas.drawRect(marginPt, rowStartY, pageWidth - marginPt, rowEndY, gridPaint)
                    canvas.drawLine(marginPt + 30f, rowStartY, marginPt + 30f, rowEndY, gridPaint) // Num col separator
                    canvas.drawLine(pageWidth - marginPt - 40f, rowStartY, pageWidth - marginPt - 40f, rowEndY, gridPaint) // Marks col separator
                }

                // Draw Q Number
                canvas.drawText(qNumStr, marginPt + 4f, rowStartY + settings.fontBodySp + 4f, boldBodyPaint)

                // Draw Marks
                val marksStr = "[${q.marks}]"
                canvas.drawText(marksStr, pageWidth - marginPt - 36f, rowStartY + settings.fontBodySp + 4f, labelPaint)

                // Draw Question text
                var textY = rowStartY + settings.fontBodySp + 4f
                qLines.forEach { l ->
                    canvas.drawText(l, marginPt + 34f, textY, bodyPaint)
                    textY += lineH
                }
                textY += 4f

                // Draw Options
                optRowLines.forEach { row ->
                    canvas.drawText(row.first, marginPt + 34f, textY, bodyPaint)
                    row.second?.let {
                        canvas.drawText(it, marginPt + 34f + (contentWidth / 2f), textY, bodyPaint)
                    }
                    textY += lineH
                }

                if (expLines.isNotEmpty()) {
                    textY += 4f
                    expLines.forEach { l ->
                        canvas.drawText(l, marginPt + 34f, textY, labelPaint)
                        textY += lineH
                    }
                }

                currentY = rowEndY
            }"""

content = content.replace(target, replacement)

# Now fix answer key rendering
target2 = """                questions.forEachIndexed { index, q ->
                    val ansLine = "Q${index + 1}: ${q.answer}"
                    val expText = if (q.explanation.isNotBlank()) " — ${q.explanation}" else ""
                    val fullAns = ansLine + expText
                    val lines = wrapText(fullAns, bodyPaint, printableWidth - 8f)
                    val lineH = settings.fontBodySp + settings.lineSpacingExtra

                    checkNewPage(lines.size * lineH + 6f)
                    lines.forEach { l ->
                        canvas.drawText(l, marginPt + 4f, currentY + settings.fontBodySp, boldBodyPaint)
                        currentY += lineH
                    }
                    currentY += 4f
                }"""

replacement2 = """                val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
                val sortedQuestions = questions.sortedBy { typeOrder[it.type] ?: 5 }
                sortedQuestions.forEachIndexed { index, q ->
                    val ansLine = "Q${index + 1}: ${q.answer}"
                    val expText = if (q.explanation.isNotBlank()) " — ${q.explanation}" else ""
                    val fullAns = ansLine + expText
                    val lines = wrapText(fullAns, bodyPaint, printableWidth - 8f)
                    val lineH = settings.fontBodySp + settings.lineSpacingExtra

                    checkNewPage(lines.size * lineH + 6f)
                    lines.forEach { l ->
                        canvas.drawText(l, marginPt + 4f, currentY + settings.fontBodySp, boldBodyPaint)
                        currentY += lineH
                    }
                    currentY += 4f
                }"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/util/PdfPrintUtils.kt', 'w') as f:
    f.write(content)
