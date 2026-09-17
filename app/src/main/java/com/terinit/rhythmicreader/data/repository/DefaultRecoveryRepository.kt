package com.terinit.rhythmicreader.data.repository

import android.content.ContentResolver
import com.terinit.rhythmicreader.data.db.QualifiedPageEntity
import com.terinit.rhythmicreader.data.db.RecoveryDao
import com.terinit.rhythmicreader.data.db.RecoverySessionEntity
import com.terinit.rhythmicreader.domain.model.RecoveryRequirement
import com.terinit.rhythmicreader.domain.model.RecoverySession
import com.terinit.rhythmicreader.domain.model.RecoveryStatus
import com.terinit.rhythmicreader.integration.rhythmic.RecoveryProtocol
import com.terinit.rhythmicreader.integration.rhythmic.RecoveryRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRecoveryRepository(
    private val recoveryDao: RecoveryDao,
    private val contentResolver: ContentResolver? = null
) : RecoveryRepository {

    override suspend fun createSession(
        requirement: RecoveryRequirement,
        sessionId: String
    ): RecoverySession {
        val now = System.currentTimeMillis()
        val entity = RecoverySessionEntity(
            sessionId = sessionId,
            requiredActiveSeconds = requirement.requiredActiveSeconds,
            requiredQualifiedPages = requirement.requiredQualifiedPages,
            accumulatedActiveMs = 0L,
            status = RecoveryStatus.ACTIVE.name,
            createdAtEpochMs = now,
            completedAtEpochMs = null,
            expiresAtEpochMs = null,
            protocolVersion = 1
        )
        recoveryDao.insertSession(entity)
        notifyChange(sessionId)
        return RecoverySession(
            sessionId = sessionId,
            requirement = requirement,
            accumulatedActiveMs = 0L,
            qualifiedPages = 0,
            status = RecoveryStatus.ACTIVE,
            createdAtEpochMs = now,
            protocolVersion = 1
        )
    }

    override suspend fun acceptExternalRequest(request: RecoveryRequest): RecoverySession? {
        if (!request.isValid) return null
        val existing = recoveryDao.getSession(request.sessionId)
        if (existing != null) {
            val matches = existing.requiredActiveSeconds == request.requiredSeconds &&
                existing.requiredQualifiedPages == request.requiredPages &&
                existing.expiresAtEpochMs == request.expiresAt
            return if (matches) {
                val count = recoveryDao.qualifiedPageCount(request.sessionId)
                existing.toDomain(count)
            } else {
                null
            }
        }

        val entity = RecoverySessionEntity(
            sessionId = request.sessionId,
            requiredActiveSeconds = request.requiredSeconds,
            requiredQualifiedPages = request.requiredPages,
            accumulatedActiveMs = 0L,
            status = RecoveryStatus.ACTIVE.name,
            createdAtEpochMs = request.createdAt,
            completedAtEpochMs = null,
            expiresAtEpochMs = request.expiresAt,
            protocolVersion = request.protocolVersion
        )
        recoveryDao.insertSession(entity)
        notifyChange(request.sessionId)

        return RecoverySession(
            sessionId = request.sessionId,
            requirement = RecoveryRequirement(
                requiredActiveSeconds = request.requiredSeconds,
                requiredQualifiedPages = request.requiredPages
            ),
            accumulatedActiveMs = 0L,
            qualifiedPages = 0,
            status = RecoveryStatus.ACTIVE,
            createdAtEpochMs = request.createdAt,
            completedAtEpochMs = null,
            expiresAtEpochMs = request.expiresAt,
            protocolVersion = request.protocolVersion
        )
    }

    override suspend fun getSession(sessionId: String): RecoverySession? {
        val entity = recoveryDao.getSession(sessionId) ?: return null
        val count = recoveryDao.qualifiedPageCount(sessionId)
        return entity.toDomain(count)
    }

    override suspend fun getActiveSession(): RecoverySession? {
        val entity = recoveryDao.getLatestActiveSession() ?: return null
        val count = recoveryDao.qualifiedPageCount(entity.sessionId)
        return entity.toDomain(count)
    }

    override suspend fun updateActiveTime(sessionId: String, activeMs: Long) {
        recoveryDao.updateActiveTime(sessionId, activeMs)
        notifyChange(sessionId)
    }

    override suspend fun qualifyPage(
        sessionId: String,
        bookId: String,
        pageIndex: Int
    ): Boolean {
        val rowId = recoveryDao.insertQualifiedPage(
            QualifiedPageEntity(
                sessionId = sessionId,
                bookId = bookId,
                pageIndex = pageIndex,
                qualifiedAtEpochMs = System.currentTimeMillis()
            )
        )
        val qualified = rowId != -1L
        if (qualified) {
            notifyChange(sessionId)
        }
        return qualified
    }

    override suspend fun markComplete(sessionId: String) {
        val current = recoveryDao.getSession(sessionId) ?: return
        if (current.status == RecoveryStatus.COMPLETE.name) {
            return
        }
        recoveryDao.markComplete(
            sessionId = sessionId,
            status = RecoveryStatus.COMPLETE.name,
            completedAt = System.currentTimeMillis()
        )
        notifyChange(sessionId)
    }

    override fun observeSession(sessionId: String): Flow<RecoverySession?> {
        return recoveryDao.observeSession(sessionId)
            .combine(recoveryDao.observeQualifiedPageCount(sessionId)) { entity, count ->
                entity?.toDomain(count)
            }
    }

    override fun observeActiveSession(): Flow<RecoverySession?> {
        return recoveryDao.observeLatestActiveSession().flatMapLatest { entity ->
            if (entity == null) {
                flowOf(null)
            } else {
                observeSession(entity.sessionId)
            }
        }
    }

    private fun notifyChange(sessionId: String) {
        runCatching {
            contentResolver?.notifyChange(RecoveryProtocol.sessionUri(sessionId), null)
        }
    }

    private fun RecoverySessionEntity.toDomain(qualifiedPagesCount: Int): RecoverySession {
        val parsedStatus = runCatching {
            RecoveryStatus.valueOf(status)
        }.getOrDefault(RecoveryStatus.INVALID)

        return RecoverySession(
            sessionId = sessionId,
            requirement = RecoveryRequirement(
                requiredActiveSeconds = requiredActiveSeconds,
                requiredQualifiedPages = requiredQualifiedPages
            ),
            accumulatedActiveMs = accumulatedActiveMs,
            qualifiedPages = qualifiedPagesCount,
            status = parsedStatus,
            createdAtEpochMs = createdAtEpochMs,
            completedAtEpochMs = completedAtEpochMs,
            expiresAtEpochMs = expiresAtEpochMs,
            protocolVersion = protocolVersion
        )
    }
}
