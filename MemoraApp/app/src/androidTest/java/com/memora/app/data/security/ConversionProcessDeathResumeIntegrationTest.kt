package com.memora.app.data.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.local.AssetEntity
import com.memora.app.data.local.DiscoveryCheckpointEntity
import com.memora.app.data.local.DocumentTreeApprovalEntity
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.local.MemoraDatabaseMigrations
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Crash-resume proofs for production conversion: process death at ROWS_COPIED /
 * SWITCH_PENDING (and mid-finalize file layouts) must preserve rows and complete
 * exactly once on the next open().
 */
@RunWith(AndroidJUnit4::class)
class ConversionProcessDeathResumeIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val journal = DatabaseEncryptionConversionJournal(
        context = context,
        journalFileName = ProductionDatabaseIdentity.JOURNAL_FILE,
    )

    @Before
    fun setUp() {
        ProductionDatabaseTestCleanup.clearAll(context)
    }

    @After
    fun tearDown() {
        ProductionDatabaseTestCleanup.clearAll(context)
    }

    @Test
    fun rows_copied_death_preserves_plaintext_and_completes_on_resume() = runBlocking {
        seedPlaintextFixtures()

        MemoraEncryptedDatabaseOpener.prepareConversionStoppingAfterPhaseForTest(
            context,
            DatabaseEncryptionConversionPhase.ROWS_COPIED,
        )
        assertEquals(DatabaseEncryptionConversionPhase.ROWS_COPIED, journal.read().phase)
        assertTrue(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
        assertTrue(
            context.getDatabasePath(ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME).exists(),
        )

        val database = MemoraEncryptedDatabaseOpener.open(context)
        try {
            assertEquals(1, database.assetDao().count())
            assertEquals(FIXTURE_MARKER, database.assetDao().findAll().single().fingerprint)
            assertEquals(1, database.discoveryCheckpointDao().count())
            assertEquals(1, database.documentTreeApprovalDao().count())
            assertFalse(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
            assertFalse(
                context.getDatabasePath(ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME).exists(),
            )
            assertEquals(DatabaseEncryptionConversionPhase.COMPLETED, journal.read().phase)
        } finally {
            database.close()
        }
    }

    @Test
    fun switch_pending_death_finalizes_exactly_once_on_resume() = runBlocking {
        seedPlaintextFixtures()

        MemoraEncryptedDatabaseOpener.prepareConversionStoppingAfterPhaseForTest(
            context,
            DatabaseEncryptionConversionPhase.SWITCH_PENDING,
        )
        assertEquals(DatabaseEncryptionConversionPhase.SWITCH_PENDING, journal.read().phase)
        assertTrue(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
        assertTrue(
            context.getDatabasePath(ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME).exists(),
        )

        val database = MemoraEncryptedDatabaseOpener.open(context)
        try {
            assertEquals(1, database.assetDao().count())
            assertEquals(FIXTURE_MARKER, database.assetDao().findAll().single().fingerprint)
            assertFalse(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
            assertFalse(
                context.getDatabasePath(ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME).exists(),
            )
            assertFalse(
                context.getDatabasePath(ProductionDatabaseIdentity.PLAINTEXT_RETAINED_NAME).exists(),
            )
            assertEquals(DatabaseEncryptionConversionPhase.COMPLETED, journal.read().phase)
        } finally {
            database.close()
        }
    }

    @Test
    fun switch_pending_after_plaintext_retained_completes_on_resume() = runBlocking {
        seedPlaintextFixtures()
        MemoraEncryptedDatabaseOpener.prepareConversionStoppingAfterPhaseForTest(
            context,
            DatabaseEncryptionConversionPhase.SWITCH_PENDING,
        )
        assertTrue(
            renameDatabaseFiles(
                ProductionDatabaseIdentity.DATABASE_NAME,
                ProductionDatabaseIdentity.PLAINTEXT_RETAINED_NAME,
            ),
        )
        assertTrue(
            context.getDatabasePath(ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME).exists(),
        )
        assertFalse(context.getDatabasePath(ProductionDatabaseIdentity.DATABASE_NAME).exists())
        assertEquals(DatabaseEncryptionConversionPhase.SWITCH_PENDING, journal.read().phase)

        val database = MemoraEncryptedDatabaseOpener.open(context)
        try {
            assertEquals(1, database.assetDao().count())
            assertEquals(FIXTURE_MARKER, database.assetDao().findAll().single().fingerprint)
            assertFalse(
                context.getDatabasePath(ProductionDatabaseIdentity.PLAINTEXT_RETAINED_NAME).exists(),
            )
            assertEquals(DatabaseEncryptionConversionPhase.COMPLETED, journal.read().phase)
        } finally {
            database.close()
        }
    }

    @Test
    fun switch_pending_after_candidate_promoted_completes_and_drops_retained() = runBlocking {
        seedPlaintextFixtures()
        MemoraEncryptedDatabaseOpener.prepareConversionStoppingAfterPhaseForTest(
            context,
            DatabaseEncryptionConversionPhase.SWITCH_PENDING,
        )
        assertTrue(
            renameDatabaseFiles(
                ProductionDatabaseIdentity.DATABASE_NAME,
                ProductionDatabaseIdentity.PLAINTEXT_RETAINED_NAME,
            ),
        )
        assertTrue(
            renameDatabaseFiles(
                ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME,
                ProductionDatabaseIdentity.DATABASE_NAME,
            ),
        )
        assertTrue(
            context.getDatabasePath(ProductionDatabaseIdentity.PLAINTEXT_RETAINED_NAME).exists(),
        )
        assertFalse(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
        assertEquals(DatabaseEncryptionConversionPhase.SWITCH_PENDING, journal.read().phase)

        val database = MemoraEncryptedDatabaseOpener.open(context)
        try {
            assertEquals(1, database.assetDao().count())
            assertEquals(FIXTURE_MARKER, database.assetDao().findAll().single().fingerprint)
            assertFalse(
                context.getDatabasePath(ProductionDatabaseIdentity.PLAINTEXT_RETAINED_NAME).exists(),
            )
            assertEquals(DatabaseEncryptionConversionPhase.COMPLETED, journal.read().phase)
        } finally {
            database.close()
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
                MemoraDatabaseMigrations.MIGRATION_3_4,
                MemoraDatabaseMigrations.MIGRATION_12_13,
            )
            .build()
        try {
            plaintext.openHelper.writableDatabase
            plaintext.assetDao().upsert(
                AssetEntity(
                    sourceId = "death-source",
                    sourceAssetKey = "death-asset-1",
                    assetType = "PDF",
                    location = "content://com.memora.poc/death/1",
                    fingerprint = FIXTURE_MARKER,
                    discoveredAtEpochMillis = 1_700_000_500_000L,
                    displayName = "death-fixture.pdf",
                    sourceModifiedAtEpochMillis = 1_700_000_500_050L,
                    indexingStatus = "DISCOVERED",
                    indexingAttemptCount = 0,
                    failureCode = null,
                    failureMessage = null,
                ),
            )
            plaintext.discoveryCheckpointDao().upsert(
                DiscoveryCheckpointEntity(
                    sourceId = "death-source",
                    cursorValue = "death-cursor-1",
                    savedAtEpochMillis = 1_700_000_500_000L,
                ),
            )
            plaintext.documentTreeApprovalDao().upsert(
                DocumentTreeApprovalEntity(
                    sourceId = "death-source",
                    treeUri = "content://com.memora.poc/tree/death",
                    approvedAtEpochMillis = 1_700_000_500_100L,
                ),
            )
        } finally {
            plaintext.close()
        }
        assertTrue(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
    }

    private fun renameDatabaseFiles(fromName: String, toName: String): Boolean {
        val from = context.getDatabasePath(fromName)
        val to = context.getDatabasePath(toName)
        if (!from.exists()) return false
        to.parentFile?.mkdirs()
        File(to.path).delete()
        File(to.path + "-wal").delete()
        File(to.path + "-shm").delete()
        File(to.path + "-journal").delete()
        if (!from.renameTo(to)) return false
        listOf("-wal", "-shm", "-journal").forEach { suffix ->
            val source = File(from.path + suffix)
            if (source.exists()) {
                val target = File(to.path + suffix)
                target.delete()
                source.renameTo(target)
            }
        }
        return to.exists()
    }

    private fun probeStandardSqlite(databaseName: String): Boolean =
        StandardSqliteDatabaseProbe.canOpenWithoutPassphrase(context.getDatabasePath(databaseName))

    companion object {
        private const val FIXTURE_MARKER = "memora-death-resume-fixture-v1"
    }
}
