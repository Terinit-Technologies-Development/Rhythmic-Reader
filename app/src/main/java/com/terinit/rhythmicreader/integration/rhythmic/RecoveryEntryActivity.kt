package com.terinit.rhythmicreader.integration.rhythmic

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.terinit.rhythmicreader.app.MainActivity
import com.terinit.rhythmicreader.app.RhythmicReaderApplication
import kotlinx.coroutines.launch

class RecoveryEntryActivity : ComponentActivity() {

    companion object {
        private const val TAG = "RecoveryEntryActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val request = RecoveryRequestParser.fromIntent(intent)
        if (request == null) {
            Log.w(TAG, "Invalid recovery request intent received; finishing activity.")
            finish()
            return
        }

        val app = application as? RhythmicReaderApplication
        if (app == null) {
            Log.e(TAG, "Application is not RhythmicReaderApplication")
            finish()
            return
        }

        lifecycleScope.launch {
            val session = app.container.recoveryRepository.acceptExternalRequest(request)
            if (session == null) {
                Log.w(TAG, "Recovery request rejected (conflict or invalid): ${request.sessionId}")
                finish()
                return@launch
            }

            app.container.recoveryCoordinator.restoreSession(session.sessionId)

            val mainIntent = Intent(this@RecoveryEntryActivity, MainActivity::class.java).apply {
                putExtra(RecoveryProtocol.EXTRA_SESSION_ID, session.sessionId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(mainIntent)
            finish()
        }
    }
}
