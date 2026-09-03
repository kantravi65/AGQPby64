package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BookEntity
import com.example.data.model.PaperEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.TestAttemptEntity
import com.example.data.repository.OtsRepository
import com.example.util.WebServerManager
import com.example.util.WebServerState
import com.example.service.WebServerService
import com.example.util.DatabaseSharingManager
import com.example.util.SharedDatabaseItem
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class DatabaseImportResult(
    val success: Boolean,
    val booksImported: Int = 0,
    val questionsImported: Int = 0,
    val papersImported: Int = 0,
    val errorMessage: String? = null
)

class OtsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager by lazy { com.example.util.SettingsManager(application) }
    private var autoSyncJob: kotlinx.coroutines.Job? = null
    
    val webServerUrl: StateFlow<String?> = WebServerState.url
    val webServerHttpUrl: StateFlow<String?> = WebServerState.httpUrl
    val webServerPublicUrl: StateFlow<String?> = WebServerState.publicUrl

    var publicTunnelUrl: String
        get() = settingsManager.publicTunnelUrl
        set(value) {
            settingsManager.publicTunnelUrl = value
            WebServerState.setPublicUrl(value)
        }

    init {
        if (settingsManager.publicTunnelUrl.isNotBlank()) {
            WebServerState.setPublicUrl(settingsManager.publicTunnelUrl)
        }
    }

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

    private val _selectedLivePaperId = MutableStateFlow<String?>(null)
    val selectedLivePaperId: StateFlow<String?> = _selectedLivePaperId.asStateFlow()

    // Expose candidates session list from LiveTestState
    val liveCandidates: StateFlow<List<com.example.util.CandidateSession>> = com.example.util.LiveTestState.candidates

    var liveStartTimeInput = androidx.compose.runtime.mutableStateOf("")

    fun updateLiveTestConfig(
        examName: String,
        subject: String,
        mcqs: Int,
        fibs: Int,
        tfs: Int,
        duration: Int,
        startTimeInput: String = "",
        keepPaper: Boolean = false
    ) {
        if (!keepPaper) {
            _selectedLivePaperId.value = null // Deselect paper since user edited manually
        }
        _liveTestExamName.value = examName
        _liveTestSubject.value = subject
        _liveTestMcqCount.value = mcqs
        _liveTestFibCount.value = fibs
        _liveTestTfCount.value = tfs
        _liveTestDuration.value = duration
        liveStartTimeInput.value = startTimeInput
        
        var startTimeMillis = 0L
        try {
            if (startTimeInput.isNotBlank()) {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                startTimeMillis = sdf.parse(startTimeInput)?.time ?: 0L
            }
        } catch (e: Exception) {
            startTimeMillis = 0L
        }
        
        com.example.util.LiveTestState.config = com.example.util.LiveTestConfig(
            examName = examName,
            subject = subject,
            mcqCount = mcqs,
            fibCount = fibs,
            tfCount = tfs,
            durationMinutes = duration,
            startTimeMillis = startTimeMillis,
            paperId = if (keepPaper) _selectedLivePaperId.value else null
        )
    }

    fun selectPaperForLiveTest(paper: PaperEntity?) {
        _selectedLivePaperId.value = paper?.id
        if (paper != null) {
            _liveTestExamName.value = paper.title
            _liveTestSubject.value = paper.subject
            _liveTestMcqCount.value = 0
            _liveTestFibCount.value = 0
            _liveTestTfCount.value = 0
            _liveTestDuration.value = paper.durationMinutes
            
            var startTimeMillis = 0L
            try {
                if (liveStartTimeInput.value.isNotBlank()) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    startTimeMillis = sdf.parse(liveStartTimeInput.value)?.time ?: 0L
                }
            } catch (e: Exception) {
                startTimeMillis = 0L
            }
            
            com.example.util.LiveTestState.config = com.example.util.LiveTestConfig(
                examName = paper.title,
                subject = paper.subject,
                mcqCount = 0,
                fibCount = 0,
                tfCount = 0,
                durationMinutes = paper.durationMinutes,
                startTimeMillis = startTimeMillis,
                paperId = paper.id
            )
        } else {
            _liveTestMcqCount.value = 10
            _liveTestFibCount.value = 0
            _liveTestTfCount.value = 0
            
            var startTimeMillis = 0L
            try {
                if (liveStartTimeInput.value.isNotBlank()) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    startTimeMillis = sdf.parse(liveStartTimeInput.value)?.time ?: 0L
                }
            } catch (e: Exception) {
                startTimeMillis = 0L
            }
            
            com.example.util.LiveTestState.config = com.example.util.LiveTestConfig(
                examName = _liveTestExamName.value,
                subject = _liveTestSubject.value,
                mcqCount = 10,
                fibCount = 0,
                tfCount = 0,
                durationMinutes = _liveTestDuration.value,
                startTimeMillis = startTimeMillis,
                paperId = null
            )
        }
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

    fun dispatchAllCompletedCandidates(context: android.content.Context) {
        val candidates = com.example.util.LiveTestState.candidates.value
        val toDispatch = candidates.filter { it.status != "Testing" && !it.isDispatched }
        toDispatch.forEach { c ->
            com.example.util.LiveTestState.dispatchMarksheet(c.rollNumber)
            if (c.mobile.isNotEmpty()) {
                try {
                    val msg = "Exam Result: Dear ${c.name} (Roll: ${c.rollNumber}), score ${c.score}/${c.totalMarks}. Status: ${c.status}."
                    val smsManager = android.telephony.SmsManager.getDefault()
                    val parts = smsManager.divideMessage(msg)
                    smsManager.sendMultipartTextMessage(c.mobile, null, parts, null, null)
                } catch (e: Exception) {
                    android.util.Log.e("OtsViewModel", "SMS error for ${c.rollNumber}", e)
                }
            }
        }
        android.widget.Toast.makeText(context, "Dispatched ${toDispatch.size} candidate marksheet(s)!", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun resolveDispute(submissionId: String, newScore: Int, proctorRemarks: String, disputeStatus: String) {
        viewModelScope.launch {
            val sub = repository.getSubmissionById(submissionId)
            if (sub != null) {
                repository.updateSubmission(sub.copy(
                    score = newScore,
                    proctorRemarks = proctorRemarks,
                    disputeStatus = disputeStatus,
                    evaluatedBy = "${settingsManager.activeSupervisorName} (${settingsManager.activeSupervisorRole})"
                ))
            }
        }
    }

    fun declareAllResults(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val subs = repository.allSubmissions.first()
            val toDeclare = subs
                .filter { it.status != "In-Progress" }
                .sortedWith(
                    compareByDescending<com.example.data.model.TestSubmissionEntity> { it.score }
                        .thenBy { it.warningCount }
                        .thenBy { it.submitTime }
                )
            toDeclare.forEachIndexed { index, s ->
                repository.updateSubmission(s.copy(
                    isResultDeclared = true,
                    rank = index + 1,
                    evaluatedBy = "${settingsManager.activeSupervisorName} (${settingsManager.activeSupervisorRole})"
                ))
            }
            onComplete?.invoke(toDeclare.size)
        }
    }

    fun dispatchSubmissionResultSms(context: android.content.Context, sub: com.example.data.model.TestSubmissionEntity) {
        if (sub.candidateMobile.isBlank()) {
            android.widget.Toast.makeText(context, "No mobile number recorded for candidate.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val rankText = if (sub.rank > 0) " Rank: #${sub.rank}." else ""
            val msg = "Exam Result: Dear ${sub.candidateName} (Roll: ${sub.candidateRollNumber}), score ${sub.score}/${sub.maxMarks}.$rankText Status: ${sub.status}."
            val smsManager = android.telephony.SmsManager.getDefault()
            val parts = smsManager.divideMessage(msg)
            smsManager.sendMultipartTextMessage(sub.candidateMobile, null, parts, null, null)
            android.widget.Toast.makeText(context, "SMS sent to ${sub.candidateRollNumber}!", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Failed to send SMS: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun dispatchAllDeclaredResultsSms(context: android.content.Context) {
        viewModelScope.launch {
            val declared = repository.allSubmissions.first().filter { it.isResultDeclared && it.candidateMobile.isNotBlank() }
            var count = 0
            declared.forEach { sub ->
                try {
                    val rankText = if (sub.rank > 0) " Rank: #${sub.rank}." else ""
                    val msg = "Exam Result: Dear ${sub.candidateName} (Roll: ${sub.candidateRollNumber}), score ${sub.score}/${sub.maxMarks}.$rankText Status: ${sub.status}."
                    val smsManager = android.telephony.SmsManager.getDefault()
                    val parts = smsManager.divideMessage(msg)
                    smsManager.sendMultipartTextMessage(sub.candidateMobile, null, parts, null, null)
                    count++
                } catch (e: Exception) {
                    android.util.Log.e("OtsViewModel", "SMS error for ${sub.candidateRollNumber}", e)
                }
            }
            android.widget.Toast.makeText(context, "Sent results SMS to $count candidates!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun generateSubmissionsMeritGazette(context: android.content.Context, examTitle: String = "Online Exam") {
        viewModelScope.launch {
            val subs = repository.allSubmissions.first().filter { it.status != "In-Progress" }
            val file = com.example.util.PdfPrintUtils.generateSubmissionsMeritGazettePdf(context, subs, examTitle)
            if (file != null) {
                android.widget.Toast.makeText(context, "Merit Gazette Generated: ${file.name}", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Failed to generate Merit Gazette.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun generateCandidateMarksheetPdf(context: android.content.Context, sub: com.example.data.model.TestSubmissionEntity) {
        viewModelScope.launch {
            val file = com.example.util.PdfPrintUtils.generateCandidateMarksheetPdf(context, sub)
            if (file != null) {
                android.widget.Toast.makeText(context, "Marksheet Generated: ${file.name}", android.widget.Toast.LENGTH_SHORT).show()
                com.example.util.PdfPrintUtils.viewPdf(context, file)
            } else {
                android.widget.Toast.makeText(context, "Failed to generate Marksheet.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteSubmission(id: String) {
        viewModelScope.launch {
            repository.deleteSubmission(id)
        }
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
    val testAttempts: StateFlow<List<TestAttemptEntity>>
    val testSubmissions: StateFlow<List<com.example.data.model.TestSubmissionEntity>>

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
            database.testAttemptDao(),
            database.testSubmissionDao()
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

        testAttempts = repository.allAttempts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        testSubmissions = repository.allSubmissions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        // Database Cloud Sharing State
        if (settingsManager.isGoogleSignedIn && settingsManager.googleAccountEmail.isNotBlank()) {
            initSharingListeners(settingsManager.googleAccountEmail)
        }

        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            questions.collect { qList ->
                syncSubjectsFromQuestionsList(qList)
            }
        }
        startAutoSyncLoop()
    }

    // --- DATABASE CLOUD SHARING STATE & FLOWS ---
    private val _receivedShares = MutableStateFlow<List<SharedDatabaseItem>>(emptyList())
    val receivedShares: StateFlow<List<SharedDatabaseItem>> = _receivedShares.asStateFlow()

    private val _sentShares = MutableStateFlow<List<SharedDatabaseItem>>(emptyList())
    val sentShares: StateFlow<List<SharedDatabaseItem>> = _sentShares.asStateFlow()

    val unreadReceivedSharesCount: StateFlow<Int> = _receivedShares.map { list ->
        list.count { it.status == "shared" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var sharingJob: kotlinx.coroutines.Job? = null

    fun initSharingListeners(userEmail: String) {
        val clean = userEmail.trim().lowercase()
        if (clean.isBlank()) return
        sharingJob?.cancel()
        sharingJob = viewModelScope.launch {
            launch {
                DatabaseSharingManager.getReceivedSharesFlow(clean).collect {
                    _receivedShares.value = it
                }
            }
            launch {
                DatabaseSharingManager.getSentSharesFlow(clean).collect {
                    _sentShares.value = it
                }
            }
        }
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

    fun exportFullDatabaseBundle(selectedBookIds: Set<String>? = null): Triple<String, Int, Int> {
        val allB = books.value
        val allQ = questions.value
        val allP = papers.value

        val targetBooks = if (selectedBookIds != null) allB.filter { it.id in selectedBookIds } else allB
        val targetBookTitles = targetBooks.map { it.title.trim().lowercase() }.toSet()
        val targetBookIds = targetBooks.map { it.id }.toSet()

        val targetQuestions = if (selectedBookIds != null) {
            allQ.filter { it.bookId in targetBookIds || it.bookTitle.trim().lowercase() in targetBookTitles }
        } else {
            allQ
        }
        val targetQuestionIds = targetQuestions.map { it.id }.toSet()

        val targetPapers = if (selectedBookIds != null) {
            allP.filter { paper ->
                try {
                    val qIds = JSONArray(paper.questionIdsJson)
                    var hasMatch = false
                    for (i in 0 until qIds.length()) {
                        if (qIds.getString(i) in targetQuestionIds) {
                            hasMatch = true
                            break
                        }
                    }
                    hasMatch
                } catch (_: Exception) { false }
            }
        } else {
            allP
        }

        val root = JSONObject()
        root.put("version", 1)
        root.put("type", "database_bundle")
        root.put("exportedAt", System.currentTimeMillis())

        val booksArr = JSONArray()
        targetBooks.forEach { b ->
            val bObj = JSONObject()
            bObj.put("id", b.id)
            bObj.put("title", b.title)
            bObj.put("chapterCount", b.chapterCount)
            booksArr.put(bObj)
        }
        root.put("books", booksArr)

        val questionsArr = JSONArray()
        targetQuestions.forEach { q ->
            val qObj = JSONObject()
            qObj.put("id", q.id)
            qObj.put("bookId", q.bookId)
            qObj.put("bookTitle", q.bookTitle)
            qObj.put("chapter", q.chapter)
            qObj.put("type", q.type)
            qObj.put("difficulty", q.difficulty)
            qObj.put("question", q.question)
            qObj.put("optionsJson", q.optionsJson)
            qObj.put("answer", q.answer)
            qObj.put("explanation", q.explanation)
            qObj.put("marks", q.marks)
            qObj.put("isBookmarked", q.isBookmarked)
            qObj.put("createdAt", q.createdAt)
            questionsArr.put(qObj)
        }
        root.put("questions", questionsArr)

        val papersArr = JSONArray()
        targetPapers.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("title", p.title)
            pObj.put("subject", p.subject)
            pObj.put("durationMinutes", p.durationMinutes)
            pObj.put("totalMarks", p.totalMarks)
            pObj.put("questionIdsJson", p.questionIdsJson)
            pObj.put("createdAt", p.createdAt)
            papersArr.put(pObj)
        }
        root.put("papers", papersArr)

        return Triple(root.toString(2), targetQuestions.size, targetPapers.size)
    }

    suspend fun importFullDatabaseBundle(jsonStr: String): DatabaseImportResult {
        return try {
            val trimmed = jsonStr.trim()
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                return DatabaseImportResult(false, 0, 0, 0, "Invalid JSON data")
            }

            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                if (root.has("type") && root.getString("type") == "database_bundle") {
                    // Import bundle
                    var booksCount = 0
                    val booksArr = root.optJSONArray("books") ?: JSONArray()
                    for (i in 0 until booksArr.length()) {
                        val bObj = booksArr.getJSONObject(i)
                        val title = bObj.optString("title", "").trim()
                        if (title.isNotBlank()) {
                            val id = bObj.optString("id", "b_" + UUID.randomUUID().toString().take(8))
                            val chapters = bObj.optInt("chapterCount", 5)
                            val book = BookEntity(id, title, chapters)
                            repository.insertBook(book)
                            booksCount++
                        }
                    }

                    val questionsArr = root.optJSONArray("questions") ?: JSONArray()
                    val rawQuestionsJson = questionsArr.toString()
                    val qImportPair = importQuestionsFromJson(rawQuestionsJson)

                    var papersCount = 0
                    val papersArr = root.optJSONArray("papers") ?: JSONArray()
                    for (i in 0 until papersArr.length()) {
                        val pObj = papersArr.getJSONObject(i)
                        val title = pObj.optString("title", "").trim()
                        if (title.isNotBlank()) {
                            val id = pObj.optString("id", "p_" + UUID.randomUUID().toString().take(8))
                            val subject = pObj.optString("subject", "General")
                            val duration = pObj.optInt("durationMinutes", 30)
                            val totalMarks = pObj.optInt("totalMarks", 0)
                            val qIds = pObj.optString("questionIdsJson", "[]")
                            val paper = PaperEntity(id, title, subject, duration, totalMarks, qIds)
                            repository.insertPaper(paper)
                            papersCount++
                        }
                    }

                    onDatabaseChanged()
                    return DatabaseImportResult(true, booksCount, qImportPair.second, papersCount)
                }
            }

            // Fallback to importing standard questions
            val qRes = importQuestionsFromJson(jsonStr)
            if (qRes.first) {
                onDatabaseChanged()
                return DatabaseImportResult(true, 0, qRes.second, 0)
            } else {
                return DatabaseImportResult(false, 0, 0, 0, "Could not parse question data")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return DatabaseImportResult(false, 0, 0, 0, e.localizedMessage ?: e.message)
        }
    }

    fun shareDatabaseWithUser(
        senderEmail: String,
        senderName: String,
        recipientEmail: String,
        title: String,
        description: String,
        selectedBookIds: Set<String>? = null,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val bundle = exportFullDatabaseBundle(selectedBookIds)
                val jsonPayload = bundle.first
                val qCount = bundle.second
                val pCount = bundle.third
                val allB = books.value
                val bCount = if (selectedBookIds != null) allB.count { it.id in selectedBookIds } else allB.size

                val res = DatabaseSharingManager.shareDatabasePackage(
                    senderEmail = senderEmail,
                    senderName = senderName,
                    recipientEmail = recipientEmail,
                    title = title,
                    description = description,
                    payloadJson = jsonPayload,
                    questionsCount = qCount,
                    booksCount = bCount,
                    papersCount = pCount
                )

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    res.fold(
                        onSuccess = { shareId ->
                            onResult(true, "Database successfully shared with $recipientEmail!")
                        },
                        onFailure = { err ->
                            onResult(false, "Sharing failed: ${err.localizedMessage ?: err.message}")
                        }
                    )
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Error: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    fun markShareImported(shareId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            DatabaseSharingManager.markShareAsImported(shareId)
        }
    }

    fun deleteOrRevokeShare(shareId: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val res = DatabaseSharingManager.deleteShare(shareId)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult?.invoke(res.isSuccess)
            }
        }
    }

    fun exportAllDataToJson(): String {
        val rootObj = JSONObject()
        rootObj.put("questions", JSONArray(exportQuestionsToJson()))
        
        val booksArr = JSONArray()
        books.value.forEach { b ->
            val o = JSONObject()
            o.put("id", b.id)
            o.put("title", b.title)
            o.put("chapterCount", b.chapterCount)
            booksArr.put(o)
        }
        rootObj.put("books", booksArr)
        
        val papersArr = JSONArray()
        papers.value.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id)
            o.put("title", p.title)
            o.put("subject", p.subject)
            o.put("durationMinutes", p.durationMinutes)
            o.put("totalMarks", p.totalMarks)
            o.put("questionIdsJson", p.questionIdsJson)
            papersArr.put(o)
        }
        rootObj.put("papers", papersArr)
        
        val attemptsArr = JSONArray()
        testAttempts.value.forEach { a ->
            val o = JSONObject()
            o.put("id", a.id)
            o.put("paperId", a.paperId)
            o.put("paperTitle", a.paperTitle)
            o.put("candidateName", a.candidateName)
            o.put("score", a.score)
            o.put("maxMarks", a.maxMarks)
            o.put("totalQuestions", a.totalQuestions)
            o.put("correctAnswers", a.correctAnswers)
            o.put("timestamp", a.timestamp)
            attemptsArr.put(o)
        }
        rootObj.put("testAttempts", attemptsArr)
        
        val submissionsArr = JSONArray()
        testSubmissions.value.forEach { s ->
            val o = JSONObject()
            o.put("id", s.id)
            o.put("paperId", s.paperId)
            o.put("paperTitle", s.paperTitle)
            o.put("candidateName", s.candidateName)
            o.put("candidateRollNumber", s.candidateRollNumber)
            o.put("candidateEmail", s.candidateEmail)
            o.put("candidateMobile", s.candidateMobile)
            o.put("portraitBase64", s.portraitBase64)
            o.put("status", s.status)
            o.put("questionsJson", s.questionsJson)
            o.put("answersJson", s.answersJson)
            o.put("score", s.score)
            o.put("maxMarks", s.maxMarks)
            o.put("warningCount", s.warningCount)
            o.put("loginTime", s.loginTime)
            o.put("submitTime", s.submitTime)
            submissionsArr.put(o)
        }
        rootObj.put("testSubmissions", submissionsArr)
        
        return rootObj.toString(2)
    }

    suspend fun importAllDataFromJson(jsonStr: String): Pair<Boolean, Int> {
        return try {
            val rootObj = JSONObject(jsonStr)
            var totalImported = 0
            
            if (rootObj.has("questions")) {
                val qArr = rootObj.getJSONArray("questions")
                val (qSuccess, qCount) = importQuestionsFromJson(qArr.toString())
                if (qSuccess) totalImported += qCount
            }
            
            if (rootObj.has("books")) {
                val booksArr = rootObj.getJSONArray("books")
                for (i in 0 until booksArr.length()) {
                    val o = booksArr.getJSONObject(i)
                    repository.insertBook(BookEntity(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        chapterCount = o.optInt("chapterCount", 0)
                    ))
                    totalImported++
                }
            }
            
            if (rootObj.has("papers")) {
                val papersArr = rootObj.getJSONArray("papers")
                for (i in 0 until papersArr.length()) {
                    val o = papersArr.getJSONObject(i)
                    repository.insertPaper(PaperEntity(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        subject = o.optString("subject", ""),
                        durationMinutes = o.optInt("durationMinutes", 30),
                        totalMarks = o.optInt("totalMarks", 0),
                        questionIdsJson = o.optString("questionIdsJson", "[]")
                    ))
                    totalImported++
                }
            }
            
            if (rootObj.has("testAttempts")) {
                val attemptsArr = rootObj.getJSONArray("testAttempts")
                for (i in 0 until attemptsArr.length()) {
                    val o = attemptsArr.getJSONObject(i)
                    repository.recordTestAttempt(com.example.data.model.TestAttemptEntity(
                        id = o.getString("id"),
                        paperId = o.getString("paperId"),
                        paperTitle = o.getString("paperTitle"),
                        candidateName = o.getString("candidateName"),
                        score = o.optInt("score", 0),
                        maxMarks = o.optInt("maxMarks", 0),
                        totalQuestions = o.optInt("totalQuestions", 0),
                        correctAnswers = o.optInt("correctAnswers", 0),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    ))
                    totalImported++
                }
            }
            
            if (rootObj.has("testSubmissions")) {
                val subArr = rootObj.getJSONArray("testSubmissions")
                for (i in 0 until subArr.length()) {
                    val o = subArr.getJSONObject(i)
                    repository.insertSubmission(com.example.data.model.TestSubmissionEntity(
                        id = o.getString("id"),
                        paperId = o.optString("paperId", null),
                        paperTitle = o.getString("paperTitle"),
                        candidateName = o.getString("candidateName"),
                        candidateRollNumber = o.getString("candidateRollNumber"),
                        candidateEmail = o.optString("candidateEmail", ""),
                        candidateMobile = o.optString("candidateMobile", ""),
                        portraitBase64 = o.optString("portraitBase64", ""),
                        status = o.getString("status"),
                        questionsJson = o.optString("questionsJson", "[]"),
                        answersJson = o.optString("answersJson", "{}"),
                        score = o.optInt("score", 0),
                        maxMarks = o.optInt("maxMarks", 0),
                        warningCount = o.optInt("warningCount", 0),
                        loginTime = o.optLong("loginTime", System.currentTimeMillis()),
                        submitTime = o.optLong("submitTime", System.currentTimeMillis())
                    ))
                    totalImported++
                }
            }
            
            Pair(true, totalImported)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to importing just questions if the root is an array
            importQuestionsFromJson(jsonStr)
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

    fun startWebServer(
        mode: String = "admin",
        adminUser: String = settingsManager.webAdminUser,
        adminPass: String = settingsManager.webAdminPass
    ) {
        settingsManager.webAdminUser = adminUser
        settingsManager.webAdminPass = adminPass
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
        val app = getApplication<Application>()
        val intent = Intent(app, WebServerService::class.java).apply {
            action = WebServerService.ACTION_STOP
        }
        try {
            app.startService(intent)
        } catch (e: Exception) {
            app.stopService(intent)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
