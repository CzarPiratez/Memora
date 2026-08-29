package com.memora.app.application.notes

import com.memora.app.application.memory.MemoryEvidenceExcerptSearch
import com.memora.app.application.memory.SearchMemoryEvidence
import com.memora.app.domain.asset.AssetType
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads how much READY note Memory evidence is available for keyword Find.
 *
 * MIG-07 readiness source: Memory evidence substrate (not raw
 * NotePageExtractionDao searchable corpus). Honest inventory only — does
 * not claim meaning recall. [noteCount] is distinct note assets
 * with READY Memory evidence (not raw note extraction rows).
 */
class LoadPersistedNotePageKeywordSearchReadiness @Inject constructor(
    private val excerptSearch: MemoryEvidenceExcerptSearch,
) {
    suspend operator fun invoke(): NotePageKeywordSearchReadiness =
        withContext(Dispatchers.IO) {
            val counts = excerptSearch.countCurrentReadyEvidenceCorpus(
                assemblySchemaVersion = SearchMemoryEvidence.DEFAULT_ASSEMBLY_SCHEMA,
                assetType = AssetType.NOTE,
            )
            NotePageKeywordSearchReadiness(noteCount = counts.documentCount)
        }
}

data class NotePageKeywordSearchReadiness(
    val noteCount: Int,
) {
    init {
        require(noteCount >= 0) { "Note count cannot be negative." }
    }

    val isEmpty: Boolean get() = noteCount == 0
}
