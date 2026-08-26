package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val bookTitle: String,
    val chapter: String,
    val type: String, // "mcq", "subjective", "fib", "tf"
    val difficulty: String, // "easy", "medium", "hard"
    val question: String,
    val optionsJson: String, // JSON array string e.g. ["Option A", "Option B"]
    val answer: String,
    val explanation: String,
    val marks: Int,
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
