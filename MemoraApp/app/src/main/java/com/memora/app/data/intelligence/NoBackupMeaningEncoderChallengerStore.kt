package com.memora.app.data.intelligence

import android.content.Context
import com.memora.app.domain.intelligence.MeaningEncoderChallengerPackPresence
import com.memora.app.domain.intelligence.OnnxBgeSmallEnV15Spec
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-private no-backup store for the ADR-055 slice 3 BGE challenger ONNX.
 * Separate from product USE and from the Stage A rerank pack.
 */
@Singleton
class NoBackupMeaningEncoderChallengerStore @Inject constructor(
    @ApplicationContext context: Context,
) : MeaningEncoderChallengerPackPresence {
    private val rootDir = File(
        context.applicationContext.noBackupFilesDir,
        ROOT_DIR_NAME,
    )

    fun ensureRoot(): File {
        rootDir.mkdirs()
        return rootDir
    }

    fun modelFile(): File = File(rootDir, OnnxBgeSmallEnV15Spec.FILE_NAME)

    fun tempFile(): File = File(rootDir, "${OnnxBgeSmallEnV15Spec.FILE_NAME}.tmp")

    override fun isInstalled(): Boolean {
        val file = modelFile()
        return file.exists() && file.length() > 0L
    }

    fun absoluteModelPath(): String? {
        val file = modelFile()
        if (!file.exists() || file.length() <= 0L) return null
        return file.absolutePath
    }

    fun clear() {
        modelFile().delete()
        tempFile().delete()
        if (rootDir.exists() && rootDir.list().isNullOrEmpty()) {
            rootDir.delete()
        }
    }

    companion object {
        const val ROOT_DIR_NAME = "memora_meaning_encoder_challenger_v1"
        /** Same BERT WordPiece vocab already shipped for Stage A. */
        const val VOCAB_ASSET = NoBackupRecallRankPackStore.VOCAB_ASSET
    }
}
