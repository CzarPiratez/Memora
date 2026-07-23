package com.memora.app.data.pdfbox.isolation

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import com.memora.app.data.pdfbox.PdfBoxPdfDocumentParser
import com.memora.app.data.pdfbox.PdfDocumentParseResult
import com.memora.app.domain.extraction.PdfTextCoverage
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Private worker for parsing one descriptor in Android's isolated-process sandbox.
 *
 * Its Binder contract deliberately has no URI, path, source identity, text, metadata,
 * Room, Hilt, or UI parameter. It returns only a versioned, bounded, validated result
 * envelope. The ordinary-process client validates that envelope and currently discards all
 * chunks after deriving a status summary; no extraction is persisted in this checkpoint.
 */
class IsolatedPdfParserService : Service() {
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "memora-isolated-pdf-parser")
    }

    private val parser = PdfBoxPdfDocumentParser()

    private val binder = object : IIsolatedPdfParser.Stub() {
        override fun parse(
            source: ParcelFileDescriptor,
            protocolVersion: Int,
        ): Bundle {
            if (protocolVersion != PROTOCOL_VERSION) {
                source.closeQuietly()
                return protocolFailure()
            }

            return try {
                worker.submit<Bundle> {
                    ParcelFileDescriptor.AutoCloseInputStream(source).use { input ->
                        parser.parse(input).toWireEnvelope()
                    }
                }.get()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                source.closeQuietly()
                retryableFailure()
            } catch (_: ExecutionException) {
                source.closeQuietly()
                retryableFailure()
            } catch (_: RuntimeException) {
                source.closeQuietly()
                retryableFailure()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun PdfDocumentParseResult.toWireEnvelope(): Bundle = when (this) {
            is PdfDocumentParseResult.Parsed -> {
                if (textCoverage == PdfTextCoverage.NoExtractableText) {
                    envelope(
                        IsolatedPdfParserWireResult(
                            schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
                            outcome = IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT,
                            retryable = false,
                            pageCount = pageCount,
                        ),
                    )
                } else {
                    envelope(
                        IsolatedPdfParserWireResult(
                            schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
                            outcome = IsolatedPdfParserWireOutcome.EXTRACTED,
                            retryable = false,
                            pageCount = pageCount,
                            chunks = pages.flatMap { page -> page.text.toChunks(page.pageNumber) },
                        ),
                    )
                }
            }

            PdfDocumentParseResult.PasswordProtected -> envelope(
                IsolatedPdfParserWireResult(
                    schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
                    outcome = IsolatedPdfParserWireOutcome.PASSWORD_PROTECTED,
                    retryable = false,
                ),
            )

            is PdfDocumentParseResult.Failed -> {
                envelope(
                    IsolatedPdfParserWireResult(
                        schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
                        outcome = IsolatedPdfParserWireOutcome.FAILURE,
                        retryable = retryable,
                    ),
                )
            }
        }

    private fun String.toChunks(pageNumber: Int): List<IsolatedPdfParserPageTextChunk> {
        if (isEmpty()) {
            return listOf(IsolatedPdfParserPageTextChunk(pageNumber, 0, true, ""))
        }

        val chunks = mutableListOf<IsolatedPdfParserPageTextChunk>()
        var start = 0
        while (start < length) {
            var end = minOf(start + IsolatedPdfParserSyntheticResultPolicy.MAXIMUM_CHUNK_TEXT_CODE_UNITS, length)
            if (end < length && this[end - 1].isHighSurrogate() && this[end].isLowSurrogate()) {
                end -= 1
            }
            chunks += IsolatedPdfParserPageTextChunk(
                pageNumber = pageNumber,
                chunkIndex = chunks.size,
                isFinalChunk = end == length,
                text = substring(start, end),
            )
            start = end
        }
        return chunks
    }

    private fun envelope(candidate: IsolatedPdfParserWireResult): Bundle {
        val boundedResult = IsolatedPdfParserResultBundleCodec.encode(
            candidate,
            IsolatedPdfParserSyntheticResultPolicy.limits,
        ) ?: return retryableFailure()
        return Bundle().apply {
            putBoolean(KEY_IS_ISOLATED, isRunningIsolated())
            putBundle(KEY_BOUNDED_RESULT, boundedResult)
        }
    }

    private fun retryableFailure(): Bundle = envelopeFailure(retryable = true)

    private fun protocolFailure(): Bundle = envelopeFailure(retryable = false)

    private fun envelopeFailure(retryable: Boolean): Bundle = Bundle().apply {
        putBoolean(KEY_IS_ISOLATED, isRunningIsolated())
        putBundle(
            KEY_BOUNDED_RESULT,
            checkNotNull(
                IsolatedPdfParserResultBundleCodec.encode(
                    IsolatedPdfParserWireResult(
                        schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
                        outcome = IsolatedPdfParserWireOutcome.FAILURE,
                        retryable = retryable,
                    ),
                    IsolatedPdfParserSyntheticResultPolicy.limits,
                ),
            ),
        )
    }

    private fun isRunningIsolated(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && Process.isIsolated()

    private fun ParcelFileDescriptor.closeQuietly() {
        try {
            close()
        } catch (_: IOException) {
            // A duplicate descriptor may already have been closed by the stream owner.
        }
    }

    companion object {
        const val PROTOCOL_VERSION = 2
        const val KEY_IS_ISOLATED = "is_isolated"
        const val KEY_BOUNDED_RESULT = "bounded_result"
    }
}
