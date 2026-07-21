package com.memora.app.data.pdfbox.isolation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Binds only to Memora's private isolated parser service and exposes its Binder when available.
 *
 * This transport adapter deliberately has no PDF descriptor, URI, path, tree reference, source
 * identity, or parser-request API. Source access and parser requests remain separate ADR-017
 * gates. A caller must treat every unavailable state as retryable and must not create a partial
 * extraction record.
 */
internal class AndroidIsolatedPdfParserConnection(
    private val serviceBinder: IsolatedPdfParserServiceBinder,
) : IsolatedPdfParserConnection, AutoCloseable {
    private val connectionReady = CountDownLatch(1)
    private val lock = Any()

    @Volatile
    private var status: IsolatedPdfParserBindingStatus = IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE

    @Volatile
    private var parser: IIsolatedPdfParser? = null

    private var isBound = false
    private var isClosed = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            parser = IIsolatedPdfParser.Stub.asInterface(service)
            status = IsolatedPdfParserBindingStatus.AVAILABLE
            connectionReady.countDown()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            markUnavailable()
        }

        override fun onBindingDied(name: ComponentName) {
            markUnavailable()
        }

        override fun onNullBinding(name: ComponentName) {
            markUnavailable()
        }
    }

    /** Starts one explicit private-service binding attempt without submitting any parser work. */
    fun connect(): IsolatedPdfParserBindingStatus = synchronized(lock) {
        if (isClosed) {
            return@synchronized IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE
        }
        if (isBound) {
            return@synchronized status
        }

        isBound = serviceBinder.bind(serviceConnection)
        status = if (!isBound) {
            connectionReady.countDown()
            IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE
        } else if (parser != null) {
            // A connector may deliver a successful callback before bind() returns.
            IsolatedPdfParserBindingStatus.AVAILABLE
        } else {
            IsolatedPdfParserBindingStatus.CONNECTING
        }
        status
    }

    /** Waits only for service availability, never for PDF parsing or source access. */
    fun awaitAvailability(timeoutMillis: Long): IsolatedPdfParserBindingStatus {
        require(timeoutMillis > 0) { "The isolated parser connection timeout must be positive." }

        if (status == IsolatedPdfParserBindingStatus.AVAILABLE) {
            return status
        }

        return try {
            if (connectionReady.await(timeoutMillis, TimeUnit.MILLISECONDS) &&
                status == IsolatedPdfParserBindingStatus.AVAILABLE
            ) {
                IsolatedPdfParserBindingStatus.AVAILABLE
            } else {
                IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE
        }
    }

    override fun acquire(): IIsolatedPdfParser = parser ?: throw IsolatedPdfParserUnavailableException()

    override fun close() {
        val shouldUnbind = synchronized(lock) {
            if (isClosed) {
                false
            } else {
                isClosed = true
                parser = null
                status = IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE
                val wasBound = isBound
                isBound = false
                wasBound
            }
        }

        if (shouldUnbind) {
            serviceBinder.unbind(serviceConnection)
        }
    }

    private fun markUnavailable() {
        parser = null
        status = IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE
        connectionReady.countDown()
    }
}

/** Content-free availability result for the private isolated parser service. */
internal enum class IsolatedPdfParserBindingStatus {
    CONNECTING,
    AVAILABLE,
    RETRYABLE_UNAVAILABLE,
}

/** The client maps this to a retryable content-free parser result. */
internal class IsolatedPdfParserUnavailableException : IllegalStateException()

/** Android binding is isolated behind this seam for deterministic failure/disconnection tests. */
internal interface IsolatedPdfParserServiceBinder {
    fun bind(connection: ServiceConnection): Boolean

    fun unbind(connection: ServiceConnection)
}

/** Production-only explicit binding for Memora's non-exported parser service. */
internal class ContextIsolatedPdfParserServiceBinder(
    private val context: Context,
) : IsolatedPdfParserServiceBinder {
    override fun bind(connection: ServiceConnection): Boolean = context.bindService(
        Intent(context, IsolatedPdfParserService::class.java),
        connection,
        Context.BIND_AUTO_CREATE,
    )

    override fun unbind(connection: ServiceConnection) {
        context.unbindService(connection)
    }
}
