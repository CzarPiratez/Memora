package com.memora.app.domain.intelligence

import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.memory.AssetMemoryFact
import java.time.Instant

/**
 * Truthful unavailable stubs for Local Intelligence capabilities.
 *
 * They never invent observations, text, embeddings, Memories, or rankings.
 * Used until an approved on-device AI Pack (or system runtime) is installed.
 *
 * Production MemoryBuilder is [DeterministicMemoryBuilder] (MIG-04), not
 * [UnavailableMemoryBuilder].
 */
class UnavailableVisionEngine(
    private val reason: String = DEFAULT_REASON,
) : VisionEngine {
    override fun availability(): CapabilityAvailability =
        CapabilityAvailability.Unavailable(reason)

    override fun limits(): CapabilityLimits? = null
}

class UnavailableOcrEngine(
    private val reason: String = DEFAULT_REASON,
) : OcrEngine {
    override fun availability(): CapabilityAvailability =
        CapabilityAvailability.Unavailable(reason)

    override fun limits(): CapabilityLimits? = null
}

class UnavailableDocumentEngine(
    private val reason: String = DEFAULT_REASON,
) : DocumentEngine {
    override fun availability(): CapabilityAvailability =
        CapabilityAvailability.Unavailable(reason)

    override fun limits(): CapabilityLimits? = null
}

class UnavailableEmbeddingEngine(
    private val reason: String = DEFAULT_REASON,
) : EmbeddingEngine {
    override fun availability(): CapabilityAvailability =
        CapabilityAvailability.Unavailable(reason)

    override fun limits(): CapabilityLimits? = null

    override fun embedText(text: String): EmbeddingEncodeResult {
        require(text.isNotBlank()) { "Embedding input text must not be blank." }
        return EmbeddingEncodeResult.Unavailable(reason)
    }
}

class UnavailableMemoryBuilder(
    private val reason: String = DEFAULT_REASON,
) : MemoryBuilder {
    override fun availability(): CapabilityAvailability =
        CapabilityAvailability.Unavailable(reason)

    override fun limits(): CapabilityLimits? = null

    override fun assemble(
        assetIdentity: AssetIdentity,
        assetFingerprint: AssetFingerprint,
        facts: List<AssetMemoryFact>,
        localObservations: List<LocalObservation>,
        createdAt: Instant,
    ): MemoryBuildResult = MemoryBuildResult.Unavailable(reason)
}

class UnavailableRecallRanker(
    private val reason: String = DEFAULT_REASON,
) : RecallRanker {
    override fun availability(): CapabilityAvailability =
        CapabilityAvailability.Unavailable(reason)

    override fun limits(): CapabilityLimits? = null

    override fun rank(query: String, candidates: List<RecallRankCandidate>): RecallRankResult =
        RecallRankResult.Unavailable(reason)
}

/** Factory for the full unavailable Local Intelligence set. */
object UnavailableLocalIntelligence {
    fun vision(reason: String = DEFAULT_REASON): VisionEngine = UnavailableVisionEngine(reason)

    fun ocr(reason: String = DEFAULT_REASON): OcrEngine = UnavailableOcrEngine(reason)

    fun document(reason: String = DEFAULT_REASON): DocumentEngine =
        UnavailableDocumentEngine(reason)

    fun embedding(reason: String = DEFAULT_REASON): EmbeddingEngine =
        UnavailableEmbeddingEngine(reason)

    fun memoryBuilder(reason: String = DEFAULT_REASON): MemoryBuilder =
        UnavailableMemoryBuilder(reason)

    fun recallRanker(reason: String = DEFAULT_REASON): RecallRanker =
        UnavailableRecallRanker(reason)

    fun all(reason: String = DEFAULT_REASON): List<LocalCapability> = listOf(
        vision(reason),
        ocr(reason),
        document(reason),
        embedding(reason),
        memoryBuilder(reason),
        recallRanker(reason),
    )
}

private const val DEFAULT_REASON =
    "On-device intelligence is not installed on this phone yet."
