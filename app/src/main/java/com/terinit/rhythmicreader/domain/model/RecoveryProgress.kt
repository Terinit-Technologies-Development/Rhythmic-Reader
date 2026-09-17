package com.terinit.rhythmicreader.domain.model

data class RecoveryProgress(
    val activeSeconds: Long,
    val qualifiedPages: Int,
    val status: RecoveryStatus
)

fun RecoveryProgress.meets(
    requirement: RecoveryRequirement
): Boolean {
    return activeSeconds >= requirement.requiredActiveSeconds &&
        qualifiedPages >= requirement.requiredQualifiedPages
}
