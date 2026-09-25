package com.terinit.rhythmicreader.feature.today

import com.terinit.rhythmicreader.domain.model.DailyReadingEvidenceSnapshot
import com.terinit.rhythmicreader.domain.model.RoutineReadingTargetPreview

data class TodayReadingUiState(
    val dateKey: String = "",
    val evidence: DailyReadingEvidenceSnapshot = DailyReadingEvidenceSnapshot.empty(dateKey),
    val nextRoutineTarget: RoutineReadingTargetPreview? = null,
    val routineTargetLoaded: Boolean = false,
)
