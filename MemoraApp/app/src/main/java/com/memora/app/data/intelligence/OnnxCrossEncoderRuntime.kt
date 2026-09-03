package com.memora.app.data.intelligence

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import com.memora.app.domain.intelligence.BertWordPieceTokenizer
import java.io.File
import java.nio.IntBuffer
import java.nio.LongBuffer

/**
 * ONNX session helpers for Stage A cross-encoder (product + androidTest).
 */
object OnnxCrossEncoderRuntime {
    fun openSession(modelFile: File): Pair<OrtEnvironment, OrtSession> {
        require(modelFile.isFile && modelFile.length() > 0L)
        val env = OrtEnvironment.getEnvironment()
        val session = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
        return env to session
    }

    fun scoreEncoded(
        env: OrtEnvironment,
        session: OrtSession,
        encoded: BertWordPieceTokenizer.EncodedPair,
    ): Float {
        val inputs = tensorsFor(env, session, encoded)
        try {
            session.run(inputs).use { result ->
                return firstFloat(result[0].value)
                    ?: error("Cross-encoder produced no float score.")
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

    private fun firstFloat(value: Any?): Float? = when (value) {
        is Float -> value
        is FloatArray -> value.firstOrNull()
        is Array<*> -> firstFloat(value.firstOrNull())
        else -> null
    }
}
