package com.terinit.rhythmicreader.app

import android.app.Application
import android.os.Build

class RhythmicReaderApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // Isolated service processes (such as androidx.pdf.service.PdfDocumentServiceImpl) run under
        // restricted UIDs with no filesystem access to the app's internal database directory.
        // We only initialize AppContainer (Room database, repos, coordinators) in the main process.
        if (isMainProcess()) {
            container = AppContainer(
                applicationContext
            )
        }
    }

    private fun isMainProcess(): Boolean {
        val currentProcess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()
        } else {
            packageName
        }
        if (currentProcess == null) return true
        return !currentProcess.contains(":") && currentProcess == packageName
    }
}
