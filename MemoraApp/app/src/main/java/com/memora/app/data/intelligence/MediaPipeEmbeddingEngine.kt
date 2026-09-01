package com.memora.app.data.intelligence

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelStore
import java.io.File

/**
 * Product EmbeddingEngine (ADR-031): MediaPipe when the private model file loads;
 * otherwise Unavailable.
 *
 * All [TextEmbedder] access is synchronized — MediaPipe native embed is not safe
 * for concurrent calls (index build on IO + Find-by-meaning on Default).
 */
class MediaPipeEmbeddingEngine(
    private val appContext: Context,
    private val modelStore: OnDeviceEmbeddingModelStore,
) : EmbeddingEngine {
    private val lock = Any()
    private var embedder: TextEmbedder? = null

    override fun availability(): CapabilityAvailability = synchronized(lock) {
        ensureLoadedLocked()
        if (embedder != null) {
            CapabilityAvailability.Available(modelStore.modelIdentity())
        } else {
            CapabilityAvailability.Unavailable(currentUnavailableReason())
        }
    }

    override fun limits(): CapabilityLimits? = synchronized(lock) {
        if (embedder != null) {
            CapabilityLimits(maxInputBytes = 32_768L, maxOutputItems = 1)
        } else {
            null
        }
    }

    override fun embedText(text: String): EmbeddingEncodeResult {
        require(text.isNotBlank()) { "Embedding input text must not be blank." }
        return synchronized(lock) {
            ensureLoadedLocked()
            val active = embedder
                ?: return EmbeddingEncodeResult.Unavailable(currentUnavailableReason())
            embedWithLocked(active, text)
        }
    }

    /** Drop a loaded runtime after clear-index / model delete / reinstall. */
    fun reset() {
        synchronized(lock) {
            embedder?.close()
            embedder = null
        }
    }

    private fun ensureLoadedLocked() {
        if (embedder != null) return
        val path = modelStore.absoluteModelPath() ?: return
        try {
            // Absolute private path — not APK assets; createFromFile opens the file descriptor.
            embedder = TextEmbedder.createFromFile(appContext, File(path))
        } catch (error: Throwable) {
            // Includes linkage / native init failures; never invent Available.
            Log.w(TAG, "TextEmbedder load failed", error)
            embedder = null
        }
    }

    private fun embedWithLocked(active: TextEmbedder, text: String): EmbeddingEncodeResult {
        return try {
            val result = active.embed(text)
            val embedding = result.embeddingResult().embeddings().firstOrNull()
                ?: return EmbeddingEncodeResult.Failed("Embedder returned no vector.")
            val floats = embedding.floatEmbedding()
            if (floats.isEmpty()) {
                return EmbeddingEncodeResult.Failed("Embedder returned an empty vector.")
            }
            EmbeddingEncodeResult.Success(
                vector = EmbeddingVector(floats.copyOf()),
                model = modelStore.modelIdentity(),
            )
        } catch (error: Exception) {
            val reason = error.message?.takeIf { it.isNotBlank() }
                ?: "On-device embedding failed."
            Log.w(TAG, "TextEmbedder embed failed", error)
            EmbeddingEncodeResult.Failed(reason)
        }
    }

    private fun currentUnavailableReason(): String =
        if (!modelStore.isInstalled()) {
            MODEL_MISSING_REASON
        } else {
            MODEL_LOAD_FAILED_REASON
        }

    companion object {
        private const val TAG = "MediaPipeEmbeddingEngine"

        const val MODEL_MISSING_REASON =
            "On-device meaning model is not installed on this phone yet."

        const val MODEL_LOAD_FAILED_REASON =
            "On-device meaning model could not be loaded on this phone."
    }
}
