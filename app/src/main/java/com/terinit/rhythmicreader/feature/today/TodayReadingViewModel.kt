package com.terinit.rhythmicreader.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.terinit.rhythmicreader.data.repository.DailyReadingEvidenceRepository
import com.terinit.rhythmicreader.domain.model.DailyReadingEvidenceSnapshot
import com.terinit.rhythmicreader.domain.time.LocalDateClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class TodayReadingViewModel(
    private val repository: DailyReadingEvidenceRepository,
    private val localDateClock: LocalDateClock
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
                    _uiState.value = TodayReadingUiState(
                        dateKey = dateKey,
                        evidence = snapshot ?: DailyReadingEvidenceSnapshot.empty(dateKey)
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: DailyReadingEvidenceRepository,
            localDateClock: LocalDateClock
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TodayReadingViewModel(repository, localDateClock) as T
        }
    }
}
