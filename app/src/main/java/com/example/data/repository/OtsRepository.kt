package com.example.data.repository

import com.example.data.dao.BookDao
import com.example.data.dao.PaperDao
import com.example.data.dao.QuestionDao
import com.example.data.dao.TestAttemptDao
import com.example.data.model.BookEntity
import com.example.data.model.PaperEntity
import com.example.data.model.QuestionEntity
import com.example.data.dao.TestSubmissionDao
import com.example.data.model.TestAttemptEntity
import com.example.data.model.TestSubmissionEntity
import kotlinx.coroutines.flow.Flow

class OtsRepository(
    private val questionDao: QuestionDao,
    private val bookDao: BookDao,
    private val paperDao: PaperDao,
    private val testAttemptDao: TestAttemptDao,
    private val testSubmissionDao: TestSubmissionDao
) {
    val allQuestions: Flow<List<QuestionEntity>> = questionDao.getAllQuestions()
    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()
    val allPapers: Flow<List<PaperEntity>> = paperDao.getAllPapers()
    val allAttempts: Flow<List<TestAttemptEntity>> = testAttemptDao.getAllAttempts()
    val allSubmissions: Flow<List<TestSubmissionEntity>> = testSubmissionDao.getAllSubmissions()

    suspend fun insertSubmission(submission: TestSubmissionEntity) {
        testSubmissionDao.insertSubmission(submission)
    }

    suspend fun updateSubmission(submission: TestSubmissionEntity) {
        testSubmissionDao.updateSubmission(submission)
    }

    suspend fun getSubmissionById(id: String): TestSubmissionEntity? {
        return testSubmissionDao.getSubmissionById(id)
    }

    suspend fun getSubmissionByRollNumber(rollNumber: String): TestSubmissionEntity? {
        return testSubmissionDao.getSubmissionByRollNumber(rollNumber)
    }
    
    suspend fun deleteSubmission(id: String) {
        testSubmissionDao.deleteSubmission(id)
    }

    suspend fun insertQuestion(question: QuestionEntity) {
        questionDao.insertQuestion(question)
    }

    suspend fun updateQuestion(question: QuestionEntity) {
        questionDao.updateQuestion(question)
    }

    suspend fun toggleBookmark(question: QuestionEntity) {
        questionDao.updateQuestion(question.copy(isBookmarked = !question.isBookmarked))
    }

    suspend fun insertAllQuestions(questions: List<QuestionEntity>) {
        questionDao.insertAll(questions)
    }

    suspend fun deleteQuestion(id: String) {
        questionDao.deleteById(id)
    }

    suspend fun deleteAllQuestions() {
        questionDao.deleteAll()
    }

    suspend fun insertBook(book: BookEntity) {
        bookDao.insertBook(book)
    }

    suspend fun deleteBook(book: BookEntity) {
        bookDao.deleteBook(book)
    }

    suspend fun insertPaper(paper: PaperEntity) {
        paperDao.insertPaper(paper)
    }

    suspend fun deletePaper(id: String) {
        paperDao.deleteById(id)
    }

    suspend fun recordTestAttempt(attempt: TestAttemptEntity) {
        testAttemptDao.insertAttempt(attempt)
    }

    suspend fun seedSampleDataIfEmpty() {
        // Pre-populate sample books & questions if none exist
        val initialBooks = listOf(
            BookEntity("b1", "Mathematics - Class X", 8),
            BookEntity("b2", "Science & Physics", 10),
            BookEntity("b3", "General Knowledge & Logic", 6)
        )
        bookDao.insertAll(initialBooks)

        val sampleQuestions = listOf(
            QuestionEntity(
                id = "q1",
                bookId = "b1",
                bookTitle = "Mathematics - Class X",
                chapter = "Quadratic Equations",
                type = "mcq",
                difficulty = "medium",
                question = "What are the roots of the quadratic equation x² - 5x + 6 = 0?",
                optionsJson = "[\"x = 2, 3\", \"x = -2, -3\", \"x = 1, 6\", \"x = 0, 5\"]",
                answer = "x = 2, 3",
                explanation = "Factorizing: (x-2)(x-3) = 0 gives roots 2 and 3.",
                marks = 2
            ),
            QuestionEntity(
                id = "q2",
                bookId = "b1",
                bookTitle = "Mathematics - Class X",
                chapter = "Polynomials",
                type = "mcq",
                difficulty = "easy",
                question = "What is the degree of a linear polynomial?",
                optionsJson = "[\"1\", \"2\", \"0\", \"3\"]",
                answer = "1",
                explanation = "A linear polynomial has a highest variable exponent of 1.",
                marks = 1
            ),
            QuestionEntity(
                id = "q3",
                bookId = "b2",
                bookTitle = "Science & Physics",
                chapter = "Light & Optics",
                type = "mcq",
                difficulty = "medium",
                question = "Which type of mirror is used as a driver's side mirror in motor vehicles?",
                optionsJson = "[\"Convex mirror\", \"Concave mirror\", \"Plane mirror\", \"Cylindrical mirror\"]",
                answer = "Convex mirror",
                explanation = "Convex mirrors give a wider field of view and erect images.",
                marks = 2
            ),
            QuestionEntity(
                id = "q4",
                bookId = "b2",
                bookTitle = "Science & Physics",
                chapter = "Electricity",
                type = "tf",
                difficulty = "easy",
                question = "Ohm's Law states that current is inversely proportional to voltage at constant temperature.",
                optionsJson = "[\"True\", \"False\"]",
                answer = "False",
                explanation = "Current is DIRECTLY proportional to voltage (V = IR).",
                marks = 1
            ),
            QuestionEntity(
                id = "q5",
                bookId = "b3",
                bookTitle = "General Knowledge & Logic",
                chapter = "Logical Reasoning",
                type = "mcq",
                difficulty = "easy",
                question = "If CAT is coded as 3120, how is DOG coded in the same pattern?",
                optionsJson = "[\"4157\", \"4147\", \"4156\", \"3147\"]",
                answer = "4157",
                explanation = "D=4, O=15, G=7 -> 4157.",
                marks = 2
            )
        )
        questionDao.insertAll(sampleQuestions)

        val samplePaper = PaperEntity(
            id = "p1",
            title = "All-India Mock Exam 2026",
            subject = "Science & Math General",
            durationMinutes = 30,
            totalMarks = 8,
            questionIdsJson = "[\"q1\", \"q2\", \"q3\", \"q4\", \"q5\"]"
        )
        paperDao.insertPaper(samplePaper)
    }
}
