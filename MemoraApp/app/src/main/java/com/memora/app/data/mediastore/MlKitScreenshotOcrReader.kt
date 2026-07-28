package com.memora.app.data.mediastore

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.extraction.ScreenshotOcrExtractionRecord
import com.memora.app.domain.extraction.ScreenshotOcrReadResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens a discovered MediaStore screenshot URI read-only and runs bundled Latin OCR.
 *
 * Discovery must not call this (ADR-009). Extract path only. No network upload.
 * [ScreenshotOcrReadResult.AccessStopped] is reserved for true grant loss / SecurityException.
 */
fun interface ScreenshotOcrReader {
    fun read(asset: Asset): ScreenshotOcrReadResult
}

@Singleton
class MlKitScreenshotOcrReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ScreenshotOcrReader {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override fun read(asset: Asset): ScreenshotOcrReadResult {
        if (asset.type != AssetType.SCREENSHOT) {
            return ScreenshotOcrReadResult.RetryableFailure
        }
        val uri = try {
            Uri.parse(asset.location.value)
        } catch (_: Exception) {
            return ScreenshotOcrReadResult.RetryableFailure
        }

        return try {
            // fromFilePath accepts a content Uri (ML Kit API name is historical).
            // Prefer this over BitmapFactory decode, which can fail open after EXIF succeeded.
            val image = InputImage.fromFilePath(context, uri)
            val visionText = Tasks.await(
                recognizer.process(image),
                OCR_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
            val raw = visionText.text.trim()
            val truncated = raw.length > ScreenshotOcrExtractionRecord.MAX_STORED_CHARS
            val stored = if (truncated) {
                raw.take(ScreenshotOcrExtractionRecord.MAX_STORED_CHARS)
            } else {
                raw
            }
            ScreenshotOcrReadResult.Text(
                fullText = stored,
                textTruncated = truncated,
                engineId = ScreenshotOcrExtractionRecord.ENGINE_MLKIT_LATIN_BUNDLED,
                engineVersion = ENGINE_VERSION,
            )
        } catch (_: SecurityException) {
            ScreenshotOcrReadResult.AccessStopped
        } catch (_: IOException) {
            ScreenshotOcrReadResult.RetryableFailure
        } catch (_: TimeoutException) {
            ScreenshotOcrReadResult.RetryableFailure
        } catch (_: ExecutionException) {
            ScreenshotOcrReadResult.RetryableFailure
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            ScreenshotOcrReadResult.RetryableFailure
        } catch (_: Exception) {
            ScreenshotOcrReadResult.RetryableFailure
        }
    }

    companion object {
        const val ENGINE_VERSION = "16.0.1"
        private const val OCR_TIMEOUT_SECONDS = 60L
    }
}
