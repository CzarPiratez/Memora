package com.memora.app.ui.setup

import com.memora.app.domain.intelligence.AiPackInstallState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPackDisclosureCopyTest {
    @Test
    fun honesty_copy_never_claims_available_or_meaning_search_ready() {
        val all = listOf(
            AiPackDisclosureCopy.ENTRY_LABEL,
            AiPackDisclosureCopy.SCREEN_TITLE,
            AiPackDisclosureCopy.LEAD_BODY,
            AiPackDisclosureCopy.SCOPE_BODY,
            AiPackDisclosureCopy.NETWORK_BODY,
            AiPackDisclosureCopy.SIZE_BODY,
            AiPackDisclosureCopy.LICENSE_BODY,
            AiPackDisclosureCopy.STATUS_NOT_ACKNOWLEDGED,
            AiPackDisclosureCopy.STATUS_ACKNOWLEDGED_NOT_INSTALLED,
            AiPackDisclosureCopy.STATUS_ACTIVE_NOT_CLAIMING_AVAILABLE,
            AiPackDisclosureCopy.ACKNOWLEDGE_LABEL,
            AiPackDisclosureCopy.FEEDBACK_ACKNOWLEDGED,
        ).joinToString(" ").lowercase()

        assertTrue(all.contains("on-device"))
        assertTrue(all.contains("keyword"))
        assertTrue(all.contains("estimate") || all.contains("planning"))
        assertTrue(AiPackDisclosureCopy.NETWORK_BODY.contains("does not download"))
        assertTrue(AiPackDisclosureCopy.FEEDBACK_ACKNOWLEDGED.contains("Meaning search stays off"))

        assertFalse(all.contains("available now"))
        assertFalse(all.contains("meaning search is ready"))
        assertFalse(all.contains("ai is ready"))
    }

    @Test
    fun status_body_stays_honest_for_install_states() {
        assertTrue(
            AiPackDisclosureCopy.statusBody(
                installationState = AiPackInstallState.NOT_INSTALLED,
                disclosureAcknowledged = false,
            ).contains("No disclosure"),
        )
        assertTrue(
            AiPackDisclosureCopy.statusBody(
                installationState = AiPackInstallState.NOT_INSTALLED,
                disclosureAcknowledged = true,
            ).contains("not installed"),
        )
        assertTrue(
            AiPackDisclosureCopy.statusBody(
                installationState = AiPackInstallState.ACTIVE,
                disclosureAcknowledged = true,
            ).contains("does not claim"),
        )
    }
}
