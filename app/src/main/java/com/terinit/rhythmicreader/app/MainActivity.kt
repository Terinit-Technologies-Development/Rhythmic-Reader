package com.terinit.rhythmicreader.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.terinit.rhythmicreader.integration.rhythmic.RecoveryProtocol
import com.terinit.rhythmicreader.ui.theme.RhythmicReaderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var screenStateReceiverRegistered = false
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val app = application as? RhythmicReaderApplication ?: return
            val isInteractive = app.container.screenStateReader.isInteractive()
            app.container.recoveryCoordinator.updateScreenInteractive(isInteractive)
            if (!isInteractive) app.container.recoveryCoordinator.flushPendingEvidenceAsync()
        }
    }

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

    @SuppressLint("InlinedApi")
    override fun onStart() {
        super.onStart()
        val app = application as? RhythmicReaderApplication ?: return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(screenStateReceiver, filter)
        }
        screenStateReceiverRegistered = true
        app.container.recoveryCoordinator.updateScreenInteractive(
            app.container.screenStateReader.isInteractive()
        )
    }

    override fun onStop() {
        if (screenStateReceiverRegistered) {
            unregisterReceiver(screenStateReceiver)
            screenStateReceiverRegistered = false
        }
        super.onStop()
    }

    private fun handleRecoveryIntent(intent: Intent?) {
        val sessionId = intent?.getStringExtra(RecoveryProtocol.EXTRA_SESSION_ID) ?: return
        val app = application as? RhythmicReaderApplication ?: return
        lifecycleScope.launch {
            app.container.recoveryCoordinator.restoreSession(sessionId)
        }
    }
}
