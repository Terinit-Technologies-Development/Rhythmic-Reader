package com.terinit.rhythmicreader.domain.recovery

data class ReadingActivityState(
    val sessionActive: Boolean = false,
    val appForeground: Boolean = false,
    val screenInteractive: Boolean = false,
    val documentLoaded: Boolean = false,
    val readerVisible: Boolean = false
) {
    val qualifiesForActiveTime: Boolean
        get() =
            sessionActive &&
            appForeground &&
            screenInteractive &&
            documentLoaded &&
            readerVisible
}
