package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class LiveTestConfig(
    val subject: String = "",
    val mcqCount: Int = 10,
    val fibCount: Int = 0,
    val tfCount: Int = 0,
    val durationMinutes: Int = 30
)

@Serializable
data class CandidateSession(
    val id: String,
    val name: String,
    val rollNumber: String,
    val email: String = "",
    val mobile: String = "",
    var portraitBase64: String = "",
    var latestFrameBase64: String = "",
    var activeWarningMessage: String = "",
    val loginTime: Long,
    var status: String = "Testing", // "Testing" or "Submitted"
    var questionsJson: String = "[]", // Serialized list of QuestionDto assigned
    var answersJson: String = "{}", // Serialized map of questionId -> studentAnswer
    var score: Int = 0,
    var totalMarks: Int = 0,
    var isDispatched: Boolean = false,
    var warningCount: Int = 0
)

object LiveTestState {
    fun updateFrame(rollNumber: String, frameBase64: String): String {
        var msg = ""
        val list = _candidates.value.map {
            if (it.rollNumber == rollNumber) {
                msg = it.activeWarningMessage
                it.copy(latestFrameBase64 = frameBase64, activeWarningMessage = "")
            } else {
                it
            }
        }
        _candidates.value = list
        return msg
    }
    
    fun setWarning(rollNumber: String, message: String) {
        val list = _candidates.value.map {
            if (it.rollNumber == rollNumber) it.copy(activeWarningMessage = message) else it
        }
        _candidates.value = list
    }

    var config = LiveTestConfig()
    
    private val _candidates = MutableStateFlow<List<CandidateSession>>(emptyList())
    val candidates: StateFlow<List<CandidateSession>> = _candidates
    
    fun addCandidate(candidate: CandidateSession) {
        val list = _candidates.value.toMutableList()
        list.removeAll { it.rollNumber == candidate.rollNumber } // Overwrite existing session if they re-login
        list.add(candidate)
        _candidates.value = list
    }
    
    fun updateCandidateStatus(rollNumber: String, status: String, answersJson: String, score: Int, totalMarks: Int, warnings: Int) {
        val list = _candidates.value.map {
            if (it.rollNumber == rollNumber) {
                it.copy(
                    status = status,
                    answersJson = answersJson,
                    score = score,
                    totalMarks = totalMarks,
                    warningCount = warnings
                )
            } else {
                it
            }
        }
        _candidates.value = list
    }

    fun updateWarnings(rollNumber: String, warnings: Int) {
        val list = _candidates.value.map {
            if (it.rollNumber == rollNumber) {
                it.copy(warningCount = warnings)
            } else {
                it
            }
        }
        _candidates.value = list
    }

    fun dispatchMarksheet(rollNumber: String) {
        val list = _candidates.value.map {
            if (it.rollNumber == rollNumber) {
                it.copy(isDispatched = true)
            } else {
                it
            }
        }
        _candidates.value = list
    }
    
    fun clearSessions() {
        _candidates.value = emptyList()
    }
}
