package com.memora.app.domain.notes

/**
 * Public Microsoft identity client configuration for the OneNote connector.
 *
 * Client ID is a public identifier (not a secret). Signature hash is required for
 * the Android redirect URI. Empty values mean registration is not configured.
 */
data class OneNoteAuthConfiguration(
    val clientId: String,
    val signatureHash: String,
) {
    val isRegistrationConfigured: Boolean
        get() = clientId.isNotBlank() && signatureHash.isNotBlank()

    companion object {
        const val REDIRECT_URI_SCHEME = "msauth"
        const val PACKAGE_NAME = "com.memora.app"

        fun redirectUri(signatureHash: String): String =
            "$REDIRECT_URI_SCHEME://$PACKAGE_NAME/$signatureHash"
    }
}
