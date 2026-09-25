package com.terinit.rhythmicreader.domain.recovery

import com.terinit.rhythmicreader.domain.time.MonotonicClock

sealed interface PageQualificationResult {
    data object None : PageQualificationResult

    data class Candidate(
        val pageIndex: Int,
        val dwellMs: Long
    ) : PageQualificationResult
}

class PageQualificationEngine(
    private val clock: MonotonicClock,
    private val policy: QualificationPolicy = QualificationPolicy()
) {

    private var currentPage: Int? = null
    private var pageEnteredAtMs: Long? = null
    private var candidateEmittedForCurrentDwell: Int? = null

    private val recentTransitions = ArrayDeque<Long>()

    @Synchronized
    fun onPageChanged(newPage: Int): PageQualificationResult {
        val now = clock.nowMs()

        // 1. Evaluate candidate for the page being left
        val completedPage = if (currentPage != null && candidateEmittedForCurrentDwell != currentPage) {
            evaluateCurrentPage(now)
        } else {
            PageQualificationResult.None
        }

        // 2. Set new page state
        currentPage = newPage
        pageEnteredAtMs = now
        candidateEmittedForCurrentDwell = null

        // 3. Track transition timestamp and trim expired transitions
        recentTransitions.addLast(now)
        trimTransitionWindow(now)

        return completedPage
    }

    /**
     * Checks if the currently viewed page has satisfied dwell requirements
     * without having to turn the page. Emits Candidate at most once per continuous dwell.
     */
    @Synchronized
    fun checkCurrentPageDwell(): PageQualificationResult {
        val now = clock.nowMs()
        val page = currentPage ?: return PageQualificationResult.None
        if (candidateEmittedForCurrentDwell == page) {
            return PageQualificationResult.None
        }

        val result = evaluateCurrentPage(now)
        if (result is PageQualificationResult.Candidate) {
            candidateEmittedForCurrentDwell = page
            return result
        }
        return PageQualificationResult.None
    }

    @Synchronized
    fun onPause(): PageQualificationResult {
        val now = clock.nowMs()
        val result = if (currentPage != null && candidateEmittedForCurrentDwell != currentPage) {
            evaluateCurrentPage(now)
        } else {
            PageQualificationResult.None
        }
        // Pause stops active dwell accumulation
        pageEnteredAtMs = null
        return result
    }

    @Synchronized
    fun onResume() {
        if (currentPage != null) {
            pageEnteredAtMs = clock.nowMs()
        }
    }

    /** Starts a fresh dwell on the current page at a local-day boundary. */
    @Synchronized
    fun restartCurrentDwell() {
        if (currentPage != null) {
            pageEnteredAtMs = clock.nowMs()
            candidateEmittedForCurrentDwell = null
        }
    }

    private fun evaluateCurrentPage(now: Long): PageQualificationResult {
        val page = currentPage ?: return PageQualificationResult.None
        val entered = pageEnteredAtMs ?: return PageQualificationResult.None

        val dwellMs = now - entered
        trimTransitionWindow(now)

        val rapidFlip = recentTransitions.size >= policy.rapidFlipThreshold

        return if (dwellMs >= policy.minimumPageDwellMs && !rapidFlip) {
            PageQualificationResult.Candidate(
                pageIndex = page,
                dwellMs = dwellMs
            )
        } else {
            PageQualificationResult.None
        }
    }

    private fun trimTransitionWindow(now: Long) {
        while (
            recentTransitions.isNotEmpty() &&
            now - recentTransitions.first() > policy.rapidFlipWindowMs
        ) {
            recentTransitions.removeFirst()
        }
    }

    @Synchronized
    fun reset() {
        currentPage = null
        pageEnteredAtMs = null
        candidateEmittedForCurrentDwell = null
        recentTransitions.clear()
    }
}
