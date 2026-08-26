package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "papers")
data class PaperEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val subject: String,
    val durationMinutes: Int,
    val totalMarks: Int,
    val questionIdsJson: String, // JSON array string of question IDs
    val createdAt: Long = System.currentTimeMillis()
)
