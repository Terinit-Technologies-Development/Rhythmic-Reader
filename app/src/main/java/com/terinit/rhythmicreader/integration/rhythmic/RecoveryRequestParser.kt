package com.terinit.rhythmicreader.integration.rhythmic

import android.content.Intent

object RecoveryRequestParser {

    fun fromIntent(intent: Intent?): RecoveryRequest? {
        if (intent == null) return null

        val action = intent.action
        if (action != null && action != RecoveryProtocol.ACTION_START_RECOVERY) {
            return null
        }

        val sessionId = intent.getStringExtra(RecoveryProtocol.EXTRA_SESSION_ID) ?: return null
        if (!intent.hasExtra(RecoveryProtocol.EXTRA_PROTOCOL_VERSION)) return null
        val protocolVersion = intent.getIntExtra(RecoveryProtocol.EXTRA_PROTOCOL_VERSION, -1)

        val requiredSeconds = if (intent.hasExtra(RecoveryProtocol.EXTRA_REQUIRED_SECONDS)) {
            val longSec = intent.getLongExtra(RecoveryProtocol.EXTRA_REQUIRED_SECONDS, -1L)
            if (longSec != -1L) longSec else intent.getIntExtra(RecoveryProtocol.EXTRA_REQUIRED_SECONDS, -1).toLong()
        } else {
            return null
        }

        if (!intent.hasExtra(RecoveryProtocol.EXTRA_REQUIRED_PAGES)) return null
        val requiredPages = intent.getIntExtra(RecoveryProtocol.EXTRA_REQUIRED_PAGES, -1)

        if (!intent.hasExtra(RecoveryProtocol.EXTRA_CREATED_AT)) return null
        val createdAt = intent.getLongExtra(RecoveryProtocol.EXTRA_CREATED_AT, -1L)

        if (!intent.hasExtra(RecoveryProtocol.EXTRA_EXPIRES_AT)) return null
        val expiresAt = intent.getLongExtra(RecoveryProtocol.EXTRA_EXPIRES_AT, -1L)

        val request = RecoveryRequest(
            sessionId = sessionId,
            protocolVersion = protocolVersion,
            requiredSeconds = requiredSeconds,
            requiredPages = requiredPages,
            createdAt = createdAt,
            expiresAt = expiresAt
        )

        return if (request.isValid) request else null
    }
}
