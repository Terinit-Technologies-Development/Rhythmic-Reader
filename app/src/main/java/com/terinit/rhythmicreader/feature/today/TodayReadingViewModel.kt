package com.terinit.rhythmicreader.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.terinit.rhythmicreader.data.repository.DailyReadingEvidenceRepository
import com.terinit.rhythmicreader.domain.model.DailyReadingEvidenceSnapshot
import com.terinit.rhythmicreader.domain.time.LocalDateClock
import com.terinit.rhythmicreader.integration.rhythmic.RoutineAttentionPreviewClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class TodayReadingViewModel(
    private val repository: DailyReadingEvidenceRepository,
    private val localDateClock: LocalDateClock,
    private val routineAttentionPreviewClient: RoutineAttentionPreviewClient =
        RoutineAttentionPreviewClient { null },
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayReadingUiState())
    val uiState: StateFlow<TodayReadingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            flow {
                while (currentCoroutineContext().isActive) {
                    emit(localDateClock.todayDateKey())
                    delay(1_000L)
                }
            }.distinctUntilChanged().collectLatest { dateKey ->
                val now = localDateClock.nowEpochMs()
                repository.ensureDay(dateKey, now)
                repository.observeDailySnapshot(dateKey).collect { snapshot ->
                    _uiState.update {
                        it.copy(
                            dateKey = dateKey,
                            evidence = snapshot ?: DailyReadingEvidenceSnapshot.empty(dateKey),
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            while (currentCoroutineContext().isActive) {
                val dateKey = localDateClock.todayDateKey()
                val nextTarget = routineAttentionPreviewClient.queryNextTarget(dateKey)
                _uiState.update {
                    it.copy(nextRoutineTarget = nextTarget, routineTargetLoaded = true)
                }
                delay(30_000L)
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: DailyReadingEvidenceRepository,
            localDateClock: LocalDateClock,
            routineAttentionPreviewClient: RoutineAttentionPreviewClient =
                RoutineAttentionPreviewClient { null },
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TodayReadingViewModel(repository, localDateClock, routineAttentionPreviewClient) as T
        }
    }
}
