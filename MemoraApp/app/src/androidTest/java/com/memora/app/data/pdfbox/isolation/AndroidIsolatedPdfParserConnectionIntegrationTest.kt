package com.memora.app.data.pdfbox.isolation

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies service availability only. It submits no parser request and opens no descriptor,
 * SAF URI, real PDF, Room database, or source approval.
 */
@RunWith(AndroidJUnit4::class)
class AndroidIsolatedPdfParserConnectionIntegrationTest {
    private val connections = mutableListOf<AndroidIsolatedPdfParserConnection>()

    @After
    fun closeConnections() {
        connections.forEach(AndroidIsolatedPdfParserConnection::close)
    }

    @Test
    fun binds_to_the_private_isolated_service_without_submitting_parser_work() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val connection = newConnection(ContextIsolatedPdfParserServiceBinder(context))

        assertEquals(IsolatedPdfParserBindingStatus.CONNECTING, connection.connect())
        assertEquals(IsolatedPdfParserBindingStatus.AVAILABLE, connection.awaitAvailability(10_000))
        assertNotNull(connection.acquire())
    }

    @Test
    fun reports_a_bind_failure_as_retryable_without_submitting_parser_work() {
        val binder = RecordingServiceBinder(bindResult = false)
        val connection = newConnection(binder)

        assertEquals(IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE, connection.connect())
        assertEquals(IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE, connection.awaitAvailability(100))
        assertThrows(IsolatedPdfParserUnavailableException::class.java) { connection.acquire() }
        assertFalse(binder.unbound)
    }

    @Test
    fun reports_an_explicit_disconnection_as_retryable_without_submitting_parser_work() {
        val binder = RecordingServiceBinder(bindResult = true)
        val connection = newConnection(binder)
        val parser = RecordingParser()

        assertEquals(IsolatedPdfParserBindingStatus.CONNECTING, connection.connect())
        binder.connect(parser)
        assertEquals(IsolatedPdfParserBindingStatus.AVAILABLE, connection.awaitAvailability(100))

        binder.disconnect()

        assertThrows(IsolatedPdfParserUnavailableException::class.java) { connection.acquire() }
        assertEquals(IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE, connection.awaitAvailability(100))
        assertEquals(0, parser.requestCount.get())
    }

    private fun newConnection(
        binder: IsolatedPdfParserServiceBinder,
    ): AndroidIsolatedPdfParserConnection = AndroidIsolatedPdfParserConnection(binder).also(connections::add)

    private class RecordingParser : IIsolatedPdfParser.Stub() {
        val requestCount = AtomicInteger(0)

        override fun begin(source: ParcelFileDescriptor, protocolVersion: Int): Bundle {
            requestCount.incrementAndGet()
            throw AssertionError("A binding-availability test must not submit parser work.")
        }

        override fun nextChunk(): Bundle {
            requestCount.incrementAndGet()
            throw AssertionError("A binding-availability test must not submit parser work.")
        }

        override fun cancel() {
            requestCount.incrementAndGet()
            throw AssertionError("A binding-availability test must not submit parser work.")
        }
    }

    private class RecordingServiceBinder(
        private val bindResult: Boolean,
    ) : IsolatedPdfParserServiceBinder {
        private lateinit var connection: ServiceConnection
        var unbound = false
            private set

        override fun bind(connection: ServiceConnection): Boolean {
            this.connection = connection
            return bindResult
        }

        override fun unbind(connection: ServiceConnection) {
            unbound = true
        }

        fun connect(parser: IIsolatedPdfParser) {
            connection.onServiceConnected(
                ComponentName("com.memora.app", "IsolatedPdfParserService"),
                parser.asBinder(),
            )
        }

        fun disconnect() {
            connection.onServiceDisconnected(
                ComponentName("com.memora.app", "IsolatedPdfParserService"),
            )
        }
    }
}
