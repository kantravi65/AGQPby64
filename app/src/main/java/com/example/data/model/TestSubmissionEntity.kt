package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "test_submissions")
data class TestSubmissionEntity(
    @PrimaryKey
    val id: String,
    val paperId: String?,
    val paperTitle: String,
    val candidateName: String,
    val candidateRollNumber: String,
    val candidateEmail: String,
    val candidateMobile: String,
    val portraitBase64: String,
    val status: String,
    val questionsJson: String,
    val answersJson: String,
    val score: Int,
    val maxMarks: Int,
    val warningCount: Int,
    val violationsJson: String = "[]",
    val proctorRemarks: String = "",
    val disputeStatus: String = "None", // "None", "Under Review", "Resolved", "Disqualified", "Pardoned"
    val isResultDeclared: Boolean = false,
    val rank: Int = 0,
    val evaluatedBy: String = "",
    val loginTime: Long,
    val submitTime: Long = System.currentTimeMillis()
)
