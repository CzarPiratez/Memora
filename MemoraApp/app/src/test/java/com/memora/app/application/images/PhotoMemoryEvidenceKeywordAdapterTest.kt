package com.memora.app.application.images

import com.memora.app.application.memory.MemoryEvidenceRetrievalPath
import com.memora.app.application.memory.MemoryEvidenceSearchHit
import com.memora.app.application.memory.MemoryEvidenceSearchOutcome
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.memory.EvidenceLocator
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceKind
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** MIG-07 photo adapter: MemoryEvidence hits → PhotoOcrKeywordSearchHit. */
class PhotoMemoryEvidenceKeywordAdapterTest {
    @Test
    fun maps_photo_hit_without_page() {
        val hit = sampleHit(
            locator = "image:whole",
            excerpt = "…Total 42…",
        )
        val mapped = PhotoMemoryEvidenceKeywordAdapter.toPhotoHit(hit)
        assertEquals("receipt.jpg", mapped.label)
        assertEquals("…Total 42…", mapped.excerpt)
        assertEquals("android-media-store-images", mapped.sourceId)
        assertEquals("external_primary:42", mapped.sourceAssetKey)
    }

    @Test
    fun maps_matches_outcome_preserving_all_hits() {
        val outcome = MemoryEvidenceSearchOutcome.Matches(
            query = "total",
            hits = listOf(
                sampleHit(locator = "image:whole", excerpt = "total body"),
                sampleHit(
                    locator = "image:exif",
                    excerpt = "total camera",
                    evidenceId = "e-exif",
                ),
            ),
            limitReached = false,
        )
        val photo = PhotoMemoryEvidenceKeywordAdapter.toPhotoOutcome(outcome)
            as PhotoOcrKeywordSearchOutcome.Matches
        assertEquals(2, photo.hits.size)
        assertTrue(photo.hits.all { it.excerpt.contains("total") })
    }

    @Test
    fun blank_and_nothing_saved_pass_through() {
        assertEquals(
            PhotoOcrKeywordSearchOutcome.BlankQuery,
            PhotoMemoryEvidenceKeywordAdapter.toPhotoOutcome(
                MemoryEvidenceSearchOutcome.BlankQuery,
            ),
        )
        val nothing = PhotoMemoryEvidenceKeywordAdapter.toPhotoOutcome(
            MemoryEvidenceSearchOutcome.NothingSavedToSearch(query = "hello"),
        ) as PhotoOcrKeywordSearchOutcome.NothingSavedToSearch
        assertEquals("hello", nothing.query)
    }

    private fun sampleHit(
        locator: String,
        excerpt: String,
        evidenceId: String = "e-photo",
    ) = MemoryEvidenceSearchHit(
        memoryId = MemoryId("mem-photo"),
        revisionId = MemoryRevisionId("rev-photo"),
        evidenceId = MemoryEvidenceId(evidenceId),
        kind = MemoryEvidenceKind.OCR_TEXT,
        locator = EvidenceLocator(locator),
        excerpt = excerpt,
        sourceId = SourceId("android-media-store-images"),
        sourceAssetKey = SourceAssetKey("external_primary:42"),
        assetType = AssetType.PHOTO,
        label = "receipt.jpg",
        openPageNumber = null,
        retrievalPath = MemoryEvidenceRetrievalPath.KEYWORD,
    )
}
