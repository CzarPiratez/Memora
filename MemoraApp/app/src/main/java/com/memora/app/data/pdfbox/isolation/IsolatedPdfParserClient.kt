package com.memora.app.data.pdfbox.isolation

import android.os.CancellationSignal
import android.os.DeadObjectException
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Private ordinary-process boundary for one already-opened PDF descriptor.
 *
 * Protocol v3 pulls a content-free header and bounded chunks through the session assembler.
 * On a completed session it returns a content-free status summary and, when the wire result
 * was contract-validated, the validated result for a separately governed persistence path.
 * Rejected, cancelled, and transport failures never expose partial page text.
 */
internal class IsolatedPdfParserClient(
    private val connection: IsolatedPdfParserConnection,
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "memora-pdf-parser-client").apply { isDaemon = true }
    },
) : AutoCloseable {

    /**
     * Streams the private parser session with a bounded wait and converts transport failures to
     * an explicit retryable status. The client always closes [source], including bind failure,
     * Binder death, timeout, malformed response, and cancellation paths.
     */
    fun parse(
        source: ParcelFileDescriptor,
        timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
        cancellationSignal: CancellationSignal? = null,
    ): IsolatedPdfParserClientResult {
        require(timeoutMillis > 0) { "The isolated parser timeout must be positive." }

        val requestLock = Any()
        var request: Future<IsolatedPdfParserClientResult>? = null
        var cancelled = false
        var parser: IIsolatedPdfParser? = null

        fun cancelRequest() = synchronized(requestLock) {
            request?.cancel(true)
            try {
                parser?.cancel()
            } catch (_: RemoteException) {
                // Best-effort cooperative cancel after Binder loss.
            }
        }

        return try {
            if (cancellationSignal?.isCanceled == true) {
                return retryableFailure()
            }

            cancellationSignal?.setOnCancelListener {
                synchronized(requestLock) {
                    cancelled = true
                    request?.cancel(true)
                    try {
                        parser?.cancel()
                    } catch (_: RemoteException) {
                        // Best-effort cooperative cancel after Binder loss.
                    }
                }
            }

            val acquired = connection.acquire()
            val submittedRequest = synchronized(requestLock) {
                parser = acquired
                if (cancelled || cancellationSignal?.isCanceled == true) {
                    cancelled = true
                    null
                } else {
                    worker.submit<IsolatedPdfParserClientResult> {
                        runSession(
                            parser = acquired,
                            source = source,
                            cancellationSignal = cancellationSignal,
                        )
                    }.also { request = it }
                }
            }
            if (submittedRequest == null) {
                retryableFailure()
            } else {
                submittedRequest.get(timeoutMillis, TimeUnit.MILLISECONDS).takeUnless {
                    cancellationSignal?.isCanceled == true
                } ?: retryableFailure()
            }
        } catch (_: TimeoutException) {
            cancelRequest()
            retryableFailure()
        } catch (_: CancellationException) {
            cancelRequest()
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

    private fun runSession(
        parser: IIsolatedPdfParser,
        source: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
    ): IsolatedPdfParserClientResult {
        if (cancellationSignal?.isCanceled == true) {
            parser.cancel()
            return retryableFailure()
        }

        val assembler = IsolatedPdfParserSessionAssembler(
            IsolatedPdfParserSyntheticResultPolicy.sessionLimits,
        )
        val beginMessage = IsolatedPdfParserSessionMessageCodec.decode(
            parser.begin(source, IsolatedPdfParserService.PROTOCOL_VERSION),
        ) ?: return retryableFailureAfterCancel(parser)

        val header = when (beginMessage) {
            is IsolatedPdfParserSessionMessage.Header -> beginMessage.header
            else -> return retryableFailureAfterCancel(parser)
        }

        when (val opened = assembler.accept(IsolatedPdfParserSessionEvent.Opened(header))) {
            is IsolatedPdfParserSessionState.Completed -> return opened.result.toClientResult()
            is IsolatedPdfParserSessionState.Collecting -> Unit
            IsolatedPdfParserSessionState.Rejected,
            IsolatedPdfParserSessionState.Cancelled,
            IsolatedPdfParserSessionState.AwaitingOpen,
            -> return retryableFailureAfterCancel(parser)
        }

        while (true) {
            if (cancellationSignal?.isCanceled == true || Thread.currentThread().isInterrupted) {
                parser.cancel()
                assembler.accept(IsolatedPdfParserSessionEvent.Cancelled)
                return retryableFailure()
            }

            val pull = IsolatedPdfParserSessionMessageCodec.decode(parser.nextChunk())
                ?: return retryableFailureAfterCancel(parser)

            val state = when (pull) {
                is IsolatedPdfParserSessionMessage.Chunk -> {
                    assembler.accept(IsolatedPdfParserSessionEvent.ChunkReceived(pull.chunk))
                }
                IsolatedPdfParserSessionMessage.SessionComplete -> {
                    assembler.accept(IsolatedPdfParserSessionEvent.Completed)
                }
                is IsolatedPdfParserSessionMessage.Header -> {
                    return retryableFailureAfterCancel(parser)
                }
            }

            when (state) {
                is IsolatedPdfParserSessionState.Completed -> {
                    return state.result.toClientResult()
                }
                is IsolatedPdfParserSessionState.Collecting -> Unit
                IsolatedPdfParserSessionState.Rejected,
                IsolatedPdfParserSessionState.Cancelled,
                IsolatedPdfParserSessionState.AwaitingOpen,
                -> {
                    parser.cancel()
                    return retryableFailure()
                }
            }
        }
    }

    private fun retryableFailureAfterCancel(parser: IIsolatedPdfParser): IsolatedPdfParserClientResult {
        try {
            parser.cancel()
        } catch (_: RemoteException) {
            // Best-effort cooperative cancel after Binder loss.
        }
        return retryableFailure()
    }

    private fun IsolatedPdfParserWireResult.toClientResult(): IsolatedPdfParserClientResult {
        // Assembler only Completes after IsolatedPdfParserWireResultValidation.Valid.
        val validated = IsolatedPdfParserWireResultValidation.Valid(this)
        return when (outcome) {
            IsolatedPdfParserWireOutcome.EXTRACTED -> pageResult(
                outcome = IsolatedPdfParserClientOutcome.EXTRACTED,
                validated = validated,
            )
            IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT -> pageResult(
                outcome = IsolatedPdfParserClientOutcome.NO_EXTRACTABLE_TEXT,
                validated = validated,
            )
            IsolatedPdfParserWireOutcome.PASSWORD_PROTECTED -> IsolatedPdfParserClientResult(
                outcome = IsolatedPdfParserClientOutcome.PASSWORD_PROTECTED,
                retryable = false,
                validatedResult = validated,
            )
            IsolatedPdfParserWireOutcome.FAILURE -> IsolatedPdfParserClientResult(
                outcome = IsolatedPdfParserClientOutcome.FAILURE,
                retryable = retryable,
                validatedResult = validated,
            )
        }
    }

    private fun IsolatedPdfParserWireResult.pageResult(
        outcome: IsolatedPdfParserClientOutcome,
        validated: IsolatedPdfParserWireResultValidation.Valid,
    ): IsolatedPdfParserClientResult = if (!retryable && pageCount != null && pageCount > 0) {
        IsolatedPdfParserClientResult(
            outcome = outcome,
            retryable = false,
            pageCount = pageCount,
            validatedResult = validated,
        )
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

/**
 * Ordinary-process parser outcome.
 *
 * [outcome]/[retryable], and [pageCount] remain the status surface. [validatedResult] is
 * present only after a completed, contract-validated session so a governed persistence path may
 * map deterministic text without reopening the document. Transport failures leave it null.
 */
internal data class IsolatedPdfParserClientResult(
    val outcome: IsolatedPdfParserClientOutcome,
    val retryable: Boolean,
    val pageCount: Int? = null,
    val validatedResult: IsolatedPdfParserWireResultValidation.Valid? = null,
)

internal enum class IsolatedPdfParserClientOutcome {
    EXTRACTED,
    NO_EXTRACTABLE_TEXT,
    PASSWORD_PROTECTED,
    FAILURE,
}
