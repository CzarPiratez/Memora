package com.memora.app.application.documents

import com.memora.app.application.memory.MemoryEvidenceLiteralSearchSupport
import com.memora.app.application.memory.MemoryEvidenceSearchHit
import com.memora.app.application.memory.MemoryEvidenceSearchOutcome
import com.memora.app.domain.memory.PdfPageEvidenceLocator

/**
 * Maps MIG-06/07 [MemoryEvidenceSearchOutcome] into the existing PDF keyword UI models.
 *
 * Hits without a resolvable `pdf:page:N` open page are dropped so open-original and
 * Why stay page-grounded (parity with the retired extraction-table page Find).
 */
internal object PdfMemoryEvidenceKeywordAdapter {
    fun toPdfOutcome(outcome: MemoryEvidenceSearchOutcome): PdfKeywordSearchOutcome =
        when (outcome) {
            MemoryEvidenceSearchOutcome.BlankQuery -> PdfKeywordSearchOutcome.BlankQuery
            is MemoryEvidenceSearchOutcome.NothingSavedToSearch ->
                PdfKeywordSearchOutcome.NothingSavedToSearch(query = outcome.query)
            is MemoryEvidenceSearchOutcome.Matches -> {
                val hits = outcome.hits.mapNotNull(::toPdfHitOrNull)
                PdfKeywordSearchOutcome.Matches(
                    query = outcome.query,
                    hits = hits,
                    // Dropping non-page hits can shrink the list; never claim a full
                    // page unless the mapped UI list is actually full.
                    limitReached = outcome.limitReached &&
                        hits.size >= MemoryEvidenceLiteralSearchSupport.MAX_RESULTS,
                )
            }
        }

    fun toPdfHitOrNull(hit: MemoryEvidenceSearchHit): PdfKeywordSearchHit? {
        val pageNumber = hit.openPageNumber
            ?: PdfPageEvidenceLocator.parsePageNumber(hit.locator.value)
            ?: return null
        return PdfKeywordSearchHit(
            label = hit.label,
            pageNumber = pageNumber,
            excerpt = hit.excerpt,
            sourceId = hit.sourceId.value,
            sourceAssetKey = hit.sourceAssetKey.value,
        )
    }
}
