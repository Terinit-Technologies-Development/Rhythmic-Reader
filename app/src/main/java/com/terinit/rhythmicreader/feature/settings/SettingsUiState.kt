package com.terinit.rhythmicreader.feature.settings

data class SettingsUiState(
    val bookCount: Int = 0,
    val libraryStorageBytes: Long = 0L,
    val deviceAvailableBytes: Long = 0L,
    val isLoading: Boolean = false,
    val userNotification: String? = null
) {
    val formattedLibraryUsage: String
        get() = formatBytes(libraryStorageBytes)

    val formattedAvailableSpace: String
        get() = formatBytes(deviceAvailableBytes)

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.1f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.1f KB", kb)
        }
    }
}
