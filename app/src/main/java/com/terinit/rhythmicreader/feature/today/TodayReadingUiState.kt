package com.terinit.rhythmicreader.feature.today

import com.terinit.rhythmicreader.domain.model.DailyReadingEvidenceSnapshot

data class TodayReadingUiState(
    val dateKey: String = "",
    val evidence: DailyReadingEvidenceSnapshot = DailyReadingEvidenceSnapshot.empty(dateKey)
)
