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
import java.util.ArrayDeque
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Private worker for parsing one descriptor in Android's isolated-process sandbox.
 *
 * Protocol v3 streams a content-free header, then one bounded chunk per pull, then an explicit
 * session-complete sentinel. The ordinary-process client validates through the session assembler
 * and currently discards all page text after deriving a status summary.
 */
class IsolatedPdfParserService : Service() {
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "memora-isolated-pdf-parser")
    }

    private val parser = PdfBoxPdfDocumentParser()
    private val sessionLock = Any()
    private var remainingChunks: ArrayDeque<IsolatedPdfParserPageTextChunk>? = null

    private val binder = object : IIsolatedPdfParser.Stub() {
        override fun begin(
            source: ParcelFileDescriptor,
            protocolVersion: Int,
        ): Bundle {
            clearSession()
            if (protocolVersion != PROTOCOL_VERSION) {
                source.closeQuietly()
                return protocolFailureHeader()
            }

            return try {
                worker.submit<Bundle> {
                    ParcelFileDescriptor.AutoCloseInputStream(source).use { input ->
                        parser.parse(input).toSessionBeginBundle()
                    }
                }.get()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                source.closeQuietly()
                clearSession()
                retryableFailureHeader()
            } catch (_: ExecutionException) {
                source.closeQuietly()
                clearSession()
                retryableFailureHeader()
            } catch (_: RuntimeException) {
                source.closeQuietly()
                clearSession()
                retryableFailureHeader()
            }
        }

        override fun nextChunk(): Bundle {
            val next = synchronized(sessionLock) {
                val queue = remainingChunks ?: return@synchronized null
                if (queue.isEmpty()) {
                    remainingChunks = null
                    SessionPull.Complete
                } else {
                    SessionPull.Chunk(queue.removeFirst())
                }
            }
            return when (next) {
                null -> retryableFailureHeader()
                SessionPull.Complete -> IsolatedPdfParserSessionMessageCodec.encodeSessionComplete(
                    isRunningIsolated(),
                )
                is SessionPull.Chunk -> IsolatedPdfParserSessionMessageCodec.encodeChunk(
                    chunk = next.chunk,
                    isIsolated = isRunningIsolated(),
                )
            }
        }

        override fun cancel() {
            clearSession()
        }
    }

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        clearSession()
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun PdfDocumentParseResult.toSessionBeginBundle(): Bundle = when (this) {
        is PdfDocumentParseResult.Parsed -> {
            if (textCoverage == PdfTextCoverage.NoExtractableText) {
                headerOnly(
                    IsolatedPdfParserSessionHeader(
                        schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
                        outcome = IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT,
                        retryable = false,
                        pageCount = pageCount,
                    ),
                )
            } else {
                val chunks = pages.flatMap { page -> page.text.toChunks(page.pageNumber) }
                val candidate = IsolatedPdfParserWireResult(
                    schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
                    outcome = IsolatedPdfParserWireOutcome.EXTRACTED,
                    retryable = false,
                    pageCount = pageCount,
                    chunks = chunks,
                )
                if (IsolatedPdfParserResultBundleCodec.encode(
                        candidate,
                        IsolatedPdfParserSyntheticResultPolicy.limits,
                    ) == null
                ) {
                    retryableFailureHeader()
                } else {
                    synchronized(sessionLock) {
                        remainingChunks = ArrayDeque(chunks)
                    }
                    headerOnly(
                        IsolatedPdfParserSessionHeader(
                            schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
                            outcome = IsolatedPdfParserWireOutcome.EXTRACTED,
                            retryable = false,
                            pageCount = pageCount,
                        ),
                    )
                }
            }
        }

        PdfDocumentParseResult.PasswordProtected -> headerOnly(
            IsolatedPdfParserSessionHeader(
                schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
                outcome = IsolatedPdfParserWireOutcome.PASSWORD_PROTECTED,
                retryable = false,
            ),
        )

        is PdfDocumentParseResult.Failed -> headerOnly(
            IsolatedPdfParserSessionHeader(
                schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
                outcome = IsolatedPdfParserWireOutcome.FAILURE,
                retryable = retryable,
            ),
        )
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

    private fun headerOnly(header: IsolatedPdfParserSessionHeader): Bundle =
        IsolatedPdfParserSessionMessageCodec.encodeHeader(header, isRunningIsolated())

    private fun retryableFailureHeader(): Bundle = headerOnly(
        IsolatedPdfParserSessionHeader(
            schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
            outcome = IsolatedPdfParserWireOutcome.FAILURE,
            retryable = true,
        ),
    )

    private fun protocolFailureHeader(): Bundle = headerOnly(
        IsolatedPdfParserSessionHeader(
            schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
            outcome = IsolatedPdfParserWireOutcome.FAILURE,
            retryable = false,
        ),
    )

    private fun clearSession() {
        synchronized(sessionLock) {
            remainingChunks = null
        }
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

    private sealed interface SessionPull {
        data class Chunk(val chunk: IsolatedPdfParserPageTextChunk) : SessionPull

        data object Complete : SessionPull
    }

    companion object {
        const val PROTOCOL_VERSION = 3
        const val KEY_IS_ISOLATED = "is_isolated"
    }
}
