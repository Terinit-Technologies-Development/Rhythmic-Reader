package com.terinit.rhythmicreader.integration.rhythmic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class RecoveryRequestTest {

    private val validUuid = UUID.randomUUID().toString()
    private val now = System.currentTimeMillis()

    @Test
    fun validRequest_passesValidation() {
        val request = RecoveryRequest(
            sessionId = validUuid,
            protocolVersion = 1,
            requiredSeconds = 1800L,
            requiredPages = 10,
            createdAt = now,
            expiresAt = now + 3600_000L
        )
        assertTrue(request.isValid)
        assertTrue(request.validate() is RecoveryRequest.ValidationResult.Valid)
    }

    @Test
    fun invalidProtocolVersion_rejected() {
        val request = RecoveryRequest(
            sessionId = validUuid,
            protocolVersion = 2,
            requiredSeconds = 1800L,
            requiredPages = 10,
            createdAt = now,
            expiresAt = now + 3600_000L
        )
        assertFalse(request.isValid)
    }

    @Test
    fun opaqueSessionId_passesValidation() {
        val opaqueId = "gate-ad-20261103-0800-sw005-qa-cycle-4-o4-reader"
        val request = RecoveryRequest(
            sessionId = opaqueId,
            protocolVersion = 1,
            requiredSeconds = 1800L,
            requiredPages = 11,
            createdAt = now,
            expiresAt = now + 3600_000L
        )
        assertFalse("opaque IDs must not be treated as UUIDs", RecoveryRequest.isValidUuid(opaqueId))
        assertTrue(RecoveryRequest.isValidSessionId(opaqueId))
        assertTrue(request.isValid)
        assertTrue(request.validate() is RecoveryRequest.ValidationResult.Valid)
    }

    @Test
    fun opaqueSessionId_boundaryLengthIsEnforced() {
        val maximum = RecoveryRequest(
            sessionId = "a".repeat(128),
            protocolVersion = 1,
            requiredSeconds = 1800L,
            requiredPages = 11,
            createdAt = now,
            expiresAt = now + 3600_000L
        )
        assertTrue(maximum.isValid)

        val overMaximum = RecoveryRequest(
            sessionId = "a".repeat(129),
            protocolVersion = 1,
            requiredSeconds = 1800L,
            requiredPages = 11,
            createdAt = now,
            expiresAt = now + 3600_000L
        )
        assertFalse(overMaximum.isValid)
    }

    @Test
    fun invalidSessionIdSyntax_rejected() {
        listOf(
            "",
            "-leading-punctuation",
            "contains space",
            "contains/slash",
            "contains\nnewline"
        ).forEach { sessionId ->
            val request = RecoveryRequest(
                sessionId = sessionId,
                protocolVersion = 1,
                requiredSeconds = 1800L,
                requiredPages = 11,
                createdAt = now,
                expiresAt = now + 3600_000L
            )
            assertFalse("session ID must be rejected: $sessionId", request.isValid)
        }
    }

    @Test
    fun secondsUnderMinimum_rejected() {
        val request = RecoveryRequest(
            sessionId = validUuid,
            protocolVersion = 1,
            requiredSeconds = 59L,
            requiredPages = 10,
            createdAt = now,
            expiresAt = now + 3600_000L
        )
        assertFalse(request.isValid)
    }

    @Test
    fun secondsOverMaximum_rejected() {
        val request = RecoveryRequest(
            sessionId = validUuid,
            protocolVersion = 1,
            requiredSeconds = 21601L,
            requiredPages = 10,
            createdAt = now,
            expiresAt = now + 3600_000L
        )
        assertFalse(request.isValid)
    }

    @Test
    fun pagesUnderMinimum_rejected() {
        val request = RecoveryRequest(
            sessionId = validUuid,
            protocolVersion = 1,
            requiredSeconds = 600L,
            requiredPages = 0,
            createdAt = now,
            expiresAt = now + 3600_000L
        )
        assertFalse(request.isValid)
    }

    @Test
    fun pagesOverMaximum_rejected() {
        val request = RecoveryRequest(
            sessionId = validUuid,
            protocolVersion = 1,
            requiredSeconds = 600L,
            requiredPages = 501,
            createdAt = now,
            expiresAt = now + 3600_000L
        )
        assertFalse(request.isValid)
    }

    @Test
    fun expiresAtBeforeCreatedAt_rejected() {
        val request = RecoveryRequest(
            sessionId = validUuid,
            protocolVersion = 1,
            requiredSeconds = 600L,
            requiredPages = 5,
            createdAt = now,
            expiresAt = now - 1000L
        )
        assertFalse(request.isValid)
    }

    @Test
    fun expiresAtEqualsCreatedAt_rejected() {
        val request = RecoveryRequest(
            sessionId = validUuid,
            protocolVersion = 1,
            requiredSeconds = 600L,
            requiredPages = 5,
            createdAt = now,
            expiresAt = now
        )
        assertFalse(request.isValid)
    }

    @Test
    fun nonPositiveCreatedAt_rejected() {
        val request = RecoveryRequest(
            sessionId = validUuid,
            protocolVersion = 1,
            requiredSeconds = 600L,
            requiredPages = 5,
            createdAt = 0L,
            expiresAt = 1000L
        )
        assertFalse(request.isValid)
    }
}
