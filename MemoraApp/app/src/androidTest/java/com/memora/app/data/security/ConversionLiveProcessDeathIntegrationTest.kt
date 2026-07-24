package com.memora.app.data.security

import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.memora.app.data.local.AssetEntity
import com.memora.app.data.local.DiscoveryCheckpointEntity
import com.memora.app.data.local.DocumentTreeApprovalEntity
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.local.MemoraDatabaseMigrations
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Live crash/kill of the secondary conversion worker process, then cold resume in the
 * instrumentation process via [MemoraEncryptedDatabaseOpener.open].
 */
@RunWith(AndroidJUnit4::class)
class ConversionLiveProcessDeathIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val journal = DatabaseEncryptionConversionJournal(
        context = context,
        journalFileName = ProductionDatabaseIdentity.JOURNAL_FILE,
    )

    @Before
    fun setUp() {
        ProductionDatabaseTestCleanup.clearAll(context)
        ConversionLiveDeathMarker.clear(context)
    }

    @After
    fun tearDown() {
        ProductionDatabaseTestCleanup.clearAll(context)
        ConversionLiveDeathMarker.clear(context)
    }

    @Test
    fun live_crash_at_rows_copied_resumes_with_fixture_rows() = runBlocking {
        seedPlaintextFixtures()
        crashSecondaryWorkerAt(DatabaseEncryptionConversionPhase.ROWS_COPIED)
        assertCompletedEncryptedWithFixtureRows()
    }

    @Test
    fun live_crash_at_switch_pending_resumes_with_fixture_rows() = runBlocking {
        seedPlaintextFixtures()
        crashSecondaryWorkerAt(DatabaseEncryptionConversionPhase.SWITCH_PENDING)
        assertCompletedEncryptedWithFixtureRows()
    }

    private fun crashSecondaryWorkerAt(phase: DatabaseEncryptionConversionPhase) {
        context.startService(ConversionLiveDeathWorkerService.intent(context, phase))
        val armed = awaitArmed(phase)
        assertNotEquals(
            "The conversion worker must run in a secondary process.",
            Process.myPid(),
            armed.pid,
        )
        assertEquals(phase, journal.read().phase)
        induceCrashFor(armed.pid)
        awaitProcessGone(armed.pid)
        assertEquals(
            "Journal phase must survive the live crash for resume.",
            phase,
            journal.read().phase,
        )
    }

    private suspend fun assertCompletedEncryptedWithFixtureRows() {
        val database = MemoraEncryptedDatabaseOpener.open(context)
        try {
            assertEquals(1, database.assetDao().count())
            assertEquals(FIXTURE_MARKER, database.assetDao().findAll().single().fingerprint)
            assertEquals(1, database.discoveryCheckpointDao().count())
            assertEquals(1, database.documentTreeApprovalDao().count())
            assertFalse(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
            assertEquals(
                DatabaseEncryptionConversionPhase.COMPLETED,
                journal.read().phase,
            )
        } finally {
            database.close()
        }
    }

    private fun awaitArmed(expectedPhase: DatabaseEncryptionConversionPhase): ConversionLiveDeathMarker.Armed {
        val deadline = SystemClock.elapsedRealtime() + ARM_TIMEOUT_MILLIS
        while (SystemClock.elapsedRealtime() < deadline) {
            ConversionLiveDeathMarker.readFailed(context)?.let { message ->
                throw AssertionError("Conversion live-death worker failed before arming: $message")
            }
            val armed = ConversionLiveDeathMarker.readArmed(context)
            if (armed != null) {
                assertEquals(expectedPhase, armed.phase)
                return armed
            }
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        throw AssertionError("Timed out waiting for conversion live-death worker to arm.")
    }

    private fun awaitProcessGone(pid: Int) {
        val deadline = SystemClock.elapsedRealtime() + PROCESS_GONE_TIMEOUT_MILLIS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (!processExists(pid)) {
                Thread.sleep(SETTLE_AFTER_DEATH_MILLIS)
                return
            }
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        throw AssertionError(
            "Timed out waiting for conversion worker pid $pid to die after live crash/kill. " +
                "proc_check=${shellOutput("sh -c 'ls -ld /proc/$pid 2>&1 || true'")}",
        )
    }

    private fun processExists(pid: Int): Boolean {
        val output = shellOutput("sh -c 'if [ -d /proc/$pid ]; then echo YES; else echo NO; fi'")
        return output.contains("YES")
    }

    private fun induceCrashFor(processId: Int) {
        shellOutput("am crash $processId")
        shellOutput("kill -9 $processId || true")
    }

    private fun shellOutput(command: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use {
            descriptor ->
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { reader ->
                reader.readText()
            }
        }

    private suspend fun seedPlaintextFixtures() {
        val plaintext = Room.databaseBuilder(
            context,
            MemoraDatabase::class.java,
            ProductionDatabaseIdentity.DATABASE_NAME,
        )
            .addMigrations(
                MemoraDatabaseMigrations.MIGRATION_1_2,
                MemoraDatabaseMigrations.MIGRATION_2_3,
            )
            .build()
        try {
            plaintext.openHelper.writableDatabase
            plaintext.assetDao().upsert(
                AssetEntity(
                    sourceId = "live-death-source",
                    sourceAssetKey = "live-death-asset-1",
                    assetType = "PDF",
                    location = "content://com.memora.poc/live-death/1",
                    fingerprint = FIXTURE_MARKER,
                    discoveredAtEpochMillis = 1_700_000_600_000L,
                    displayName = "live-death-fixture.pdf",
                    sourceModifiedAtEpochMillis = 1_700_000_600_050L,
                    indexingStatus = "DISCOVERED",
                    indexingAttemptCount = 0,
                    failureCode = null,
                    failureMessage = null,
                ),
            )
            plaintext.discoveryCheckpointDao().upsert(
                DiscoveryCheckpointEntity(
                    sourceId = "live-death-source",
                    cursorValue = "live-death-cursor-1",
                    savedAtEpochMillis = 1_700_000_600_000L,
                ),
            )
            plaintext.documentTreeApprovalDao().upsert(
                DocumentTreeApprovalEntity(
                    sourceId = "live-death-source",
                    treeUri = "content://com.memora.poc/tree/live-death",
                    approvedAtEpochMillis = 1_700_000_600_100L,
                ),
            )
        } finally {
            plaintext.close()
        }
        assertTrue(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
    }

    private fun probeStandardSqlite(databaseName: String): Boolean =
        StandardSqliteDatabaseProbe.canOpenWithoutPassphrase(context.getDatabasePath(databaseName))

    companion object {
        private const val FIXTURE_MARKER = "memora-live-death-fixture-v1"
        private const val ARM_TIMEOUT_MILLIS = 60_000L
        private const val PROCESS_GONE_TIMEOUT_MILLIS = 20_000L
        private const val SETTLE_AFTER_DEATH_MILLIS = 500L
        private const val POLL_INTERVAL_MILLIS = 100L
    }
}
