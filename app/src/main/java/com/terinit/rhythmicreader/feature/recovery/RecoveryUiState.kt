package com.terinit.rhythmicreader.feature.recovery

import com.terinit.rhythmicreader.domain.model.RecoveryStatus

data class RecoveryUiState(
    val sessionId: String? = null,
    val status: RecoveryStatus? = null,
    val activeSeconds: Long = 0,
    val requiredActiveSeconds: Long = 0,
    val qualifiedPages: Int = 0,
    val requiredQualifiedPages: Int = 0
) {
    val isSessionActive: Boolean
        get() = status == RecoveryStatus.ACTIVE

    val isSessionComplete: Boolean
        get() = status == RecoveryStatus.COMPLETE

    val activeMinutesDisplay: String
        get() {
            val activeMin = activeSeconds / 60
            val reqMin = (requiredActiveSeconds + 59) / 60
            return "$activeMin / $reqMin min"
        }

    val qualifiedPagesDisplay: String
        get() = "$qualifiedPages / $requiredQualifiedPages"

    val activeTimeProgress: Float
        get() = if (requiredActiveSeconds <= 0) {
            0f
        } else {
            (activeSeconds.toFloat() / requiredActiveSeconds).coerceIn(0f, 1f)
        }

    val pageProgress: Float
        get() = if (requiredQualifiedPages <= 0) {
            0f
        } else {
            (qualifiedPages.toFloat() / requiredQualifiedPages).coerceIn(0f, 1f)
        }
}
