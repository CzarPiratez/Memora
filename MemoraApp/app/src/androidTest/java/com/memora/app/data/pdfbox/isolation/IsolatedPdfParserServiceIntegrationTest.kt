package com.memora.app.data.pdfbox.isolation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.pdfbox.SyntheticPdfFixtures
import java.io.InputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the synthetic-only transport boundary required by ADR-017.
 *
 * The test creates an in-memory descriptor pipe from repository-owned fixture bytes.
 * It does not use a SAF URI, a real PDF, Room, or a persisted source approval.
 */
@RunWith(AndroidJUnit4::class)
class IsolatedPdfParserServiceIntegrationTest {
    private lateinit var context: Context
    private lateinit var connection: ServiceConnection
    private lateinit var parser: IIsolatedPdfParser
    private var isBound = false

    @Before
    fun bindParserService() {
        context = ApplicationProvider.getApplicationContext()
        val connected = CountDownLatch(1)
        connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                parser = IIsolatedPdfParser.Stub.asInterface(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit
        }

        isBound = context.bindService(
            Intent(context, IsolatedPdfParserService::class.java),
            connection,
            Context.BIND_AUTO_CREATE,
        )

        assertTrue("The private parser service should bind.", isBound)
        assertTrue("The private parser service should connect promptly.", connected.await(10, TimeUnit.SECONDS))
    }

    @After
    fun unbindParserService() {
        if (isBound) {
            context.unbindService(connection)
        }
    }

    @Test
    fun manifest_keeps_the_parser_private_and_isolated() {
        val serviceInfo = context.packageManager.getServiceInfo(
            ComponentName(context, IsolatedPdfParserService::class.java),
            0,
        )

        assertFalse(serviceInfo.exported)
        assertTrue(serviceInfo.flags and ServiceInfo.FLAG_ISOLATED_PROCESS != 0)
    }

    @Test
    fun parses_a_synthetic_descriptor_without_a_source_location_and_closes_it() {
        val result = parseSynthetic(SyntheticPdfFixtures.twoPageSelectable())

        assertTrue("The service must confirm that it runs isolated.", result.getBoolean(IsolatedPdfParserService.KEY_IS_ISOLATED))
        val decoded = decodedResult(result)

        assertEquals(IsolatedPdfParserWireOutcome.EXTRACTED, decoded.outcome)
        assertEquals(2, decoded.pageCount)
        assertEquals(setOf(1, 2), decoded.chunks.map(IsolatedPdfParserPageTextChunk::pageNumber).toSet())
        assertTrue(decoded.chunks.all(IsolatedPdfParserPageTextChunk::isFinalChunk))
    }

    @Test
    fun returns_explicit_no_text_for_an_image_only_synthetic_descriptor() {
        val result = parseSynthetic(SyntheticPdfFixtures.imageOnly())

        val decoded = decodedResult(result)

        assertEquals(IsolatedPdfParserWireOutcome.NO_EXTRACTABLE_TEXT, decoded.outcome)
        assertEquals(1, decoded.pageCount)
        assertTrue(decoded.chunks.isEmpty())
    }

    @Test
    fun returns_password_protected_without_returning_source_content() {
        val result = parseSynthetic(SyntheticPdfFixtures.passwordProtected())

        val decoded = decodedResult(result)

        assertEquals(IsolatedPdfParserWireOutcome.PASSWORD_PROTECTED, decoded.outcome)
        assertEquals(null, decoded.pageCount)
        assertTrue(decoded.chunks.isEmpty())
    }

    @Test
    fun returns_a_retryable_failure_for_a_malformed_descriptor_and_closes_it() {
        val result = parseSynthetic(SyntheticPdfFixtures.malformed())

        val decoded = decodedResult(result)

        assertEquals(IsolatedPdfParserWireOutcome.FAILURE, decoded.outcome)
        assertTrue(decoded.retryable)
        assertTrue(decoded.chunks.isEmpty())
    }

    @Test
    fun rejects_an_unsupported_protocol_without_parsing_the_descriptor() {
        val result = parseSynthetic(
            source = SyntheticPdfFixtures.malformed(),
            protocolVersion = IsolatedPdfParserService.PROTOCOL_VERSION + 1,
        )

        val decoded = decodedResult(result)

        assertEquals(IsolatedPdfParserWireOutcome.FAILURE, decoded.outcome)
        assertFalse(decoded.retryable)
        assertEquals(null, decoded.pageCount)
        assertTrue(decoded.chunks.isEmpty())
    }

    private fun decodedResult(result: Bundle): IsolatedPdfParserWireResult {
        assertTrue("The service must confirm that it runs isolated.", result.getBoolean(IsolatedPdfParserService.KEY_IS_ISOLATED))
        assertEquals(
            setOf(IsolatedPdfParserService.KEY_IS_ISOLATED, IsolatedPdfParserService.KEY_BOUNDED_RESULT),
            result.keySet(),
        )
        val payload = requireNotNull(result.getBundle(IsolatedPdfParserService.KEY_BOUNDED_RESULT))
        return when (
            val validation = IsolatedPdfParserResultBundleCodec.decode(
                payload,
                IsolatedPdfParserSyntheticResultPolicy.limits,
            )
        ) {
            is IsolatedPdfParserWireResultValidation.Valid -> validation.result
            IsolatedPdfParserWireResultValidation.Rejected -> throw AssertionError(
                "The private service must return an exact bounded result envelope.",
            )
        }
    }

    private fun parseSynthetic(
        source: InputStream,
        protocolVersion: Int = IsolatedPdfParserService.PROTOCOL_VERSION,
    ): android.os.Bundle {
        val descriptor = descriptorFor(source)

        return try {
            parser.parse(descriptor.readEnd, protocolVersion)
        } finally {
            descriptor.readEnd.close()
            descriptor.writer.join(10_000)
            assertFalse("The fixture writer should close its pipe end.", descriptor.writer.isAlive)
        }
    }

    private fun descriptorFor(source: InputStream): DescriptorWithWriter {
        val pipe = ParcelFileDescriptor.createPipe()
        val writer = thread(name = "synthetic-pdf-descriptor-writer") {
            ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                source.use { input -> input.copyTo(output) }
            }
        }
        return DescriptorWithWriter(readEnd = pipe[0], writer = writer)
    }

    private data class DescriptorWithWriter(
        val readEnd: ParcelFileDescriptor,
        val writer: Thread,
    )
}
