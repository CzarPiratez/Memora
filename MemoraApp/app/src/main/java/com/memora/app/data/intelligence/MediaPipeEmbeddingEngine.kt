package com.memora.app.data.intelligence

import android.content.Context
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelStore
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Product EmbeddingEngine (ADR-031): MediaPipe when the private model file loads;
 * otherwise Unavailable.
 */
class MediaPipeEmbeddingEngine(
    private val appContext: Context,
    private val modelStore: OnDeviceEmbeddingModelStore,
) : EmbeddingEngine {
    private val lock = Any()
    private val embedderRef = AtomicReference<TextEmbedder?>(null)

    override fun availability(): CapabilityAvailability {
        ensureLoaded()
        return if (embedderRef.get() != null) {
            CapabilityAvailability.Available(modelStore.modelIdentity())
        } else {
            CapabilityAvailability.Unavailable(currentUnavailableReason())
        }
    }

    override fun limits(): CapabilityLimits? =
        if (embedderRef.get() != null) {
            CapabilityLimits(maxInputBytes = 32_768L, maxOutputItems = 1)
        } else {
            null
        }

    override fun embedText(text: String): EmbeddingEncodeResult {
        require(text.isNotBlank()) { "Embedding input text must not be blank." }
        ensureLoaded()
        val embedder = embedderRef.get()
            ?: return EmbeddingEncodeResult.Unavailable(currentUnavailableReason())
        return try {
            val result = embedder.embed(text)
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
            EmbeddingEncodeResult.Failed(reason)
        }
    }

    /** Drop a loaded runtime after clear-index / model delete / reinstall. */
    fun reset() {
        synchronized(lock) {
            embedderRef.getAndSet(null)?.close()
        }
    }

    private fun ensureLoaded() {
        if (embedderRef.get() != null) return
        synchronized(lock) {
            if (embedderRef.get() != null) return
            val path = modelStore.absoluteModelPath() ?: return
            try {
                // Absolute private path — not APK assets; createFromFile opens the file descriptor.
                embedderRef.set(TextEmbedder.createFromFile(appContext, File(path)))
            } catch (_: Exception) {
                embedderRef.set(null)
            }
        }
    }

    private fun currentUnavailableReason(): String =
        if (!modelStore.isInstalled()) {
            MODEL_MISSING_REASON
        } else {
            MODEL_LOAD_FAILED_REASON
        }

    companion object {
        const val MODEL_MISSING_REASON =
            "On-device meaning model is not installed on this phone yet."

        const val MODEL_LOAD_FAILED_REASON =
            "On-device meaning model could not be loaded on this phone."
    }
}
