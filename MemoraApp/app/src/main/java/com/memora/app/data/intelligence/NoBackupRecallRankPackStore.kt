package com.memora.app.data.intelligence

import android.content.Context
import com.memora.app.domain.intelligence.OnnxMsMarcoMiniLmCrossEncoderSpec
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-private no-backup store for Stage A rerank ONNX weights (not in APK).
 * Vocab ships in assets until ADR-052 pack bundles tokenizer assets.
 */
@Singleton
class NoBackupRecallRankPackStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext
    private val rootDir = File(appContext.noBackupFilesDir, ROOT_DIR_NAME)
    private val spikeDir = File(appContext.noBackupFilesDir, SPIKE_STAGING_DIR)

    fun ensureRoot(): File {
        rootDir.mkdirs()
        return rootDir
    }

    fun modelFile(): File = File(rootDir, OnnxMsMarcoMiniLmCrossEncoderSpec.FILE_NAME)

    fun isModelInstalled(): Boolean {
        val primary = modelFile()
        if (primary.exists() && primary.length() > 0L) return true
        val spike = File(spikeDir, OnnxMsMarcoMiniLmCrossEncoderSpec.FILE_NAME)
        return spike.exists() && spike.length() > 0L
    }

    fun absoluteModelPath(): String? {
        val primary = modelFile()
        if (primary.exists() && primary.length() > 0L) return primary.absolutePath
        val spike = File(spikeDir, OnnxMsMarcoMiniLmCrossEncoderSpec.FILE_NAME)
        if (spike.exists() && spike.length() > 0L) return spike.absolutePath
        return null
    }

    fun clear() {
        modelFile().delete()
        if (rootDir.exists() && rootDir.list().isNullOrEmpty()) {
            rootDir.delete()
        }
    }

    companion object {
        const val ROOT_DIR_NAME = "memora_recall_rank_pack_v1"
        const val SPIKE_STAGING_DIR = "recall_rank_spike_staging"
        const val VOCAB_ASSET = "recall_rank/bert_vocab.txt"
    }
}
