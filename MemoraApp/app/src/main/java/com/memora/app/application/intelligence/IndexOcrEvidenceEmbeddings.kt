package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Indexes photo/screenshot OCR [MemoryEvidence] excerpts into
 * [MemoryEvidenceEmbeddingStore] (MIG-05 claim B slice 1).
 *
 * Evidence-only writer; fingerprint-skips unchanged text. Never invents evidence ids.
 */
class IndexOcrEvidenceEmbeddings @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val evidenceEmbeddingStore: MemoryEvidenceEmbeddingStore,
) {
    operator fun invoke(
        candidates: List<OcrEvidenceEmbeddingCandidate>,
        nowEpochMs: Long,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null,
    ): IndexOcrEvidenceEmbeddingsResult {
        require(nowEpochMs >= 0)
        when (val availability = embeddingEngine.availability()) {
            is CapabilityAvailability.Unavailable ->
                return IndexOcrEvidenceEmbeddingsResult.EngineUnavailable(availability.reason)
            is CapabilityAvailability.Available -> Unit
        }

        var indexed = 0
        var skippedUnchanged = 0
        var failed = 0
        var unresolvedEvidence = 0
        val total = candidates.size
        candidates.forEachIndexed { index, candidate ->
            onProgress?.invoke(index + 1, total)
            val evidenceId = candidate.evidenceId
            if (evidenceId == null) {
                unresolvedEvidence += 1
                failed += 1
                return@forEachIndexed
            }
            val text = ResolveMeaningPdfOpenPage.truncateForEmbed(candidate.excerpt)
            if (text.isBlank()) {
                failed += 1
                return@forEachIndexed
            }
            val fingerprint = sha256Hex(text)
            val model = when (val availability = embeddingEngine.availability()) {
                is CapabilityAvailability.Available -> availability.model
                is CapabilityAvailability.Unavailable ->
                    return IndexOcrEvidenceEmbeddingsResult.EngineUnavailable(availability.reason)
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
                    return IndexOcrEvidenceEmbeddingsResult.EngineUnavailable(encoded.reason)
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
        return IndexOcrEvidenceEmbeddingsResult.Completed(
            indexed = indexed,
            skippedUnchanged = skippedUnchanged,
            failed = failed,
            unresolvedEvidence = unresolvedEvidence,
        )
    }

    private fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

data class OcrEvidenceEmbeddingCandidate(
    val revisionId: MemoryRevisionId,
    val memoryId: MemoryId,
    val excerpt: String,
    val evidenceId: MemoryEvidenceId? = null,
) {
    init {
        require(excerpt.isNotBlank())
    }
}

sealed interface IndexOcrEvidenceEmbeddingsResult {
    data class EngineUnavailable(val reason: String) : IndexOcrEvidenceEmbeddingsResult {
        init {
            require(reason.isNotBlank())
        }
    }

    data class Completed(
        val indexed: Int,
        val skippedUnchanged: Int,
        val failed: Int,
        val unresolvedEvidence: Int = 0,
    ) : IndexOcrEvidenceEmbeddingsResult {
        init {
            require(indexed >= 0)
            require(skippedUnchanged >= 0)
            require(failed >= 0)
            require(unresolvedEvidence >= 0)
        }
    }
}
