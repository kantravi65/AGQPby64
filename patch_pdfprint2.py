import re

with open('app/src/main/java/com/example/util/PdfPrintUtils.kt', 'r') as f:
    content = f.read()

target = """                // Calculate options layout
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

                val leftX = marginPt.toFloat()
                val rightX = (widthPt - marginPt).toFloat()

                if (settings.showGridBorders) {
                    // Top line
                    canvas.drawLine(leftX, rowStartY, rightX, rowStartY, borderPaint)
                    // Bottom line
                    canvas.drawLine(leftX, rowEndY, rightX, rowEndY, borderPaint)
                    // Outer left line
                    canvas.drawLine(leftX, rowStartY, leftX, rowEndY, borderPaint)
                    // Outer right line
                    canvas.drawLine(rightX, rowStartY, rightX, rowEndY, borderPaint)
                    // Vertical divider between Q# and Content
                    canvas.drawLine(contentColX, rowStartY, contentColX, rowEndY, borderPaint)
                }

                // Draw Q Number centered in Col 1
                boldBodyPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(qNumStr, leftX + (numColWidth / 2f), rowStartY + settings.fontBodySp + 8f, boldBodyPaint)
                boldBodyPaint.textAlign = Paint.Align.LEFT

                // Draw Q Text in Col 2
                var textY = rowStartY + settings.fontBodySp + 8f
                qLines.forEach { line ->
                    canvas.drawText(line, contentColX + 8f, textY, bodyPaint)
                    textY += lineH
                }

                // Draw Options
                if (optRowLines.isNotEmpty()) {
                    textY += 2f
                    val halfW = (contentWidth - 16f) / 2f
                    optRowLines.forEach { pair ->
                        canvas.drawText(pair.first, contentColX + 8f, textY, bodyPaint)
                        if (pair.second != null) {
                            canvas.drawText(pair.second!!, contentColX + 8f + halfW, textY, bodyPaint)
                        }
                        textY += lineH
                    }
                }

                // Draw Explanation
                if (expLines.isNotEmpty()) {
                    textY += 2f
                    expLines.forEach { line ->
                        canvas.drawText(line, contentColX + 8f, textY, labelPaint)
                        textY += lineH
                    }
                }"""

replacement = """                // Calculate options layout
                var optionsHeight = 0f
                val optDrawInstructions = mutableListOf<Triple<String, Float, Float>>() // text, xOffset, yOffset
                
                if (optionsList.isNotEmpty()) {
                    val halfW = (contentWidth - 16f) / 2f
                    val fullW = contentWidth - 16f
                    
                    // Check if they can fit in 2 columns
                    var useTwoColumns = settings.twoColumnOptions && optionsList.size == 4
                    if (useTwoColumns) {
                        for (i in 0..3) {
                            val text = "(${('A' + i)}) ${optionsList[i]}"
                            if (bodyPaint.measureText(text) > halfW - 4f) {
                                useTwoColumns = false
                                break
                            }
                        }
                    }

                    var currentOptY = 0f
                    if (useTwoColumns) {
                        val optA = "(A) ${optionsList[0]}"
                        val optB = "(B) ${optionsList[1]}"
                        val optC = "(C) ${optionsList[2]}"
                        val optD = "(D) ${optionsList[3]}"
                        optDrawInstructions.add(Triple(optA, 0f, currentOptY))
                        optDrawInstructions.add(Triple(optB, halfW, currentOptY))
                        currentOptY += lineH
                        optDrawInstructions.add(Triple(optC, 0f, currentOptY))
                        optDrawInstructions.add(Triple(optD, halfW, currentOptY))
                        currentOptY += lineH
                        optionsHeight = currentOptY + 4f
                    } else {
                        optionsList.forEachIndexed { i, opt ->
                            val text = "(${('A' + i)}) $opt"
                            val wrapped = wrapText(text, bodyPaint, fullW)
                            wrapped.forEach { line ->
                                optDrawInstructions.add(Triple(line, 0f, currentOptY))
                                currentOptY += lineH
                            }
                        }
                        optionsHeight = currentOptY + 4f
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

                val leftX = marginPt.toFloat()
                val rightX = (widthPt - marginPt).toFloat()

                if (settings.showGridBorders) {
                    // Top line
                    canvas.drawLine(leftX, rowStartY, rightX, rowStartY, borderPaint)
                    // Bottom line
                    canvas.drawLine(leftX, rowEndY, rightX, rowEndY, borderPaint)
                    // Outer left line
                    canvas.drawLine(leftX, rowStartY, leftX, rowEndY, borderPaint)
                    // Outer right line
                    canvas.drawLine(rightX, rowStartY, rightX, rowEndY, borderPaint)
                    // Vertical divider between Q# and Content
                    canvas.drawLine(contentColX, rowStartY, contentColX, rowEndY, borderPaint)
                }

                // Draw Q Number centered in Col 1
                boldBodyPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(qNumStr, leftX + (numColWidth / 2f), rowStartY + settings.fontBodySp + 8f, boldBodyPaint)
                boldBodyPaint.textAlign = Paint.Align.LEFT

                // Draw Q Text in Col 2
                var textY = rowStartY + settings.fontBodySp + 8f
                qLines.forEach { line ->
                    canvas.drawText(line, contentColX + 8f, textY, bodyPaint)
                    textY += lineH
                }

                // Draw Options
                if (optDrawInstructions.isNotEmpty()) {
                    textY += 2f
                    optDrawInstructions.forEach { instruction ->
                        canvas.drawText(instruction.first, contentColX + 8f + instruction.second, textY + instruction.third, bodyPaint)
                    }
                    textY += optionsHeight - 4f
                }

                // Draw Explanation
                if (expLines.isNotEmpty()) {
                    textY += 2f
                    expLines.forEach { line ->
                        canvas.drawText(line, contentColX + 8f, textY, labelPaint)
                        textY += lineH
                    }
                }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/util/PdfPrintUtils.kt', 'w') as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Target not found")
