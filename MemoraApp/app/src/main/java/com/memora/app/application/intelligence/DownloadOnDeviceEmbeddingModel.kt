package com.memora.app.application.intelligence

import com.memora.app.data.intelligence.NoBackupOnDeviceEmbeddingModelStore
import com.memora.app.domain.intelligence.AiPackInstallLedger
import com.memora.app.domain.intelligence.EmbeddingFirstAiPackTrack
import com.memora.app.domain.intelligence.MediaPipeAverageWordEmbedderSpec
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Downloads the MediaPipe average-word embedder into private storage (ADR-031).
 *
 * Requires prior disclosure acknowledgment. Downloads model bytes only — never
 * uploads Memories or source content.
 */
class DownloadOnDeviceEmbeddingModel @Inject constructor(
    private val ledger: AiPackInstallLedger,
    private val modelStore: NoBackupOnDeviceEmbeddingModelStore,
) {
    operator fun invoke(): DownloadOnDeviceEmbeddingModelResult {
        val entry = ledger.entry(EmbeddingFirstAiPackTrack.PLANNED_PACK_ID)
        if (entry?.disclosureAcknowledgedAtEpochMs == null) {
            return DownloadOnDeviceEmbeddingModelResult.DisclosureRequired
        }
        if (modelStore.isInstalled()) {
            return DownloadOnDeviceEmbeddingModelResult.AlreadyInstalled
        }

        return try {
            modelStore.ensureRoot()
            val temp = modelStore.tempFile()
            val target = modelStore.modelFile()
            temp.delete()
            val connection = (URL(MediaPipeAverageWordEmbedderSpec.DOWNLOAD_URL)
                .openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 120_000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    return DownloadOnDeviceEmbeddingModelResult.Failed(
                        "Model download failed (HTTP $code).",
                    )
                }
                val declared = connection.contentLengthLong
                if (declared > MediaPipeAverageWordEmbedderSpec.MAX_DOWNLOAD_BYTES) {
                    return DownloadOnDeviceEmbeddingModelResult.Failed(
                        "Model download is larger than the disclosed size limit.",
                    )
                }
                BufferedInputStream(connection.inputStream).use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > MediaPipeAverageWordEmbedderSpec.MAX_DOWNLOAD_BYTES) {
                                temp.delete()
                                return DownloadOnDeviceEmbeddingModelResult.Failed(
                                    "Model download exceeded the disclosed size limit.",
                                )
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
                if (!temp.exists() || temp.length() <= 0L) {
                    temp.delete()
                    return DownloadOnDeviceEmbeddingModelResult.Failed(
                        "Model download produced an empty file.",
                    )
                }
                target.delete()
                if (!temp.renameTo(target)) {
                    temp.copyTo(target, overwrite = true)
                    temp.delete()
                }
                if (!modelStore.isInstalled()) {
                    return DownloadOnDeviceEmbeddingModelResult.Failed(
                        "Model could not be saved on this phone.",
                    )
                }
                DownloadOnDeviceEmbeddingModelResult.Installed
            } finally {
                connection.disconnect()
            }
        } catch (error: Exception) {
            modelStore.tempFile().delete()
            val reason = error.message?.takeIf { it.isNotBlank() }
                ?: "Model download failed."
            DownloadOnDeviceEmbeddingModelResult.Failed(reason)
        }
    }
}

sealed interface DownloadOnDeviceEmbeddingModelResult {
    data object DisclosureRequired : DownloadOnDeviceEmbeddingModelResult

    data object AlreadyInstalled : DownloadOnDeviceEmbeddingModelResult

    data object Installed : DownloadOnDeviceEmbeddingModelResult

    data class Failed(val reason: String) : DownloadOnDeviceEmbeddingModelResult {
        init {
            require(reason.isNotBlank())
        }
    }
}
