package com.memora.app.ui.search

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.application.intelligence.MeaningSearchReadiness
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeaningSearchCopyTest {
    @Test
    fun scope_and_readiness_stay_candidate_not_available() {
        val ready = MeaningSearchCopy.readinessBody(
            MeaningSearchReadiness.Ready(
                model = ModelVersionIdentity("m", "1"),
                indexedCount = 3,
                memoriesReadyCount = 3,
            ),
        )
        assertTrue(MeaningSearchCopy.SCOPE_BODY.contains("candidate", ignoreCase = true))
        assertTrue(ready.contains("AVAILABLE"))
        assertFalse(ready.contains("full measured meaning search is finished"))
        assertTrue(ready.contains("not a measured AVAILABLE"))
    }

    @Test
    fun why_cites_query_and_summary() {
        val why = MeaningSearchCopy.whyThisResult(
            hit = MeaningSearchHit(
                revisionId = MemoryRevisionId("r1"),
                memoryId = MemoryId("m1"),
                sourceId = SourceId("s"),
                sourceAssetKey = SourceAssetKey("k"),
                assetType = AssetType.NOTE,
                label = "Trip plan",
                summaryText = "Hotel confirmation near the cafe",
                score = 0.8f,
                model = ModelVersionIdentity("m", "1"),
            ),
            query = "hotel near coffee",
        )
        assertTrue(why.contains("hotel near coffee"))
        assertTrue(why.contains("Hotel confirmation near the cafe"))
        assertTrue(why.contains("Why this result?"))
    }

    @Test
    fun open_copy_stays_honest_about_pdf_page_one() {
        assertTrue(MeaningSearchCopy.OPEN_ORIGINAL_HINT.contains("page 1"))
        assertTrue(MeaningSearchCopy.OPEN_ORIGINAL_HINT.contains("not a live re-read"))
    }
}
