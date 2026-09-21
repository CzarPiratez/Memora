package com.memora.app.data.intelligence

import android.content.Context
import android.util.Log
import com.memora.app.domain.intelligence.BertWordPieceTokenizer
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.OnnxBgeSmallEnV15Spec
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Probe-only BGE-small [EmbeddingEngine]. Never bound as the product engine.
 * Vectors are incompatible with the USE Room index.
 */
@Singleton
class OnnxBgeSmallEmbeddingEngine @Inject constructor(
    @ApplicationContext context: Context,
    private val store: NoBackupMeaningEncoderChallengerStore,
) : EmbeddingEngine {
    private val appContext = context.applicationContext
    private val lock = Any()
    private var cachedTokenizer: BertWordPieceTokenizer? = null

    override fun availability(): CapabilityAvailability {
        if (!store.isInstalled()) {
            return CapabilityAvailability.Unavailable(
                "BGE challenger pack is not installed on this phone yet.",
            )
        }
        if (loadTokenizerOrNull() == null) {
            return CapabilityAvailability.Unavailable(
                "BGE challenger tokenizer vocab is not available.",
            )
        }
        return CapabilityAvailability.Available(OnnxBgeSmallEnV15Spec.MODEL_IDENTITY)
    }

    override fun limits(): CapabilityLimits =
        CapabilityLimits(
            maxInputBytes = MAX_INPUT_CHARS * 4L,
            maxOutputItems = 1,
        )

    override fun embedText(text: String): EmbeddingEncodeResult {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return EmbeddingEncodeResult.Failed("Embed text cannot be blank.")
        }
        when (val availability = availability()) {
            is CapabilityAvailability.Unavailable ->
                return EmbeddingEncodeResult.Unavailable(availability.reason)
            is CapabilityAvailability.Available -> Unit
        }
        val modelPath = store.absoluteModelPath()
            ?: return EmbeddingEncodeResult.Unavailable("BGE challenger model path missing.")
        val tokenizer = loadTokenizerOrNull()
            ?: return EmbeddingEncodeResult.Unavailable("BGE challenger tokenizer missing.")

        return synchronized(lock) {
            try {
                val encoded = tokenizer.encodeSingle(
                    text = trimmed,
                    maxLength = OnnxBgeSmallEnV15Spec.MAX_SEQUENCE_LENGTH,
                )
                val (env, session) = OnnxBiEncoderRuntime.openSession(File(modelPath))
                try {
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
                } finally {
                    session.close()
                }
            } catch (error: Exception) {
                Log.w(TAG, "embed failed: ${error.message}")
                EmbeddingEncodeResult.Failed(
                    error.message?.takeIf { it.isNotBlank() }
                        ?: "BGE challenger embed failed on this phone.",
                )
            }
        }
    }

    /**
     * Query encode with the BGE retrieval instruction. Document / Memory text
     * uses [embedText] without a prefix.
     */
    fun embedQuery(contentTokensJoined: String): EmbeddingEncodeResult {
        val body = contentTokensJoined.trim()
        if (body.isBlank()) {
            return EmbeddingEncodeResult.Failed("Query embed text cannot be blank.")
        }
        return embedText(OnnxBgeSmallEnV15Spec.QUERY_PREFIX + body)
    }

    fun reset() {
        synchronized(lock) {
            cachedTokenizer = null
        }
    }

    private fun loadTokenizerOrNull(): BertWordPieceTokenizer? {
        cachedTokenizer?.let { return it }
        return try {
            appContext.assets.open(NoBackupMeaningEncoderChallengerStore.VOCAB_ASSET)
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

    private companion object {
        const val TAG = "OnnxBgeSmallEmbedding"
        const val MAX_INPUT_CHARS = 2_048
    }
}
