package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class ViolationEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "TAB_SWITCH", "FULLSCREEN_EXIT", "DEVTOOLS_ATTEMPT", "CLIPBOARD_BLOCKED", "WINDOW_BLUR"
    val details: String = ""
)

@Serializable
data class LiveTestConfig(
    val examName: String = "Online Secured Exam",
    val subject: String = "",
    val mcqCount: Int = 10,
    val fibCount: Int = 0,
    val tfCount: Int = 0,
    val durationMinutes: Int = 30,
    val maxStrikes: Int = 3,
    val cameraProctoringEnabled: Boolean = true,
    val strictTabLock: Boolean = true,
    val blockClipboard: Boolean = true
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
    var forceAction: String = "", // "TERMINATE", "PARDON", or ""
    val loginTime: Long,
    var status: String = "Testing", // "Testing", "Submitted", "Disqualified"
    var questionsJson: String = "[]", // Serialized list of QuestionDto assigned
    var answersJson: String = "{}", // Serialized map of questionId -> studentAnswer
    var score: Int = 0,
    var totalMarks: Int = 0,
    var isDispatched: Boolean = false,
    var warningCount: Int = 0,
    var violationsJson: String = "[]" // Serialized list of ViolationEvent
)

@Serializable
data class HeartbeatResponse(
    val warningMessage: String = "",
    val forceAction: String = "",
    val warningCount: Int = 0,
    val maxStrikes: Int = 3,
    val status: String = "Testing"
)

object LiveTestState {
    var config = LiveTestConfig()
    
    private val _candidates = MutableStateFlow<List<CandidateSession>>(emptyList())
    val candidates: StateFlow<List<CandidateSession>> = _candidates
    
    fun addCandidate(candidate: CandidateSession) {
        val list = _candidates.value.toMutableList()
        list.removeAll { it.rollNumber == candidate.rollNumber } // Overwrite existing session if they re-login
        list.add(candidate)
        _candidates.value = list
    }

    fun updateFrameAndGetStatus(rollNumber: String, frameBase64: String): HeartbeatResponse {
        var resp = HeartbeatResponse(maxStrikes = config.maxStrikes)
        val list = _candidates.value.map {
            if (it.rollNumber == rollNumber) {
                val warningMsg = it.activeWarningMessage
                val action = it.forceAction
                resp = HeartbeatResponse(
                    warningMessage = warningMsg,
                    forceAction = action,
                    warningCount = it.warningCount,
                    maxStrikes = config.maxStrikes,
                    status = it.status
                )
                it.copy(
                    latestFrameBase64 = if (frameBase64.isNotEmpty()) frameBase64 else it.latestFrameBase64,
                    activeWarningMessage = "",
                    forceAction = ""
                )
            } else {
                it
            }
        }
        _candidates.value = list
        return resp
    }
    
    fun setWarning(rollNumber: String, message: String) {
        val list = _candidates.value.map {
            if (it.rollNumber == rollNumber) it.copy(activeWarningMessage = message) else it
        }
        _candidates.value = list
    }

    fun pardonCandidate(rollNumber: String) {
        val list = _candidates.value.map {
            if (it.rollNumber == rollNumber) {
                it.copy(
                    warningCount = 0,
                    activeWarningMessage = "Supervisor has pardoned your security strikes. Please continue carefully.",
                    forceAction = "PARDON",
                    status = if (it.status == "Disqualified") "Testing" else it.status
                )
            } else {
                it
            }
        }
        _candidates.value = list
    }

    fun forceDisqualify(rollNumber: String) {
        val list = _candidates.value.map {
            if (it.rollNumber == rollNumber) {
                it.copy(
                    status = "Disqualified",
                    forceAction = "TERMINATE",
                    activeWarningMessage = "Supervisor has terminated your exam session."
                )
            } else {
                it
            }
        }
        _candidates.value = list
    }

    fun recordViolation(rollNumber: String, type: String, details: String): Int {
        var updatedWarnings = 0
        val list = _candidates.value.map { candidate ->
            if (candidate.rollNumber == rollNumber) {
                val currentViolations = try {
                    Json.decodeFromString<List<ViolationEvent>>(candidate.violationsJson).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
                currentViolations.add(ViolationEvent(System.currentTimeMillis(), type, details))
                val newCount = candidate.warningCount + 1
                updatedWarnings = newCount
                val newStatus = if (newCount >= config.maxStrikes) "Disqualified" else candidate.status
                candidate.copy(
                    warningCount = newCount,
                    status = newStatus,
                    violationsJson = Json.encodeToString(currentViolations)
                )
            } else {
                candidate
            }
        }
        _candidates.value = list
        return updatedWarnings
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

