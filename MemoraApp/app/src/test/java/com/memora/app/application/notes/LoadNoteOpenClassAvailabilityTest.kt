package com.memora.app.application.notes

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceAvailabilityStatus
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.notes.NotesProviderSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LoadNoteOpenClassAvailabilityTest {
    private val noteId = AssetIdentity(
        SourceId(NotesProviderSession.PROVIDER_MICROSOFT_ONENOTE),
        SourceAssetKey("page-1"),
    )
    private val photoId = AssetIdentity(SourceId("photos"), SourceAssetKey("42"))

    @Test
    fun stored_observation_wins_over_open_class() = runTest {
        val persisted = mutableListOf<AssetIdentity>()
        val loader = loader(noteAsset(), hasTarget = false, grant = false, persisted)
        val stored = mapOf(noteId to SourceAvailabilityStatus.REACHABLE)
        val result = loader.enrich(listOf(noteId), stored)
        assertEquals(SourceAvailabilityStatus.REACHABLE, result[noteId])
        assertTrue(persisted.isEmpty())
    }

    @Test
    fun missing_note_asset_persists_unreachable() = runTest {
        val persisted = mutableListOf<AssetIdentity>()
        val loader = loader(asset = null, hasTarget = false, grant = true, persisted)
        val stored = mapOf(noteId to SourceAvailabilityStatus.UNKNOWN)
        val result = loader.enrich(listOf(noteId), stored)
        assertEquals(SourceAvailabilityStatus.UNREACHABLE, result[noteId])
        assertEquals(listOf(noteId), persisted)
    }

    @Test
    fun disconnect_without_open_url_is_not_persisted() = runTest {
        val persisted = mutableListOf<AssetIdentity>()
        val loader = loader(noteAsset(), hasTarget = false, grant = false, persisted)
        val stored = mapOf(noteId to SourceAvailabilityStatus.UNKNOWN)
        val result = loader.enrich(listOf(noteId), stored)
        assertEquals(SourceAvailabilityStatus.UNREACHABLE, result[noteId])
        assertTrue(persisted.isEmpty())
    }

    @Test
    fun stored_open_url_stays_unknown() = runTest {
        val persisted = mutableListOf<AssetIdentity>()
        val loader = loader(noteAsset(), hasTarget = true, grant = false, persisted)
        val stored = mapOf(noteId to SourceAvailabilityStatus.UNKNOWN)
        val result = loader.enrich(listOf(noteId), stored)
        assertEquals(SourceAvailabilityStatus.UNKNOWN, result[noteId])
        assertTrue(persisted.isEmpty())
    }

    @Test
    fun vaulted_grant_without_url_stays_unknown() = runTest {
        val persisted = mutableListOf<AssetIdentity>()
        val loader = loader(noteAsset(), hasTarget = false, grant = true, persisted)
        val stored = mapOf(noteId to SourceAvailabilityStatus.UNKNOWN)
        val result = loader.enrich(listOf(noteId), stored)
        assertEquals(SourceAvailabilityStatus.UNKNOWN, result[noteId])
        assertTrue(persisted.isEmpty())
    }

    @Test
    fun missing_stored_key_is_treated_as_unknown() = runTest {
        val persisted = mutableListOf<AssetIdentity>()
        val loader = loader(noteAsset(), hasTarget = false, grant = false, persisted)
        val result = loader.enrich(listOf(noteId), emptyMap())
        assertEquals(SourceAvailabilityStatus.UNREACHABLE, result[noteId])
        assertTrue(persisted.isEmpty())
    }

    @Test
    fun photos_are_not_note_open_class() = runTest {
        val persisted = mutableListOf<AssetIdentity>()
        val loader = loader(photoAsset(), hasTarget = false, grant = false, persisted)
        val stored = mapOf(photoId to SourceAvailabilityStatus.UNKNOWN)
        val result = loader.enrich(listOf(photoId), stored)
        assertEquals(SourceAvailabilityStatus.UNKNOWN, result[photoId])
        assertTrue(persisted.isEmpty())
    }

    private fun loader(
        asset: Asset?,
        hasTarget: Boolean,
        grant: Boolean,
        persisted: MutableList<AssetIdentity>,
    ) = LoadNoteOpenClassAvailability(
        findAsset = { asset },
        hasOpenTarget = { hasTarget },
        vaultedGrantPresent = { grant },
        persistUnreachable = { persisted += it },
    )

    private fun noteAsset() = Asset(
        identity = noteId,
        type = AssetType.NOTE,
        location = AssetLocation("onenote:page-1"),
        fingerprint = AssetFingerprint("fp"),
        discoveredAt = Instant.parse("2026-09-21T00:00:00Z"),
        displayName = "Class notes",
    )

    private fun photoAsset() = Asset(
        identity = photoId,
        type = AssetType.PHOTO,
        location = AssetLocation("content://photo/42"),
        fingerprint = AssetFingerprint("fp-photo"),
        discoveredAt = Instant.parse("2026-09-21T00:00:00Z"),
        displayName = "lake.jpg",
    )
}
