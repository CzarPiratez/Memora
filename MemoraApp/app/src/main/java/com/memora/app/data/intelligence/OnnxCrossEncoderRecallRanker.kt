package com.memora.app.data.intelligence

import android.content.Context
import android.util.Log
import com.memora.app.domain.intelligence.BertWordPieceTokenizer
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.OnnxMsMarcoMiniLmCrossEncoderSpec
import com.memora.app.domain.intelligence.RecallRankCandidate
import com.memora.app.domain.intelligence.RecallRankDevicePolicy
import com.memora.app.domain.intelligence.RecallRankLatencyPolicy
import com.memora.app.domain.intelligence.RecallRankResult
import com.memora.app.domain.intelligence.RecallRanker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stage A ONNX MiniLM cross-encoder behind [RecallRanker].
 *
 * Unavailable when pack weights are missing — callers must fall back to identity.
 * Uses S3 [RecallRankLatencyPolicy.measuredMidrangeDisposition] for pool/seqLen.
 */
@Singleton
class OnnxCrossEncoderRecallRanker @Inject constructor(
    @ApplicationContext context: Context,
    private val packStore: NoBackupRecallRankPackStore,
) : RecallRanker {
    private val appContext = context.applicationContext
    private val lock = Any()
    private var cachedTokenizer: BertWordPieceTokenizer? = null

    override fun availability(): CapabilityAvailability {
        if (!packStore.isModelInstalled()) {
            return CapabilityAvailability.Unavailable(
                "Recall rank pack is not installed on this device yet.",
            )
        }
        if (loadTokenizerOrNull() == null) {
            return CapabilityAvailability.Unavailable(
                "Recall rank tokenizer vocab is not available.",
            )
        }
        return CapabilityAvailability.Available(OnnxMsMarcoMiniLmCrossEncoderSpec.MODEL_IDENTITY)
    }

    override fun limits(): CapabilityLimits? {
        val disposition = RecallRankLatencyPolicy.measuredMidrangeDisposition()
        val pool = RecallRankLatencyPolicy.effectivePoolSize(
            com.memora.app.domain.intelligence.RecallRankExecutionTier.FULL,
            disposition,
        )
        return CapabilityLimits(
            maxInputBytes = null,
            maxOutputItems = pool.coerceAtLeast(1),
        )
    }

    override fun rank(query: String, candidates: List<RecallRankCandidate>): RecallRankResult {
        require(query.isNotBlank()) { "Recall rank query cannot be blank." }
        if (candidates.isEmpty()) {
            return RecallRankResult.Unavailable("No candidates to rank.")
        }
        when (val availability = availability()) {
            is CapabilityAvailability.Unavailable ->
                return RecallRankResult.Unavailable(availability.reason)
            is CapabilityAvailability.Available -> Unit
        }
        val modelPath = packStore.absoluteModelPath()
            ?: return RecallRankResult.Unavailable("Recall rank model path missing.")
        val tokenizer = loadTokenizerOrNull()
            ?: return RecallRankResult.Unavailable("Recall rank tokenizer missing.")

        val disposition = RecallRankLatencyPolicy.measuredMidrangeDisposition()
        val pool = RecallRankLatencyPolicy.effectivePoolSize(
            com.memora.app.domain.intelligence.RecallRankExecutionTier.FULL,
            disposition,
        )
        if (pool <= 0) {
            return RecallRankResult.Unavailable("Recall rank degraded to identity on this device.")
        }
        val maxLen = when (disposition) {
            RecallRankLatencyPolicy.MidrangeLatencyDisposition.DEGRADED_EXPLICIT ->
                RecallRankDevicePolicy.REDUCED_MAX_SEQUENCE_LENGTH
            else -> RecallRankDevicePolicy.FULL_MAX_SEQUENCE_LENGTH
        }

        val head = candidates
            .sortedByDescending { it.score }
            .take(pool)
        val headIds = head.map { it.id }.toSet()
        val remainderIds = candidates.map { it.id }.filter { it !in headIds }

        return synchronized(lock) {
            try {
                val started = System.nanoTime()
                val (env, session) = OnnxCrossEncoderRuntime.openSession(File(modelPath))
                try {
                    val scored = head.map { candidate ->
                        val excerpt = candidate.excerpt?.takeIf { it.isNotBlank() } ?: ""
                        if (excerpt.isBlank()) {
                            candidate.id to candidate.score
                        } else {
                            val encoded = tokenizer.encodePair(query, excerpt, maxLength = maxLen)
                            candidate.id to OnnxCrossEncoderRuntime.scoreEncoded(env, session, encoded)
                        }
                    }.sortedByDescending { it.second }
                    val wallMs = (System.nanoTime() - started) / 1_000_000L
                    if (RecallRankLatencyPolicy.shouldFallBackToIdentityForWallMs(wallMs)) {
                        Log.i(TAG, "rank wallMs=$wallMs over budget — identity fallback")
                        return@synchronized RecallRankResult.Unavailable(
                            "Recall rank exceeded latency budget; using identity order.",
                        )
                    }
                    val ordered = scored.map { it.first } + remainderIds
                    RecallRankResult.Ranked(ordered)
                } finally {
                    session.close()
                }
            } catch (error: Exception) {
                Log.w(TAG, "rank failed: ${error.message}")
                RecallRankResult.Unavailable(
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "Recall rank failed on this device.",
                )
            }
        }
    }

    private fun loadTokenizerOrNull(): BertWordPieceTokenizer? {
        cachedTokenizer?.let { return it }
        return try {
            appContext.assets.open(NoBackupRecallRankPackStore.VOCAB_ASSET).bufferedReader().use {
                BertWordPieceTokenizer.loadFromReader(it).also { cachedTokenizer = it }
            }
        } catch (error: Exception) {
            Log.w(TAG, "vocab load failed: ${error.message}")
            null
        }
    }

    private companion object {
        const val TAG = "OnnxCrossEncoderRecallRanker"
    }
}
