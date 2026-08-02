package com.memora.app.ui.setup

import org.junit.Assert.assertTrue
import org.junit.Test

class AiPackDisclosureViewModelTest {
    @Test
    fun acknowledge_and_download_labels_remain_honest() {
        assertTrue(AiPackDisclosureCopy.FEEDBACK_ACKNOWLEDGED.contains("meaning model"))
        assertTrue(AiPackDisclosureCopy.DOWNLOAD_MODEL_LABEL.contains("Download"))
        assertTrue(AiPackDisclosureCopy.NETWORK_BODY.contains("model bytes only"))
    }
}
