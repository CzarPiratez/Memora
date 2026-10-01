package com.memora.app.data.intelligence

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import com.memora.app.domain.intelligence.BertWordPieceTokenizer
import com.memora.app.domain.intelligence.EmbeddingVector
import java.io.File
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer
import kotlin.math.sqrt

/**
 * ONNX bi-encoder helpers for BGE-small (ADR-055 product pack).
 * CLS pooling + L2 normalize.
 */
object OnnxBiEncoderRuntime {
    fun openSession(modelFile: File): Pair<OrtEnvironment, OrtSession> {
        require(modelFile.isFile && modelFile.length() > 0L)
        val env = OrtEnvironment.getEnvironment()
        val session = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
        return env to session
    }

    fun embedEncoded(
        env: OrtEnvironment,
        session: OrtSession,
        encoded: BertWordPieceTokenizer.EncodedPair,
        expectedDimensions: Int,
    ): EmbeddingVector {
        val inputs = tensorsFor(env, session, encoded)
        try {
            session.run(inputs).use { result ->
                val raw = firstFloatArray(result[0].value)
                    ?: error("Bi-encoder produced no float output.")
                val pooled = clsPool(raw, sequenceLength = encoded.length, dimensions = expectedDimensions)
                return EmbeddingVector(l2Normalize(pooled))
            }
        } finally {
            inputs.values.forEach { it.close() }
        }
    }

    private fun tensorsFor(
        env: OrtEnvironment,
        session: OrtSession,
        encoded: BertWordPieceTokenizer.EncodedPair,
    ): Map<String, OnnxTensor> {
        val shape = longArrayOf(1, encoded.length.toLong())
        return session.inputNames.associateWith { name ->
            val lowered = name.lowercase()
            val values = when {
                lowered.contains("attention_mask") -> encoded.attentionMask
                lowered.contains("token_type") -> encoded.tokenTypeIds
                else -> encoded.inputIds
            }
            val type = (session.inputInfo[name]?.info as? TensorInfo)?.type
                ?: OnnxJavaType.INT64
            when (type) {
                OnnxJavaType.INT32 -> OnnxTensor.createTensor(
                    env,
                    IntBuffer.wrap(values.map { it.toInt() }.toIntArray()),
                    shape,
                )
                else -> OnnxTensor.createTensor(
                    env,
                    LongBuffer.wrap(values.copyOf()),
                    shape,
                )
            }
        }
    }

    /**
     * Accepts [batch, seq, dim], [seq, dim], or already-pooled [dim] / [batch, dim].
     */
    internal fun clsPool(
        raw: FloatArray,
        sequenceLength: Int,
        dimensions: Int,
    ): FloatArray {
        when (raw.size) {
            dimensions -> return raw.copyOf()
            dimensions * 2 -> {
                // [batch=1, dim] flattened oddly — take first dim.
                return raw.copyOfRange(0, dimensions)
            }
            sequenceLength * dimensions -> {
                return raw.copyOfRange(0, dimensions)
            }
            else -> {
                // Prefer leading CLS if divisible by dimensions.
                require(raw.size % dimensions == 0 && raw.size >= dimensions) {
                    "Unexpected bi-encoder output size ${raw.size} for dim=$dimensions."
                }
                return raw.copyOfRange(0, dimensions)
            }
        }
    }

    internal fun l2Normalize(values: FloatArray): FloatArray {
        var sumSquares = 0.0
        for (v in values) {
            sumSquares += v.toDouble() * v.toDouble()
        }
        if (sumSquares <= 0.0) return values.copyOf()
        val norm = sqrt(sumSquares).toFloat()
        return FloatArray(values.size) { index -> values[index] / norm }
    }

    private fun firstFloatArray(value: Any?): FloatArray? = when (value) {
        is FloatArray -> value
        is FloatBuffer -> {
            val arr = FloatArray(value.remaining())
            value.get(arr)
            arr
        }
        is Array<*> -> {
            when (val first = value.firstOrNull()) {
                is FloatArray -> first
                is Array<*> -> firstFloatArray(first)
                is Float -> FloatArray(value.size) { (value[it] as Float) }
                else -> firstFloatArray(first)
            }
        }
        else -> null
    }
}
