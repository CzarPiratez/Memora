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
 * This class deliberately has no URI, path, source identity, text, Room, UI, or source
 * access API. A later platform adapter must validate a user-approved grant before it can
 * supply any real descriptor. Until every ADR-017 release gate is complete, this client is
 * exercised only with repository-owned synthetic descriptors.
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
        if (!getBoolean(IsolatedPdfParserService.KEY_IS_ISOLATED)) {
            return retryableFailure()
        }

        return when (getString(IsolatedPdfParserService.KEY_OUTCOME)) {
            IsolatedPdfParserService.OUTCOME_EXTRACTED -> pageOutcome(
                IsolatedPdfParserClientOutcome.EXTRACTED,
            )

            IsolatedPdfParserService.OUTCOME_NO_EXTRACTABLE_TEXT -> pageOutcome(
                IsolatedPdfParserClientOutcome.NO_EXTRACTABLE_TEXT,
            )

            IsolatedPdfParserService.OUTCOME_PASSWORD_PROTECTED -> IsolatedPdfParserClientResult(
                outcome = IsolatedPdfParserClientOutcome.PASSWORD_PROTECTED,
                retryable = false,
            )

            IsolatedPdfParserService.OUTCOME_FAILURE -> IsolatedPdfParserClientResult(
                outcome = IsolatedPdfParserClientOutcome.FAILURE,
                // A missing retryability field is a malformed response, not a permanent result.
                retryable = if (containsKey(IsolatedPdfParserService.KEY_RETRYABLE)) {
                    getBoolean(IsolatedPdfParserService.KEY_RETRYABLE)
                } else {
                    true
                },
            )

            else -> retryableFailure()
        }
    }

    private fun Bundle.pageOutcome(
        outcome: IsolatedPdfParserClientOutcome,
    ): IsolatedPdfParserClientResult {
        if (!containsKey(IsolatedPdfParserService.KEY_PAGE_COUNT)) {
            return retryableFailure()
        }

        val pageCount = getInt(IsolatedPdfParserService.KEY_PAGE_COUNT)
        return if (pageCount > 0) {
            IsolatedPdfParserClientResult(
                outcome = outcome,
                retryable = false,
                pageCount = pageCount,
            )
        } else {
            retryableFailure()
        }
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
