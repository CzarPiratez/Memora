package com.memora.app.application.intelligence

import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.MemoryEmbeddingRecord
import com.memora.app.domain.intelligence.MemoryEmbeddingStore
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Indexes current READY Memory summaries when [EmbeddingEngine] is Available.
 *
 * When the engine is Unavailable (product default through E5a), returns a truthful
 * unavailable outcome and writes nothing — never invents vectors.
 */
class IndexMemoryEmbeddings @Inject constructor(
    private val embeddingEngine: EmbeddingEngine,
    private val embeddingStore: MemoryEmbeddingStore,
) {
    operator fun invoke(
        candidates: List<MemoryEmbeddingCandidate>,
        nowEpochMs: Long,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null,
    ): IndexMemoryEmbeddingsResult {
        require(nowEpochMs >= 0)
        when (val availability = embeddingEngine.availability()) {
            is CapabilityAvailability.Unavailable ->
                return IndexMemoryEmbeddingsResult.EngineUnavailable(availability.reason)
            is CapabilityAvailability.Available -> Unit
        }

        var indexed = 0
        var skippedUnchanged = 0
        var failed = 0
        val total = candidates.size
        candidates.forEachIndexed { index, candidate ->
            onProgress?.invoke(index + 1, total)
            if (candidate.summaryText.isBlank()) {
                failed += 1
                return@forEachIndexed
            }
            val fingerprint = sha256Hex(candidate.summaryText)
            val model = when (val availability = embeddingEngine.availability()) {
                is CapabilityAvailability.Available -> availability.model
                is CapabilityAvailability.Unavailable ->
                    return IndexMemoryEmbeddingsResult.EngineUnavailable(availability.reason)
            }
            val existing = embeddingStore.find(candidate.revisionId, model)
            if (existing != null && existing.sourceTextFingerprint == fingerprint) {
                skippedUnchanged += 1
                return@forEachIndexed
            }
            when (val encoded = embeddingEngine.embedText(candidate.summaryText)) {
                is EmbeddingEncodeResult.Unavailable ->
                    return IndexMemoryEmbeddingsResult.EngineUnavailable(encoded.reason)
                is EmbeddingEncodeResult.Failed -> failed += 1
                is EmbeddingEncodeResult.Success -> {
                    embeddingStore.upsert(
                        MemoryEmbeddingRecord(
                            revisionId = candidate.revisionId,
                            memoryId = candidate.memoryId,
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
        return IndexMemoryEmbeddingsResult.Completed(
            indexed = indexed,
            skippedUnchanged = skippedUnchanged,
            failed = failed,
        )
    }

    private fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

data class MemoryEmbeddingCandidate(
    val revisionId: MemoryRevisionId,
    val memoryId: MemoryId,
    val summaryText: String,
)

sealed interface IndexMemoryEmbeddingsResult {
    data class EngineUnavailable(val reason: String) : IndexMemoryEmbeddingsResult {
        init {
            require(reason.isNotBlank())
        }
    }

    data class Completed(
        val indexed: Int,
        val skippedUnchanged: Int,
        val failed: Int,
    ) : IndexMemoryEmbeddingsResult {
        init {
            require(indexed >= 0)
            require(skippedUnchanged >= 0)
            require(failed >= 0)
        }
    }
}
