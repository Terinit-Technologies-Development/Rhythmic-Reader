package com.terinit.rhythmicreader.integration.rhythmic

import java.util.UUID

data class RecoveryRequest(
    val sessionId: String,
    val protocolVersion: Int,
    val requiredSeconds: Long,
    val requiredPages: Int,
    val createdAt: Long,
    val expiresAt: Long
) {
    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val reason: String) : ValidationResult
    }

    fun validate(): ValidationResult {
        if (protocolVersion != RecoveryProtocol.PROTOCOL_VERSION) {
            return ValidationResult.Invalid("Unsupported protocol version: $protocolVersion (expected ${RecoveryProtocol.PROTOCOL_VERSION})")
        }
        if (!isValidUuid(sessionId)) {
            return ValidationResult.Invalid("Invalid session ID format; must be UUID: $sessionId")
        }
        if (requiredSeconds !in MIN_REQUIRED_SECONDS..MAX_REQUIRED_SECONDS) {
            return ValidationResult.Invalid("requiredSeconds $requiredSeconds out of bounds ($MIN_REQUIRED_SECONDS..$MAX_REQUIRED_SECONDS)")
        }
        if (requiredPages !in MIN_REQUIRED_PAGES..MAX_REQUIRED_PAGES) {
            return ValidationResult.Invalid("requiredPages $requiredPages out of bounds ($MIN_REQUIRED_PAGES..$MAX_REQUIRED_PAGES)")
        }
        if (createdAt <= 0L) {
            return ValidationResult.Invalid("createdAt must be positive: $createdAt")
        }
        if (expiresAt <= createdAt) {
            return ValidationResult.Invalid("expiresAt ($expiresAt) must be strictly greater than createdAt ($createdAt)")
        }
        return ValidationResult.Valid
    }

    val isValid: Boolean
        get() = validate() is ValidationResult.Valid

    companion object {
        const val MIN_REQUIRED_SECONDS = 60L
        const val MAX_REQUIRED_SECONDS = 21600L // 6 hours
        const val MIN_REQUIRED_PAGES = 1
        const val MAX_REQUIRED_PAGES = 500

        fun isValidUuid(uuid: String): Boolean {
            return runCatching {
                UUID.fromString(uuid).toString().equals(uuid, ignoreCase = true)
            }.getOrDefault(false)
        }
    }
}
