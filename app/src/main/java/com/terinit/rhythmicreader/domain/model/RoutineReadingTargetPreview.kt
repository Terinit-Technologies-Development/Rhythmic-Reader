package com.terinit.rhythmicreader.domain.model

/**
 * Informational target supplied by Rhythmic Routine; it is NOT an active
 * recovery session. Reader is evidence authority, Routine is policy authority:
 * these numbers arrive from Routine and Reader only renders them.
 *
 * Protocol V1 rows carry the core fields only; Protocol V2 rows additionally
 * describe Routine's restorative model (requirement kind, gate status,
 * selected provider, cooldown activity, and the discrete CD4+ reader
 * requirement). Unknown protocol versions are never parsed into a preview.
 */
data class RoutineReadingTargetPreview(
    val dateKey: String,
    val nextCooldownOrdinal: Int,
    val requiredActiveSeconds: Long,
    val requiredQualifiedPages: Int,
    /** Routine's requirement kind: none / baseline-reading / restorative-choice / legacy-reading. */
    val requirementKind: String = "none",
    /** Gate lifecycle as Routine sees it: none / pending-selection / in-progress / satisfied. */
    val gateStatus: String = "none",
    /** reader / meditation / none. */
    val selectedProvider: String = "none",
    val cooldownActive: Boolean = false,
    /** Discrete CD4+ Reader path requirement (per bound recovery session). */
    val restorativeReadingSeconds: Long = 0L,
    val restorativeQualifiedPages: Int = 0,
    val protocolVersion: Int = 1,
) {
    /**
     * Reader-side display vocabulary (Pass 4 UI):
     * - [DisplayKind.DAILY_BASELINE]: the cumulative CD3 daily reading baseline
     * - [DisplayKind.RESTORATIVE_READING]: the discrete CD4+ restorative choice
     * - [DisplayKind.READING_TARGET]: neutral label (V1 preview rows)
     */
    enum class DisplayKind { DAILY_BASELINE, RESTORATIVE_READING, READING_TARGET }

    val displayKind: DisplayKind
        get() = when (requirementKind) {
            "baseline-reading" -> DisplayKind.DAILY_BASELINE
            "restorative-choice" -> DisplayKind.RESTORATIVE_READING
            "legacy-reading" -> DisplayKind.READING_TARGET
            else -> DisplayKind.READING_TARGET
        }

    /** True when this preview describes the discrete restorative reading path. */
    val isRestorativeReading: Boolean
        get() = displayKind == DisplayKind.RESTORATIVE_READING

    /** True when Routine reports the reader as the selected restorative provider. */
    val readerSelected: Boolean
        get() = selectedProvider == "reader"

    /** True when a restorative gate exists but has not selected a provider yet. */
    val awaitingSelection: Boolean
        get() = gateStatus == "pending-selection"

    /** True when a reader restorative session is running (Routine's view). */
    val readerRecoveryInProgress: Boolean
        get() = isRestorativeReading && readerSelected && gateStatus == "in-progress"

    /** True when the restorative requirement is complete while the cooldown runs. */
    val restorativeCompleteCooldownActive: Boolean
        get() = gateStatus == "satisfied" && cooldownActive

    /** The requirement Reader should display for the restorative path. */
    val displayedRestorativeRequirement: Pair<Long, Int>
        get() = if (restorativeReadingSeconds > 0 && restorativeQualifiedPages > 0) {
            restorativeReadingSeconds to restorativeQualifiedPages
        } else {
            DEFAULT_RESTORATIVE_READING_SECONDS to DEFAULT_RESTORATIVE_QUALIFIED_PAGES
        }

    companion object {
        /** CD4+ discrete Reader requirement (30 min + 11 pages). */
        const val DEFAULT_RESTORATIVE_READING_SECONDS = 30L * 60L
        const val DEFAULT_RESTORATIVE_QUALIFIED_PAGES = 11
    }
}
