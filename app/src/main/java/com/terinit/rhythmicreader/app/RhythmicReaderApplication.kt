package com.terinit.rhythmicreader.app

import android.app.Application

class RhythmicReaderApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        container = AppContainer(
            applicationContext
        )
    }
}
