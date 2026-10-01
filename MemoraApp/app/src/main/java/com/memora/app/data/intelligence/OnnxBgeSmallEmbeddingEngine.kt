package com.memora.app.data.intelligence

import android.content.Context
import android.util.Log
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.memora.app.domain.intelligence.BertWordPieceTokenizer
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelStore
import com.memora.app.domain.intelligence.OnnxBgeSmallEnV15Spec
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Product [EmbeddingEngine] (ADR-055 slice 4): BGE-small-en-v1.5 ONNX.
 *
 * Documents / Memories use [embedText] without a prefix. Find cues use
 * [embedQuery] with the BGE retrieval instruction. Session is cached.
 */
@Singleton
class OnnxBgeSmallEmbeddingEngine @Inject constructor(
    private val appContext: Context,
    private val modelStore: OnDeviceEmbeddingModelStore,
) : EmbeddingEngine {
    private val lock = Any()
    private var cachedTokenizer: BertWordPieceTokenizer? = null
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    override fun availability(): CapabilityAvailability = synchronized(lock) {
        if (!modelStore.isInstalled()) {
            return CapabilityAvailability.Unavailable(MODEL_MISSING_REASON)
        }
        if (loadTokenizerOrNull() == null) {
            return CapabilityAvailability.Unavailable(TOKENIZER_MISSING_REASON)
        }
        ensureSessionLocked()
        if (ortSession == null) {
            return CapabilityAvailability.Unavailable(MODEL_LOAD_FAILED_REASON)
        }
        CapabilityAvailability.Available(OnnxBgeSmallEnV15Spec.MODEL_IDENTITY)
    }

    override fun limits(): CapabilityLimits =
        CapabilityLimits(
            maxInputBytes = MAX_INPUT_CHARS * 4L,
            maxOutputItems = 1,
        )

    override fun embedText(text: String): EmbeddingEncodeResult =
        embedRaw(text.trim(), forQuery = false)

    override fun embedQuery(text: String): EmbeddingEncodeResult {
        val body = text.trim()
        if (body.isBlank()) {
            return EmbeddingEncodeResult.Failed("Query embed text cannot be blank.")
        }
        return embedRaw(OnnxBgeSmallEnV15Spec.QUERY_PREFIX + body, forQuery = true)
    }

    /** Drop a loaded runtime after clear-index / model delete / reinstall. */
    fun reset() {
        synchronized(lock) {
            closeSessionLocked()
            cachedTokenizer = null
        }
    }

    private fun embedRaw(text: String, forQuery: Boolean): EmbeddingEncodeResult {
        if (text.isBlank()) {
            return EmbeddingEncodeResult.Failed("Embed text cannot be blank.")
        }
        return synchronized(lock) {
            when (val availability = availabilityUnlocked()) {
                is CapabilityAvailability.Unavailable ->
                    return@synchronized EmbeddingEncodeResult.Unavailable(availability.reason)
                is CapabilityAvailability.Available -> Unit
            }
            val session = ortSession
                ?: return@synchronized EmbeddingEncodeResult.Unavailable(MODEL_LOAD_FAILED_REASON)
            val env = ortEnv
                ?: return@synchronized EmbeddingEncodeResult.Unavailable(MODEL_LOAD_FAILED_REASON)
            val tokenizer = loadTokenizerOrNull()
                ?: return@synchronized EmbeddingEncodeResult.Unavailable(TOKENIZER_MISSING_REASON)
            try {
                val encoded = tokenizer.encodeSingle(
                    text = text,
                    maxLength = OnnxBgeSmallEnV15Spec.MAX_SEQUENCE_LENGTH,
                )
                val vector = OnnxBiEncoderRuntime.embedEncoded(
                    env = env,
                    session = session,
                    encoded = encoded,
                    expectedDimensions = OnnxBgeSmallEnV15Spec.EMBEDDING_DIMENSIONS,
                )
                EmbeddingEncodeResult.Success(
                    vector = vector,
                    model = OnnxBgeSmallEnV15Spec.MODEL_IDENTITY,
                )
            } catch (error: Exception) {
                Log.w(TAG, "embed failed forQuery=$forQuery: ${error.message}")
                EmbeddingEncodeResult.Failed(
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "On-device meaning embed failed on this phone.",
                )
            }
        }
    }

    private fun availabilityUnlocked(): CapabilityAvailability {
        if (!modelStore.isInstalled()) {
            return CapabilityAvailability.Unavailable(MODEL_MISSING_REASON)
        }
        if (loadTokenizerOrNull() == null) {
            return CapabilityAvailability.Unavailable(TOKENIZER_MISSING_REASON)
        }
        ensureSessionLocked()
        return if (ortSession != null) {
            CapabilityAvailability.Available(OnnxBgeSmallEnV15Spec.MODEL_IDENTITY)
        } else {
            CapabilityAvailability.Unavailable(MODEL_LOAD_FAILED_REASON)
        }
    }

    private fun ensureSessionLocked() {
        if (ortSession != null) return
        val path = modelStore.absoluteModelPath() ?: return
        try {
            val opened = OnnxBiEncoderRuntime.openSession(File(path))
            ortEnv = opened.first
            ortSession = opened.second
        } catch (error: Exception) {
            Log.w(TAG, "ONNX session load failed", error)
            closeSessionLocked()
        }
    }

    private fun closeSessionLocked() {
        try {
            ortSession?.close()
        } catch (_: Exception) {
        }
        ortSession = null
        ortEnv = null
    }

    private fun loadTokenizerOrNull(): BertWordPieceTokenizer? {
        cachedTokenizer?.let { return it }
        return try {
            appContext.assets.open(NoBackupRecallRankPackStore.VOCAB_ASSET)
                .bufferedReader()
                .use {
                    BertWordPieceTokenizer.loadFromReader(it).also { loaded ->
                        cachedTokenizer = loaded
                    }
                }
        } catch (error: Exception) {
            Log.w(TAG, "vocab load failed: ${error.message}")
            null
        }
    }

    companion object {
        private const val TAG = "OnnxBgeSmallEmbedding"
        private const val MAX_INPUT_CHARS = 2_048

        const val MODEL_MISSING_REASON =
            "On-device meaning model is not installed on this phone yet."

        const val MODEL_LOAD_FAILED_REASON =
            "On-device meaning model could not be loaded on this phone."

        const val TOKENIZER_MISSING_REASON =
            "On-device meaning tokenizer vocab is not available."
    }
}
