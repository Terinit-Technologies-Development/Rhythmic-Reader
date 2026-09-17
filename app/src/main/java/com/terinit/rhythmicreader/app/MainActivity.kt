package com.terinit.rhythmicreader.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.terinit.rhythmicreader.integration.rhythmic.RecoveryProtocol
import com.terinit.rhythmicreader.ui.theme.RhythmicReaderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleRecoveryIntent(intent)
        enableEdgeToEdge()

        val app = application as RhythmicReaderApplication

        setContent {
            RhythmicReaderTheme {
                ReaderApp(container = app.container)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRecoveryIntent(intent)
    }

    private fun handleRecoveryIntent(intent: Intent?) {
        val sessionId = intent?.getStringExtra(RecoveryProtocol.EXTRA_SESSION_ID) ?: return
        val app = application as? RhythmicReaderApplication ?: return
        lifecycleScope.launch {
            app.container.recoveryCoordinator.restoreSession(sessionId)
        }
    }
}
