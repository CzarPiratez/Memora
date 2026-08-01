package com.memora.app.domain.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OneNoteAuthConfigurationTest {

    @Test
    fun registrationRequiresBothPublicClientFields() {
        assertFalse(
            OneNoteAuthConfiguration(clientId = "", signatureHash = "hash")
                .isRegistrationConfigured,
        )
        assertFalse(
            OneNoteAuthConfiguration(clientId = "client", signatureHash = "")
                .isRegistrationConfigured,
        )
        assertTrue(
            OneNoteAuthConfiguration(clientId = "client", signatureHash = "hash")
                .isRegistrationConfigured,
        )
    }

    @Test
    fun redirectUriUsesPackageAndSignatureHash() {
        assertEquals(
            "msauth://com.memora.app/AbCd",
            OneNoteAuthConfiguration.redirectUri("AbCd"),
        )
    }
}
