package com.memora.app.application.notes

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

/** MIG-07 note adapter: MemoryEvidence hits → NotePageKeywordSearchHit. */
class NoteMemoryEvidenceKeywordAdapterTest {
    @Test
    fun maps_note_hit_with_source_identity() {
        val hit = sampleHit(
            locator = "note:body",
            excerpt = "…travel plan…",
        )
        val mapped = NoteMemoryEvidenceKeywordAdapter.toNoteHit(hit)
        assertEquals("Ideas", mapped.label)
        assertEquals("…travel plan…", mapped.excerpt)
        assertEquals("microsoft.onenote", mapped.sourceId)
        assertEquals("p1", mapped.sourceAssetKey)
    }

    @Test
    fun maps_matches_outcome_preserving_all_hits() {
        val outcome = MemoryEvidenceSearchOutcome.Matches(
            query = "plan",
            hits = listOf(
                sampleHit(locator = "note:body", excerpt = "plan body"),
                sampleHit(
                    locator = "note:title",
                    excerpt = "plan title",
                    evidenceId = "e-title",
                ),
            ),
            limitReached = false,
        )
        val note = NoteMemoryEvidenceKeywordAdapter.toNoteOutcome(outcome)
            as NotePageKeywordSearchOutcome.Matches
        assertEquals(2, note.hits.size)
        assertTrue(note.hits.all { it.excerpt.contains("plan") })
    }

    @Test
    fun blank_and_nothing_saved_pass_through() {
        assertEquals(
            NotePageKeywordSearchOutcome.BlankQuery,
            NoteMemoryEvidenceKeywordAdapter.toNoteOutcome(
                MemoryEvidenceSearchOutcome.BlankQuery,
            ),
        )
        val nothing = NoteMemoryEvidenceKeywordAdapter.toNoteOutcome(
            MemoryEvidenceSearchOutcome.NothingSavedToSearch(query = "hello"),
        ) as NotePageKeywordSearchOutcome.NothingSavedToSearch
        assertEquals("hello", nothing.query)
    }

    private fun sampleHit(
        locator: String,
        excerpt: String,
        evidenceId: String = "e-note",
    ) = MemoryEvidenceSearchHit(
        memoryId = MemoryId("mem-note"),
        revisionId = MemoryRevisionId("rev-note"),
        evidenceId = MemoryEvidenceId(evidenceId),
        kind = MemoryEvidenceKind.NOTE_TEXT,
        locator = EvidenceLocator(locator),
        excerpt = excerpt,
        sourceId = SourceId("microsoft.onenote"),
        sourceAssetKey = SourceAssetKey("p1"),
        assetType = AssetType.NOTE,
        label = "Ideas",
        openPageNumber = null,
        retrievalPath = MemoryEvidenceRetrievalPath.KEYWORD,
    )
}
