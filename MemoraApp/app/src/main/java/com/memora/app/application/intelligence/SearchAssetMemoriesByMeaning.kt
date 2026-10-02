package com.memora.app.application.intelligence

import android.util.Log
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingSimilarity
import com.memora.app.domain.intelligence.MeaningRecallCue
import com.memora.app.domain.intelligence.MeaningRecallRoles
import com.memora.app.domain.intelligence.MeaningRoleAdmission
import com.memora.app.domain.intelligence.MeaningRoleScorer
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.RecallPrecision
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryEvidenceSearchRow
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryMeaningLookup
import com.memora.app.domain.memory.MemoryRepository
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.PdfPageEvidenceLocator
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Candidate Find-by-meaning over indexed Memory summaries and evidence-level
 * vectors (MIG-05 step 3+; PdfPageEmbedding* retired in step 4).
 *
 * Page/evidence ranking reads [MemoryEvidenceEmbeddingStore] + stored
 * [com.memora.app.domain.memory.MemoryEvidence] excerpts. Returns cosine
 * similarity candidates only; token boost and anchor ranking live in the
 * shared meaning ranker inside the Canonical Recall application boundary
 * (MIG-07B Slice 4). Does not use
 * [com.memora.app.domain.extraction.SavedPdfPageTextSource] for ranking.
 * Does not claim Local Intelligence marketing AVAILABLE / SLA (ADR-024/025).
 */
class SearchAssetMemoriesByMeaning @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val embeddingStore: MemoryEmbeddingStore,
    private val evidenceEmbeddingStore: MemoryEvidenceEmbeddingStore,
    private val memoryRepository: MemoryRepository,
    private val applyMig05EvidenceSearchCutover: ApplyMig05EvidenceSearchCutover,
) {
    suspend operator fun invoke(
        rawQuery: String,
        limit: Int = DEFAULT_LIMIT,
    ): MeaningSearchOutcome = withContext(Dispatchers.Default) {
        try {
            searchInternal(rawQuery = rawQuery, limit = limit)
        } catch (error: Exception) {
            Log.w(TAG, "meaning candidate search failed", error)
            MeaningSearchOutcome.Failed(
                error.message?.takeIf { it.isNotBlank() }
                    ?: "Meaning search failed on this phone.",
            )
        }
    }

    private suspend fun searchInternal(
        rawQuery: String,
        limit: Int,
    ): MeaningSearchOutcome {
        require(limit > 0)
        val query = MeaningRecallCue.displayQuery(rawQuery)
        if (query.isEmpty()) return MeaningSearchOutcome.BlankQuery
        val model = when (val availability = embeddingEngine.availability()) {
            is CapabilityAvailability.Unavailable ->
                return MeaningSearchOutcome.EngineUnavailable(availability.reason)
            is CapabilityAvailability.Available -> availability.model
        }

        applyMig05EvidenceSearchCutover.ensureApplied(model)

        val summaryIndexed = embeddingStore.listForModel(model)
        val evidenceIndexed = evidenceEmbeddingStore.listForModel(model)
        if (summaryIndexed.isEmpty() && evidenceIndexed.isEmpty()) {
            return MeaningSearchOutcome.NothingIndexed(query = query)
        }

        // I5: ask-shape only (`show me the files`) must not embed and list neighbors.
        if (MeaningRecallCue.contentTokens(rawQuery).isEmpty()) {
            return MeaningSearchOutcome.Matches(
                query = query,
                hits = emptyList(),
                limitReached = false,
                model = model,
                debugTrace = MeaningSearchTrace.withPool(
                    vectorsScanned = summaryIndexed.size + evidenceIndexed.size,
                    survivedFloor = 0,
                    assetsAfterCollapse = 0,
                    collapseHits = emptyList(),
                    admittedHits = emptyList(),
                    poolTruncated = false,
                    rawQuery = rawQuery,
                ),
            )
        }

        val embedQuery = MeaningRecallCue.embedText(rawQuery)
        val queryVector = when (val encoded = embeddingEngine.embedQuery(embedQuery)) {
            is EmbeddingEncodeResult.Unavailable ->
                return MeaningSearchOutcome.EngineUnavailable(encoded.reason)
            is EmbeddingEncodeResult.Failed ->
                return MeaningSearchOutcome.Failed(encoded.reason)
            is EmbeddingEncodeResult.Success -> encoded.vector
        }

        val revisionIds = (summaryIndexed.map { it.revisionId } + evidenceIndexed.map { it.revisionId })
            .distinct()
        val lookups = memoryRepository.findMeaningIndexLookups(revisionIds)
        val evidenceRows = memoryRepository.findEvidenceSearchRows(revisionIds)
        val dropStats = MeaningSearchCandidateDropStats()

        val summaryHits = summaryIndexed.mapNotNull { record ->
            val lookup = lookups[record.revisionId]
            if (lookup == null) {
                dropStats.missingLookup += 1
                return@mapNotNull null
            }
            if (record.vector.dimensions != queryVector.dimensions) {
                dropStats.dimensionMismatch += 1
                return@mapNotNull null
            }
            val cosine = EmbeddingSimilarity.cosine(queryVector, record.vector)
            if (cosine < MIN_CANDIDATE_SCORE) {
                dropStats.belowMinScore += 1
                return@mapNotNull null
            }
            val label = lookup.displayLabel.trim()
            val summaryText = lookup.summaryText.trim()
            if (label.isBlank() || summaryText.isBlank()) {
                dropStats.blankLabelOrSummary += 1
                return@mapNotNull null
            }
            MeaningSearchHit(
                revisionId = record.revisionId,
                memoryId = record.memoryId,
                sourceId = lookup.sourceId,
                sourceAssetKey = lookup.sourceAssetKey,
                assetType = lookup.assetType,
                label = label,
                summaryText = summaryText,
                citedPdfPageNumber = lookup.citedPdfPageNumber,
                rankedPdfPageNumber = null,
                score = cosine,
                model = model,
                evidenceTokenBoosted = false,
                precisionText = precisionTextFor(lookup, record.revisionId, evidenceRows),
            )
        }

        val evidenceHits = evidenceIndexed.mapNotNull { record ->
            val lookup = lookups[record.revisionId]
            if (lookup == null) {
                dropStats.missingLookup += 1
                return@mapNotNull null
            }
            if (record.vector.dimensions != queryVector.dimensions) {
                dropStats.dimensionMismatch += 1
                return@mapNotNull null
            }
            val evidence = evidenceRows[record.revisionId]?.get(record.evidenceId)
            if (evidence == null) {
                dropStats.missingEvidenceRow += 1
                return@mapNotNull null
            }
            val excerpt = ResolveMeaningPdfOpenPage.truncateForEmbed(evidence.excerpt)
                .takeIf { it.isNotBlank() }
            if (excerpt == null) {
                dropStats.blankExcerpt += 1
                return@mapNotNull null
            }
            val rankedPage = PdfPageEvidenceLocator.parsePageNumber(evidence.locator)
            val cosine = EmbeddingSimilarity.cosine(queryVector, record.vector)
            if (cosine < MIN_CANDIDATE_SCORE) {
                dropStats.belowMinScore += 1
                return@mapNotNull null
            }
            val label = lookup.displayLabel.trim()
            if (label.isBlank()) {
                dropStats.blankLabelOrSummary += 1
                return@mapNotNull null
            }
            MeaningSearchHit(
                revisionId = record.revisionId,
                memoryId = record.memoryId,
                sourceId = lookup.sourceId,
                sourceAssetKey = lookup.sourceAssetKey,
                assetType = lookup.assetType,
                label = label,
                summaryText = excerpt,
                citedPdfPageNumber = lookup.citedPdfPageNumber,
                rankedPdfPageNumber = rankedPage,
                score = cosine,
                model = model,
                evidenceTokenBoosted = false,
                precisionText = precisionTextFor(lookup, record.revisionId, evidenceRows),
            )
        }

        val deduped = (summaryHits + evidenceHits)
            .groupBy { "${it.sourceId.value}|${it.sourceAssetKey.value}" }
            .values
            .map { group ->
                group.maxWithOrNull(
                    compareBy<MeaningSearchHit> { it.score }
                        .thenBy { if (it.rankedPdfPageNumber != null) 1 else 0 },
                )!!
            }
            .sortedByDescending { it.score }

        if (deduped.isEmpty() && dropStats.hasDrops()) {
            Log.w(
                TAG,
                dropStats.toLogMessage(
                    summaryIndexed = summaryIndexed.size,
                    evidenceIndexed = evidenceIndexed.size,
                    query = query,
                ),
            )
        }

        val limited = selectCandidatePool(deduped, rawQuery, limit)
        return MeaningSearchOutcome.Matches(
            query = query,
            hits = limited,
            limitReached = deduped.size > limited.size,
            model = model,
            debugTrace = MeaningSearchTrace.withPool(
                vectorsScanned = summaryIndexed.size + evidenceIndexed.size,
                survivedFloor = summaryHits.size + evidenceHits.size,
                assetsAfterCollapse = deduped.size,
                collapseHits = deduped,
                admittedHits = limited,
                poolTruncated = deduped.size > limited.size,
                rawQuery = rawQuery,
            ),
        )
    }

    /**
     * Cosine order decides rank; it must not decide reachability.
     *
     * The candidate pool is a fixed slice of the corpus and every later stage —
     * token boost, lexical precision, rerank, anchors — can only subtract. So
     * once the library outgrew the slice, a Memory holding the very words the
     * person named could sit outside it and be truncated away before the
     * precision gate ever saw it. Queries that passed against 25 memories
     * answered nothing against ~1000 for that reason alone (defect D-11).
     *
     * Candidates claim seats by [MeaningRoleScorer.band] (ADR-055): with a
     * constraint, topic + constraint before leftover job words. Remaining
     * seats keep the best cosine neighbours, so a cue with no literal
     * overlap still degrades to meaning rather than to empty.
     *
     * Admission uses the same [MeaningRoleScorer] as the shown page, so the
     * two cannot drift apart. The pool is handed back in cosine order because
     * this class generates candidates and does not rank them.
     */
    private fun selectCandidatePool(
        candidates: List<MeaningSearchHit>,
        rawQuery: String,
        limit: Int,
    ): List<MeaningSearchHit> {
        if (candidates.size <= limit) return candidates
        val roles = MeaningRecallRoles.parse(rawQuery)
        if (roles.isEmpty()) return candidates.take(limit)
        val scorer = MeaningRoleScorer.forQuery(rawQuery)
        return MeaningRoleAdmission.take(candidates, limit, scorer) { hit ->
            hit.lexicalHaystack()
        }
    }

    private fun precisionTextFor(
        lookup: MemoryMeaningLookup,
        revisionId: MemoryRevisionId,
        evidenceRows: Map<MemoryRevisionId, Map<MemoryEvidenceId, MemoryEvidenceSearchRow>>,
    ): String {
        val excerpts = evidenceRows[revisionId]?.values.orEmpty().map { it.excerpt }
        return (listOf(lookup.summaryText, lookup.displayLabel, lookup.sourceAssetKey.value) + excerpts)
            .joinToString(" ")
    }

    companion object {
        private const val TAG = "SearchAssetMemoriesByMeaning"

        /** Candidate-generation default only. Product meaning page size is Canonical Recall's cap of 20. */
        const val DEFAULT_LIMIT = 10

        /** Soft floor so near-zero noise is not listed as a candidate. */
        const val MIN_CANDIDATE_SCORE = MeaningRecallCue.MIN_CANDIDATE_SCORE
    }
}

data class MeaningSearchHit(
    val revisionId: MemoryRevisionId,
    val memoryId: MemoryId,
    val sourceId: SourceId,
    val sourceAssetKey: SourceAssetKey,
    val assetType: AssetType,
    val label: String,
    val summaryText: String,
    val score: Float,
    val model: ModelVersionIdentity,
    /** 1-based PDF page cited by the Memory summary, when known. */
    val citedPdfPageNumber: Int? = null,
    /**
     * 1-based PDF page that won meaning ranking via an indexed evidence vector
     * whose locator is `pdf:page:N`. Null for summary-only / non-page hits.
     */
    val rankedPdfPageNumber: Int? = null,
    /** True when a significant cue token was found in evidence and boosted score. */
    val evidenceTokenBoosted: Boolean = false,
    /**
     * All stored text for this Asset used by lexical precision (summary + every
     * evidence excerpt + label). Empty means fall back to the ranked snippet.
     */
    val precisionText: String = "",
    /**
     * Similarity as the embedding model measured it, before any later stage
     * adjusted [score].
     *
     * [score] is a working rank value: token boost adds to it, anchors adjust
     * it. A gate that must reason about *meaning* rather than rank has to read
     * this instead — the meaning-only floor was being cleared by a
     * [com.memora.app.domain.intelligence.MeaningEvidenceTokenBoost] the cue's
     * named words never earned.
     */
    val cosine: Float = score,
) {
    init {
        require(label.isNotBlank())
        require(summaryText.isNotBlank())
        require(score.isFinite())
        require(cosine.isFinite())
        require(citedPdfPageNumber == null || citedPdfPageNumber > 0)
        require(rankedPdfPageNumber == null || rankedPdfPageNumber > 0)
    }

    /**
     * Text used for lexical precision (MF-1). Prefers [precisionText] (whole
     * Memory) so a word like `scan` on any saved page is enough — not only the
     * cosine-winning snippet.
     */
    fun lexicalHaystack(): String {
        if (precisionText.isNotBlank()) return precisionText
        val key = sourceAssetKey.value
        return if (key.isNotBlank() &&
            !label.contains(key, ignoreCase = true) &&
            !summaryText.contains(key, ignoreCase = true)
        ) {
            "$summaryText $label $key"
        } else {
            "$summaryText $label"
        }
    }
}

sealed interface MeaningSearchOutcome {
    data object BlankQuery : MeaningSearchOutcome

    data class EngineUnavailable(val reason: String) : MeaningSearchOutcome {
        init {
            require(reason.isNotBlank())
        }
    }

    data class NothingIndexed(val query: String) : MeaningSearchOutcome {
        init {
            require(query.isNotBlank())
        }
    }

    data class Failed(val reason: String) : MeaningSearchOutcome {
        init {
            require(reason.isNotBlank())
        }
    }

    data class Matches(
        val query: String,
        val hits: List<MeaningSearchHit>,
        val limitReached: Boolean,
        val model: ModelVersionIdentity,
        /**
         * Which of the named words this list's evidence actually contains.
         * Candidate generation never demotes a list, so it always reports
         * [RecallPrecision.Exact]; the tier is decided by the precision stage
         * inside Canonical Recall (defect D-12, meaning-only tier).
         */
        val precision: RecallPrecision = RecallPrecision.Exact,
        /**
         * Phase 0 / D-22 live-path counts and gold membership. Null on
         * keyword-unrelated fixtures. Never holds excerpts. Does not affect
         * ranking.
         */
        val debugTrace: MeaningSearchDebugTrace? = null,
    ) : MeaningSearchOutcome {
        init {
            require(query.isNotBlank())
            require(hits.isNotEmpty() || precision == RecallPrecision.Exact) {
                "An empty list cannot be a partial or meaning-only match."
            }
        }
    }
}
