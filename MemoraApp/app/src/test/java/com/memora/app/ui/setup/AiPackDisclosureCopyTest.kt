package com.memora.app.ui.setup

import com.memora.app.domain.intelligence.AiPackInstallState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPackDisclosureCopyTest {
    @Test
    fun honesty_copy_names_model_download_and_keeps_keyword_path() {
        val all = listOf(
            AiPackDisclosureCopy.ENTRY_LABEL,
            AiPackDisclosureCopy.LEAD_BODY,
            AiPackDisclosureCopy.SCOPE_BODY,
            AiPackDisclosureCopy.NETWORK_BODY,
            AiPackDisclosureCopy.STATUS_NEED_MODEL,
            AiPackDisclosureCopy.STATUS_MODEL_READY,
            AiPackDisclosureCopy.DOWNLOAD_MODEL_LABEL,
            AiPackDisclosureCopy.BUILD_INDEX_LABEL,
            AiPackDisclosureCopy.FEEDBACK_MODEL_INSTALLED,
        ).joinToString(" ").lowercase()

        assertTrue(all.contains("on-device"))
        assertTrue(all.contains("model"))
        assertTrue(AiPackDisclosureCopy.SCOPE_BODY.contains("Universal Sentence Encoder"))
        assertTrue(AiPackDisclosureCopy.SIZE_BODY.contains("rebuild the meaning index"))
        assertTrue(AiPackDisclosureCopy.NETWORK_BODY.contains("model bytes only"))
        assertFalse(all.contains("available now"))
        assertFalse(all.contains("uploads your memories"))
    }

    @Test
    fun status_body_tracks_model_install() {
        assertTrue(
            AiPackDisclosureCopy.statusBody(
                installationState = AiPackInstallState.NOT_INSTALLED,
                disclosureAcknowledged = true,
                modelInstalled = false,
                embeddingAvailable = false,
            ).contains("Download"),
        )
        assertTrue(
            AiPackDisclosureCopy.statusBody(
                installationState = AiPackInstallState.ACTIVE,
                disclosureAcknowledged = true,
                modelInstalled = true,
                embeddingAvailable = true,
            ).contains("Meaning model is installed"),
        )
    }
}
