package com.terinit.rhythmicreader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.terinit.rhythmicreader.ui.theme.RhythmicReaderTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val app = application as RhythmicReaderApplication

        setContent {
            RhythmicReaderTheme {
                ReaderApp(container = app.container)
            }
        }
    }
}
