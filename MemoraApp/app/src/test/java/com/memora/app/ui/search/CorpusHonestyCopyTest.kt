package com.memora.app.ui.search

import com.memora.app.domain.memory.CorpusCompletenessBlocked
import com.memora.app.domain.memory.CorpusCompletenessCounts
import com.memora.app.domain.memory.CorpusCompletenessSnapshot
import org.junit.Assert.assertTrue
import org.junit.Test

class CorpusHonestyCopyTest {
    @Test
    fun summary_lists_indexed_pending_and_blocked() {
        val body = CorpusHonestyCopy.summaryBody(
            CorpusCompletenessSnapshot(
                counts = CorpusCompletenessCounts(
                    memoriesReady = 2,
                    memoriesPendingAssembly = 1,
                    meaningSummaryIndexed = 3,
                    meaningEvidenceIndexed = 4,
                    meaningIndexPending = 2,
                ),
                blocked = CorpusCompletenessBlocked.BuildMeaningIndex,
            ),
        )
        assertTrue(body.contains("Indexed on this phone"))
        assertTrue(body.contains("3 memory summaries"))
        assertTrue(body.contains("4 evidence vectors"))
        assertTrue(body.contains("Pending:"))
        assertTrue(body.contains("2 memories await meaning indexing"))
        assertTrue(body.contains("1 asset awaits memory build"))
        assertTrue(body.contains("Blocked:"))
        assertTrue(body.contains("build the meaning index"))
    }

    @Test
    fun asset_memory_readiness_mentions_pending_assembly() {
        val body = CorpusHonestyCopy.assetMemoryReadiness(
            CorpusCompletenessCounts(
                memoriesReady = 5,
                memoriesPendingAssembly = 2,
                meaningSummaryIndexed = 0,
                meaningEvidenceIndexed = 0,
                meaningIndexPending = 0,
            ),
        )
        assertTrue(body.contains("5 Asset Memories"))
        assertTrue(body.contains("2 assets still need memory build"))
    }
}
