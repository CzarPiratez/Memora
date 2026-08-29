package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.PdfPageEvidenceLocator
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Indexes capped PDF page texts into [MemoryEvidenceEmbeddingStore] for E5c
 * Find-by-meaning evidence ranking.
 *
 * MIG-05 step 4: evidence-only writer. No PdfPageEmbedding* dual-write.
 * Fingerprint-skips against the evidence store. Unresolved [evidenceId]
 * counts as unresolved/fail — never invents ids. Product search already ranks
 * the evidence store (MIG-05 step 3).
 *
 * PDF page candidates may still be built at meaning-index time from saved page
 * text (index-time only; not product ranking).
 */
class IndexPdfPageEmbeddings @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val evidenceEmbeddingStore: MemoryEvidenceEmbeddingStore,
) {
    operator fun invoke(
        candidates: List<PdfPageEmbeddingCandidate>,
        nowEpochMs: Long,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null,
    ): IndexPdfPageEmbeddingsResult {
        require(nowEpochMs >= 0)
        when (val availability = embeddingEngine.availability()) {
            is CapabilityAvailability.Unavailable ->
                return IndexPdfPageEmbeddingsResult.EngineUnavailable(availability.reason)
            is CapabilityAvailability.Available -> Unit
        }

        var indexed = 0
        var skippedUnchanged = 0
        var failed = 0
        var unresolvedEvidence = 0
        val total = candidates.size
        candidates.forEachIndexed { index, candidate ->
            onProgress?.invoke(index + 1, total)
            val evidenceId = resolvedEvidenceId(candidate.evidenceId)
            if (evidenceId == null) {
                unresolvedEvidence += 1
                failed += 1
                return@forEachIndexed
            }
            val text = ResolveMeaningPdfOpenPage.truncateForEmbed(candidate.pageText)
            if (text.isBlank()) {
                failed += 1
                return@forEachIndexed
            }
            val fingerprint = sha256Hex(text)
            val model = when (val availability = embeddingEngine.availability()) {
                is CapabilityAvailability.Available -> availability.model
                is CapabilityAvailability.Unavailable ->
                    return IndexPdfPageEmbeddingsResult.EngineUnavailable(availability.reason)
            }
            val existing = evidenceEmbeddingStore.find(
                candidate.revisionId,
                evidenceId,
                model,
            )
            if (existing != null && existing.sourceTextFingerprint == fingerprint) {
                skippedUnchanged += 1
                return@forEachIndexed
            }
            when (val encoded = embeddingEngine.embedText(text)) {
                is EmbeddingEncodeResult.Unavailable ->
                    return IndexPdfPageEmbeddingsResult.EngineUnavailable(encoded.reason)
                is EmbeddingEncodeResult.Failed -> failed += 1
                is EmbeddingEncodeResult.Success -> {
                    evidenceEmbeddingStore.upsert(
                        MemoryEvidenceEmbeddingRecord(
                            revisionId = candidate.revisionId,
                            memoryId = candidate.memoryId,
                            evidenceId = evidenceId,
                            model = encoded.model,
                            vector = encoded.vector,
                            sourceTextFingerprint = fingerprint,
                            createdAtEpochMs = nowEpochMs,
                        ),
                    )
                    indexed += 1
                }
            }
        }
        return IndexPdfPageEmbeddingsResult.Completed(
            indexed = indexed,
            skippedUnchanged = skippedUnchanged,
            failed = failed,
            unresolvedEvidence = unresolvedEvidence,
        )
    }

    /**
     * Requires a real evidence id. Never invents an id and never uses a
     * `pdf:page:N` locator string as [MemoryEvidenceId].
     */
    private fun resolvedEvidenceId(evidenceId: MemoryEvidenceId?): MemoryEvidenceId? {
        if (evidenceId == null) return null
        // Locator strings are not evidence ids (builder assigns e{n}).
        if (PdfPageEvidenceLocator.parsePageNumber(evidenceId.value) != null) return null
        return evidenceId
    }

    private fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

data class PdfPageEmbeddingCandidate(
    val revisionId: MemoryRevisionId,
    val memoryId: MemoryId,
    val pageNumber: Int,
    val pageText: String,
    /**
     * Real [MemoryEvidenceId] for `pdf:page:[pageNumber]` on this revision,
     * or null when unresolved. Must never be the locator string itself.
     */
    val evidenceId: MemoryEvidenceId? = null,
) {
    init {
        require(pageNumber > 0)
    }
}

sealed interface IndexPdfPageEmbeddingsResult {
    data class EngineUnavailable(val reason: String) : IndexPdfPageEmbeddingsResult {
        init {
            require(reason.isNotBlank())
        }
    }

    data class Completed(
        val indexed: Int,
        val skippedUnchanged: Int,
        val failed: Int,
        /** Candidate lacked a resolvable evidenceId (or used a locator-shaped id). */
        val unresolvedEvidence: Int = 0,
    ) : IndexPdfPageEmbeddingsResult {
        init {
            require(indexed >= 0)
            require(skippedUnchanged >= 0)
            require(failed >= 0)
            require(unresolvedEvidence >= 0)
        }
    }
}
