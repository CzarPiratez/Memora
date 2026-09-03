package com.memora.app.domain.intelligence

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.memora.app.data.intelligence.OnnxCrossEncoderRuntime
import java.io.File

/**
 * S1/S2 ONNX helpers — androidTest only. Product path uses [OnnxCrossEncoderRuntime].
 */
object OnnxCrossEncoderSpikeSupport {
    data class SpikeResult(
        val modelLoaded: Boolean,
        val pairCount: Int,
        val totalWallMs: Long,
        val averagePairMs: Double,
        val lastScore: Float?,
        val inputNames: List<String>,
    )

    fun runSpike(
        modelFile: File,
        pairCount: Int = 20,
        sequenceLength: Int = 96,
    ): SpikeResult {
        require(pairCount > 0)
        require(sequenceLength > 8)
        require(modelFile.isFile && modelFile.length() > 0L) {
            "Cross-encoder model file must exist for spike."
        }

        val (env, session) = OnnxCrossEncoderRuntime.openSession(modelFile)
        try {
            val inputNames = session.inputNames.sorted()
            val started = System.nanoTime()
            var lastScore: Float? = null
            repeat(pairCount) {
                val dummy = BertWordPieceTokenizer.EncodedPair(
                    inputIds = LongArray(sequenceLength) { if (it == 0) 101L else 0L },
                    attentionMask = LongArray(sequenceLength) { 1L },
                    tokenTypeIds = LongArray(sequenceLength) { 0L },
                )
                lastScore = OnnxCrossEncoderRuntime.scoreEncoded(env, session, dummy)
            }
            val wallMs = (System.nanoTime() - started) / 1_000_000L
            return SpikeResult(
                modelLoaded = true,
                pairCount = pairCount,
                totalWallMs = wallMs,
                averagePairMs = wallMs.toDouble() / pairCount.toDouble(),
                lastScore = lastScore,
                inputNames = inputNames,
            )
        } finally {
            session.close()
        }
    }

    fun openSession(modelFile: File): Pair<OrtEnvironment, OrtSession> =
        OnnxCrossEncoderRuntime.openSession(modelFile)

    fun scoreEncoded(
        env: OrtEnvironment,
        session: OrtSession,
        encoded: BertWordPieceTokenizer.EncodedPair,
    ): Float = OnnxCrossEncoderRuntime.scoreEncoded(env, session, encoded)
}

class OnnxCrossEncoderPairScorer(
    private val env: OrtEnvironment,
    private val session: OrtSession,
    private val tokenizer: BertWordPieceTokenizer,
    private val maxLength: Int,
) : CrossEncoderPairScorer {
    override fun score(query: String, passage: String): Float {
        val encoded = tokenizer.encodePair(query, passage, maxLength = maxLength)
        return OnnxCrossEncoderRuntime.scoreEncoded(env, session, encoded)
    }
}
