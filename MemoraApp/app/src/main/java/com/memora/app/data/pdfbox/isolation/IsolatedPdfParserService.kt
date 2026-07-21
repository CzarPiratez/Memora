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
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Private worker for parsing one descriptor in Android's isolated-process sandbox.
 *
 * Its Binder contract deliberately has no URI, path, source identity, text, metadata,
 * Room, Hilt, or UI parameter. This first boundary exercise returns only a bounded
 * parser-status summary; later work must add an independently reviewed page-chunk
 * protocol before any real source can be enabled.
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
                source.close()
                return protocolFailure()
            }

            return try {
            worker.submit<Bundle> {
                ParcelFileDescriptor.AutoCloseInputStream(source).use { input ->
                    parser.parse(input).toWireSummary()
                }
            }.get()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                retryableFailure()
            } catch (_: ExecutionException) {
                retryableFailure()
            } catch (_: RuntimeException) {
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

    private fun PdfDocumentParseResult.toWireSummary(): Bundle = Bundle().apply {
        putBoolean(KEY_IS_ISOLATED, isRunningIsolated())
        when (this@toWireSummary) {
            is PdfDocumentParseResult.Parsed -> {
                putString(
                    KEY_OUTCOME,
                    if (textCoverage is com.memora.app.domain.extraction.PdfTextCoverage.NoExtractableText) {
                        OUTCOME_NO_EXTRACTABLE_TEXT
                    } else {
                        OUTCOME_EXTRACTED
                    },
                )
                putInt(KEY_PAGE_COUNT, pageCount)
            }

            PdfDocumentParseResult.PasswordProtected -> putString(KEY_OUTCOME, OUTCOME_PASSWORD_PROTECTED)

            is PdfDocumentParseResult.Failed -> {
                putString(KEY_OUTCOME, OUTCOME_FAILURE)
                putBoolean(KEY_RETRYABLE, retryable)
            }
        }
    }

    private fun retryableFailure(): Bundle = Bundle().apply {
        putBoolean(KEY_IS_ISOLATED, isRunningIsolated())
        putString(KEY_OUTCOME, OUTCOME_FAILURE)
        putBoolean(KEY_RETRYABLE, true)
    }

    private fun protocolFailure(): Bundle = Bundle().apply {
        putBoolean(KEY_IS_ISOLATED, isRunningIsolated())
        putString(KEY_OUTCOME, OUTCOME_FAILURE)
        putBoolean(KEY_RETRYABLE, false)
    }

    private fun isRunningIsolated(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && Process.isIsolated()

    companion object {
        const val PROTOCOL_VERSION = 1
        const val KEY_IS_ISOLATED = "is_isolated"
        const val KEY_OUTCOME = "outcome"
        const val KEY_PAGE_COUNT = "page_count"
        const val KEY_RETRYABLE = "retryable"

        const val OUTCOME_EXTRACTED = "extracted"
        const val OUTCOME_NO_EXTRACTABLE_TEXT = "no_extractable_text"
        const val OUTCOME_PASSWORD_PROTECTED = "password_protected"
        const val OUTCOME_FAILURE = "failure"
    }
}
