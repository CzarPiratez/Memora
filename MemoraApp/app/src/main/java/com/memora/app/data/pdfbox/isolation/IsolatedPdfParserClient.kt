package com.memora.app.data.pdfbox.isolation

import android.os.Bundle
import android.os.CancellationSignal
import android.os.DeadObjectException
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Private ordinary-process boundary for one already-opened synthetic PDF descriptor.
 *
 * This class deliberately has no URI, path, source identity, Room, UI, or source-access API.
 * It validates the private service's bounded result envelope, then discards all page text and
 * returns a content-free status summary. A later, separately governed persistence boundary may
 * receive validated deterministic text; until then this client is synthetic-fixture-only.
 */
internal class IsolatedPdfParserClient(
    private val connection: IsolatedPdfParserConnection,
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "memora-pdf-parser-client").apply { isDaemon = true }
    },
) : AutoCloseable {

    /**
     * Calls the private parser with a bounded wait and converts transport failures to an
     * explicit retryable status. The client always closes [source], including bind failure,
     * Binder death, timeout, malformed response, and cancellation paths.
     */
    fun parse(
        source: ParcelFileDescriptor,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        cancellationSignal: CancellationSignal? = null,
    ): IsolatedPdfParserClientResult {
        require(timeoutMillis > 0) { "The isolated parser timeout must be positive." }

        val requestLock = Any()
        var request: Future<Bundle>? = null
        var cancelled = false

        fun cancelRequest() = synchronized(requestLock) {
            request?.cancel(true)
        }

        return try {
            if (cancellationSignal?.isCanceled == true) {
                return retryableFailure()
            }

            cancellationSignal?.setOnCancelListener {
                synchronized(requestLock) {
                    cancelled = true
                    request?.cancel(true)
                }
            }

            val parser = connection.acquire()
            val submittedRequest = synchronized(requestLock) {
                if (cancelled || cancellationSignal?.isCanceled == true) {
                    cancelled = true
                    null
                } else {
                    worker.submit<Bundle> {
                        parser.parse(source, IsolatedPdfParserService.PROTOCOL_VERSION)
                    }.also { request = it }
                }
            }
            if (submittedRequest == null) {
                retryableFailure()
            } else {
                submittedRequest.get(timeoutMillis, TimeUnit.MILLISECONDS).toClientResult().takeUnless {
                    cancellationSignal?.isCanceled == true
                } ?: retryableFailure()
            }
        } catch (_: TimeoutException) {
            cancelRequest()
            retryableFailure()
        } catch (_: CancellationException) {
            retryableFailure()
        } catch (_: DeadObjectException) {
            retryableFailure()
        } catch (_: RemoteException) {
            retryableFailure()
        } catch (_: ExecutionException) {
            // Binder death and remote exceptions raised on the worker arrive wrapped here.
            retryableFailure()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            cancelRequest()
            retryableFailure()
        } catch (_: RuntimeException) {
            // Binding and malformed-Bundle failures must never expose source details.
            retryableFailure()
        } finally {
            cancellationSignal?.setOnCancelListener(null)
            source.closeQuietly()
        }
    }

    override fun close() {
        worker.shutdownNow()
    }

    private fun Bundle.toClientResult(): IsolatedPdfParserClientResult {
        if (
            !getBoolean(IsolatedPdfParserService.KEY_IS_ISOLATED) ||
            keySet() != setOf(
                IsolatedPdfParserService.KEY_IS_ISOLATED,
                IsolatedPdfParserService.KEY_BOUNDED_RESULT,
            )
        ) {
            return retryableFailure()
        }
        val boundedResult = getBundle(IsolatedPdfParserService.KEY_BOUNDED_RESULT)
            ?: return retryableFailure()
        return when (
            val validation = IsolatedPdfParserResultBundleCodec.decode(
                boundedResult,
                IsolatedPdfParserSyntheticResultPolicy.limits,
            )
        ) {
            is IsolatedPdfParserWireResultValidation.Valid -> validation.result.toClientSummary()
            IsolatedPdfParserWireResultValidation.Rejected -> retryableFailure()
        }
    }

    private fun IsolatedPdfParserWireResult.toClientSummary(): IsolatedPdfParserClientResult = when (outcome) {
        IsolatedPdfParserWireOutcome.EXTRACTED -> pageSummary(IsolatedPdfParserClientOutcome.EXTRACTED)
        IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT -> {
            pageSummary(IsolatedPdfParserClientOutcome.NO_EXTRACTABLE_TEXT)
        }
        IsolatedPdfParserWireOutcome.PASSWORD_PROTECTED -> IsolatedPdfParserClientResult(
            outcome = IsolatedPdfParserClientOutcome.PASSWORD_PROTECTED,
            retryable = false,
        )
        IsolatedPdfParserWireOutcome.FAILURE -> IsolatedPdfParserClientResult(
            outcome = IsolatedPdfParserClientOutcome.FAILURE,
            retryable = retryable,
        )
    }

    private fun IsolatedPdfParserWireResult.pageSummary(
        outcome: IsolatedPdfParserClientOutcome,
    ): IsolatedPdfParserClientResult = if (!retryable && pageCount != null && pageCount > 0) {
        IsolatedPdfParserClientResult(outcome = outcome, retryable = false, pageCount = pageCount)
    } else {
        retryableFailure()
    }

    private fun retryableFailure(): IsolatedPdfParserClientResult = IsolatedPdfParserClientResult(
        outcome = IsolatedPdfParserClientOutcome.FAILURE,
        retryable = true,
    )

    private fun ParcelFileDescriptor.closeQuietly() {
        try {
            close()
        } catch (_: IOException) {
            // The service may already have consumed and closed the same descriptor.
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 15_000L
    }
}

/**
 * The future Android binding adapter owns service lifecycle and reconnect policy. It exposes
 * only the private Binder; it never receives a source identity or source-access capability.
 */
internal fun interface IsolatedPdfParserConnection {
    fun acquire(): IIsolatedPdfParser
}

/** A bounded, content-free parser status for the ordinary app process. */
internal data class IsolatedPdfParserClientResult(
    val outcome: IsolatedPdfParserClientOutcome,
    val retryable: Boolean,
    val pageCount: Int? = null,
)

internal enum class IsolatedPdfParserClientOutcome {
    EXTRACTED,
    NO_EXTRACTABLE_TEXT,
    PASSWORD_PROTECTED,
    FAILURE,
}
