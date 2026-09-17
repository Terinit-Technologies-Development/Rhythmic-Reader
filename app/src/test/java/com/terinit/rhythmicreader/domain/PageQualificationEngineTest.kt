package com.terinit.rhythmicreader.domain

import com.terinit.rhythmicreader.domain.recovery.PageQualificationEngine
import com.terinit.rhythmicreader.domain.recovery.PageQualificationResult
import com.terinit.rhythmicreader.domain.recovery.QualificationPolicy
import com.terinit.rhythmicreader.test.FakeMonotonicClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PageQualificationEngineTest {

    private lateinit var clock: FakeMonotonicClock
    private lateinit var engine: PageQualificationEngine

    @Before
    fun setup() {
        clock = FakeMonotonicClock(10_000L)
        engine = PageQualificationEngine(
            clock = clock,
            policy = QualificationPolicy(
                minimumPageDwellMs = 15_000L,
                rapidFlipWindowMs = 10_000L,
                rapidFlipThreshold = 4
            )
        )
    }

    @Test
    fun under15SecondsDwell_producesNoCandidate() {
        engine.onPageChanged(1)

        // Advance 14,999 ms
        clock.advanceBy(14_999L)

        val result = engine.onPageChanged(2)
        assertTrue(result is PageQualificationResult.None)
    }

    @Test
    fun exact15SecondsDwell_producesCandidate() {
        engine.onPageChanged(1)

        // Advance 15,000 ms
        clock.advanceBy(15_000L)

        val result = engine.onPageChanged(2)
        assertTrue(result is PageQualificationResult.Candidate)
        val candidate = result as PageQualificationResult.Candidate
        assertEquals(1, candidate.pageIndex)
        assertEquals(15_000L, candidate.dwellMs)
    }

    @Test
    fun continuousReading_satisfiesDwellWithoutChangingPage() {
        engine.onPageChanged(5)
        clock.advanceBy(15_000L)

        val result = engine.checkCurrentPageDwell()
        assertTrue(result is PageQualificationResult.Candidate)
        assertEquals(5, (result as PageQualificationResult.Candidate).pageIndex)

        // Subscribing again during the same dwell should not re-emit
        val secondCheck = engine.checkCurrentPageDwell()
        assertTrue(secondCheck is PageQualificationResult.None)
    }

    @Test
    fun rapidFourPageBurst_producesNoQualifiedCandidates() {
        // User flips rapidly: 0 -> 1 -> 2 -> 3 -> 4 within 4 seconds
        engine.onPageChanged(0)
        clock.advanceBy(500L)

        val r1 = engine.onPageChanged(1)
        clock.advanceBy(500L)

        val r2 = engine.onPageChanged(2)
        clock.advanceBy(500L)

        val r3 = engine.onPageChanged(3)
        clock.advanceBy(500L)

        val r4 = engine.onPageChanged(4)

        assertTrue(r1 is PageQualificationResult.None)
        assertTrue(r2 is PageQualificationResult.None)
        assertTrue(r3 is PageQualificationResult.None)
        assertTrue(r4 is PageQualificationResult.None)
    }

    @Test
    fun rapidBurstEnds_stableLaterPageQualifies() {
        // Burst 4 pages in 2 seconds
        engine.onPageChanged(10)
        clock.advanceBy(500L)
        engine.onPageChanged(11)
        clock.advanceBy(500L)
        engine.onPageChanged(12)
        clock.advanceBy(500L)
        engine.onPageChanged(13)
        clock.advanceBy(500L)

        // Settle on page 14 for 20 seconds (burst window of 10s expires!)
        engine.onPageChanged(14)
        clock.advanceBy(20_000L)

        // Move to page 15
        val result = engine.onPageChanged(15)
        assertTrue(result is PageQualificationResult.Candidate)
        val candidate = result as PageQualificationResult.Candidate
        assertEquals(14, candidate.pageIndex)
        assertEquals(20_000L, candidate.dwellMs)
    }

    @Test
    fun onPause_evaluatesDwellOfCurrentPage() {
        engine.onPageChanged(20)
        clock.advanceBy(16_000L)

        val result = engine.onPause()
        assertTrue(result is PageQualificationResult.Candidate)
        assertEquals(20, (result as PageQualificationResult.Candidate).pageIndex)
    }
}
