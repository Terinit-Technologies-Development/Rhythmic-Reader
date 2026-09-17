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
    fun nonUuidSessionId_rejected() {
        val request = RecoveryRequest(
            sessionId = "not-a-valid-uuid",
            protocolVersion = 1,
            requiredSeconds = 1800L,
            requiredPages = 10,
            createdAt = now,
            expiresAt = now + 3600_000L
        )
        assertFalse(request.isValid)
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
