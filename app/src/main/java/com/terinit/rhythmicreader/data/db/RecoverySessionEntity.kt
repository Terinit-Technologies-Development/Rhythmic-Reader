package com.terinit.rhythmicreader.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "recovery_sessions"
)
data class RecoverySessionEntity(
    @PrimaryKey
    val sessionId: String,
    val requiredActiveSeconds: Long,
    val requiredQualifiedPages: Int,
    val accumulatedActiveMs: Long,
    val status: String,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val expiresAtEpochMs: Long?,
    val protocolVersion: Int = 1
)
