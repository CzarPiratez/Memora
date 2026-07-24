package com.memora.app.data.security

/**
 * Content-free local categories for database-secret recovery diagnostics.
 * User-facing copy must never mention SQLCipher, keys, or encryption failures.
 */
enum class DatabaseSecretFailureCategory {
    KEY_UNAVAILABLE,
    WRAPPER_INVALID,
    DATABASE_AUTH_FAILED,
    CONVERSION_VALIDATION_FAILED,
    WAITING_FOR_USER_UNLOCK,
    USER_CONFIRMED_INDEX_CLEAR,
}
