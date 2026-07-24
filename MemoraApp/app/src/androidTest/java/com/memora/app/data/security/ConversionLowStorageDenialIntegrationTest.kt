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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Low-storage / interruption denial must leave plaintext intact and expose
 * [DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED].
 */
@RunWith(AndroidJUnit4::class)
class ConversionLowStorageDenialIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val journal = DatabaseEncryptionConversionJournal(
        context = context,
        journalFileName = ProductionDatabaseIdentity.JOURNAL_FILE,
    )

    @Before
    fun setUp() {
        ProductionDatabaseTestCleanup.clearAll(context)
        MemoraEncryptedDatabaseOpener.setStorageGuardForTest(null)
        MemoraEncryptedDatabaseOpener.setForceIoFailureDuringConversionForTest(false)
        MemoraEncryptedDatabaseOpener.clearLastFailureCategoryForTest()
    }

    @After
    fun tearDown() {
        MemoraEncryptedDatabaseOpener.setStorageGuardForTest(null)
        MemoraEncryptedDatabaseOpener.setForceIoFailureDuringConversionForTest(false)
        MemoraEncryptedDatabaseOpener.clearLastFailureCategoryForTest()
        ProductionDatabaseTestCleanup.clearAll(context)
    }

    @Test
    fun low_storage_denial_keeps_plaintext_and_exposes_conversion_validation_failed() = runBlocking {
        seedPlaintextFixtures()
        val plaintextLength = context.getDatabasePath(ProductionDatabaseIdentity.DATABASE_NAME).length()

        MemoraEncryptedDatabaseOpener.setStorageGuardForTest(
            ConversionStorageGuard { _, _ -> false },
        )

        val deniedOpen = MemoraEncryptedDatabaseOpener.open(context)
        try {
            assertEquals(1, deniedOpen.assetDao().count())
            assertEquals(FIXTURE_MARKER, deniedOpen.assetDao().findAll().single().fingerprint)
            assertTrue(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
            assertFalse(
                context.getDatabasePath(ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME).exists(),
            )
            assertEquals(plaintextLength, context.getDatabasePath(ProductionDatabaseIdentity.DATABASE_NAME).length())
            assertEquals(DatabaseEncryptionConversionPhase.FAILED_SAFE, journal.read().phase)
            assertEquals(
                DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED,
                MemoraEncryptedDatabaseOpener.lastFailureCategory,
            )
        } finally {
            deniedOpen.close()
        }

        MemoraEncryptedDatabaseOpener.setStorageGuardForTest(null)
        MemoraEncryptedDatabaseOpener.clearLastFailureCategoryForTest()

        val converted = MemoraEncryptedDatabaseOpener.open(context)
        try {
            assertEquals(1, converted.assetDao().count())
            assertEquals(FIXTURE_MARKER, converted.assetDao().findAll().single().fingerprint)
            assertFalse(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
            assertEquals(DatabaseEncryptionConversionPhase.COMPLETED, journal.read().phase)
        } finally {
            converted.close()
        }
    }

    @Test
    fun io_interruption_during_candidate_keeps_plaintext_and_exposes_conversion_validation_failed() =
        runBlocking {
            seedPlaintextFixtures()

            MemoraEncryptedDatabaseOpener.setForceIoFailureDuringConversionForTest(true)

            val deniedOpen = MemoraEncryptedDatabaseOpener.open(context)
            try {
                assertEquals(1, deniedOpen.assetDao().count())
                assertEquals(FIXTURE_MARKER, deniedOpen.assetDao().findAll().single().fingerprint)
                assertTrue(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
                assertFalse(
                    context.getDatabasePath(ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME).exists(),
                )
                assertEquals(DatabaseEncryptionConversionPhase.FAILED_SAFE, journal.read().phase)
                assertEquals(
                    DatabaseSecretFailureCategory.CONVERSION_VALIDATION_FAILED,
                    MemoraEncryptedDatabaseOpener.lastFailureCategory,
                )
            } finally {
                deniedOpen.close()
            }

            MemoraEncryptedDatabaseOpener.setForceIoFailureDuringConversionForTest(false)
            MemoraEncryptedDatabaseOpener.clearLastFailureCategoryForTest()

            val converted = MemoraEncryptedDatabaseOpener.open(context)
            try {
                assertEquals(1, converted.assetDao().count())
                assertFalse(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
                assertEquals(DatabaseEncryptionConversionPhase.COMPLETED, journal.read().phase)
            } finally {
                converted.close()
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
                    sourceId = "low-storage-source",
                    sourceAssetKey = "low-storage-asset-1",
                    assetType = "PDF",
                    location = "content://com.memora.poc/low-storage/1",
                    fingerprint = FIXTURE_MARKER,
                    discoveredAtEpochMillis = 1_700_000_700_000L,
                    displayName = "low-storage-fixture.pdf",
                    sourceModifiedAtEpochMillis = 1_700_000_700_050L,
                    indexingStatus = "DISCOVERED",
                    indexingAttemptCount = 0,
                    failureCode = null,
                    failureMessage = null,
                ),
            )
            plaintext.discoveryCheckpointDao().upsert(
                DiscoveryCheckpointEntity(
                    sourceId = "low-storage-source",
                    cursorValue = "low-storage-cursor-1",
                    savedAtEpochMillis = 1_700_000_700_000L,
                ),
            )
            plaintext.documentTreeApprovalDao().upsert(
                DocumentTreeApprovalEntity(
                    sourceId = "low-storage-source",
                    treeUri = "content://com.memora.poc/tree/low-storage",
                    approvedAtEpochMillis = 1_700_000_700_100L,
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
        private const val FIXTURE_MARKER = "memora-low-storage-fixture-v1"
    }
}
