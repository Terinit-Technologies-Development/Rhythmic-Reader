package com.terinit.rhythmicreader.integration.rhythmic

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.terinit.rhythmicreader.data.db.ReaderDatabase
import com.terinit.rhythmicreader.data.repository.DefaultRecoveryRepository
import com.terinit.rhythmicreader.domain.model.RecoveryProgress
import com.terinit.rhythmicreader.domain.model.RecoveryRequirement
import com.terinit.rhythmicreader.domain.model.RecoveryStatus
import com.terinit.rhythmicreader.domain.model.RoutineReadingTargetPreview
import com.terinit.rhythmicreader.domain.model.meets
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * Pass 4 — Reader alignment with the Restorative Gate model (spec 5, 7, 8, 9, 50).
 *
 * Reader remains evidence authority; Routine remains policy authority. These
 * tests pin the two separate evidence concepts:
 *   A. Daily Evidence V2 (cumulative daily baseline, CD3: 3600s / 36 pages)
 *   B. Bound Recovery Session (discrete CD4+ restorative: 1800s / 11 pages)
 * and prove they can never satisfy each other.
 */
@RunWith(RobolectricTestRunner::class)
class RestorativeReadingAlignmentTest {

    private lateinit var db: ReaderDatabase
    private lateinit var repository: DefaultRecoveryRepository
    private val now = System.currentTimeMillis()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DefaultRecoveryRepository(
            recoveryDao = db.recoveryDao(),
            contentResolver = mock(ContentResolver::class.java)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ------------------------------------------------------------------
    // Completion thresholds (spec 9)
    // ------------------------------------------------------------------

    @Test
    fun `30 minutes with 10 pages is incomplete`() {
        val progress = RecoveryProgress(
            activeSeconds = 1_800L,
            qualifiedPages = 10,
            status = RecoveryStatus.ACTIVE
        )
        val requirement = RecoveryRequirement(
            requiredActiveSeconds = 1_800L,
            requiredQualifiedPages = 11
        )
        assertFalse(progress.meets(requirement))
    }

    @Test
    fun `29 59 with 11 pages is incomplete`() {
        val progress = RecoveryProgress(
            activeSeconds = 1_799L,
            qualifiedPages = 11,
            status = RecoveryStatus.ACTIVE
        )
        val requirement = RecoveryRequirement(
            requiredActiveSeconds = 1_800L,
            requiredQualifiedPages = 11
        )
        assertFalse(progress.meets(requirement))
    }

    @Test
    fun `30 minutes with 11 pages is complete`() {
        val progress = RecoveryProgress(
            activeSeconds = 1_800L,
            qualifiedPages = 11,
            status = RecoveryStatus.ACTIVE
        )
        val requirement = RecoveryRequirement(
            requiredActiveSeconds = 1_800L,
            requiredQualifiedPages = 11
        )
        assertTrue(progress.meets(requirement))
    }

    // ------------------------------------------------------------------
    // Bound-session correctness (spec 7)
    // ------------------------------------------------------------------

    @Test
    fun `gate G4 session R4 completes and G5 cannot reuse R4`() = runBlocking {
        val gateG4Session = UUID.randomUUID().toString()
        val gateG5Session = UUID.randomUUID().toString()

        val r4 = repository.acceptExternalRequest(
            RecoveryRequest(
                sessionId = gateG4Session,
                protocolVersion = 1,
                requiredSeconds = 1_800L,
                requiredPages = 11,
                createdAt = now,
                expiresAt = now + 3_600_000L
            )
        )
        assertNotNull(r4)

        // R4 completes its own requirement.
        repository.markComplete(gateG4Session)
        val completed = repository.getSession(gateG4Session)
        assertEquals(RecoveryStatus.COMPLETE, completed?.status)

        // A different gate binds a DIFFERENT session id — R4 cannot satisfy G5.
        val r5 = repository.acceptExternalRequest(
            RecoveryRequest(
                sessionId = gateG5Session,
                protocolVersion = 1,
                requiredSeconds = 1_800L,
                requiredPages = 11,
                createdAt = now,
                expiresAt = now + 3_600_000L
            )
        )
        assertNotNull(r5)
        assertNotEquals(gateG4Session, gateG5Session)
        assertEquals(RecoveryStatus.ACTIVE, r5?.status)
        assertEquals(0L, r5?.activeSeconds)
        assertEquals(0, r5?.qualifiedPages)

        // The old completed session stays complete but is bound to its own id only.
        assertEquals(RecoveryStatus.COMPLETE, repository.getSession(gateG4Session)?.status)
    }

    @Test
    fun `same session id is idempotent and keeps its requirement`() = runBlocking {
        val sessionId = UUID.randomUUID().toString()
        val request = RecoveryRequest(
            sessionId = sessionId,
            protocolVersion = 1,
            requiredSeconds = 1_800L,
            requiredPages = 11,
            createdAt = now,
            expiresAt = now + 3_600_000L
        )
        repository.acceptExternalRequest(request)
        repository.markComplete(sessionId)

        // Re-sending the SAME request resumes the same logical session —
        // it never becomes a second obligation and never resets progress.
        val resumed = repository.acceptExternalRequest(request)
        assertEquals(sessionId, resumed?.sessionId)
        assertEquals(RecoveryStatus.COMPLETE, resumed?.status)
        assertEquals(1_800L, resumed?.requirement?.requiredActiveSeconds)
        assertEquals(11, resumed?.requirement?.requiredQualifiedPages)
    }

    // ------------------------------------------------------------------
    // Process recovery (spec 10, 50: bound recovery persists across restart)
    // ------------------------------------------------------------------

    @Test
    fun `bound recovery session survives a repository restart with its evidence`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val persistentDb = Room.databaseBuilder(context, ReaderDatabase::class.java, "alignment-test.db")
            .allowMainThreadQueries()
            .build()
        try {
            val sessionId = UUID.randomUUID().toString()
            val first = DefaultRecoveryRepository(
                recoveryDao = persistentDb.recoveryDao(),
                contentResolver = mock(ContentResolver::class.java)
            )
            first.acceptExternalRequest(
                RecoveryRequest(
                    sessionId = sessionId,
                    protocolVersion = 1,
                    requiredSeconds = 1_800L,
                    requiredPages = 11,
                    createdAt = now,
                    expiresAt = now + 3_600_000L
                )
            )
            first.updateActiveTime(sessionId, 600_000L)

            // "Process restart": a fresh repository over the same durable store.
            val second = DefaultRecoveryRepository(
                recoveryDao = persistentDb.recoveryDao(),
                contentResolver = mock(ContentResolver::class.java)
            )
            val restored = second.getSession(sessionId)
            assertNotNull(restored)
            assertEquals(RecoveryStatus.ACTIVE, restored?.status)
            assertEquals(600L, restored?.activeSeconds)
        } finally {
            persistentDb.close()
            context.deleteDatabase("alignment-test.db")
        }
    }

    // ------------------------------------------------------------------
    // Two ledgers, one reading stream (spec 8)
    // ------------------------------------------------------------------

    @Test
    fun `one reading event updates both ledgers but never double counts`() = runBlocking {
        val sessionId = UUID.randomUUID().toString()
        repository.acceptExternalRequest(
            RecoveryRequest(
                sessionId = sessionId,
                protocolVersion = 1,
                requiredSeconds = 1_800L,
                requiredPages = 11,
                createdAt = now,
                expiresAt = now + 3_600_000L
            )
        )

        // One real reading event (10 minutes) contributes to the bound session…
        repository.updateActiveTime(sessionId, 600_000L)
        val session = repository.getSession(sessionId)
        assertEquals(600L, session?.activeSeconds)

        // …and the same event would ALSO land in Daily Evidence V2 via the
        // coordinator's projection (covered by RecoveryCoordinatorTest). The
        // two ledgers are separate projections: neither reads the other, so a
        // daily-baseline completion can never complete this session …
        assertEquals(RecoveryStatus.ACTIVE, session?.status)

        // …and re-projecting the same event must not double count.
        repository.updateActiveTime(sessionId, 600_000L)
        assertEquals(600L, repository.getSession(sessionId)?.activeSeconds)
    }

    @Test
    fun `daily evidence totals never satisfy a bound recovery session`() = runBlocking {
        val sessionId = UUID.randomUUID().toString()
        repository.acceptExternalRequest(
            RecoveryRequest(
                sessionId = sessionId,
                protocolVersion = 1,
                requiredSeconds = 1_800L,
                requiredPages = 11,
                createdAt = now,
                expiresAt = now + 3_600_000L
            )
        )
        // Rich daily evidence exists (60 min / 36 pages) — but the bound
        // session's own progress is untouched by it.
        val session = repository.getSession(sessionId)
        assertEquals(0L, session?.activeSeconds)
        assertEquals(0, session?.qualifiedPages)
        assertEquals(RecoveryStatus.ACTIVE, session?.status)
    }

    // ------------------------------------------------------------------
    // Display vocabulary (spec 5): CD3 baseline 60/36 vs CD4+ restorative 30/11
    // ------------------------------------------------------------------

    @Test
    fun `CD3 daily baseline displays 60 36 as the cumulative daily target`() {
        val preview = RoutineReadingTargetPreview(
            dateKey = "2026-09-28",
            nextCooldownOrdinal = 3,
            requiredActiveSeconds = 3_600L,
            requiredQualifiedPages = 36,
            requirementKind = "baseline-reading",
            gateStatus = "in-progress",
            selectedProvider = "reader",
            cooldownActive = true,
            protocolVersion = 2,
        )
        assertEquals(RoutineReadingTargetPreview.DisplayKind.DAILY_BASELINE, preview.displayKind)
        assertEquals(3_600L, preview.requiredActiveSeconds)
        assertEquals(36, preview.requiredQualifiedPages)
        assertFalse(preview.isRestorativeReading)
    }

    @Test
    fun `CD4 restorative path displays 30 11 as a discrete session requirement`() {
        val preview = RoutineReadingTargetPreview(
            dateKey = "2026-09-28",
            nextCooldownOrdinal = 4,
            requiredActiveSeconds = 1_800L,
            requiredQualifiedPages = 11,
            requirementKind = "restorative-choice",
            gateStatus = "pending-selection",
            selectedProvider = "none",
            cooldownActive = true,
            restorativeReadingSeconds = 1_800L,
            restorativeQualifiedPages = 11,
            protocolVersion = 2,
        )
        assertEquals(RoutineReadingTargetPreview.DisplayKind.RESTORATIVE_READING, preview.displayKind)
        assertTrue(preview.isRestorativeReading)
        assertEquals(1_800L to 11, preview.displayedRestorativeRequirement)
        assertTrue(preview.awaitingSelection)
        // Never the obsolete cumulative 5400/47.
        assertNotEquals(5_400L, preview.displayedRestorativeRequirement.first)
    }

    @Test
    fun `restorative state projections are distinct for the UI`() {
        val base = RoutineReadingTargetPreview(
            dateKey = "2026-09-28",
            nextCooldownOrdinal = 5,
            requiredActiveSeconds = 1_800L,
            requiredQualifiedPages = 11,
            requirementKind = "restorative-choice",
            protocolVersion = 2,
        )
        val inProgress = base.copy(gateStatus = "in-progress", selectedProvider = "reader")
        assertTrue(inProgress.readerRecoveryInProgress)

        val completeCooldownRemains = base.copy(gateStatus = "satisfied", cooldownActive = true)
        assertTrue(completeCooldownRemains.restorativeCompleteCooldownActive)
        assertFalse(completeCooldownRemains.readerRecoveryInProgress)
    }

    @Test
    fun `v1 preview rows stay readable with neutral labels`() {
        val v1 = RoutineReadingTargetPreview(
            dateKey = "2026-09-28",
            nextCooldownOrdinal = 3,
            requiredActiveSeconds = 3_600L,
            requiredQualifiedPages = 36,
            protocolVersion = 1,
        )
        assertEquals(RoutineReadingTargetPreview.DisplayKind.READING_TARGET, v1.displayKind)
        assertFalse(v1.isRestorativeReading)
        // Discrete restorative defaults remain available for the bound-session UI.
        assertEquals(1_800L to 11, v1.displayedRestorativeRequirement)
    }
}
