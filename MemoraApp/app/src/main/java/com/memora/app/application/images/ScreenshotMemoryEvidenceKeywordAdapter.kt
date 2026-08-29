package com.memora.app.application.images

import com.memora.app.application.memory.MemoryEvidenceLiteralSearchSupport
import com.memora.app.application.memory.MemoryEvidenceSearchHit
import com.memora.app.application.memory.MemoryEvidenceSearchOutcome

/**
 * Maps MIG-06/07 [MemoryEvidenceSearchOutcome] into existing screenshot keyword UI models.
 *
 * Screenshots are not page-grounded (locator is typically `image:whole`). All filtered
 * SCREENSHOT evidence hits map through; open-original uses source identity only.
 */
internal object ScreenshotMemoryEvidenceKeywordAdapter {
    fun toScreenshotOutcome(outcome: MemoryEvidenceSearchOutcome): ScreenshotOcrKeywordSearchOutcome =
        when (outcome) {
            MemoryEvidenceSearchOutcome.BlankQuery -> ScreenshotOcrKeywordSearchOutcome.BlankQuery
            is MemoryEvidenceSearchOutcome.NothingSavedToSearch ->
                ScreenshotOcrKeywordSearchOutcome.NothingSavedToSearch(query = outcome.query)
            is MemoryEvidenceSearchOutcome.Matches -> {
                val hits = outcome.hits.map(::toScreenshotHit)
                ScreenshotOcrKeywordSearchOutcome.Matches(
                    query = outcome.query,
                    hits = hits,
                    limitReached = outcome.limitReached &&
                        hits.size >= MemoryEvidenceLiteralSearchSupport.MAX_RESULTS,
                )
            }
        }

    fun toScreenshotHit(hit: MemoryEvidenceSearchHit): ScreenshotOcrKeywordSearchHit =
        ScreenshotOcrKeywordSearchHit(
            label = hit.label,
            excerpt = hit.excerpt,
            sourceId = hit.sourceId.value,
            sourceAssetKey = hit.sourceAssetKey.value,
        )
}
