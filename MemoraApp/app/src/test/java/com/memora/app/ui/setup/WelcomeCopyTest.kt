package com.memora.app.ui.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeCopyTest {
    @Test
    fun tagline_is_the_privacy_promise_not_a_recall_prompt() {
        assertEquals("UNFYND", WelcomeCopy.PRODUCT_NAME)
        assertEquals("Your privacy first, on-device AI", WelcomeCopy.TAGLINE)
        assertFalse(
            WelcomeCopy.TAGLINE.contains("trying to remember", ignoreCase = true),
        )
        assertTrue(WelcomeCopy.TAGLINE.contains("privacy", ignoreCase = true))
        assertTrue(WelcomeCopy.TAGLINE.contains("on-device AI"))
    }
}
