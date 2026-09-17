package com.terinit.rhythmicreader.domain

import com.terinit.rhythmicreader.domain.model.RecoveryProgress
import com.terinit.rhythmicreader.domain.model.RecoveryRequirement
import com.terinit.rhythmicreader.domain.model.RecoveryStatus
import com.terinit.rhythmicreader.domain.model.meets
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryRequirementTest {

    private val requirement = RecoveryRequirement(
        requiredActiveSeconds = 1800L, // 30 minutes
        requiredQualifiedPages = 10
    )

    @Test
    fun timeMet_pagesNotMet_isIncomplete() {
        val progress = RecoveryProgress(
            activeSeconds = 1800L,
            qualifiedPages = 6,
            status = RecoveryStatus.ACTIVE
        )
        assertFalse(progress.meets(requirement))
    }

    @Test
    fun pagesMet_timeNotMet_isIncomplete() {
        val progress = RecoveryProgress(
            activeSeconds = 1080L, // 18 minutes
            qualifiedPages = 10,
            status = RecoveryStatus.ACTIVE
        )
        assertFalse(progress.meets(requirement))
    }

    @Test
    fun neitherMet_isIncomplete() {
        val progress = RecoveryProgress(
            activeSeconds = 500L,
            qualifiedPages = 2,
            status = RecoveryStatus.ACTIVE
        )
        assertFalse(progress.meets(requirement))
    }

    @Test
    fun bothThresholdsMet_isComplete() {
        val progress = RecoveryProgress(
            activeSeconds = 1800L,
            qualifiedPages = 10,
            status = RecoveryStatus.ACTIVE
        )
        assertTrue(progress.meets(requirement))
    }

    @Test
    fun bothThresholdsExceeded_isComplete() {
        val progress = RecoveryProgress(
            activeSeconds = 2400L,
            qualifiedPages = 15,
            status = RecoveryStatus.ACTIVE
        )
        assertTrue(progress.meets(requirement))
    }
}
