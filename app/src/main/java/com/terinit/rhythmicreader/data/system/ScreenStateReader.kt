package com.terinit.rhythmicreader.data.system

import android.content.Context
import android.os.PowerManager

class ScreenStateReader(
    context: Context
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    fun isInteractive(): Boolean {
        return powerManager?.isInteractive ?: false
    }
}
