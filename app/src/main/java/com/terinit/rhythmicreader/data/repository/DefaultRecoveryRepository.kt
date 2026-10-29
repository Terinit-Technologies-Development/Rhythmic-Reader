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

        // Insert-if-absent is atomic at the database level, so concurrent
        // acceptors (including independent repository instances) cannot both
        // create the same session. A conflict means another caller won; reload
        // and accept only an exact immutable-binding match. Never replace,
        // reset, or delete the winner's persisted evidence.
        val insertedRowId = recoveryDao.insertSessionIfAbsent(entity)
        if (insertedRowId != -1L) {
            notifyChange(request.sessionId)
            val persisted = recoveryDao.getSession(request.sessionId)
            return persisted?.toDomain(recoveryDao.qualifiedPageCount(request.sessionId))
                ?: entity.toDomain(0)
        }

        val existing = recoveryDao.getSession(request.sessionId) ?: return null
        if (!existing.matches(request)) return null

        val count = recoveryDao.qualifiedPageCount(request.sessionId)
        return existing.toDomain(count)
    }

    private fun RecoverySessionEntity.matches(request: RecoveryRequest): Boolean {
        // The Routine-issued identity is immutable; a replay with any different
        // metadata is a conflict, not a reset.
        return sessionId == request.sessionId &&
            requiredActiveSeconds == request.requiredSeconds &&
            requiredQualifiedPages == request.requiredPages &&
            expiresAtEpochMs == request.expiresAt &&
            createdAtEpochMs == request.createdAt &&
            protocolVersion == request.protocolVersion
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
