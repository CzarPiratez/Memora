package com.memora.app.data.pdfbox.isolation

import android.os.Bundle
import android.os.CancellationSignal
import android.os.DeadObjectException
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the ordinary-process failure contract with synthetic descriptors only.
 *
 * No case binds Android's service, uses a SAF URI, opens a real PDF, accesses Room, or returns
 * source text. Binder death is simulated by a private test Binder so recovery behavior is
 * deterministic; a later live-process death test remains an ADR-017 release gate.
 */
@RunWith(AndroidJUnit4::class)
class IsolatedPdfParserClientIntegrationTest {
    private val clients = mutableListOf<IsolatedPdfParserClient>()

    @After
    fun closeClients() {
        clients.forEach(IsolatedPdfParserClient::close)
    }

    @Test
    fun maps_a_valid_isolated_status_without_returning_content_and_closes_the_descriptor() {
        val descriptor = descriptor()
        val result = clientWith { _, _ -> extractedBundle(pageCount = 2) }.parse(descriptor)

        assertEquals(IsolatedPdfParserClientOutcome.EXTRACTED, result.outcome)
        assertFalse(result.retryable)
        assertEquals(2, result.pageCount)
        assertDescriptorClosed(descriptor)
    }

    @Test
    fun maps_bind_failure_to_a_retryable_status_and_closes_the_descriptor() {
        val descriptor = descriptor()
        val client = newClient(IsolatedPdfParserConnection {
            throw IllegalStateException("synthetic bind failure")
        })

        val result = client.parse(descriptor)

        assertRetryableFailure(result)
        assertDescriptorClosed(descriptor)
    }

    @Test
    fun maps_binder_death_to_a_retryable_status_and_closes_the_descriptor() {
        val descriptor = descriptor()
        val result = clientWith { _, _ -> throw DeadObjectException() }.parse(descriptor)

        assertRetryableFailure(result)
        assertDescriptorClosed(descriptor)
    }

    @Test
    fun maps_a_bounded_timeout_to_a_retryable_status_and_closes_the_descriptor() {
        val descriptor = descriptor()
        val parserStarted = CountDownLatch(1)
        val neverCompletes = CountDownLatch(1)
        val client = clientWith { _, _ ->
            parserStarted.countDown()
            neverCompletes.await()
            extractedBundle(pageCount = 1)
        }

        val result = client.parse(source = descriptor, timeoutMillis = 100)

        assertTrue("The synthetic parser call should have started.", parserStarted.await(1, TimeUnit.SECONDS))
        assertRetryableFailure(result)
        assertDescriptorClosed(descriptor)
    }

    @Test
    fun rejects_an_unknown_isolated_outcome_as_retryable_and_closes_the_descriptor() {
        val descriptor = descriptor()
        val result = clientWith { _, _ ->
            Bundle().apply {
                putBoolean(IsolatedPdfParserService.KEY_IS_ISOLATED, true)
                putBundle(
                    IsolatedPdfParserService.KEY_BOUNDED_RESULT,
                    Bundle().apply {
                        putInt(IsolatedPdfParserResultBundleCodec.KEY_SCHEMA_VERSION, 1)
                        putString(IsolatedPdfParserResultBundleCodec.KEY_OUTCOME, "unknown_outcome")
                        putBoolean(IsolatedPdfParserResultBundleCodec.KEY_RETRYABLE, false)
                        putParcelableArrayList(
                            IsolatedPdfParserResultBundleCodec.KEY_CHUNKS,
                            arrayListOf(),
                        )
                    },
                )
            }
        }.parse(descriptor)

        assertRetryableFailure(result)
        assertDescriptorClosed(descriptor)
    }

    @Test
    fun rejects_a_non_isolated_response_as_retryable_and_closes_the_descriptor() {
        val descriptor = descriptor()
        val result = clientWith { _, _ ->
            extractedBundle(pageCount = 2).apply {
                putBoolean(IsolatedPdfParserService.KEY_IS_ISOLATED, false)
            }
        }.parse(descriptor)

        assertRetryableFailure(result)
        assertDescriptorClosed(descriptor)
    }

    @Test
    fun rejects_a_page_outcome_without_a_page_count_and_closes_the_descriptor() {
        val descriptor = descriptor()
        val result = clientWith { _, _ ->
            Bundle().apply {
                putBoolean(IsolatedPdfParserService.KEY_IS_ISOLATED, true)
                putBundle(
                    IsolatedPdfParserService.KEY_BOUNDED_RESULT,
                    Bundle().apply {
                        putInt(IsolatedPdfParserResultBundleCodec.KEY_SCHEMA_VERSION, 1)
                        putString(IsolatedPdfParserResultBundleCodec.KEY_OUTCOME, "extracted")
                        putBoolean(IsolatedPdfParserResultBundleCodec.KEY_RETRYABLE, false)
                        putParcelableArrayList(
                            IsolatedPdfParserResultBundleCodec.KEY_CHUNKS,
                            arrayListOf(),
                        )
                    },
                )
            }
        }.parse(descriptor)

        assertRetryableFailure(result)
        assertDescriptorClosed(descriptor)
    }

    @Test
    fun rejects_an_invalid_page_count_and_closes_the_descriptor() {
        val descriptor = descriptor()
        val result = clientWith { _, _ ->
            Bundle().apply {
                putBoolean(IsolatedPdfParserService.KEY_IS_ISOLATED, true)
                putBundle(
                    IsolatedPdfParserService.KEY_BOUNDED_RESULT,
                    Bundle().apply {
                        putInt(IsolatedPdfParserResultBundleCodec.KEY_SCHEMA_VERSION, 1)
                        putString(IsolatedPdfParserResultBundleCodec.KEY_OUTCOME, "extracted")
                        putBoolean(IsolatedPdfParserResultBundleCodec.KEY_RETRYABLE, false)
                        putInt(IsolatedPdfParserResultBundleCodec.KEY_PAGE_COUNT, 0)
                        putParcelableArrayList(
                            IsolatedPdfParserResultBundleCodec.KEY_CHUNKS,
                            arrayListOf(),
                        )
                    },
                )
            }
        }.parse(descriptor)

        assertRetryableFailure(result)
        assertDescriptorClosed(descriptor)
    }

    @Test
    fun does_not_submit_an_already_cancelled_request_and_closes_the_descriptor() {
        val descriptor = descriptor()
        val cancellationSignal = CancellationSignal().also(CancellationSignal::cancel)
        var parserCalled = false
        val result = clientWith { _, _ ->
            parserCalled = true
            extractedBundle(pageCount = 1)
        }.parse(source = descriptor, cancellationSignal = cancellationSignal)

        assertRetryableFailure(result)
        assertFalse("An already cancelled request must not reach the parser.", parserCalled)
        assertDescriptorClosed(descriptor)
    }

    @Test
    fun cancels_a_waiting_request_as_retryable_and_closes_the_descriptor() {
        val descriptor = descriptor()
        val cancellationSignal = CancellationSignal()
        val parserStarted = CountDownLatch(1)
        val client = clientWith { _, _ ->
            parserStarted.countDown()
            CountDownLatch(1).await()
            extractedBundle(pageCount = 1)
        }
        val caller = Executors.newSingleThreadExecutor()

        try {
            val pendingResult = caller.submit<IsolatedPdfParserClientResult> {
                client.parse(
                    source = descriptor,
                    timeoutMillis = 5_000,
                    cancellationSignal = cancellationSignal,
                )
            }

            assertTrue("The synthetic parser request should have started.", parserStarted.await(1, TimeUnit.SECONDS))
            cancellationSignal.cancel()

            assertRetryableFailure(pendingResult.get(1, TimeUnit.SECONDS))
            assertDescriptorClosed(descriptor)
        } finally {
            caller.shutdownNow()
        }
    }

    private fun clientWith(
        response: (ParcelFileDescriptor, Int) -> Bundle,
    ): IsolatedPdfParserClient = newClient(
        IsolatedPdfParserConnection {
            object : IIsolatedPdfParser.Stub() {
                override fun parse(source: ParcelFileDescriptor, protocolVersion: Int): Bundle =
                    response(source, protocolVersion)
            }
        },
    )

    private fun newClient(connection: IsolatedPdfParserConnection): IsolatedPdfParserClient =
        IsolatedPdfParserClient(connection).also(clients::add)

    private fun descriptor(): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        pipe[1].close()
        return pipe[0]
    }

    private fun extractedBundle(pageCount: Int): Bundle = Bundle().apply {
        putBoolean(IsolatedPdfParserService.KEY_IS_ISOLATED, true)
        putBundle(
            IsolatedPdfParserService.KEY_BOUNDED_RESULT,
            requireNotNull(
                IsolatedPdfParserResultBundleCodec.encode(
                    IsolatedPdfParserWireResult(
                        schemaVersion = IsolatedPdfParserResultContract.SUPPORTED_SCHEMA_VERSION,
                        outcome = IsolatedPdfParserWireOutcome.EXTRACTED,
                        retryable = false,
                        pageCount = pageCount,
                        chunks = (1..pageCount.coerceAtLeast(0)).map { pageNumber ->
                            IsolatedPdfParserPageTextChunk(pageNumber, 0, true, "fixture")
                        },
                    ),
                    IsolatedPdfParserSyntheticResultPolicy.limits,
                ),
            ),
        )
    }

    private fun assertRetryableFailure(result: IsolatedPdfParserClientResult) {
        assertEquals(IsolatedPdfParserClientOutcome.FAILURE, result.outcome)
        assertTrue(result.retryable)
        assertEquals(null, result.pageCount)
    }

    private fun assertDescriptorClosed(descriptor: ParcelFileDescriptor) {
        assertFalse("The ordinary-process client must close every supplied descriptor.", descriptor.fileDescriptor.valid())
    }
}
