package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_attempts")
data class TestAttemptEntity(
    @PrimaryKey
    val id: String,
    val paperId: String,
    val paperTitle: String,
    val candidateName: String,
    val score: Int,
    val maxMarks: Int,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val timestamp: Long = System.currentTimeMillis()
)
