package com.terinit.rhythmicreader.domain.model

/** Informational next target supplied by Rhythmic Routine; it is not an active recovery session. */
data class RoutineReadingTargetPreview(
    val dateKey: String,
    val nextCooldownOrdinal: Int,
    val requiredActiveSeconds: Long,
    val requiredQualifiedPages: Int,
)
