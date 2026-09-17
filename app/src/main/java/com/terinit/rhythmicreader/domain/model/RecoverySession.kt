package com.terinit.rhythmicreader.domain.model

data class RecoverySession(
    val sessionId: String,
    val requirement: RecoveryRequirement,
    val accumulatedActiveMs: Long,
    val qualifiedPages: Int,
    val status: RecoveryStatus,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
    val expiresAtEpochMs: Long? = null
) {
    val activeSeconds: Long
        get() = accumulatedActiveMs / 1000L

    val isComplete: Boolean
        get() = status == RecoveryStatus.COMPLETE

    fun toProgress(): RecoveryProgress = RecoveryProgress(
        activeSeconds = activeSeconds,
        qualifiedPages = qualifiedPages,
        status = status
    )
}
