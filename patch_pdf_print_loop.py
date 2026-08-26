import re

with open('app/src/main/java/com/example/util/PdfPrintUtils.kt', 'r') as f:
    content = f.read()

target = """            questions.forEachIndexed { index, q ->
                val qNumStr = "${index + 1}."
                val qText = q.question.trim()

                // Parse options"""

replacement = """            val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
            val typeNames = mapOf("mcq" to "MULTIPLE CHOICE QUESTIONS (MCQ)", "tf" to "TRUE / FALSE", "fib" to "FILL IN THE BLANKS", "subjective" to "SUBJECTIVE QUESTIONS")
            val groupedQuestions = questions.groupBy { it.type }.toSortedMap(compareBy { typeOrder[it] ?: 5 })
            
            var globalIdx = 1
            groupedQuestions.forEach { (type, typeQuestions) ->
                // Draw Header for this group
                val headerText = typeNames[type] ?: "OTHER"
                checkNewPage(settings.fontBodySp * 2 + 16f)
                currentY += 8f
                canvas.drawText(headerText, (widthPt / 2).toFloat(), currentY + settings.fontBodySp, subTitlePaint)
                currentY += settings.fontBodySp * 2 + 8f

                typeQuestions.forEach { q ->
                    val qNumStr = "${globalIdx++}."
                    val qText = q.question.trim()

                    // Parse options ONLY IF MCQ
                    val optionsList = mutableListOf<String>()
                    if (q.type == "mcq") {
                        try {
                            val arr = org.json.JSONArray(q.optionsJson)
                            for (i in 0 until arr.length()) optionsList.add(arr.getString(i))
                        } catch (e: Exception) {}
                    }
"""

content = content.replace(target, replacement)

# We need to change the border drawing logic.
border_target = """                if (settings.showGridBorders) {
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
                }"""

border_replacement = """                if (settings.showGridBorders && q.type == "mcq") {
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
                }"""

content = content.replace(border_target, border_replacement)

# Finally, we have to close the extra loop bracket for `groupedQuestions.forEach`
end_target = """                currentY = rowEndY
            }

            // --- 5. ANSWER KEY & SOLUTIONS (ALWAYS ON SEPARATE PAGE) ---"""

end_replacement = """                currentY = rowEndY
                }
            }

            // --- 5. ANSWER KEY & SOLUTIONS (ALWAYS ON SEPARATE PAGE) ---"""

content = content.replace(end_target, end_replacement)

# Ensure org.json.JSONArray is imported if not used properly but we used full path.

with open('app/src/main/java/com/example/util/PdfPrintUtils.kt', 'w') as f:
    f.write(content)
