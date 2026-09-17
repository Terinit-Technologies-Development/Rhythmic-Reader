package com.terinit.rhythmicreader.domain.recovery

data class QualificationPolicy(
    val minimumPageDwellMs: Long = 15_000L,
    val rapidFlipWindowMs: Long = 10_000L,
    val rapidFlipThreshold: Int = 4
) {
    init {
        require(minimumPageDwellMs >= 0) { "minimumPageDwellMs must be non-negative" }
        require(rapidFlipWindowMs >= 0) { "rapidFlipWindowMs must be non-negative" }
        require(rapidFlipThreshold >= 1) { "rapidFlipThreshold must be at least 1" }
    }
}
