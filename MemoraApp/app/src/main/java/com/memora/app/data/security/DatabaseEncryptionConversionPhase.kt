package com.memora.app.data.security

/**
 * Crash-resume phases for a forward-only plaintext-to-encrypted database conversion.
 * Plaintext files must remain until [COMPLETED] after a validated encrypted reopen.
 */
enum class DatabaseEncryptionConversionPhase {
    NOT_STARTED,
    PLAINTEXT_VALIDATED,
    CANDIDATE_CREATED,
    ROWS_COPIED,
    CANDIDATE_VALIDATED,
    SWITCH_PENDING,
    COMPLETED,
    FAILED_SAFE,
}
