package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_attempts")
data class TestAttemptEntity(
    @PrimaryKey
    val id: String,
    val paperId: String = "",
    val paperTitle: String = "",
    val candidateName: String,
    val rollNumber: String = "",
    val status: String = "Submitted", // "Testing", "Submitted", "Disqualified"
    val score: Int = 0,
    val maxMarks: Int = 0,
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val warningCount: Int = 0,
    val violationsJson: String = "[]",
    val portraitBase64: String = "",
    val submittedAnswersJson: String = "{}",
    val timestamp: Long = System.currentTimeMillis()
)
