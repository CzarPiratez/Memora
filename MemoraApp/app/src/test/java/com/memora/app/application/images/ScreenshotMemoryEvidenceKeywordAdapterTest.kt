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

/** MIG-07 screenshot adapter: MemoryEvidence hits → ScreenshotOcrKeywordSearchHit. */
class ScreenshotMemoryEvidenceKeywordAdapterTest {
    @Test
    fun maps_screenshot_hit_without_page() {
        val hit = sampleHit(
            locator = "image:whole",
            excerpt = "…Screenshot note…",
        )
        val mapped = ScreenshotMemoryEvidenceKeywordAdapter.toScreenshotHit(hit)
        assertEquals("Screenshot_memora_note.png", mapped.label)
        assertEquals("…Screenshot note…", mapped.excerpt)
        assertEquals("android-media-store-images", mapped.sourceId)
        assertEquals("external_primary:1", mapped.sourceAssetKey)
    }

    @Test
    fun maps_matches_outcome_preserving_all_hits() {
        val outcome = MemoryEvidenceSearchOutcome.Matches(
            query = "note",
            hits = listOf(
                sampleHit(locator = "image:whole", excerpt = "note body"),
                sampleHit(
                    locator = "image:exif",
                    excerpt = "note camera",
                    evidenceId = "e-exif",
                ),
            ),
            limitReached = false,
        )
        val screenshot = ScreenshotMemoryEvidenceKeywordAdapter.toScreenshotOutcome(outcome)
            as ScreenshotOcrKeywordSearchOutcome.Matches
        assertEquals(2, screenshot.hits.size)
        assertTrue(screenshot.hits.all { it.excerpt.contains("note") })
    }

    @Test
    fun blank_and_nothing_saved_pass_through() {
        assertEquals(
            ScreenshotOcrKeywordSearchOutcome.BlankQuery,
            ScreenshotMemoryEvidenceKeywordAdapter.toScreenshotOutcome(
                MemoryEvidenceSearchOutcome.BlankQuery,
            ),
        )
        val nothing = ScreenshotMemoryEvidenceKeywordAdapter.toScreenshotOutcome(
            MemoryEvidenceSearchOutcome.NothingSavedToSearch(query = "hello"),
        ) as ScreenshotOcrKeywordSearchOutcome.NothingSavedToSearch
        assertEquals("hello", nothing.query)
    }

    private fun sampleHit(
        locator: String,
        excerpt: String,
        evidenceId: String = "e-ss",
    ) = MemoryEvidenceSearchHit(
        memoryId = MemoryId("mem-ss"),
        revisionId = MemoryRevisionId("rev-ss"),
        evidenceId = MemoryEvidenceId(evidenceId),
        kind = MemoryEvidenceKind.OCR_TEXT,
        locator = EvidenceLocator(locator),
        excerpt = excerpt,
        sourceId = SourceId("android-media-store-images"),
        sourceAssetKey = SourceAssetKey("external_primary:1"),
        assetType = AssetType.SCREENSHOT,
        label = "Screenshot_memora_note.png",
        openPageNumber = null,
        retrievalPath = MemoryEvidenceRetrievalPath.KEYWORD,
    )
}
