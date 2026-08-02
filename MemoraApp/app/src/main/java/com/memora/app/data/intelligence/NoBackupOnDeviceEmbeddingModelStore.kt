package com.memora.app.data.intelligence

import android.content.Context
import com.memora.app.domain.intelligence.MediaPipeAverageWordEmbedderSpec
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

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
        MediaPipeAverageWordEmbedderSpec.MODEL_IDENTITY

    override fun clear() {
        modelFile().delete()
        tempFile().delete()
        if (rootDir.exists() && rootDir.list().isNullOrEmpty()) {
            rootDir.delete()
        }
    }

    fun modelFile(): File = File(rootDir, MediaPipeAverageWordEmbedderSpec.FILE_NAME)

    fun tempFile(): File = File(rootDir, "${MediaPipeAverageWordEmbedderSpec.FILE_NAME}.tmp")

    fun ensureRoot(): File {
        rootDir.mkdirs()
        return rootDir
    }

    companion object {
        const val ROOT_DIR_NAME = "memora_embedding_models"
    }
}
