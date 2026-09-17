package com.terinit.rhythmicreader.data.system

import android.content.Context
import android.os.PowerManager

class ScreenStateReader(
    context: Context
) {
    private val powerManager = context.getSystemService(PowerManager::class.java)

    fun isInteractive(): Boolean {
        return powerManager?.isInteractive ?: true
    }
}
