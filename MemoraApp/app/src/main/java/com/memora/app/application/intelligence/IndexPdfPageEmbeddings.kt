package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.PdfPageEmbeddingRecord
import com.memora.app.domain.intelligence.PdfPageEmbeddingStore
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.PdfPageEvidenceLocator
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Indexes capped PDF page texts for E5c Find-by-meaning page ranking.
 *
 * Writes nothing when the engine is Unavailable. Fingerprint-skips unchanged
 * pages. Does not claim measured AVAILABLE.
 *
 * MIG-05 step 2: on successful page-store upsert (and on fingerprint-skip
 * backfill), also dual-writes the same vector into
 * [MemoryEvidenceEmbeddingStore] when [PdfPageEmbeddingCandidate.evidenceId]
 * resolves to a real [MemoryEvidenceId]. Search remains on
 * [PdfPageEmbeddingStore]. No mass STALE_REINDEX.
 */
class IndexPdfPageEmbeddings @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val pageEmbeddingStore: PdfPageEmbeddingStore,
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
        var evidenceDualWrites = 0
        val total = candidates.size
        candidates.forEachIndexed { index, candidate ->
            onProgress?.invoke(index + 1, total)
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
            val existing = pageEmbeddingStore.find(
                candidate.revisionId,
                candidate.pageNumber,
                model,
            )
            if (existing != null && existing.sourceTextFingerprint == fingerprint) {
                skippedUnchanged += 1
                when (
                    dualWriteEvidence(
                        candidate = candidate,
                        model = existing.model,
                        vector = existing.vector,
                        fingerprint = fingerprint,
                        createdAtEpochMs = existing.createdAtEpochMs,
                    )
                ) {
                    DualWriteOutcome.Written -> evidenceDualWrites += 1
                    DualWriteOutcome.AlreadyCurrent -> Unit
                    DualWriteOutcome.Unresolved -> unresolvedEvidence += 1
                }
                return@forEachIndexed
            }
            when (val encoded = embeddingEngine.embedText(text)) {
                is EmbeddingEncodeResult.Unavailable ->
                    return IndexPdfPageEmbeddingsResult.EngineUnavailable(encoded.reason)
                is EmbeddingEncodeResult.Failed -> failed += 1
                is EmbeddingEncodeResult.Success -> {
                    pageEmbeddingStore.upsert(
                        PdfPageEmbeddingRecord(
                            revisionId = candidate.revisionId,
                            memoryId = candidate.memoryId,
                            pageNumber = candidate.pageNumber,
                            model = encoded.model,
                            vector = encoded.vector,
                            sourceTextFingerprint = fingerprint,
                            createdAtEpochMs = nowEpochMs,
                        ),
                    )
                    indexed += 1
                    when (
                        dualWriteEvidence(
                            candidate = candidate,
                            model = encoded.model,
                            vector = encoded.vector,
                            fingerprint = fingerprint,
                            createdAtEpochMs = nowEpochMs,
                        )
                    ) {
                        DualWriteOutcome.Written -> evidenceDualWrites += 1
                        DualWriteOutcome.AlreadyCurrent -> Unit
                        DualWriteOutcome.Unresolved -> unresolvedEvidence += 1
                    }
                }
            }
        }
        return IndexPdfPageEmbeddingsResult.Completed(
            indexed = indexed,
            skippedUnchanged = skippedUnchanged,
            failed = failed,
            unresolvedEvidence = unresolvedEvidence,
            evidenceDualWrites = evidenceDualWrites,
        )
    }

    /**
     * Dual-writes when [candidate] carries a real evidence id. Never invents
     * an id and never uses a `pdf:page:N` locator string as [MemoryEvidenceId].
     */
    private fun dualWriteEvidence(
        candidate: PdfPageEmbeddingCandidate,
        model: ModelVersionIdentity,
        vector: EmbeddingVector,
        fingerprint: String,
        createdAtEpochMs: Long,
    ): DualWriteOutcome {
        val evidenceId = resolvedEvidenceId(candidate.evidenceId) ?: return DualWriteOutcome.Unresolved
        val existingEvidence = evidenceEmbeddingStore.find(
            candidate.revisionId,
            evidenceId,
            model,
        )
        if (existingEvidence != null && existingEvidence.sourceTextFingerprint == fingerprint) {
            return DualWriteOutcome.AlreadyCurrent
        }
        evidenceEmbeddingStore.upsert(
            MemoryEvidenceEmbeddingRecord(
                revisionId = candidate.revisionId,
                memoryId = candidate.memoryId,
                evidenceId = evidenceId,
                model = model,
                vector = vector,
                sourceTextFingerprint = fingerprint,
                createdAtEpochMs = createdAtEpochMs,
            ),
        )
        return DualWriteOutcome.Written
    }

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

    private enum class DualWriteOutcome {
        Written,
        AlreadyCurrent,
        Unresolved,
    }
}

data class PdfPageEmbeddingCandidate(
    val revisionId: MemoryRevisionId,
    val memoryId: MemoryId,
    val pageNumber: Int,
    val pageText: String,
    /**
     * Real [MemoryEvidence.id] for `pdf:page:[pageNumber]` on this revision,
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
        /** Page path succeeded (or fingerprint-skipped) but evidenceId was missing/invalid. */
        val unresolvedEvidence: Int = 0,
        /** Evidence-store upserts performed (new dual-write or fingerprint-skip backfill). */
        val evidenceDualWrites: Int = 0,
    ) : IndexPdfPageEmbeddingsResult {
        init {
            require(indexed >= 0)
            require(skippedUnchanged >= 0)
            require(failed >= 0)
            require(unresolvedEvidence >= 0)
            require(evidenceDualWrites >= 0)
        }
    }
}
