package com.example.util

import android.content.Context
import android.util.Xml
import com.example.data.model.QuestionEntity
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object DocxXlsxHelper {

    // --- TEMPLATE GENERATION ---

    fun generateDocxTemplate(outputStream: OutputStream) {
        val zipOut = ZipOutputStream(outputStream)

        // 1. [Content_Types].xml
        zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
        val contentTypes = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()
        zipOut.write(contentTypes.toByteArray())
        zipOut.closeEntry()

        // 2. _rels/.rels
        zipOut.putNextEntry(ZipEntry("_rels/.rels"))
        val rels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent()
        zipOut.write(rels.toByteArray())
        zipOut.closeEntry()

        // 3. word/document.xml
        zipOut.putNextEntry(ZipEntry("word/document.xml"))
        val documentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>OTS QUESTION BANK WORD TEMPLATE</w:t></w:r></w:p>
                <w:p><w:r><w:t>Instructions: Use the tags [Question], [Subject], [Chapter], [Type], [Difficulty], [Options], [Answer], [Explanation], [Marks] to define each question. Leave a blank line between questions.</w:t></w:r></w:p>
                <w:p><w:r><w:t></w:t></w:r></w:p>
                <w:p><w:r><w:t>[Question] What is the speed of light in vacuum?</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Subject] Physics Fundamentals</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Chapter] Optics &amp; Light</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Type] mcq</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Difficulty] medium</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Options] 3 x 10^8 m/s|1.5 x 10^8 m/s|3 x 10^6 m/s|300 m/s</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Answer] 3 x 10^8 m/s</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Explanation] In vacuum, electromagnetic waves travel at approximately 299,792,458 m/s.</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Marks] 2</w:t></w:r></w:p>
                <w:p><w:r><w:t></w:t></w:r></w:p>
                <w:p><w:r><w:t>[Question] Sound waves can travel through a complete vacuum.</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Subject] Physics Fundamentals</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Chapter] Acoustics</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Type] tf</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Difficulty] easy</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Options] True|False</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Answer] False</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Explanation] Sound requires a material medium to propagate.</w:t></w:r></w:p>
                <w:p><w:r><w:t>[Marks] 1</w:t></w:r></w:p>
              </w:body>
            </w:document>
        """.trimIndent()
        zipOut.write(documentXml.toByteArray())
        zipOut.closeEntry()

        zipOut.close()
    }

    fun generateXlsxTemplate(outputStream: OutputStream) {
        val zipOut = ZipOutputStream(outputStream)

        // 1. [Content_Types].xml
        zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
        val contentTypes = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
        """.trimIndent()
        zipOut.write(contentTypes.toByteArray())
        zipOut.closeEntry()

        // 2. _rels/.rels
        zipOut.putNextEntry(ZipEntry("_rels/.rels"))
        val rels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent()
        zipOut.write(rels.toByteArray())
        zipOut.closeEntry()

        // 3. xl/_rels/workbook.xml.rels
        zipOut.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        val workbookRels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            </Relationships>
        """.trimIndent()
        zipOut.write(workbookRels.toByteArray())
        zipOut.closeEntry()

        // 4. xl/workbook.xml
        zipOut.putNextEntry(ZipEntry("xl/workbook.xml"))
        val workbookXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheets>
                <sheet name="Questions" sheetId="1" r:id="rId1" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
              </sheets>
            </workbook>
        """.trimIndent()
        zipOut.write(workbookXml.toByteArray())
        zipOut.closeEntry()

        // 5. xl/worksheets/sheet1.xml
        zipOut.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val sheet1Xml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
                <row r="1">
                  <c r="A1" t="inlineStr"><is><t>Question</t></is></c>
                  <c r="B1" t="inlineStr"><is><t>BookTitle</t></is></c>
                  <c r="C1" t="inlineStr"><is><t>Chapter</t></is></c>
                  <c r="D1" t="inlineStr"><is><t>Type</t></is></c>
                  <c r="E1" t="inlineStr"><is><t>Difficulty</t></is></c>
                  <c r="F1" t="inlineStr"><is><t>Options</t></is></c>
                  <c r="G1" t="inlineStr"><is><t>Answer</t></is></c>
                  <c r="H1" t="inlineStr"><is><t>Explanation</t></is></c>
                  <c r="I1" t="inlineStr"><is><t>Marks</t></is></c>
                </row>
                <row r="2">
                  <c r="A2" t="inlineStr"><is><t>What is the speed of light in vacuum?</t></is></c>
                  <c r="B2" t="inlineStr"><is><t>Physics Fundamentals</t></is></c>
                  <c r="C2" t="inlineStr"><is><t>Optics &amp; Light</t></is></c>
                  <c r="D2" t="inlineStr"><is><t>mcq</t></is></c>
                  <c r="E2" t="inlineStr"><is><t>medium</t></is></c>
                  <c r="F2" t="inlineStr"><is><t>3 x 10^8 m/s|1.5 x 10^8 m/s|3 x 10^6 m/s|300 m/s</t></is></c>
                  <c r="G2" t="inlineStr"><is><t>3 x 10^8 m/s</t></is></c>
                  <c r="H2" t="inlineStr"><is><t>In vacuum, light travels at approx 3 x 10^8 m/s.</t></is></c>
                  <c r="I2"><v>2</v></c>
                </row>
                <row r="3">
                  <c r="A3" t="inlineStr"><is><t>The acceleration due to gravity on Earth is approximately ___ m/s².</t></is></c>
                  <c r="B3" t="inlineStr"><is><t>Physics Fundamentals</t></is></c>
                  <c r="C3" t="inlineStr"><is><t>Gravitation</t></is></c>
                  <c r="D3" t="inlineStr"><is><t>fib</t></is></c>
                  <c r="E3" t="inlineStr"><is><t>easy</t></is></c>
                  <c r="F3" t="inlineStr"><is></is></c>
                  <c r="G3" t="inlineStr"><is><t>9.8</t></is></c>
                  <c r="H3" t="inlineStr"><is><t>Standard gravity is defined as exactly 9.8 m/s².</t></is></c>
                  <c r="I3"><v>1</v></c>
                </row>
              </sheetData>
            </worksheet>
        """.trimIndent()
        zipOut.write(sheet1Xml.toByteArray())
        zipOut.closeEntry()

        zipOut.close()
    }


    // --- WORD (.docx) PARSING ---

    fun parseDocx(inputStream: InputStream): List<QuestionEntity> {
        val paragraphs = mutableListOf<String>()
        val zipIn = ZipInputStream(inputStream)
        var entry = zipIn.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                paragraphs.addAll(extractParagraphsFromDocumentXml(zipIn))
                break
            }
            entry = zipIn.nextEntry
        }
        zipIn.close()

        return parseQuestionBlocks(paragraphs)
    }

    private fun extractParagraphsFromDocumentXml(inputStream: InputStream): List<String> {
        val list = mutableListOf<String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType
            var currentParagraph = java.lang.StringBuilder()
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        if (name == "p") {
                            currentParagraph = java.lang.StringBuilder()
                        } else if (name == "t") {
                            currentParagraph.append(parser.nextText())
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name
                        if (name == "p") {
                            list.add(currentParagraph.toString().trim())
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseQuestionBlocks(paragraphs: List<String>): List<QuestionEntity> {
        val importedList = mutableListOf<QuestionEntity>()
        var currentMap = mutableMapOf<String, String>()

        fun buildQuestionFromMap() {
            val qText = currentMap["question"]?.trim() ?: ""
            if (qText.isNotBlank()) {
                val bookTitle = currentMap["subject"]?.ifBlank { "General Subject" } ?: "General Subject"
                val chapter = currentMap["chapter"]?.ifBlank { "Chapter 1" } ?: "Chapter 1"
                val type = currentMap["type"]?.ifBlank { "mcq" } ?: "mcq"
                val difficulty = currentMap["difficulty"]?.ifBlank { "medium" } ?: "medium"
                val optionsRaw = currentMap["options"] ?: ""
                val optionsArray = if (optionsRaw.contains("|")) {
                    val optsList = optionsRaw.split("|").map { it.trim() }
                    JSONArray(optsList).toString()
                } else if (optionsRaw.startsWith("[")) {
                    optionsRaw
                } else if (optionsRaw.isNotBlank()) {
                    JSONArray(listOf(optionsRaw)).toString()
                } else "[]"

                val answer = currentMap["answer"] ?: ""
                val explanation = currentMap["explanation"] ?: ""
                val marks = currentMap["marks"]?.toIntOrNull() ?: 1

                val q = QuestionEntity(
                    id = "q_" + UUID.randomUUID().toString().take(8),
                    bookId = "b1",
                    bookTitle = bookTitle,
                    chapter = chapter,
                    type = type,
                    difficulty = difficulty,
                    question = qText,
                    optionsJson = optionsArray,
                    answer = answer,
                    explanation = explanation,
                    marks = marks,
                    isBookmarked = false
                )
                importedList.add(q)
            }
            currentMap = mutableMapOf()
        }

        for (p in paragraphs) {
            val clean = p.trim()
            if (clean.isBlank()) {
                continue
            }

            var matched = false
            val tags = listOf("question", "subject", "chapter", "type", "difficulty", "options", "answer", "explanation", "marks")
            for (tag in tags) {
                if (clean.startsWith("[$tag]", ignoreCase = true)) {
                    if (tag == "question" && currentMap.containsKey("question")) {
                        // We found a new question block before a blank line, build previous
                        buildQuestionFromMap()
                    }
                    val value = clean.substring(("[$tag]").length).trim()
                    currentMap[tag] = value
                    matched = true
                    break
                }
            }

            if (!matched && currentMap.containsKey("question")) {
                // If it doesn't start with a tag but we are in a block, append to question text or relevant field
                val currentQ = currentMap["question"] ?: ""
                currentMap["question"] = "$currentQ\n$clean".trim()
            }
        }

        // Build the last one
        buildQuestionFromMap()

        return importedList
    }


    // --- EXCEL (.xlsx) PARSING ---

    fun parseXlsx(inputStream: InputStream): List<QuestionEntity> {
        var sharedStrings = emptyList<String>()
        val zipIn = ZipInputStream(inputStream)
        val files = mutableMapOf<String, ByteArray>()
        var entry = zipIn.nextEntry
        while (entry != null) {
            if (entry.name == "xl/sharedStrings.xml" || entry.name == "xl/worksheets/sheet1.xml") {
                val bos = ByteArrayOutputStream()
                val buf = ByteArray(1024)
                var len: Int
                while (zipIn.read(buf).also { len = it } != -1) {
                    bos.write(buf, 0, len)
                }
                files[entry.name] = bos.toByteArray()
            }
            entry = zipIn.nextEntry
        }
        zipIn.close()

        val sharedStringsBytes = files["xl/sharedStrings.xml"]
        if (sharedStringsBytes != null) {
            sharedStrings = parseSharedStrings(sharedStringsBytes.inputStream())
        }

        val sheetBytes = files["xl/worksheets/sheet1.xml"] ?: return emptyList()
        val rows = parseSheet(sheetBytes.inputStream(), sharedStrings)

        return parseXlsxRows(rows)
    }

    private fun parseSharedStrings(inputStream: InputStream): List<String> {
        val list = mutableListOf<String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType
            var inT = false
            var currentStr = java.lang.StringBuilder()
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "t") {
                            inT = true
                            currentStr = java.lang.StringBuilder()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inT) {
                            currentStr.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "t") {
                            list.add(currentStr.toString())
                            inT = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun parseSheet(inputStream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val sheetRows = mutableListOf<List<String>>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType

            var currentRow = mutableMapOf<Int, String>()
            var currentCellCol = -1
            var currentCellType = ""
            var inV = false
            var inIsT = false
            var currentVal = java.lang.StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        if (name == "row") {
                            currentRow = mutableMapOf()
                        } else if (name == "c") {
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            currentCellCol = colLetterToIndex(ref.filter { it.isLetter() })
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            currentVal = java.lang.StringBuilder()
                        } else if (name == "v") {
                            inV = true
                        } else if (name == "t") {
                            inIsT = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inV || inIsT) {
                            currentVal.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name
                        if (name == "v") {
                            inV = false
                        } else if (name == "t") {
                            inIsT = false
                        } else if (name == "c") {
                            val raw = currentVal.toString()
                            var finalVal = raw
                            if (currentCellType == "s") {
                                val idx = raw.toIntOrNull() ?: -1
                                if (idx in sharedStrings.indices) {
                                    finalVal = sharedStrings[idx]
                                }
                            }
                            if (currentCellCol >= 0) {
                                currentRow[currentCellCol] = finalVal
                            }
                        } else if (name == "row") {
                            val maxCol = if (currentRow.keys.isNotEmpty()) currentRow.keys.maxOrNull() ?: 0 else -1
                            val rowList = ArrayList<String>(maxCol + 1)
                            for (c in 0..maxCol) {
                                rowList.add(currentRow[c] ?: "")
                            }
                            sheetRows.add(rowList)
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sheetRows
    }

    private fun colLetterToIndex(colLetter: String): Int {
        var index = 0
        for (i in colLetter.indices) {
            index = index * 26 + (colLetter[i].uppercaseChar() - 'A' + 1)
        }
        return index - 1
    }

    private fun parseXlsxRows(rows: List<List<String>>): List<QuestionEntity> {
        if (rows.size <= 1) return emptyList()
        val importedList = mutableListOf<QuestionEntity>()

        // Check if first row is headers
        val startRow = if (rows[0].getOrNull(0)?.lowercase()?.contains("question") == true) 1 else 0

        for (i in startRow until rows.size) {
            val row = rows[i]
            val questionText = row.getOrNull(0) ?: ""
            if (questionText.isBlank()) continue

            val bookTitle = row.getOrNull(1)?.ifBlank { "General Subject" } ?: "General Subject"
            val chapter = row.getOrNull(2)?.ifBlank { "Chapter 1" } ?: "Chapter 1"
            val type = row.getOrNull(3)?.ifBlank { "mcq" } ?: "mcq"
            val difficulty = row.getOrNull(4)?.ifBlank { "medium" } ?: "medium"

            val optionsRaw = row.getOrNull(5) ?: ""
            val optionsArray = if (optionsRaw.contains("|")) {
                val optsList = optionsRaw.split("|").map { it.trim() }
                JSONArray(optsList).toString()
            } else if (optionsRaw.startsWith("[")) {
                optionsRaw
            } else if (optionsRaw.isNotBlank()) {
                JSONArray(listOf(optionsRaw)).toString()
            } else "[]"

            val answer = row.getOrNull(6) ?: ""
            val explanation = row.getOrNull(7) ?: ""
            val marks = row.getOrNull(8)?.toIntOrNull() ?: 1

            val q = QuestionEntity(
                id = "q_" + UUID.randomUUID().toString().take(8),
                bookId = "b1",
                bookTitle = bookTitle,
                chapter = chapter,
                type = type,
                difficulty = difficulty,
                question = questionText,
                optionsJson = optionsArray,
                answer = answer,
                explanation = explanation,
                marks = marks,
                isBookmarked = false
            )
            importedList.add(q)
        }
        return importedList
    }
}
