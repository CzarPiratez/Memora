package com.memora.app.application.notes

import com.memora.app.application.memory.MemoryEvidenceLiteralSearchSupport
import com.memora.app.application.memory.MemoryEvidenceSearchHit
import com.memora.app.application.memory.MemoryEvidenceSearchOutcome

/**
 * Maps MIG-06/07 [MemoryEvidenceSearchOutcome] into existing note keyword UI models.
 *
 * Notes are not PDF-page-grounded. All filtered NOTE evidence hits map through;
 * open-original uses source identity (OneNote URLs) only.
 */
internal object NoteMemoryEvidenceKeywordAdapter {
    fun toNoteOutcome(outcome: MemoryEvidenceSearchOutcome): NotePageKeywordSearchOutcome =
        when (outcome) {
            MemoryEvidenceSearchOutcome.BlankQuery -> NotePageKeywordSearchOutcome.BlankQuery
            is MemoryEvidenceSearchOutcome.NothingSavedToSearch ->
                NotePageKeywordSearchOutcome.NothingSavedToSearch(query = outcome.query)
            is MemoryEvidenceSearchOutcome.Matches -> {
                val hits = outcome.hits.map(::toNoteHit)
                NotePageKeywordSearchOutcome.Matches(
                    query = outcome.query,
                    hits = hits,
                    limitReached = outcome.limitReached &&
                        hits.size >= MemoryEvidenceLiteralSearchSupport.MAX_RESULTS,
                )
            }
        }

    fun toNoteHit(hit: MemoryEvidenceSearchHit): NotePageKeywordSearchHit =
        NotePageKeywordSearchHit(
            label = hit.label,
            excerpt = hit.excerpt,
            sourceId = hit.sourceId.value,
            sourceAssetKey = hit.sourceAssetKey.value,
        )
}
