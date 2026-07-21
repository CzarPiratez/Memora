package com.memora.app.data.pdfbox.isolation

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.pdfbox.SyntheticPdfFixtures
import java.io.InputStream
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises one complete private Binder/client round trip using only a repository-owned fixture.
 *
 * The test deliberately supplies a pipe descriptor rather than a SAF URI or source location. It
 * neither accesses a user PDF nor writes Room data, and the client result exposes status only.
 */
@RunWith(AndroidJUnit4::class)
class IsolatedPdfParserEndToEndIntegrationTest {
    private lateinit var connection: AndroidIsolatedPdfParserConnection
    private lateinit var client: IsolatedPdfParserClient

    @Before
    fun connectPrivateParserService() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        connection = AndroidIsolatedPdfParserConnection(
            ContextIsolatedPdfParserServiceBinder(context),
        )
        client = IsolatedPdfParserClient(connection)

        assertEquals(IsolatedPdfParserBindingStatus.CONNECTING, connection.connect())
        assertEquals(IsolatedPdfParserBindingStatus.AVAILABLE, connection.awaitAvailability(10_000))
    }

    @After
    fun closeTransport() {
        client.close()
        connection.close()
    }

    @Test
    fun parses_a_repository_owned_descriptor_through_the_private_service_and_closes_it() {
        val descriptor = descriptorFor(SyntheticPdfFixtures.twoPageSelectable())

        val result = client.parse(descriptor.readEnd)

        assertEquals(IsolatedPdfParserClientOutcome.EXTRACTED, result.outcome)
        assertFalse(result.retryable)
        assertEquals(2, result.pageCount)
        assertFalse(
            "The ordinary-process client must close its supplied descriptor.",
            descriptor.readEnd.fileDescriptor.valid(),
        )
        descriptor.writer.join(10_000)
        assertFalse("The fixture writer should finish promptly.", descriptor.writer.isAlive)
        assertTrue(
            "The repository-owned fixture pipe must not fail while the service reads it.",
            descriptor.writerFailure.get() == null,
        )
    }

    private fun descriptorFor(source: InputStream): DescriptorWithWriter {
        val pipe = ParcelFileDescriptor.createPipe()
        val writerFailure = AtomicReference<Throwable?>(null)
        val writer = thread(name = "synthetic-pdf-end-to-end-writer") {
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                    source.use { input -> input.copyTo(output) }
                }
            } catch (failure: Throwable) {
                writerFailure.set(failure)
            }
        }
        return DescriptorWithWriter(
            readEnd = pipe[0],
            writer = writer,
            writerFailure = writerFailure,
        )
    }

    private data class DescriptorWithWriter(
        val readEnd: ParcelFileDescriptor,
        val writer: Thread,
        val writerFailure: AtomicReference<Throwable?>,
    )
}
