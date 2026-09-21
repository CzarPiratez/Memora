package com.memora.app.application.notes

import com.memora.app.domain.asset.SourceAvailabilityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteOpenClassPolicyTest {
    @Test
    fun missing_asset_is_unreachable_and_persisted() {
        val learned = NoteOpenClassPolicy.learn(
            assetPresent = false,
            openTargetPresent = false,
            vaultedGrantPresent = true,
        )
        assertEquals(SourceAvailabilityStatus.UNREACHABLE, learned!!.status)
        assertTrue(learned.persistUnreachable)
    }

    @Test
    fun stored_open_url_is_not_assumed_gone() {
        assertNull(
            NoteOpenClassPolicy.learn(
                assetPresent = true,
                openTargetPresent = true,
                vaultedGrantPresent = false,
            ),
        )
    }

    @Test
    fun vaulted_grant_without_url_leaves_graph_fallback_unprobed() {
        assertNull(
            NoteOpenClassPolicy.learn(
                assetPresent = true,
                openTargetPresent = false,
                vaultedGrantPresent = true,
            ),
        )
    }

    @Test
    fun no_url_and_no_grant_is_standing_unreachable_without_persist() {
        val learned = NoteOpenClassPolicy.learn(
            assetPresent = true,
            openTargetPresent = false,
            vaultedGrantPresent = false,
        )
        assertEquals(SourceAvailabilityStatus.UNREACHABLE, learned!!.status)
        assertFalse(learned.persistUnreachable)
    }
}
