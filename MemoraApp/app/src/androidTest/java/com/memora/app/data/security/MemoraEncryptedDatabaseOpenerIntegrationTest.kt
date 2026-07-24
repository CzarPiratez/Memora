package com.memora.app.data.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.di.PersistenceModule
import com.memora.app.data.local.AssetEntity
import com.memora.app.data.local.DiscoveryCheckpointEntity
import com.memora.app.data.local.DocumentTreeApprovalEntity
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.local.MemoraDatabaseMigrations
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Live production opener proofs against disposable memora.db identity files.
 * TearDown clears production DB/wrapper/journal so the installed app is not stranded.
 */
@RunWith(AndroidJUnit4::class)
class MemoraEncryptedDatabaseOpenerIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        ProductionDatabaseTestCleanup.clearAll(context)
    }

    @After
    fun tearDown() {
        ProductionDatabaseTestCleanup.clearAll(context)
    }

    @Test
    fun persistence_module_targets_production_database_name() {
        assertEquals(
            ProductionDatabaseIdentity.DATABASE_NAME,
            PersistenceModule.DATABASE_NAME,
        )
    }

    @Test
    fun fresh_open_creates_encrypted_memora_db() {
        val database = MemoraEncryptedDatabaseOpener.open(context)
        try {
            assertEquals(4, database.openHelper.readableDatabase.version)
            assertEquals(0, runBlocking { database.assetDao().count() })
            assertFalse(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
            assertEquals(
                DatabaseEncryptionConversionPhase.COMPLETED,
                DatabaseEncryptionConversionJournal(
                    context = context,
                    journalFileName = ProductionDatabaseIdentity.JOURNAL_FILE,
                ).read().phase,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun converts_existing_plaintext_memora_db_then_serves_encrypted_rows() = runBlocking {
        seedPlaintextFixtures()

        val database = MemoraEncryptedDatabaseOpener.open(context)
        try {
            assertEquals(1, database.assetDao().count())
            assertEquals(
                FIXTURE_MARKER,
                database.assetDao().findAll().single().fingerprint,
            )
            assertEquals(1, database.discoveryCheckpointDao().count())
            assertEquals(1, database.documentTreeApprovalDao().count())
            assertFalse(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
            assertFalse(
                context.getDatabasePath(ProductionDatabaseIdentity.PLAINTEXT_RETAINED_NAME).exists(),
            )
            assertEquals(
                DatabaseEncryptionConversionPhase.COMPLETED,
                DatabaseEncryptionConversionJournal(
                    context = context,
                    journalFileName = ProductionDatabaseIdentity.JOURNAL_FILE,
                ).read().phase,
            )
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
            )
            .build()
        try {
            plaintext.openHelper.writableDatabase
            plaintext.assetDao().upsert(
                AssetEntity(
                    sourceId = "opener-source",
                    sourceAssetKey = "opener-asset-1",
                    assetType = "PDF",
                    location = "content://com.memora.poc/opener/1",
                    fingerprint = FIXTURE_MARKER,
                    discoveredAtEpochMillis = 1_700_000_300_000L,
                    displayName = "opener-fixture.pdf",
                    sourceModifiedAtEpochMillis = 1_700_000_300_050L,
                    indexingStatus = "DISCOVERED",
                    indexingAttemptCount = 0,
                    failureCode = null,
                    failureMessage = null,
                ),
            )
            plaintext.discoveryCheckpointDao().upsert(
                DiscoveryCheckpointEntity(
                    sourceId = "opener-source",
                    cursorValue = "opener-cursor-1",
                    savedAtEpochMillis = 1_700_000_300_000L,
                ),
            )
            plaintext.documentTreeApprovalDao().upsert(
                DocumentTreeApprovalEntity(
                    sourceId = "opener-source",
                    treeUri = "content://com.memora.poc/tree/opener",
                    approvedAtEpochMillis = 1_700_000_300_100L,
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
        private const val FIXTURE_MARKER = "memora-opener-fixture-marker-v1"
    }
}
