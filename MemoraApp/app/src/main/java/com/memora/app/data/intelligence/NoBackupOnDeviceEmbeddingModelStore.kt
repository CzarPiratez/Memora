package com.memora.app.data.intelligence

import android.content.Context
import com.memora.app.domain.intelligence.MediaPipeAverageWordEmbedderSpec
import com.memora.app.domain.intelligence.MediaPipeUniversalSentenceEncoderSpec
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelStore
import com.memora.app.domain.intelligence.OnnxBgeSmallEnV15Spec
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Private product meaning-model store (ADR-055 slice 4: BGE-small ONNX).
 *
 * Legacy MediaPipe USE / average-word files may remain until a successful BGE
 * install clears them. They never count as installed.
 */
@Singleton
class NoBackupOnDeviceEmbeddingModelStore @Inject constructor(
    @ApplicationContext context: Context,
) : OnDeviceEmbeddingModelStore {
    private val rootDir = File(
        context.applicationContext.noBackupFilesDir,
        ROOT_DIR_NAME,
    )

    override fun isInstalled(): Boolean {
        val file = modelFile()
        return file.exists() && file.length() > 0L
    }

    override fun absoluteModelPath(): String? {
        val file = modelFile()
        if (!file.exists() || file.length() <= 0L) return null
        return file.absolutePath
    }

    override fun modelIdentity(): ModelVersionIdentity =
        OnnxBgeSmallEnV15Spec.MODEL_IDENTITY

    override fun clear() {
        modelFile().delete()
        tempFile().delete()
        legacyUseFile().delete()
        legacyAverageWordFile().delete()
        if (rootDir.exists() && rootDir.list().isNullOrEmpty()) {
            rootDir.delete()
        }
    }

    fun modelFile(): File =
        File(rootDir, OnnxBgeSmallEnV15Spec.FILE_NAME)

    fun tempFile(): File =
        File(rootDir, "${OnnxBgeSmallEnV15Spec.FILE_NAME}.tmp")

    fun legacyUseFile(): File =
        File(rootDir, MediaPipeUniversalSentenceEncoderSpec.FILE_NAME)

    fun legacyAverageWordFile(): File =
        File(rootDir, MediaPipeAverageWordEmbedderSpec.FILE_NAME)

    fun ensureRoot(): File {
        rootDir.mkdirs()
        return rootDir
    }

    companion object {
        const val ROOT_DIR_NAME = "memora_embedding_models"
    }
}
