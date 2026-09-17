package com.terinit.rhythmicreader.domain.model

data class RecoveryRequirement(
    val requiredActiveSeconds: Long,
    val requiredQualifiedPages: Int
) {
    init {
        require(requiredActiveSeconds >= 0) { "requiredActiveSeconds must be non-negative" }
        require(requiredQualifiedPages >= 0) { "requiredQualifiedPages must be non-negative" }
    }
}
