package com.memora.app.data.intelligence

import com.memora.app.domain.intelligence.MediaPipeUniversalSentenceEncoderSpec
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelDownloadOutcome
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelDownloader
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpOnDeviceEmbeddingModelDownloader @Inject constructor(
    private val modelStore: NoBackupOnDeviceEmbeddingModelStore,
) : OnDeviceEmbeddingModelDownloader {
    override fun downloadProductModel(): OnDeviceEmbeddingModelDownloadOutcome {
        return try {
        modelStore.ensureRoot()
        val temp = modelStore.tempFile()
        val target = modelStore.modelFile()
        temp.delete()
        val connection = (URL(MediaPipeUniversalSentenceEncoderSpec.DOWNLOAD_URL)
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            val code = connection.responseCode
            if (code !in HTTP_SUCCESS_RANGE) {
                return OnDeviceEmbeddingModelDownloadOutcome.Failed(
                    "Model download failed (HTTP $code).",
                )
            }
            val declared = connection.contentLengthLong
            if (declared > MediaPipeUniversalSentenceEncoderSpec.MAX_DOWNLOAD_BYTES) {
                return OnDeviceEmbeddingModelDownloadOutcome.Failed(
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
                        if (total > MediaPipeUniversalSentenceEncoderSpec.MAX_DOWNLOAD_BYTES) {
                            temp.delete()
                            return OnDeviceEmbeddingModelDownloadOutcome.Failed(
                                "Model download exceeded the disclosed size limit.",
                            )
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
            if (!temp.exists() || temp.length() <= 0L) {
                temp.delete()
                return OnDeviceEmbeddingModelDownloadOutcome.Failed(
                    "Model download produced an empty file.",
                )
            }
            target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            if (!modelStore.isInstalled()) {
                return OnDeviceEmbeddingModelDownloadOutcome.Failed(
                    "Model could not be saved on this phone.",
                )
            }
            modelStore.legacyAverageWordFile().delete()
            OnDeviceEmbeddingModelDownloadOutcome.Installed
        } finally {
            connection.disconnect()
        }
    } catch (error: Exception) {
        modelStore.tempFile().delete()
        val reason = error.message?.takeIf { it.isNotBlank() }
            ?: "Model download failed."
        OnDeviceEmbeddingModelDownloadOutcome.Failed(reason)
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 300_000
        val HTTP_SUCCESS_RANGE = 200..299
    }
}
