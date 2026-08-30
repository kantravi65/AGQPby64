package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BookEntity
import com.example.data.model.PaperEntity
import com.example.data.model.QuestionEntity
import com.example.data.repository.OtsRepository
import com.example.util.WebServerManager
import com.example.util.WebServerState
import com.example.service.WebServerService
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class OtsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager by lazy { com.example.util.SettingsManager(application) }
    private var autoSyncJob: kotlinx.coroutines.Job? = null
    
    val webServerUrl: StateFlow<String?> = WebServerState.url

    // Live Test Configuration State
    private val _liveTestExamName = MutableStateFlow("Online Secured Exam")
    val liveTestExamName: StateFlow<String> = _liveTestExamName.asStateFlow()
    private val _liveTestSubject = MutableStateFlow("")
    val liveTestSubject: StateFlow<String> = _liveTestSubject.asStateFlow()

    private val _liveTestMcqCount = MutableStateFlow(10)
    val liveTestMcqCount: StateFlow<Int> = _liveTestMcqCount.asStateFlow()

    private val _liveTestFibCount = MutableStateFlow(0)
    val liveTestFibCount: StateFlow<Int> = _liveTestFibCount.asStateFlow()

    private val _liveTestTfCount = MutableStateFlow(0)
    val liveTestTfCount: StateFlow<Int> = _liveTestTfCount.asStateFlow()

    private val _liveTestDuration = MutableStateFlow(30)
    val liveTestDuration: StateFlow<Int> = _liveTestDuration.asStateFlow()

    // Expose candidates session list from LiveTestState
    val liveCandidates: StateFlow<List<com.example.util.CandidateSession>> = com.example.util.LiveTestState.candidates

    fun updateLiveTestConfig(examName: String, subject: String, mcqs: Int, fibs: Int, tfs: Int, duration: Int) {
        _liveTestExamName.value = examName
        _liveTestSubject.value = subject
        _liveTestMcqCount.value = mcqs
        _liveTestFibCount.value = fibs
        _liveTestTfCount.value = tfs
        _liveTestDuration.value = duration
        
        com.example.util.LiveTestState.config = com.example.util.LiveTestConfig(
            examName = examName,
            subject = subject,
            mcqCount = mcqs,
            fibCount = fibs,
            tfCount = tfs,
            durationMinutes = duration
        )
    }
    
    fun generateMeritListPdf(context: android.content.Context) {
        val candidates = com.example.util.LiveTestState.candidates.value
        val config = com.example.util.LiveTestState.config
        val pdf = com.example.util.PdfPrintUtils.generateLiveTestReportPdf(context, candidates, config.subject, config.durationMinutes)
        if (pdf != null) {
            android.widget.Toast.makeText(context, "Merit List Generated!", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Failed to generate Merit List.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun dispatchCandidateMarksheet(rollNumber: String) {
        com.example.util.LiveTestState.dispatchMarksheet(rollNumber)
    }

    fun clearLiveTestSessions() {
        com.example.util.LiveTestState.clearSessions()
    }

    private val repository: OtsRepository
    private val bookSyncMutex = kotlinx.coroutines.sync.Mutex()
    private var lastManuallyAddedBookId: String? = null

    val questions: StateFlow<List<QuestionEntity>>
    val books: StateFlow<List<BookEntity>>
    val papers: StateFlow<List<PaperEntity>>

    // Search and filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBookFilter = MutableStateFlow<String?>(null)
    val selectedBookFilter: StateFlow<String?> = _selectedBookFilter.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<String?>(null)
    val selectedTypeFilter: StateFlow<String?> = _selectedTypeFilter.asStateFlow()

    private val _selectedDifficultyFilter = MutableStateFlow<String?>(null)
    val selectedDifficultyFilter: StateFlow<String?> = _selectedDifficultyFilter.asStateFlow()

    private val _showBookmarkedOnly = MutableStateFlow(false)
    val showBookmarkedOnly: StateFlow<Boolean> = _showBookmarkedOnly.asStateFlow()

    // Practice / Flashcard mode state
    private val _isPracticeMode = MutableStateFlow(false)
    val isPracticeMode: StateFlow<Boolean> = _isPracticeMode.asStateFlow()

    private val _practiceIndex = MutableStateFlow(0)
    val practiceIndex: StateFlow<Int> = _practiceIndex.asStateFlow()

    private val _userSelectedOption = MutableStateFlow<String?>(null)
    val userSelectedOption: StateFlow<String?> = _userSelectedOption.asStateFlow()

    private val _showPracticeExplanation = MutableStateFlow(false)
    val showPracticeExplanation: StateFlow<Boolean> = _showPracticeExplanation.asStateFlow()

    private val _practiceAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val practiceAnswers: StateFlow<Map<Int, String>> = _practiceAnswers.asStateFlow()

    private val _practiceQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val practiceQuestions: StateFlow<List<QuestionEntity>> = _practiceQuestions.asStateFlow()

    private val _isAppLocked = MutableStateFlow<Boolean?>(null)
    val isAppLocked: StateFlow<Boolean?> = _isAppLocked.asStateFlow()

    val isAppLockEnabledVal: Boolean
        get() = settingsManager.isAppLockEnabled

    fun setAppLocked(locked: Boolean) {
        _isAppLocked.value = locked
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = OtsRepository(
            database.questionDao(),
            database.bookDao(),
            database.paperDao(),
            database.testAttemptDao()
        )

        questions = repository.allQuestions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        books = repository.allBooks.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        papers = repository.allPapers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            questions.collect { qList ->
                syncSubjectsFromQuestionsList(qList)
            }
        }
        startAutoSyncLoop()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setBookFilter(bookId: String?) {
        _selectedBookFilter.value = bookId
    }

    fun setTypeFilter(type: String?) {
        _selectedTypeFilter.value = type
    }

    fun setDifficultyFilter(difficulty: String?) {
        _selectedDifficultyFilter.value = difficulty
    }

    fun toggleBookmarkedOnly() {
        _showBookmarkedOnly.value = !_showBookmarkedOnly.value
    }

    fun addQuestion(
        bookId: String,
        bookTitle: String,
        chapter: String,
        type: String,
        difficulty: String,
        questionText: String,
        options: List<String>,
        answer: String,
        explanation: String,
        marks: Int
    ) {
        viewModelScope.launch {
            val jsonArray = JSONArray()
            options.forEach { jsonArray.put(it) }

            val question = QuestionEntity(
                id = "q_" + UUID.randomUUID().toString().take(8),
                bookId = bookId,
                bookTitle = bookTitle,
                chapter = chapter,
                type = type,
                difficulty = difficulty,
                question = questionText,
                optionsJson = jsonArray.toString(),
                answer = answer,
                explanation = explanation,
                marks = marks
            )
            repository.insertQuestion(question)
            onDatabaseChanged()
        }
    }

    fun updateQuestion(
        id: String,
        bookId: String,
        bookTitle: String,
        chapter: String,
        type: String,
        difficulty: String,
        questionText: String,
        options: List<String>,
        answer: String,
        explanation: String,
        marks: Int,
        isBookmarked: Boolean
    ) {
        viewModelScope.launch {
            val jsonArray = JSONArray()
            options.forEach { jsonArray.put(it) }

            val question = QuestionEntity(
                id = id,
                bookId = bookId,
                bookTitle = bookTitle,
                chapter = chapter,
                type = type,
                difficulty = difficulty,
                question = questionText,
                optionsJson = jsonArray.toString(),
                answer = answer,
                explanation = explanation,
                marks = marks,
                isBookmarked = isBookmarked
            )
            repository.updateQuestion(question)
            
            // Sync with practice mode state
            _practiceQuestions.value = _practiceQuestions.value.map { 
                if (it.id == question.id) question else it 
            }
            
            onDatabaseChanged()
        }
    }

    fun toggleBookmark(question: QuestionEntity) {
        viewModelScope.launch {
            repository.toggleBookmark(question)
            
            val updated = question.copy(isBookmarked = !question.isBookmarked)
            _practiceQuestions.value = _practiceQuestions.value.map { 
                if (it.id == question.id) updated else it 
            }
        }
    }

    fun batchUpdateSubject(questionIds: List<String>, bookId: String, bookTitle: String) {
        viewModelScope.launch {
            questionIds.forEach { qId ->
                val q = questions.value.find { it.id == qId }
                if (q != null) {
                    val updatedQ = q.copy(bookId = bookId, bookTitle = bookTitle)
                    repository.updateQuestion(updatedQ)
                }
            }
        }
    }

    fun batchUpdateSubjectWithNewBook(questionIds: List<String>, newBookTitle: String) {
        viewModelScope.launch {
            val bookId = "b_" + UUID.randomUUID().toString().take(8)
            val newBook = BookEntity(id = bookId, title = newBookTitle, chapterCount = 0)
            repository.insertBook(newBook)
            
            questionIds.forEach { qId ->
                val q = questions.value.find { it.id == qId }
                if (q != null) {
                    val updatedQ = q.copy(bookId = bookId, bookTitle = newBookTitle)
                    repository.updateQuestion(updatedQ)
                }
            }
        }
    }

    fun deleteQuestion(questionId: String) {
        viewModelScope.launch {
            repository.deleteQuestion(questionId)
            onDatabaseChanged()
            onDatabaseChanged()
        }
    }

    fun softDeleteQuestion(questionId: String, settingsManager: com.example.util.SettingsManager) {
        viewModelScope.launch {
            val q = questions.value.find { it.id == questionId }
            if (q != null) {
                try {
                    val currentBinArr = try { JSONArray(settingsManager.recycleBinJson) } catch (e: Exception) { JSONArray() }
                    val qObj = JSONObject().apply {
                        put("id", q.id)
                        put("bookId", q.bookId)
                        put("bookTitle", q.bookTitle)
                        put("chapter", q.chapter)
                        put("type", q.type)
                        put("difficulty", q.difficulty)
                        put("question", q.question)
                        put("optionsJson", q.optionsJson)
                        put("answer", q.answer)
                        put("explanation", q.explanation)
                        put("marks", q.marks)
                        put("isBookmarked", q.isBookmarked)
                        put("deletedTimestamp", System.currentTimeMillis())
                    }
                    currentBinArr.put(qObj)
                    settingsManager.recycleBinJson = currentBinArr.toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            repository.deleteQuestion(questionId)
            onDatabaseChanged()
        }
    }

    fun getRecycleBinQuestions(settingsManager: com.example.util.SettingsManager): List<QuestionEntity> {
        val binJsonStr = settingsManager.recycleBinJson
        if (binJsonStr.isBlank() || binJsonStr == "[]") return emptyList()
        return try {
            val arr = JSONArray(binJsonStr)
            val list = mutableListOf<QuestionEntity>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val q = QuestionEntity(
                    id = if (obj.has("id")) obj.getString("id") else "q_" + UUID.randomUUID().toString().take(8),
                    bookId = if (obj.has("bookId")) obj.getString("bookId") else "b1",
                    bookTitle = obj.optString("bookTitle", "General Subject"),
                    chapter = obj.optString("chapter", "Chapter 1"),
                    type = obj.optString("type", "mcq"),
                    difficulty = obj.optString("difficulty", "medium"),
                    question = obj.optString("question", ""),
                    optionsJson = obj.optString("optionsJson", "[]"),
                    answer = obj.optString("answer", ""),
                    explanation = obj.optString("explanation", ""),
                    marks = obj.optInt("marks", 2),
                    isBookmarked = obj.optBoolean("isBookmarked", false)
                )
                if (q.question.isNotBlank()) {
                    list.add(q)
                }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun restoreRecycleBinItems(settingsManager: com.example.util.SettingsManager, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val binQuestions = getRecycleBinQuestions(settingsManager)
                if (binQuestions.isNotEmpty()) {
                    repository.insertAllQuestions(binQuestions)
                    syncSubjectsFromQuestionsList(binQuestions)
                    settingsManager.recycleBinJson = "[]"
                    onResult(binQuestions.size)
                } else {
                    onResult(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(0)
            }
        }
    }

    fun restoreSingleRecycleBinQuestion(questionId: String, settingsManager: com.example.util.SettingsManager, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val binQuestions = getRecycleBinQuestions(settingsManager)
                val targetQ = binQuestions.find { it.id == questionId }
                if (targetQ != null) {
                    repository.insertQuestion(targetQ)
                    syncSubjectsFromQuestionsList(listOf(targetQ))
                    val updatedBin = binQuestions.filter { it.id != questionId }
                    val newArr = JSONArray()
                    updatedBin.forEach { q ->
                        val obj = JSONObject().apply {
                            put("id", q.id)
                            put("bookId", q.bookId)
                            put("bookTitle", q.bookTitle)
                            put("chapter", q.chapter)
                            put("type", q.type)
                            put("difficulty", q.difficulty)
                            put("question", q.question)
                            put("optionsJson", q.optionsJson)
                            put("answer", q.answer)
                            put("explanation", q.explanation)
                            put("marks", q.marks)
                            put("isBookmarked", q.isBookmarked)
                        }
                        newArr.put(obj)
                    }
                    settingsManager.recycleBinJson = newArr.toString()
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun createSnapshotPoint(settingsManager: com.example.util.SettingsManager) {
        val json = exportQuestionsToJson()
        settingsManager.lastBackupJson = json
        settingsManager.lastBackupTime = System.currentTimeMillis()
    }

    fun restoreSnapshotPoint(settingsManager: com.example.util.SettingsManager, onResult: (Boolean, Int) -> Unit) {
        val json = settingsManager.lastBackupJson
        if (json.isBlank()) {
            onResult(false, 0)
            return
        }
        viewModelScope.launch {
            val res = importQuestionsFromJson(json)
            onResult(res.first, res.second)
        }
    }

    fun addBook(title: String, chapterCount: Int) {
        viewModelScope.launch {
            val bookId = "b_" + UUID.randomUUID().toString().take(8)
            lastManuallyAddedBookId = bookId
            val book = BookEntity(
                id = bookId,
                title = title,
                chapterCount = chapterCount
            )
            repository.insertBook(book)
        }
    }

    fun createPaper(
        title: String,
        subject: String,
        selectedQuestions: List<QuestionEntity>,
        durationMinutes: Int
    ) {
        viewModelScope.launch {
            val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
            val sortedQuestions = selectedQuestions.sortedBy { typeOrder[it.type] ?: 5 }
            val jsonArray = JSONArray()
            sortedQuestions.forEach { jsonArray.put(it.id) }
            val totalMarks = selectedQuestions.sumOf { it.marks }

            val paper = PaperEntity(
                id = "p_" + UUID.randomUUID().toString().take(8),
                title = title,
                subject = subject,
                questionIdsJson = jsonArray.toString(),
                totalMarks = totalMarks,
                durationMinutes = durationMinutes
            )
            repository.insertPaper(paper)
        }
    }

    fun deletePaper(paperId: String) {
        viewModelScope.launch {
            repository.deletePaper(paperId)
        }
    }

    fun updatePaper(paper: PaperEntity) {
        viewModelScope.launch {
            repository.insertPaper(paper) // Or a new repository method
        }
    }

    fun startPracticeMode(questionsToUse: List<QuestionEntity> = emptyList()) {
        _isPracticeMode.value = true
        if (questionsToUse.isNotEmpty()) {
            _practiceQuestions.value = questionsToUse.shuffled()
        } else {
            val currentBookId = _selectedBookFilter.value
            val currentType = _selectedTypeFilter.value
            val allQ = questions.value
            
            val bookTitle = books.value.find { it.id == currentBookId }?.title
            
            val filtered = allQ.filter { q ->
                val matchesBook = if (currentBookId == null) {
                    true
                } else {
                    q.bookId == currentBookId ||
                    q.bookTitle.equals(currentBookId, ignoreCase = true) ||
                    (bookTitle != null && q.bookTitle.equals(bookTitle, ignoreCase = true))
                }
                val matchesType = currentType == null || q.type.equals(currentType, ignoreCase = true)
                matchesBook && matchesType
            }
            _practiceQuestions.value = filtered.shuffled()
        }
        _practiceIndex.value = 0
        _userSelectedOption.value = null
        _showPracticeExplanation.value = false
        _practiceAnswers.value = emptyMap()
    }

    fun stopPracticeMode() {
        _isPracticeMode.value = false
        _userSelectedOption.value = null
        _showPracticeExplanation.value = false
        _practiceAnswers.value = emptyMap()
    }

    fun selectPracticeOption(option: String) {
        _userSelectedOption.value = option
        _showPracticeExplanation.value = true
        _practiceAnswers.value = _practiceAnswers.value + (_practiceIndex.value to option)
    }

    fun jumpToPracticeQuestion(index: Int, totalSize: Int) {
        if (index in 0 until totalSize) {
            _practiceIndex.value = index
            val savedAnswer = _practiceAnswers.value[index]
            _userSelectedOption.value = savedAnswer
            _showPracticeExplanation.value = savedAnswer != null
        }
    }

    fun nextPracticeQuestion(totalSize: Int) {
        if (_practiceIndex.value < totalSize - 1) {
            jumpToPracticeQuestion(_practiceIndex.value + 1, totalSize)
        }
    }

    fun prevPracticeQuestion() {
        if (_practiceIndex.value > 0) {
            jumpToPracticeQuestion(_practiceIndex.value - 1, questions.value.size)
        }
    }

    fun exportQuestionsToJson(): String {
        val list = questions.value
        val jsonArray = JSONArray()
        list.forEach { q ->
            val obj = JSONObject()
            obj.put("bookTitle", q.bookTitle)
            obj.put("chapter", q.chapter)
            obj.put("type", q.type)
            obj.put("difficulty", q.difficulty)
            obj.put("question", q.question)

            val optionsArr = try {
                JSONArray(q.optionsJson)
            } catch (e: Exception) {
                JSONArray()
            }
            obj.put("options", optionsArr)
            obj.put("answer", q.answer)
            obj.put("explanation", q.explanation)
            obj.put("marks", q.marks)

            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    fun exportQuestionsToCsv(): String {
        val list = questions.value
        val sb = StringBuilder()
        // CSV Header
        sb.append("Question,BookTitle,Chapter,Type,Difficulty,Options,Answer,Explanation,Marks\n")
        list.forEach { q ->
            // Format options array as pipe-separated list e.g. "Opt1|Opt2|Opt3|Opt4"
            val optionsStr = try {
                val arr = JSONArray(q.optionsJson)
                val items = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    items.add(arr.getString(i))
                }
                items.joinToString("|")
            } catch (e: Exception) {
                ""
            }

            fun escapeCsv(text: String): String {
                val clean = text.replace("\"", "\"\"")
                return if (clean.contains(",") || clean.contains("\n") || clean.contains("\"")) {
                    "\"$clean\""
                } else {
                    clean
                }
            }

            val row = listOf(
                escapeCsv(q.question),
                escapeCsv(q.bookTitle),
                escapeCsv(q.chapter),
                escapeCsv(q.type),
                escapeCsv(q.difficulty),
                escapeCsv(optionsStr),
                escapeCsv(q.answer),
                escapeCsv(q.explanation),
                q.marks.toString()
            ).joinToString(",")
            sb.append(row).append("\n")
        }
        return sb.toString()
    }

    private suspend fun upsertDeduplicatedQuestions(incomingList: List<QuestionEntity>): Int {
        return bookSyncMutex.withLock {
            val currentQuestions = repository.allQuestions.first()
            val currentBooks = repository.allBooks.first()

            val existingById = currentQuestions.associateBy { it.id }
            val existingByNormalizedKey = currentQuestions.associateBy {
                "${it.question.trim().lowercase().replace(Regex("\\s+"), " ")}|${it.bookTitle.trim().lowercase()}|${it.type.trim().lowercase()}"
            }

            val booksByTitle = currentBooks.associateBy { it.title.trim().lowercase() }.toMutableMap()

            val toInsert = mutableListOf<QuestionEntity>()
            val toUpdate = mutableListOf<QuestionEntity>()

            incomingList.forEach { incoming ->
                val normTitle = incoming.bookTitle.trim().lowercase()
                val matchedBook = booksByTitle[normTitle]
                val finalBookId: String
                val finalBookTitle: String

                if (matchedBook != null) {
                    finalBookId = matchedBook.id
                    finalBookTitle = matchedBook.title
                } else {
                    val newBookId = "b_" + UUID.randomUUID().toString().take(8)
                    val newBookTitle = incoming.bookTitle.trim().ifBlank { "General Subject" }
                    val newBook = BookEntity(id = newBookId, title = newBookTitle, chapterCount = 5)
                    repository.insertBook(newBook)
                    booksByTitle[normTitle] = newBook
                    finalBookId = newBookId
                    finalBookTitle = newBookTitle
                }

                val normKey = "${incoming.question.trim().lowercase().replace(Regex("\\s+"), " ")}|${finalBookTitle.lowercase()}|${incoming.type.trim().lowercase()}"
                val existing = existingById[incoming.id] ?: existingByNormalizedKey[normKey]

                if (existing != null) {
                    // Update existing record rather than inserting a duplicate
                    val updated = existing.copy(
                        bookId = finalBookId,
                        bookTitle = finalBookTitle,
                        chapter = incoming.chapter.ifBlank { existing.chapter },
                        type = incoming.type.ifBlank { existing.type },
                        difficulty = incoming.difficulty.ifBlank { existing.difficulty },
                        question = incoming.question.ifBlank { existing.question },
                        optionsJson = if (incoming.optionsJson != "[]") incoming.optionsJson else existing.optionsJson,
                        answer = incoming.answer.ifBlank { existing.answer },
                        explanation = incoming.explanation.ifBlank { existing.explanation },
                        marks = incoming.marks
                    )
                    toUpdate.add(updated)
                } else {
                    val newQuestion = incoming.copy(
                        bookId = finalBookId,
                        bookTitle = finalBookTitle
                    )
                    toInsert.add(newQuestion)
                }
            }

            if (toInsert.isNotEmpty()) {
                repository.insertAllQuestions(toInsert)
            }
            toUpdate.forEach { repository.updateQuestion(it) }

            toInsert.size + toUpdate.size
        }
    }

    suspend fun importQuestionsFromJson(jsonStr: String): Pair<Boolean, Int> {
        return try {
            val trimmed = jsonStr.trim()
            val jsonArray = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else if (trimmed.startsWith("{")) {
                val rootObj = JSONObject(trimmed)
                if (rootObj.has("questions")) {
                    rootObj.getJSONArray("questions")
                } else if (rootObj.has("data")) {
                    rootObj.getJSONArray("data")
                } else {
                    JSONArray().put(rootObj)
                }
            } else {
                JSONArray()
            }

            val importedList = mutableListOf<QuestionEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val bookTitle = obj.optString("bookTitle", "General Subject").ifBlank { "General Subject" }
                val chapter = obj.optString("chapter", "Chapter 1").ifBlank { "Chapter 1" }
                val type = obj.optString("type", "mcq").ifBlank { "mcq" }
                val difficulty = obj.optString("difficulty", "medium").ifBlank { "medium" }
                val questionText = obj.optString("question", "").trim()

                if (questionText.isBlank()) continue

                val optionsJsonStr: String
                val optionsList = mutableListOf<String>()

                if (obj.has("options")) {
                    val optVal = obj.get("options")
                    if (optVal is JSONArray) {
                        for (j in 0 until optVal.length()) {
                            optionsList.add(optVal.getString(j))
                        }
                        optionsJsonStr = optVal.toString()
                    } else if (optVal is String) {
                        if (optVal.trim().startsWith("[")) {
                            optionsJsonStr = optVal
                            try {
                                val arr = JSONArray(optVal)
                                for (j in 0 until arr.length()) optionsList.add(arr.getString(j))
                            } catch (e: Exception) {}
                        } else {
                            val items = optVal.split("|").map { it.trim() }
                            val arr = JSONArray(items)
                            optionsList.addAll(items)
                            optionsJsonStr = arr.toString()
                        }
                    } else {
                        optionsJsonStr = "[]"
                    }
                } else if (obj.has("optionsJson")) {
                    optionsJsonStr = obj.optString("optionsJson", "[]")
                    try {
                        val arr = JSONArray(optionsJsonStr)
                        for (j in 0 until arr.length()) optionsList.add(arr.getString(j))
                    } catch (e: Exception) {}
                } else {
                    optionsJsonStr = "[]"
                }

                var rawAnswer = obj.optString("answer", "").trim()
                if (rawAnswer.length == 1 && rawAnswer.uppercase()[0] in 'A'..'Z' && optionsList.isNotEmpty()) {
                    val idx = rawAnswer.uppercase()[0] - 'A'
                    if (idx in 0 until optionsList.size) {
                        rawAnswer = optionsList[idx]
                    }
                }

                val q = QuestionEntity(
                    id = if (obj.has("id") && obj.getString("id").isNotBlank()) obj.getString("id") else "q_" + UUID.randomUUID().toString().take(8),
                    bookId = if (obj.has("bookId") && obj.getString("bookId").isNotBlank()) obj.getString("bookId") else "b1",
                    bookTitle = bookTitle,
                    chapter = chapter,
                    type = type,
                    difficulty = difficulty,
                    question = questionText,
                    optionsJson = optionsJsonStr,
                    answer = rawAnswer,
                    explanation = obj.optString("explanation", ""),
                    marks = obj.optInt("marks", 1),
                    isBookmarked = obj.optBoolean("isBookmarked", false)
                )
                importedList.add(q)
            }

            if (importedList.isNotEmpty()) {
                val processedCount = upsertDeduplicatedQuestions(importedList)
                Pair(true, processedCount)
            } else Pair(false, 0)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, 0)
        }
    }

    suspend fun importQuestionsFromDocx(inputStream: java.io.InputStream): Pair<Boolean, Int> {
        return try {
            val list = com.example.util.DocxXlsxHelper.parseDocx(inputStream)
            if (list.isNotEmpty()) {
                val processedCount = upsertDeduplicatedQuestions(list)
                onDatabaseChanged()
                Pair(true, processedCount)
            } else Pair(false, 0)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, 0)
        }
    }

    suspend fun importQuestionsFromXlsx(inputStream: java.io.InputStream): Pair<Boolean, Int> {
        return try {
            val list = com.example.util.DocxXlsxHelper.parseXlsx(inputStream)
            if (list.isNotEmpty()) {
                val processedCount = upsertDeduplicatedQuestions(list)
                onDatabaseChanged()
                Pair(true, processedCount)
            } else Pair(false, 0)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, 0)
        }
    }

    suspend fun importQuestionsFromCsv(csvStr: String): Pair<Boolean, Int> {
        return try {
            val lines = csvStr.lines().filter { it.isNotBlank() }
            if (lines.size <= 1) return Pair(false, 0)

            val importedList = mutableListOf<QuestionEntity>()
            val startIdx = if (lines[0].lowercase().contains("question")) 1 else 0

            for (i in startIdx until lines.size) {
                val line = lines[i]
                val tokens = parseCsvLine(line)
                if (tokens.isNotEmpty()) {
                    val questionText = tokens.getOrNull(0) ?: ""
                    if (questionText.isBlank()) continue

                    val bookTitle = tokens.getOrNull(1)?.ifBlank { "General Subject" } ?: "General Subject"
                    val chapter = tokens.getOrNull(2)?.ifBlank { "Chapter 1" } ?: "Chapter 1"
                    val type = tokens.getOrNull(3)?.ifBlank { "mcq" } ?: "mcq"
                    val difficulty = tokens.getOrNull(4)?.ifBlank { "medium" } ?: "medium"

                    val optionsRaw = tokens.getOrNull(5) ?: ""
                    val optionsArray = if (optionsRaw.contains("|")) {
                        val optsList = optionsRaw.split("|").map { it.trim() }
                        JSONArray(optsList).toString()
                    } else if (optionsRaw.startsWith("[")) {
                        optionsRaw
                    } else if (optionsRaw.isNotBlank()) {
                        JSONArray(listOf(optionsRaw)).toString()
                    } else "[]"

                    val answer = tokens.getOrNull(6) ?: ""
                    val explanation = tokens.getOrNull(7) ?: ""
                    val marks = tokens.getOrNull(8)?.toIntOrNull() ?: 1

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
            }

            if (importedList.isNotEmpty()) {
                val processedCount = upsertDeduplicatedQuestions(importedList)
                Pair(true, processedCount)
            } else Pair(false, 0)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, 0)
        }
    }

    private fun syncSubjectsFromQuestionsList(questionsList: List<QuestionEntity>) {
        viewModelScope.launch {
            try {
                bookSyncMutex.withLock {
                    val currentBooks = repository.allBooks.first()
                    
                    // Deduplicate existing duplicate books in the database
                    val seenNormTitles = mutableSetOf<String>()
                    val booksToDelete = mutableListOf<BookEntity>()
                    val uniqueBooks = mutableListOf<BookEntity>()

                    currentBooks.forEach { book ->
                        val norm = book.title.trim().lowercase()
                        if (seenNormTitles.contains(norm)) {
                            booksToDelete.add(book)
                        } else {
                            seenNormTitles.add(norm)
                            uniqueBooks.add(book)
                        }
                    }

                    booksToDelete.forEach { book ->
                        repository.deleteBook(book)
                    }

                    val existingTitles = uniqueBooks.map { it.title.lowercase().trim() }.toMutableSet()
                    val activeQuestionTitles = questionsList.map { it.bookTitle.trim().lowercase() }.toSet()

                    // 1. Insert missing subjects (case-insensitive deduplication)
                    questionsList
                        .map { it.bookTitle.trim() }
                        .filter { it.isNotBlank() }
                        .distinctBy { it.lowercase() }
                        .forEach { title ->
                            val norm = title.lowercase()
                            if (!existingTitles.contains(norm)) {
                                val newBook = BookEntity(
                                    id = "b_" + UUID.randomUUID().toString().take(8),
                                    title = title,
                                    chapterCount = 5
                                )
                                repository.insertBook(newBook)
                                existingTitles.add(norm)
                            }
                        }

                    // 2. Automatically delete subjects with 0 questions (except the last manually added book)
                    uniqueBooks.forEach { book ->
                        val norm = book.title.trim().lowercase()
                        if (!activeQuestionTitles.contains(norm) && book.id != lastManuallyAddedBookId) {
                            repository.deleteBook(book)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }

    fun getSampleQuestionsJson(): String {
        return """
[
  {
    "question": "What is the speed of light in vacuum?",
    "bookTitle": "Physics Fundamentals",
    "chapter": "Optics & Light",
    "type": "mcq",
    "difficulty": "medium",
    "options": ["3 x 10^8 m/s", "1.5 x 10^8 m/s", "3 x 10^6 m/s", "300 m/s"],
    "answer": "3 x 10^8 m/s",
    "explanation": "In vacuum, electromagnetic waves travel at approximately 299,792,458 m/s.",
    "marks": 2
  },
  {
    "question": "The acceleration due to gravity on Earth is approximately ___ m/s².",
    "bookTitle": "Physics Fundamentals",
    "chapter": "Gravitation",
    "type": "fib",
    "difficulty": "easy",
    "options": [],
    "answer": "9.8",
    "explanation": "Standard gravity is defined as exactly 9.80665 m/s².",
    "marks": 1
  },
  {
    "question": "Sound waves can travel through a complete vacuum.",
    "bookTitle": "Physics Fundamentals",
    "chapter": "Acoustics",
    "type": "tf",
    "difficulty": "easy",
    "options": ["True", "False"],
    "answer": "False",
    "explanation": "Sound requires a material medium to propagate.",
    "marks": 1
  },
  {
    "question": "Explain Newton's Second Law of Motion with mathematical derivation.",
    "bookTitle": "Physics Fundamentals",
    "chapter": "Laws of Motion",
    "type": "subjective",
    "difficulty": "hard",
    "options": [],
    "answer": "F = ma. Rate of change of momentum is directly proportional to applied force.",
    "explanation": "Key points: Force equation F=ma, vector direction, SI units in Newtons.",
    "marks": 5
  }
]
        """.trimIndent()
    }

    fun getSampleQuestionsCsv(): String {
        return """
Question,BookTitle,Chapter,Type,Difficulty,Options,Answer,Explanation,Marks
"What is the chemical formula of water?","Chemistry","Basics","mcq","easy","H2O|CO2|NaCl|O2","H2O","Water consists of H2O.",1
"The powerhouse of the cell is the ___.","Biology","Cytology","fib","easy","","Mitochondria","Mitochondria generate ATP.",1
"Light behaves both as a wave and as a particle.","Physics","Quantum Physics","tf","medium","True|False","True","Demonstrated by wave-particle duality.",1
"Describe the process of photosynthesis in detail.","Biology","Plant Physiology","subjective","hard","","Sunlight convert CO2 and H2O to glucose and O2.","Include light and dark reaction stages.",5
        """.trimIndent()
    }

    data class DuplicateGroup(
        val normalizedQuestion: String,
        val questions: List<QuestionEntity>
    )

    fun findDuplicateGroups(): List<DuplicateGroup> {
        val all = questions.value
        return all.groupBy { it.question.trim().lowercase().replace(Regex("\\s+"), " ") }
            .filter { it.value.size > 1 }
            .map { DuplicateGroup(it.key, it.value) }
    }

    fun removeDuplicates(keepStrategy: String = "bookmarked", onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val all = questions.value
            val groups = all.groupBy { it.question.trim().lowercase().replace(Regex("\\s+"), " ") }
                .filter { it.value.size > 1 }

            val idsToDelete = mutableListOf<String>()
            for ((_, list) in groups) {
                val sorted = if (keepStrategy == "bookmarked") {
                    list.sortedWith(compareByDescending<QuestionEntity> { it.isBookmarked }.thenBy { it.id })
                } else {
                    list.sortedBy { it.id }
                }
                idsToDelete.addAll(sorted.drop(1).map { it.id })
            }

            idsToDelete.forEach { id ->
                repository.deleteQuestion(id)
            }
            onComplete(idsToDelete.size)
        }
    }

    fun clearAllQuestions() {
        viewModelScope.launch {
            repository.deleteAllQuestions()
        }
    }

    fun startAutoSyncLoop() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(5000)
            while (isActive) {
                if (settingsManager.ftpAutoSync || settingsManager.isGoogleDriveSyncEnabled) {
                    triggerGlobalSync(getApplication(), settingsManager)
                }
                val intervalMins = settingsManager.autoSyncIntervalMins.coerceAtLeast(1)
                kotlinx.coroutines.delay(intervalMins * 60 * 1000L)
            }
        }
    }

    private fun onDatabaseChanged() {
        triggerGlobalSync(getApplication(), settingsManager)
    }

    fun triggerGlobalSync(context: android.content.Context, settingsManager: com.example.util.SettingsManager, onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val statusList = mutableListOf<String>()
            val jsonPayload = exportQuestionsToJson()

            // 1. Local Device Sync
            if (settingsManager.storageFolderPath.isNotBlank()) {
                try {
                    val file = java.io.File(settingsManager.storageFolderPath)
                    if (!file.exists()) file.mkdirs()
                    val backupFile = java.io.File(file, "ots_question_bank_backup.json")
                    backupFile.writeText(jsonPayload)
                    statusList.add("Local Device: Success")
                } catch (e: Exception) {
                    statusList.add("Local Device: Failed (${e.localizedMessage})")
                }
            }

            // 2. Google Drive Sync
            if (settingsManager.isGoogleSignedIn) {
                var gdDone = false
                com.example.util.GoogleDriveSyncManager.backupToDrive(context, this@OtsViewModel, settingsManager) { success, msg ->
                    if (success) {
                        statusList.add("Google Drive: Success")
                    } else {
                        statusList.add("Google Drive: Failed (${msg.take(30)})")
                    }
                    gdDone = true
                }
                var elapsed = 0
                while (!gdDone && elapsed < 50) {
                    kotlinx.coroutines.delay(100)
                    elapsed++
                }
            } else {
                statusList.add("Google Drive: Not connected")
            }

            // 3. FTP/SMB Sync
            if (settingsManager.ftpHost.isNotBlank()) {
                val res = com.example.util.NetworkStorageManager.uploadJson(
                    host = settingsManager.ftpHost.trim(),
                    port = settingsManager.ftpPort,
                    user = settingsManager.ftpUser.trim(),
                    pass = settingsManager.ftpPass,
                    remoteDir = settingsManager.ftpRemoteDir.trim(),
                    fileName = "ots_question_bank_backup.json",
                    jsonContent = jsonPayload,
                    usePassive = settingsManager.ftpUsePassive,
                    useFtps = settingsManager.ftpUseFtps
                )
                res.fold(
                    onSuccess = {
                        settingsManager.ftpLastSyncTime = System.currentTimeMillis()
                        statusList.add("Server Storage: Success")
                    },
                    onFailure = { err ->
                        statusList.add("Server Storage: Failed (${err.localizedMessage ?: err.message})")
                    }
                )
            } else {
                statusList.add("Server Storage: Not connected")
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                val finalMsg = statusList.joinToString("\n")
                onComplete?.invoke(finalMsg)
            }
        }
    }

    fun startWebServer(mode: String = "admin", adminUser: String = "admin", adminPass: String = "1234") {
        val intent = Intent(getApplication(), WebServerService::class.java).apply {
            putExtra("SERVER_MODE", mode)
            putExtra("ADMIN_USER", adminUser)
            putExtra("ADMIN_PASS", adminPass)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    fun stopWebServer() {
        val intent = Intent(getApplication(), WebServerService::class.java)
        getApplication<Application>().stopService(intent)
    }

    override fun onCleared() {
        super.onCleared()
    }
}
