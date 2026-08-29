package com.memora.app.application.images

import com.memora.app.application.memory.MemoryEvidenceLiteralSearchSupport
import com.memora.app.application.memory.MemoryEvidenceSearchHit
import com.memora.app.application.memory.MemoryEvidenceSearchOutcome

/**
 * Maps MIG-06/07 [MemoryEvidenceSearchOutcome] into existing photo keyword UI models.
 *
 * Photos are not page-grounded (locator is typically `image:whole`). All filtered
 * PHOTO evidence hits map through; open-original uses source identity only.
 */
internal object PhotoMemoryEvidenceKeywordAdapter {
    fun toPhotoOutcome(outcome: MemoryEvidenceSearchOutcome): PhotoOcrKeywordSearchOutcome =
        when (outcome) {
            MemoryEvidenceSearchOutcome.BlankQuery -> PhotoOcrKeywordSearchOutcome.BlankQuery
            is MemoryEvidenceSearchOutcome.NothingSavedToSearch ->
                PhotoOcrKeywordSearchOutcome.NothingSavedToSearch(query = outcome.query)
            is MemoryEvidenceSearchOutcome.Matches -> {
                val hits = outcome.hits.map(::toPhotoHit)
                PhotoOcrKeywordSearchOutcome.Matches(
                    query = outcome.query,
                    hits = hits,
                    limitReached = outcome.limitReached &&
                        hits.size >= MemoryEvidenceLiteralSearchSupport.MAX_RESULTS,
                )
            }
        }

    fun toPhotoHit(hit: MemoryEvidenceSearchHit): PhotoOcrKeywordSearchHit =
        PhotoOcrKeywordSearchHit(
            label = hit.label,
            excerpt = hit.excerpt,
            sourceId = hit.sourceId.value,
            sourceAssetKey = hit.sourceAssetKey.value,
        )
}
