package com.terinit.rhythmicreader.data.repository

import com.terinit.rhythmicreader.domain.model.RecoveryRequirement
import com.terinit.rhythmicreader.domain.model.RecoverySession
import com.terinit.rhythmicreader.integration.rhythmic.RecoveryRequest
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface RecoveryRepository {

    suspend fun createSession(
        requirement: RecoveryRequirement,
        sessionId: String = UUID.randomUUID().toString()
    ): RecoverySession

    suspend fun acceptExternalRequest(
        request: RecoveryRequest
    ): RecoverySession?

    suspend fun getSession(
        sessionId: String
    ): RecoverySession?

    suspend fun getActiveSession(): RecoverySession?

    suspend fun updateActiveTime(
        sessionId: String,
        activeMs: Long
    )

    suspend fun qualifyPage(
        sessionId: String,
        bookId: String,
        pageIndex: Int
    ): Boolean

    suspend fun markComplete(
        sessionId: String
    )

    fun observeSession(
        sessionId: String
    ): Flow<RecoverySession?>

    fun observeActiveSession(): Flow<RecoverySession?>
}
