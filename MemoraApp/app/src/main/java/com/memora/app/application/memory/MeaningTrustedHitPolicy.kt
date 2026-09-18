package com.memora.app.application.memory

import com.memora.app.application.intelligence.MeaningSearchHit
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.intelligence.MeaningEvidenceLexicalFilter
import com.memora.app.domain.intelligence.MeaningRecallCue

/**
 * Trust policy for meaning result lists (scenario bar MF-1).
 *
 * Prefer a short band near the top score over padding to a fixed top-10 of
 * weak neighbors. When Exact and Partial hits share a list (defect D-20),
 * that band must not be measured against the Exact top score, and reserved
 * Partial seats prefer a modifier match (`swimming`) over a generic head
 * (`schedule`) so a schedule-only image cannot take the neighbour seats.
 *
 * Pictures of a document still outrank the PDF on USE cosine. Keyword Find
 * already returns those PDFs, so the shown five reserve document seats when
 * the ranked pool still has them — swapping images, not growing the list.
 */
object MeaningTrustedHitPolicy {
    const val MAX_TRUSTED_HITS = 5
    const val RELATIVE_SCORE_GAP = 0.22f

    /**
     * Seats reserved for Partial hits when Exact competitors exist, so the
     * shown five cannot be Exact-only. Leaves at least one seat for Exact.
     */
    const val RESERVED_PARTIAL_WHEN_MIXED = 2

    /**
     * Seats reserved for PDF/note originals when images would otherwise fill
     * the shown five. Leaves image neighbours in place if there is no image
     * seat to swap.
     */
    const val RESERVED_DOCUMENT_WHEN_IMAGES = 2

    fun apply(hits: List<MeaningSearchHit>, limit: Int): List<MeaningSearchHit> {
        if (hits.isEmpty()) return hits
        val cappedLimit = limit.coerceAtMost(MAX_TRUSTED_HITS).coerceAtLeast(1)
        val topScore = hits.first().score
        val inBand = hits.filter { hit ->
            topScore - hit.score <= RELATIVE_SCORE_GAP
        }
        return inBand.take(cappedLimit)
    }

    fun apply(
        hits: List<MeaningSearchHit>,
        limit: Int,
        rawQuery: String,
    ): List<MeaningSearchHit> {
        if (hits.isEmpty()) return hits
        val cap = limit.coerceAtMost(MAX_TRUSTED_HITS).coerceAtLeast(1)
        val required = MeaningRecallCue.contentTokens(rawQuery)
        val seated = if (required.size < 2) {
            apply(hits, limit)
        } else {
            seatMixedLexical(hits, limit, required)
        }
        return ensureDocumentSeats(seated, hits, cap)
    }

    private fun seatMixedLexical(
        hits: List<MeaningSearchHit>,
        limit: Int,
        required: List<String>,
    ): List<MeaningSearchHit> {
        val cue = MeaningEvidenceLexicalFilter.prepare(required)
        val exact = hits.filter { hit ->
            cue.matchingTokens(hit.lexicalHaystack()).size == required.size
        }
        val partial = hits.filter { hit ->
            val matched = cue.matchingTokens(hit.lexicalHaystack()).size
            matched in 1 until required.size
        }
        if (exact.isEmpty() || partial.isEmpty()) return apply(hits, limit)

        val cap = limit.coerceAtMost(MAX_TRUSTED_HITS).coerceAtLeast(1)
        val orderedPartial = orderPartialsByModifierThenScore(partial, required, cue)
        val reservedCount = minOf(RESERVED_PARTIAL_WHEN_MIXED, orderedPartial.size, cap - 1)
        val reservedPartial = orderedPartial.take(reservedCount)
        val exactKeep = apply(exact, cap - reservedPartial.size)
        val keepIds = (exactKeep + reservedPartial).map { it.revisionId }.toHashSet()
        val leftover = cap - keepIds.size
        if (leftover > 0) {
            orderedPartial.asSequence()
                .filter { it.revisionId !in keepIds }
                .take(leftover)
                .forEach { keepIds.add(it.revisionId) }
        }
        return hits.filter { it.revisionId in keepIds }
    }

    /**
     * English noun compounds put the distinctive modifier first (`swimming
     * schedule`, `passport photo`) and a generic head last. A Partial that only
     * has the head is a weaker neighbour than one that has the modifier.
     * Score order is kept inside each group. Not a synonym net: `schedule` is
     * never treated as `timetable`.
     */
    private fun orderPartialsByModifierThenScore(
        partial: List<MeaningSearchHit>,
        required: List<String>,
        cue: MeaningEvidenceLexicalFilter.PreparedCue,
    ): List<MeaningSearchHit> {
        val modifiers = required.dropLast(1).toSet()
        if (modifiers.isEmpty()) return partial
        val withModifier = partial.filter { hit ->
            cue.matchingTokens(hit.lexicalHaystack()).any { it in modifiers }
        }
        if (withModifier.isEmpty()) return partial
        val headOnly = partial.filter { hit ->
            withModifier.none { it.revisionId == hit.revisionId }
        }
        return withModifier + headOnly
    }

    private fun ensureDocumentSeats(
        selected: List<MeaningSearchHit>,
        pool: List<MeaningSearchHit>,
        cap: Int,
    ): List<MeaningSearchHit> {
        if (selected.isEmpty()) return selected
        val documents = pool.filter { it.assetType.isDocument }
        if (documents.isEmpty()) return selected
        val selectedDocs = selected.count { it.assetType.isDocument }
        val want = minOf(RESERVED_DOCUMENT_WHEN_IMAGES, documents.size, cap)
        if (selectedDocs >= want) return selected
        val add = documents.filter { candidate ->
            selected.none { it.revisionId == candidate.revisionId }
        }.take(want - selectedDocs)
        if (add.isEmpty()) return selected
        val keep = selected.toMutableList()
        for (document in add) {
            val dropAt = keep.indexOfLast { it.assetType.isImage }
            if (dropAt < 0) break
            keep.removeAt(dropAt)
            keep.add(document)
        }
        val keepIds = keep.map { it.revisionId }.toHashSet()
        return pool.filter { it.revisionId in keepIds }
    }

    private val AssetType.isDocument: Boolean
        get() = this == AssetType.PDF || this == AssetType.NOTE

    private val AssetType.isImage: Boolean
        get() = this == AssetType.PHOTO || this == AssetType.SCREENSHOT
}
