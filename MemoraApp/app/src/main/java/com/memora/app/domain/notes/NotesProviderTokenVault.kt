package com.memora.app.domain.notes

/**
 * Opaque OneNote-class provider session kept only in Memora-owned secure storage.
 *
 * Access tokens must never appear in Git, BuildConfig secrets, or packaged
 * resource fixtures. N2a stores sessions written by tests or future MSAL (N2b).
 */
data class NotesProviderSession(
    val providerId: String,
    val accountId: String,
    val accountDisplayLabel: String,
    val accessToken: String,
    val accessTokenExpiresAtEpochMs: Long,
) {
    init {
        require(providerId.isNotBlank()) { "providerId must not be blank." }
        require(accountId.isNotBlank()) { "accountId must not be blank." }
        require(accountDisplayLabel.isNotBlank()) { "accountDisplayLabel must not be blank." }
        require(accessToken.isNotBlank()) { "accessToken must not be blank." }
        require(accessTokenExpiresAtEpochMs > 0L) {
            "accessTokenExpiresAtEpochMs must be positive."
        }
    }

    companion object {
        const val PROVIDER_MICROSOFT_ONENOTE = "microsoft.onenote"
    }
}

/**
 * Platform-backed secure vault for provider session material.
 * Implementations must use Android Keystore (or equal) and no-backup private files.
 */
interface NotesProviderTokenVault {
    fun readSession(): NotesProviderSession?

    fun writeSession(session: NotesProviderSession)

    fun clearSession()
}
