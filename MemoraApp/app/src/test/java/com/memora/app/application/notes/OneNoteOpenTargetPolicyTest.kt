package com.memora.app.application.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OneNoteOpenTargetPolicyTest {
    @Test
    fun the_onenote_app_link_is_attempted_before_the_browser() {
        assertEquals(
            listOf("onenote:https://d.docs.live.net/page", "https://onedrive.live.com/page"),
            OneNoteOpenTargetPolicy.orderedTargets(
                webUrl = "https://onedrive.live.com/page",
                clientUrl = "onenote:https://d.docs.live.net/page",
            ),
        )
    }

    @Test
    fun the_browser_still_opens_when_graph_returns_no_client_link() {
        assertEquals(
            listOf("https://onedrive.live.com/page"),
            OneNoteOpenTargetPolicy.orderedTargets(
                webUrl = "https://onedrive.live.com/page",
                clientUrl = "   ",
            ),
        )
        assertEquals(
            emptyList<String>(),
            OneNoteOpenTargetPolicy.orderedTargets(webUrl = null, clientUrl = null),
        )
    }

    @Test
    fun an_identical_pair_is_not_attempted_twice() {
        assertEquals(
            listOf("https://onedrive.live.com/page"),
            OneNoteOpenTargetPolicy.orderedTargets(
                webUrl = "https://onedrive.live.com/page",
                clientUrl = " https://onedrive.live.com/page ",
            ),
        )
    }

    @Test
    fun browsable_is_required_for_web_links_and_withheld_from_app_schemes() {
        assertTrue(OneNoteOpenTargetPolicy.needsBrowsableCategory("https"))
        assertTrue(OneNoteOpenTargetPolicy.needsBrowsableCategory("HTTP"))
        assertFalse(OneNoteOpenTargetPolicy.needsBrowsableCategory("onenote"))
        assertFalse(OneNoteOpenTargetPolicy.needsBrowsableCategory(null))
    }
}
