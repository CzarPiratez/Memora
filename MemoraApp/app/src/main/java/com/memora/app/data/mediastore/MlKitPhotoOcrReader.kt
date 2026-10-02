package com.memora.app.data.mediastore

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.extraction.PhotoOcrExtractionRecord
import com.memora.app.domain.extraction.PhotoOcrReadResult
import com.memora.app.domain.extraction.PhotoOcrReader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MlKitPhotoOcrReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PhotoOcrReader {
    override val engineVersion: String get() = ENGINE_VERSION

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override fun read(asset: Asset): PhotoOcrReadResult {
        if (asset.type != AssetType.PHOTO) return PhotoOcrReadResult.RetryableFailure
        val uri = try {
            Uri.parse(asset.location.value)
        } catch (_: Exception) {
            return PhotoOcrReadResult.RetryableFailure
        }
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val raw = Tasks.await(
                recognizer.process(image),
                OCR_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            ).text.trim()
            val truncated = raw.length > PhotoOcrExtractionRecord.MAX_STORED_CHARS
            PhotoOcrReadResult.Text(
                fullText = if (truncated) {
                    raw.take(PhotoOcrExtractionRecord.MAX_STORED_CHARS)
                } else {
                    raw
                },
                textTruncated = truncated,
                engineId = PhotoOcrExtractionRecord.ENGINE_MLKIT_LATIN_BUNDLED,
                engineVersion = ENGINE_VERSION,
            )
        } catch (_: SecurityException) {
            PhotoOcrReadResult.AccessStopped
        } catch (_: IOException) {
            PhotoOcrReadResult.RetryableFailure
        } catch (_: TimeoutException) {
            PhotoOcrReadResult.RetryableFailure
        } catch (_: ExecutionException) {
            PhotoOcrReadResult.RetryableFailure
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            PhotoOcrReadResult.RetryableFailure
        } catch (_: Exception) {
            PhotoOcrReadResult.RetryableFailure
        }
    }

    companion object {
        const val ENGINE_VERSION = "16.0.1"
        private const val OCR_TIMEOUT_SECONDS = 60L
    }
}
