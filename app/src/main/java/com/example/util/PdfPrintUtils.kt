package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.PaperEntity
import com.example.data.model.QuestionEntity
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class PaperSize(val displayName: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (210 x 297 mm)", 595, 842),
    LETTER("US Letter (8.5 x 11 in)", 612, 792),
    LEGAL("US Legal (8.5 x 14 in)", 612, 1008),
    A5("A5 (148 x 210 mm)", 420, 595)
}

enum class MarginSize(val displayName: String, val marginPt: Int) {
    NARROW("Narrow (18 pt)", 18),
    NORMAL("Normal (36 pt)", 36),
    WIDE("Wide (54 pt)", 54)
}

enum class FontSize(val displayName: String, val titleSp: Float, val bodySp: Float, val subSp: Float) {
    COMPACT("Compact (10 pt)", 13f, 10f, 8.5f),
    MEDIUM("Standard (12 pt)", 15f, 12f, 10f),
    LARGE("Large (14 pt)", 18f, 14f, 11.5f)
}

enum class WatermarkPattern(val displayName: String, val description: String) {
    MULTIPLE_GRID("Multiple Grid (Non-removable)", "Repeated diagonal tiled matrix covering full page"),
    SINGLE_CENTER("Single Center", "Large diagonal watermark across page center"),
    HEADER_STAMP("Header Stamp", "Security watermark banner at top header")
}

data class PdfPrintSettings(
    val paperSize: PaperSize = PaperSize.A4,
    val marginPt: Int = 36,
    val fontBodySp: Float = 11f,
    val fontTitleSp: Float = 15f,
    val lineSpacingExtra: Float = 4f,
    val mainTitle: String = "GEN TEST: FLT ENG",
    val subTitle: String = "TECH II",
    val paperCode: String = "QP-178566",
    val dateStr: String = "2026-08-05",
    val totalMarksText: String = "",
    val serNoLabel: String = "SER NO: ______",
    val rankLabel: String = "RANK: ______",
    val nameLabel: String = "NAME: __________________",
    val sectionHeading: String = "MULTIPLE CHOICE QUESTIONS (MCQ)",
    val showCandidateBox: Boolean = true,
    val showGridBorders: Boolean = true,
    val twoColumnOptions: Boolean = true,
    val showAnswerKey: Boolean = false,
    val showExplanations: Boolean = false,
    // Watermark Settings
    val watermarkEnabled: Boolean = true,
    val watermarkText: String = "Ravikant",
    val watermarkIsCursive: Boolean = true,
    val watermarkSizeSp: Float = 26f,
    val watermarkOpacity: Float = 0.20f,
    val watermarkPattern: WatermarkPattern = WatermarkPattern.MULTIPLE_GRID,
    val watermarkAngle: Float = -35f
)

object PdfPrintUtils {

    fun generatePdfFile(
        context: Context,
        paper: PaperEntity,
        questions: List<QuestionEntity>,
        settings: PdfPrintSettings
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val widthPt = settings.paperSize.widthPt
            val heightPt = settings.paperSize.heightPt
            val marginPt = settings.marginPt
            val printableWidth = widthPt - (marginPt * 2)

            val paperCodeVal = try {
                val startDate = java.time.LocalDate.of(2008, 12, 31)
                val createdEpoch = if (paper.createdAt > 0L) paper.createdAt else System.currentTimeMillis()
                val paperDate = java.time.Instant.ofEpochMilli(createdEpoch)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                val period = java.time.Period.between(startDate, paperDate)
                val formattedDate = String.format("%02d%02d%02d", period.years, period.months, period.days)
                val seed = paper.id.hashCode().toLong()
                val randomNum = kotlin.random.Random(seed).nextInt(10, 100)
                "RYQP-$formattedDate-$randomNum"
            } catch (e: Exception) {
                val seed = paper.id.hashCode().toLong()
                val randomNum = kotlin.random.Random(seed).nextInt(10, 100)
                "RYQP-170726-$randomNum"
            }

            val titlePaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = settings.fontTitleSp
                color = Color.BLACK
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val subTitlePaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = settings.fontBodySp
                color = Color.BLACK
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val labelPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = settings.fontBodySp - 1f
                color = Color.BLACK
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                typeface = Typeface.DEFAULT
                textSize = settings.fontBodySp
                color = Color.BLACK
                isAntiAlias = true
            }

            val boldBodyPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = settings.fontBodySp
                color = Color.BLACK
                isAntiAlias = true
            }

            val footerPaint = Paint().apply {
                typeface = Typeface.DEFAULT
                textSize = 9f
                color = Color.GRAY
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val borderPaint = Paint().apply {
                color = Color.BLACK
                strokeWidth = 1.2f
                style = Paint.Style.STROKE
            }

            val fillHeaderPaint = Paint().apply {
                color = Color.parseColor("#F2F2F2")
                style = Paint.Style.FILL
            }

            val watermarkPaint = Paint().apply {
                color = Color.DKGRAY
                alpha = (settings.watermarkOpacity.coerceIn(0.01f, 1.0f) * 255).toInt().coerceIn(5, 255)
                textSize = settings.watermarkSizeSp
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = if (settings.watermarkIsCursive) {
                    Typeface.create("cursive", Typeface.ITALIC)
                } else {
                    Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            }

            fun drawWatermarkLayer(targetCanvas: Canvas) {
                if (!settings.watermarkEnabled || settings.watermarkText.isBlank()) return

                when (settings.watermarkPattern) {
                    WatermarkPattern.MULTIPLE_GRID -> {
                        val colPositions = listOf(widthPt * 0.22f, widthPt * 0.50f, widthPt * 0.78f)
                        val rowPositions = listOf(
                            heightPt * 0.12f,
                            heightPt * 0.28f,
                            heightPt * 0.44f,
                            heightPt * 0.60f,
                            heightPt * 0.76f,
                            heightPt * 0.90f
                        )
                        rowPositions.forEach { y ->
                            colPositions.forEach { x ->
                                targetCanvas.save()
                                targetCanvas.rotate(settings.watermarkAngle, x, y)
                                targetCanvas.drawText(settings.watermarkText, x, y, watermarkPaint)
                                targetCanvas.restore()
                            }
                        }
                    }
                    WatermarkPattern.SINGLE_CENTER -> {
                        val cx = widthPt / 2f
                        val cy = heightPt / 2f
                        targetCanvas.save()
                        targetCanvas.rotate(settings.watermarkAngle, cx, cy)
                        targetCanvas.drawText(settings.watermarkText, cx, cy, watermarkPaint)
                        targetCanvas.restore()
                    }
                    WatermarkPattern.HEADER_STAMP -> {
                        val cx = widthPt / 2f
                        val cy = marginPt + 22f
                        targetCanvas.save()
                        targetCanvas.rotate(settings.watermarkAngle, cx, cy)
                        targetCanvas.drawText(settings.watermarkText, cx, cy, watermarkPaint)
                        targetCanvas.restore()
                    }
                }
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(widthPt, heightPt, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            var currentY = marginPt.toFloat()

            fun drawFooter() {
                drawWatermarkLayer(canvas)
                val footerY = (heightPt - marginPt / 2).toFloat()
                val codeText = "Paper Code: $paperCodeVal | "
                canvas.drawText("${codeText}Page $pageNumber", (widthPt / 2).toFloat(), footerY, footerPaint)
            }

            fun startNewPage() {
                drawFooter()
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(widthPt, heightPt, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = marginPt.toFloat()
            }

            fun checkNewPage(neededHeight: Float) {
                if (currentY + neededHeight > heightPt - marginPt - 20) {
                    startNewPage()
                }
            }

            // --- 1. MAIN TITLE & SUBTITLE ---
            val titleText = if (settings.mainTitle.isNotBlank()) settings.mainTitle else paper.title
            checkNewPage(titlePaint.textSize + 10)
            canvas.drawText(titleText.uppercase(), (widthPt / 2).toFloat(), currentY + titlePaint.textSize, titlePaint)
            currentY += titlePaint.textSize + 6f

            if (settings.subTitle.isNotBlank()) {
                checkNewPage(subTitlePaint.textSize + 6)
                canvas.drawText(settings.subTitle.uppercase(), (widthPt / 2).toFloat(), currentY + subTitlePaint.textSize, subTitlePaint)
                currentY += subTitlePaint.textSize + 8f
            } else {
                currentY += 4f
            }

            // --- 2. CANDIDATE & PAPER DETAILS BOX ---
            if (settings.showCandidateBox) {
                val boxHeight = (settings.fontBodySp * 2.8f) + 16f
                checkNewPage(boxHeight + 8f)

                val leftX = marginPt.toFloat()
                val rightX = (widthPt - marginPt).toFloat()
                val col1X = leftX + (printableWidth * 0.33f)
                val col2X = leftX + (printableWidth * 0.65f)
                val midY = currentY + (boxHeight / 2f)

                // Outer rectangle
                canvas.drawRect(leftX, currentY, rightX, currentY + boxHeight, borderPaint)
                // Mid horizontal line
                canvas.drawLine(leftX, midY, rightX, midY, borderPaint)
                // Vertical lines
                canvas.drawLine(col1X, currentY, col1X, currentY + boxHeight, borderPaint)
                canvas.drawLine(col2X, currentY, col2X, currentY + boxHeight, borderPaint)

                // Row 1 Text
                val r1TextY = currentY + (settings.fontBodySp * 1.1f) + 3f
                canvas.drawText(settings.serNoLabel, leftX + 8f, r1TextY, labelPaint)
                canvas.drawText(settings.rankLabel, col1X + 8f, r1TextY, labelPaint)
                canvas.drawText(settings.nameLabel, col2X + 8f, r1TextY, labelPaint)

                // Row 2 Text
                val r2TextY = midY + (settings.fontBodySp * 1.1f) + 3f
                val dateVal = if (settings.dateStr.isNotBlank()) settings.dateStr else "2026-08-05"
                val codeVal = paperCodeVal
                val marksVal = if (settings.totalMarksText.isNotBlank()) settings.totalMarksText else "TOTAL MARKS: ${questions.size}X${questions.firstOrNull()?.marks ?: 1}=${paper.totalMarks}"

                canvas.drawText("DATE: $dateVal", leftX + 8f, r2TextY, labelPaint)
                canvas.drawText("PAPER CODE: $codeVal", col1X + 8f, r2TextY, labelPaint)
                canvas.drawText(marksVal, col2X + 8f, r2TextY, labelPaint)

                currentY += boxHeight + 10f
            }

            // --- 3. SECTION HEADING BOX REMOVED ---

            // --- 4. QUESTIONS TABLE / GRID ---
            val numColWidth = 42f
            val contentColX = marginPt + numColWidth
            val contentWidth = printableWidth - numColWidth

            // Helper to wrap text into lines
            fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
                val words = text.split(" ")
                val lines = mutableListOf<String>()
                var currentLine = ""
                words.forEach { word ->
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (paint.measureText(testLine) > maxWidth) {
                        if (currentLine.isNotEmpty()) lines.add(currentLine)
                        currentLine = word
                    } else {
                        currentLine = testLine
                    }
                }
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                return lines
            }

            val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
            val typeNames = mapOf("mcq" to "MULTIPLE CHOICE QUESTIONS (MCQ)", "tf" to "TRUE / FALSE", "fib" to "FILL IN THE BLANKS", "subjective" to "SUBJECTIVE QUESTIONS")
            val groupedQuestions = questions.groupBy { it.type }.toSortedMap(compareBy { typeOrder[it] ?: 5 })
            
            var globalIdx = 1
            groupedQuestions.forEach { (type, typeQuestions) ->
                // Draw Header for this group
                val headerText = typeNames[type] ?: "OTHER"
                checkNewPage(settings.fontBodySp * 2 + 16f)
                currentY += 8f
                subTitlePaint.textAlign = Paint.Align.CENTER
                canvas.drawText(headerText, (widthPt / 2).toFloat(), currentY + settings.fontBodySp, subTitlePaint)
                subTitlePaint.textAlign = Paint.Align.LEFT
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



                val qLines = wrapText(qText, bodyPaint, contentWidth - 12f)
                val lineH = settings.fontBodySp + settings.lineSpacingExtra

                // Calculate options layout
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

                if (settings.showGridBorders && q.type == "mcq") {
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
                }

                currentY = rowEndY
                }
            }

            // --- 5. ANSWER KEY & SOLUTIONS (ALWAYS ON SEPARATE PAGE) ---
            if (settings.showAnswerKey) {
                // Keep question paper and answer key on separate page
                startNewPage()

                val leftX = marginPt.toFloat()
                val rightX = (widthPt - marginPt).toFloat()

                canvas.drawRect(leftX, currentY, rightX, currentY + 24f, fillHeaderPaint)
                canvas.drawRect(leftX, currentY, rightX, currentY + 24f, borderPaint)

                subTitlePaint.textAlign = Paint.Align.CENTER
                canvas.drawText("ANSWER KEY & SOLUTIONS", (widthPt / 2).toFloat(), currentY + 16f, subTitlePaint)
                subTitlePaint.textAlign = Paint.Align.LEFT

                currentY += 32f

                val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
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
                }
            }

            drawFooter()
            pdfDocument.finishPage(page)

            val fileName = "Paper_${paper.id}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun printPdf(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager != null) {
            val printAdapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val pdi = PrintDocumentInfo.Builder("$jobName.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()
                    callback?.onLayoutFinished(pdi, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        FileInputStream(pdfFile).use { input ->
                            FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.localizedMessage)
                    }
                }
            }
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        } else {
            Toast.makeText(context, "Print Service not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdf(context: Context, pdfFile: File, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Question Paper PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "PDF saved to temporary cache: ${pdfFile.name}", Toast.LENGTH_LONG).show()
        }
    }

    fun sharePdfViaEmail(context: Context, pdfFile: File, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Question Paper: $title")
                putExtra(Intent.EXTRA_TEXT, "Please find attached the customized Question Paper PDF for: $title.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Send PDF via Email"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error launching email app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdfViaWhatsApp(context: Context, pdfFile: File, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Question Paper: $title")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp is not installed. Opening general share...", Toast.LENGTH_SHORT).show()
            sharePdf(context, pdfFile, title)
        }
    }

    fun generateLiveTestReportPdf(
        context: Context,
        candidates: List<CandidateSession>,
        subject: String,
        durationMinutes: Int
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val widthPt = 595 // A4 width
            val heightPt = 842 // A4 height
            val marginPt = 36
            val printableWidth = widthPt - (marginPt * 2)

            val titlePaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 15f
                color = Color.BLACK
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 9.5f
                color = Color.DKGRAY
                isAntiAlias = true
            }

            val tableHeaderPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 9.5f
                color = Color.BLACK
                isAntiAlias = true
            }

            val tableCellPaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 9.5f
                color = Color.BLACK
                isAntiAlias = true
            }

            val cardTitlePaint = Paint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 11.5f
                color = Color.BLACK
                isAntiAlias = true
            }

            val borderPaint = Paint().apply {
                color = Color.GRAY
                strokeWidth = 0.8f
                style = Paint.Style.STROKE
            }

            val fillHeaderPaint = Paint().apply {
                color = Color.parseColor("#F5F5F5")
                style = Paint.Style.FILL
            }

            val fillCardPaint = Paint().apply {
                color = Color.parseColor("#FAFAFA")
                style = Paint.Style.FILL
            }

            // Sort candidates by score descending (Merit List)
            val sortedCandidates = candidates.sortedWith(
                compareByDescending<CandidateSession> { it.score }
                    .thenBy { it.rollNumber }
            )

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(widthPt, heightPt, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var currentY = marginPt.toFloat()

            fun drawFooter() {
                val footerPaint = Paint().apply {
                    typeface = Typeface.DEFAULT
                    textSize = 8f
                    color = Color.GRAY
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                val footerY = (heightPt - marginPt / 2).toFloat()
                canvas.drawText("Live Test Report | Page $pageNumber", (widthPt / 2).toFloat(), footerY, footerPaint)
            }

            fun startNewPage() {
                drawFooter()
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(widthPt, heightPt, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = marginPt.toFloat()
            }

            fun checkNewPage(neededHeight: Float) {
                if (currentY + neededHeight > heightPt - marginPt) {
                    startNewPage()
                }
            }

            // --- Draw Header ---
            checkNewPage(80f)
            canvas.drawText("LIVE SECURED TEST REPORT & MERIT LIST", marginPt.toFloat(), currentY + 18, titlePaint)
            currentY += 24
            
            val subjectStr = if (subject.isEmpty()) "All Subjects" else subject
            canvas.drawText("Subject: $subjectStr | Duration: $durationMinutes Mins | Date: ${java.time.LocalDate.now()}", marginPt.toFloat(), currentY + 12, subtitlePaint)
            currentY += 16
            canvas.drawText("Total Candidates: ${candidates.size} | Submitted: ${candidates.count { it.status == "Submitted" }}", marginPt.toFloat(), currentY + 12, subtitlePaint)
            currentY += 28

            // Draw divider
            canvas.drawLine(marginPt.toFloat(), currentY, (widthPt - marginPt).toFloat(), currentY, borderPaint)
            currentY += 16

            // --- Merit List Table ---
            checkNewPage(40f)
            canvas.drawText("PART A: CANDIDATE MERIT LIST", marginPt.toFloat(), currentY + 14, cardTitlePaint)
            currentY += 22

            // Table headers
            checkNewPage(24f)
            val colX = floatArrayOf(
                marginPt.toFloat(),               // Rank (0)
                marginPt.toFloat() + 45f,         // Name (1)
                marginPt.toFloat() + 155f,        // Roll No (2)
                marginPt.toFloat() + 245f,        // Status (3)
                marginPt.toFloat() + 325f,        // Score (4)
                marginPt.toFloat() + 395f,        // % (5)
                marginPt.toFloat() + 445f         // Warnings (6)
            )

            canvas.drawRect(marginPt.toFloat(), currentY, (widthPt - marginPt).toFloat(), currentY + 20, fillHeaderPaint)
            canvas.drawRect(marginPt.toFloat(), currentY, (widthPt - marginPt).toFloat(), currentY + 20, borderPaint)
            
            canvas.drawText("Rank", colX[0] + 5, currentY + 14, tableHeaderPaint)
            canvas.drawText("Candidate Name", colX[1] + 5, currentY + 14, tableHeaderPaint)
            canvas.drawText("Roll No", colX[2] + 5, currentY + 14, tableHeaderPaint)
            canvas.drawText("Status", colX[3] + 5, currentY + 14, tableHeaderPaint)
            canvas.drawText("Score", colX[4] + 5, currentY + 14, tableHeaderPaint)
            canvas.drawText("%", colX[5] + 5, currentY + 14, tableHeaderPaint)
            canvas.drawText("Warnings", colX[6] + 5, currentY + 14, tableHeaderPaint)
            currentY += 20

            // Draw rows
            sortedCandidates.forEachIndexed { index, candidate ->
                checkNewPage(20f)
                canvas.drawRect(marginPt.toFloat(), currentY, (widthPt - marginPt).toFloat(), currentY + 20, borderPaint)
                
                val percentage = if (candidate.totalMarks > 0) {
                    (candidate.score.toFloat() / candidate.totalMarks.toFloat() * 100f)
                } else 0f
                val percentStr = String.format(java.util.Locale.US, "%.1f%%", percentage)

                canvas.drawText("#${index + 1}", colX[0] + 5, currentY + 14, tableCellPaint)
                
                // Truncate candidate name to fit if too long
                var dispName = candidate.name
                if (dispName.length > 18) {
                    dispName = dispName.take(16) + ".."
                }
                canvas.drawText(dispName, colX[1] + 5, currentY + 14, tableCellPaint)
                canvas.drawText(candidate.rollNumber, colX[2] + 5, currentY + 14, tableCellPaint)
                canvas.drawText(candidate.status, colX[3] + 5, currentY + 14, tableCellPaint)
                canvas.drawText("${candidate.score} / ${candidate.totalMarks}", colX[4] + 5, currentY + 14, tableCellPaint)
                canvas.drawText(percentStr, colX[5] + 5, currentY + 14, tableCellPaint)
                canvas.drawText("${candidate.warningCount} / 3", colX[6] + 5, currentY + 14, tableCellPaint)
                currentY += 20
            }

            currentY += 25

            // --- Candidate Scorecards ---
            startNewPage()
            canvas.drawText("PART B: INDIVIDUAL SCORECARDS", marginPt.toFloat(), currentY + 14, cardTitlePaint)
            currentY += 24

            sortedCandidates.forEach { candidate ->
                checkNewPage(120f)
                // Draw card container
                canvas.drawRect(marginPt.toFloat(), currentY, (widthPt - marginPt).toFloat(), currentY + 105, fillCardPaint)
                canvas.drawRect(marginPt.toFloat(), currentY, (widthPt - marginPt).toFloat(), currentY + 105, borderPaint)

                // Left column info
                canvas.drawText("Candidate: ${candidate.name}", marginPt.toFloat() + 15, currentY + 20, tableHeaderPaint)
                canvas.drawText("Roll Number: ${candidate.rollNumber}", marginPt.toFloat() + 15, currentY + 38, tableCellPaint)
                
                val loginDateTime = java.time.Instant.ofEpochMilli(candidate.loginTime)
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                canvas.drawText("Login Time: $loginDateTime", marginPt.toFloat() + 15, currentY + 56, tableCellPaint)
                canvas.drawText("Warnings Logged: ${candidate.warningCount} / 3", marginPt.toFloat() + 15, currentY + 74, tableCellPaint)
                canvas.drawText("Status: ${candidate.status}", marginPt.toFloat() + 15, currentY + 92, tableCellPaint)

                // Right column score box
                val rightBoxLeft = widthPt - marginPt - 180f
                canvas.drawLine(rightBoxLeft, currentY, rightBoxLeft, currentY + 105, borderPaint)
                
                val percentage = if (candidate.totalMarks > 0) {
                    (candidate.score.toFloat() / candidate.totalMarks.toFloat() * 100f)
                } else 0f
                val percentStr = String.format(java.util.Locale.US, "%.1f%%", percentage)

                canvas.drawText("GRAND SCORECARD", rightBoxLeft + 15, currentY + 20, tableHeaderPaint)
                canvas.drawText("Obtained: ${candidate.score} Marks", rightBoxLeft + 15, currentY + 42, tableCellPaint)
                canvas.drawText("Max Marks: ${candidate.totalMarks} Marks", rightBoxLeft + 15, currentY + 60, tableCellPaint)
                canvas.drawText("Percentage: $percentStr", rightBoxLeft + 15, currentY + 78, tableHeaderPaint)

                val verdict = if (candidate.status == "Disqualified") {
                    "DISQUALIFIED"
                } else if (candidate.score >= candidate.totalMarks * 0.4) {
                    "PASSED"
                } else {
                    "FAILED"
                }
                canvas.drawText("Verdict: $verdict", rightBoxLeft + 15, currentY + 94, tableHeaderPaint)

                currentY += 120
            }

            drawFooter()
            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "live_test_merit_list.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }
}

