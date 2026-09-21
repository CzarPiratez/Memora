package com.memora.app.data.intelligence

import com.memora.app.domain.intelligence.MeaningEncoderChallengerDownloader
import com.memora.app.domain.intelligence.OnDeviceEmbeddingModelDownloadOutcome
import com.memora.app.domain.intelligence.OnnxBgeSmallEnV15Spec
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HttpMeaningEncoderChallengerDownloader @Inject constructor(
    private val store: NoBackupMeaningEncoderChallengerStore,
) : MeaningEncoderChallengerDownloader {
    override fun downloadChallengerModel(): OnDeviceEmbeddingModelDownloadOutcome {
        return try {
            store.ensureRoot()
            val temp = store.tempFile()
            val target = store.modelFile()
            temp.delete()
            val connection = (URL(OnnxBgeSmallEnV15Spec.DOWNLOAD_URL)
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
                        "Challenger model download failed (HTTP $code).",
                    )
                }
                val declared = connection.contentLengthLong
                if (declared > OnnxBgeSmallEnV15Spec.MAX_DOWNLOAD_BYTES) {
                    return OnDeviceEmbeddingModelDownloadOutcome.Failed(
                        "Challenger model is larger than the disclosed size limit.",
                    )
                }
                val digest = MessageDigest.getInstance("SHA-256")
                BufferedInputStream(connection.inputStream).use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            if (total > OnnxBgeSmallEnV15Spec.MAX_DOWNLOAD_BYTES) {
                                temp.delete()
                                return OnDeviceEmbeddingModelDownloadOutcome.Failed(
                                    "Challenger model exceeded the disclosed size limit.",
                                )
                            }
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                        }
                    }
                }
                if (!temp.exists() || temp.length() <= 0L) {
                    temp.delete()
                    return OnDeviceEmbeddingModelDownloadOutcome.Failed(
                        "Challenger model download produced an empty file.",
                    )
                }
                val hex = digest.digest().joinToString("") { byte ->
                    "%02x".format(byte)
                }
                if (hex != OnnxBgeSmallEnV15Spec.EXPECTED_SHA256) {
                    temp.delete()
                    return OnDeviceEmbeddingModelDownloadOutcome.Failed(
                        "Challenger model integrity check failed.",
                    )
                }
                target.delete()
                if (!temp.renameTo(target)) {
                    temp.copyTo(target, overwrite = true)
                    temp.delete()
                }
                if (!store.isInstalled()) {
                    return OnDeviceEmbeddingModelDownloadOutcome.Failed(
                        "Challenger model could not be saved on this phone.",
                    )
                }
                OnDeviceEmbeddingModelDownloadOutcome.Installed
            } finally {
                connection.disconnect()
            }
        } catch (error: Exception) {
            store.tempFile().delete()
            val reason = error.message?.takeIf { it.isNotBlank() }
                ?: "Challenger model download failed."
            OnDeviceEmbeddingModelDownloadOutcome.Failed(reason)
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 300_000
        val HTTP_SUCCESS_RANGE = 200..299
    }
}
