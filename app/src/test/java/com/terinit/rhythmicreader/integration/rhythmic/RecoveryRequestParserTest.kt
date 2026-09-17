package com.terinit.rhythmicreader.integration.rhythmic

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class RecoveryRequestParserTest {

    private val validUuid = UUID.randomUUID().toString()
    private val now = System.currentTimeMillis()

    @Test
    fun parseValidIntent_returnsRequest() {
        val intent = Intent(RecoveryProtocol.ACTION_START_RECOVERY).apply {
            putExtra(RecoveryProtocol.EXTRA_SESSION_ID, validUuid)
            putExtra(RecoveryProtocol.EXTRA_PROTOCOL_VERSION, 1)
            putExtra(RecoveryProtocol.EXTRA_REQUIRED_SECONDS, 1800)
            putExtra(RecoveryProtocol.EXTRA_REQUIRED_PAGES, 10)
            putExtra(RecoveryProtocol.EXTRA_CREATED_AT, now)
            putExtra(RecoveryProtocol.EXTRA_EXPIRES_AT, now + 3600_000L)
        }

        val parsed = RecoveryRequestParser.fromIntent(intent)
        assertNotNull(parsed)
        assertEquals(validUuid, parsed?.sessionId)
        assertEquals(1, parsed?.protocolVersion)
        assertEquals(1800L, parsed?.requiredSeconds)
        assertEquals(10, parsed?.requiredPages)
        assertEquals(now, parsed?.createdAt)
        assertEquals(now + 3600_000L, parsed?.expiresAt)
    }

    @Test
    fun nullIntent_returnsNull() {
        assertNull(RecoveryRequestParser.fromIntent(null))
    }

    @Test
    fun wrongAction_returnsNull() {
        val intent = Intent("android.intent.action.VIEW").apply {
            putExtra(RecoveryProtocol.EXTRA_SESSION_ID, validUuid)
            putExtra(RecoveryProtocol.EXTRA_PROTOCOL_VERSION, 1)
            putExtra(RecoveryProtocol.EXTRA_REQUIRED_SECONDS, 1800)
            putExtra(RecoveryProtocol.EXTRA_REQUIRED_PAGES, 10)
            putExtra(RecoveryProtocol.EXTRA_CREATED_AT, now)
            putExtra(RecoveryProtocol.EXTRA_EXPIRES_AT, now + 3600_000L)
        }
        assertNull(RecoveryRequestParser.fromIntent(intent))
    }

    @Test
    fun missingSessionId_returnsNull() {
        val intent = Intent(RecoveryProtocol.ACTION_START_RECOVERY).apply {
            putExtra(RecoveryProtocol.EXTRA_PROTOCOL_VERSION, 1)
            putExtra(RecoveryProtocol.EXTRA_REQUIRED_SECONDS, 1800)
            putExtra(RecoveryProtocol.EXTRA_REQUIRED_PAGES, 10)
            putExtra(RecoveryProtocol.EXTRA_CREATED_AT, now)
            putExtra(RecoveryProtocol.EXTRA_EXPIRES_AT, now + 3600_000L)
        }
        assertNull(RecoveryRequestParser.fromIntent(intent))
    }

    @Test
    fun missingProtocolVersion_returnsNull() {
        val intent = Intent(RecoveryProtocol.ACTION_START_RECOVERY).apply {
            putExtra(RecoveryProtocol.EXTRA_SESSION_ID, validUuid)
            putExtra(RecoveryProtocol.EXTRA_REQUIRED_SECONDS, 1800)
            putExtra(RecoveryProtocol.EXTRA_REQUIRED_PAGES, 10)
            putExtra(RecoveryProtocol.EXTRA_CREATED_AT, now)
            putExtra(RecoveryProtocol.EXTRA_EXPIRES_AT, now + 3600_000L)
        }
        assertNull(RecoveryRequestParser.fromIntent(intent))
    }

    @Test
    fun outOfBoundsExtras_returnsNull() {
        val intent = Intent(RecoveryProtocol.ACTION_START_RECOVERY).apply {
            putExtra(RecoveryProtocol.EXTRA_SESSION_ID, validUuid)
            putExtra(RecoveryProtocol.EXTRA_PROTOCOL_VERSION, 1)
            putExtra(RecoveryProtocol.EXTRA_REQUIRED_SECONDS, 10) // Below 60s minimum
            putExtra(RecoveryProtocol.EXTRA_REQUIRED_PAGES, 10)
            putExtra(RecoveryProtocol.EXTRA_CREATED_AT, now)
            putExtra(RecoveryProtocol.EXTRA_EXPIRES_AT, now + 3600_000L)
        }
        assertNull(RecoveryRequestParser.fromIntent(intent))
    }
}
