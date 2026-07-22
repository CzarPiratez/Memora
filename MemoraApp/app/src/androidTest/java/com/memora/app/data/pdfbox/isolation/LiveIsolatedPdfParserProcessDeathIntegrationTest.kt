package com.memora.app.data.pdfbox.isolation

import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Induces death only in the uniquely identified live isolated parser process.
 *
 * It creates no file and supplies no user data: the held-open pipe keeps a synthetic request
 * in flight while the ordinary process observes real Binder/process-death recovery.
 */
@RunWith(AndroidJUnit4::class)
class LiveIsolatedPdfParserProcessDeathIntegrationTest {
    private lateinit var context: Context
    private lateinit var connection: AndroidIsolatedPdfParserConnection
    private lateinit var client: IsolatedPdfParserClient
    private lateinit var caller: ExecutorService
    private var pipe: HeldOpenPipe? = null

    @Before
    fun bindPrivateIsolatedParser() {
        context = ApplicationProvider.getApplicationContext()
        connection = AndroidIsolatedPdfParserConnection(
            ContextIsolatedPdfParserServiceBinder(context),
        )
        client = IsolatedPdfParserClient(connection)
        caller = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "memora-live-death-test-caller")
        }

        assertEquals(IsolatedPdfParserBindingStatus.CONNECTING, connection.connect())
        assertEquals(IsolatedPdfParserBindingStatus.AVAILABLE, connection.awaitAvailability(BIND_TIMEOUT_MILLIS))
    }

    @After
    fun closeTestResources() {
        pipe?.close()
        caller.shutdownNow()
        client.close()
        connection.close()
    }

    @Test
    fun live_isolated_process_death_becomes_a_retryable_content_free_failure() {
        val heldOpenPipe = HeldOpenPipe.create().also { pipe = it }
        val pendingResult = caller.submit<IsolatedPdfParserClientResult> {
            client.parse(heldOpenPipe.readEnd, timeoutMillis = REQUEST_TIMEOUT_MILLIS)
        }

        assertRequestIsStillInFlight(pendingResult)
        val isolatedProcessId = uniquelyIdentifiedIsolatedProcessId()
        induceCrashFor(isolatedProcessId)

        val result = pendingResult.get(RESULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        assertEquals(IsolatedPdfParserClientOutcome.FAILURE, result.outcome)
        assertTrue("A live parser-process death must be retryable.", result.retryable)
        assertEquals(null, result.pageCount)
        assertFalse(
            "The ordinary-process client must close its descriptor after parser-process death.",
            heldOpenPipe.readEnd.fileDescriptor.valid(),
        )
        assertEquals(
            IsolatedPdfParserBindingStatus.RETRYABLE_UNAVAILABLE,
            connection.awaitAvailability(AVAILABILITY_TIMEOUT_MILLIS),
        )
    }

    private fun assertRequestIsStillInFlight(pendingResult: Future<IsolatedPdfParserClientResult>) {
        val deadline = SystemClock.elapsedRealtime() + IN_FLIGHT_OBSERVATION_MILLIS
        while (!pendingResult.isDone && SystemClock.elapsedRealtime() < deadline) {
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        assertFalse(
            "The synthetic request must still be in flight before inducing isolated-process death.",
            pendingResult.isDone,
        )
    }

    private fun uniquelyIdentifiedIsolatedProcessId(): Int {
        val processes = shellOutput("ps -A -o PID,UID,NAME,ARGS")
            .lineSequence()
            .mapNotNull(::parseProcess)
            .toList()
        val ordinaryProcess = processes.firstOrNull { it.pid == Process.myPid() }
            ?: throw AssertionError("The ordinary Memora test process was absent from Android's process list.")
        val candidates = processes.filter { process ->
            process.pid != ordinaryProcess.pid &&
                process.uid != ordinaryProcess.uid &&
                (process.name == context.packageName ||
                    process.arguments == context.packageName ||
                    process.arguments.startsWith("${context.packageName}:"))
        }

        assertEquals(
            "Refusing to induce a crash because Android did not expose exactly one isolated " +
                "Memora process. No process was harmed.",
            1,
            candidates.size,
        )
        return candidates.single().pid
    }

    private fun induceCrashFor(processId: Int) {
        shellOutput("am crash $processId")
    }

    private fun shellOutput(command: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use {
            descriptor ->
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { reader ->
                reader.readText()
            }
        }

    private fun parseProcess(line: String): AndroidProcess? {
        val columns = line.trim().split(Regex("\\s+"), limit = 4)
        val processId = columns.firstOrNull()?.toIntOrNull() ?: return null
        if (columns.size < 3) {
            return null
        }
        return AndroidProcess(
            pid = processId,
            uid = columns[1],
            name = columns[2],
            arguments = columns.getOrElse(3) { "" },
        )
    }

    private data class AndroidProcess(
        val pid: Int,
        val uid: String,
        val name: String,
        val arguments: String,
    )

    private class HeldOpenPipe private constructor(
        val readEnd: ParcelFileDescriptor,
        private val writer: OutputStream,
    ) : AutoCloseable {
        override fun close() {
            writer.close()
        }

        companion object {
            fun create(): HeldOpenPipe {
                val pipe = ParcelFileDescriptor.createPipe()
                val writer = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
                writer.write(SYNTHETIC_INCOMPLETE_PDF_PREFIX)
                writer.flush()
                return HeldOpenPipe(readEnd = pipe[0], writer = writer)
            }
        }
    }

    private companion object {
        const val BIND_TIMEOUT_MILLIS = 10_000L
        const val REQUEST_TIMEOUT_MILLIS = 15_000L
        const val RESULT_TIMEOUT_MILLIS = 10_000L
        const val AVAILABILITY_TIMEOUT_MILLIS = 1_000L
        const val IN_FLIGHT_OBSERVATION_MILLIS = 400L
        const val POLL_INTERVAL_MILLIS = 25L
        val SYNTHETIC_INCOMPLETE_PDF_PREFIX = "%PDF-1.7\\n1 0 obj\\n<<>>\\nendobj\\n".toByteArray()
    }
}
