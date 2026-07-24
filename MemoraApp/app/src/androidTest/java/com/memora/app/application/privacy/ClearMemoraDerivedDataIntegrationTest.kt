package com.memora.app.application.privacy

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.local.AssetEntity
import com.memora.app.data.local.DiscoveryCheckpointEntity
import com.memora.app.data.local.DocumentTreeApprovalEntity
import com.memora.app.data.security.DatabaseEncryptionConversionJournal
import com.memora.app.data.security.DatabaseEncryptionConversionPhase
import com.memora.app.data.security.MemoraDatabaseHandle
import com.memora.app.data.security.ProductionDatabaseIdentity
import com.memora.app.data.security.ProductionDatabaseTestCleanup
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
 * User-confirmed clear of Memora-owned derived data against production identity files.
 * TearDown restores a clean device state for the installed app.
 */
@RunWith(AndroidJUnit4::class)
class ClearMemoraDerivedDataIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var handle: MemoraDatabaseHandle
    private lateinit var clearDerivedData: ClearMemoraDerivedData

    @Before
    fun setUp() {
        ProductionDatabaseTestCleanup.clearAll(context)
        handle = MemoraDatabaseHandle(context)
        clearDerivedData = ClearMemoraDerivedData(handle)
    }

    @After
    fun tearDown() {
        runCatching { handle.database().close() }
        ProductionDatabaseTestCleanup.clearAll(context)
    }

    @Test
    fun clears_seeded_rows_and_serves_fresh_empty_encrypted_database() = runBlocking {
        val grantsBefore = context.contentResolver.persistedUriPermissions.size
        seedDerivedRows()
        assertEquals(1, handle.database().assetDao().count())
        assertEquals(1, handle.database().documentTreeApprovalDao().count())

        val result = clearDerivedData()
        assertTrue(result is ClearMemoraDerivedDataResult.Cleared)
        assertEquals(
            ClearMemoraDerivedData.APPROVED_REBUILD_MESSAGE,
            (result as ClearMemoraDerivedDataResult.Cleared).message,
        )

        assertEquals(0, handle.database().assetDao().count())
        assertEquals(0, handle.database().discoveryCheckpointDao().count())
        assertEquals(0, handle.database().documentTreeApprovalDao().count())
        assertEquals(3, handle.database().openHelper.readableDatabase.version)
        assertFalse(probeStandardSqlite(ProductionDatabaseIdentity.DATABASE_NAME))
        assertFalse(
            context.getDatabasePath(ProductionDatabaseIdentity.ENCRYPTED_CANDIDATE_NAME).exists(),
        )
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
        assertTrue(
            File(context.noBackupFilesDir, ProductionDatabaseIdentity.WRAPPER_FILE).exists(),
        )
        assertEquals(grantsBefore, context.contentResolver.persistedUriPermissions.size)
    }

    private suspend fun seedDerivedRows() {
        val database = handle.database()
        database.assetDao().upsert(
            AssetEntity(
                sourceId = "clear-source",
                sourceAssetKey = "clear-asset-1",
                assetType = "PDF",
                location = "content://com.memora.poc/clear/1",
                fingerprint = "clear-fixture-marker",
                discoveredAtEpochMillis = 1_700_000_400_000L,
                displayName = "clear-fixture.pdf",
                sourceModifiedAtEpochMillis = 1_700_000_400_050L,
                indexingStatus = "DISCOVERED",
                indexingAttemptCount = 0,
                failureCode = null,
                failureMessage = null,
            ),
        )
        database.discoveryCheckpointDao().upsert(
            DiscoveryCheckpointEntity(
                sourceId = "clear-source",
                cursorValue = "clear-cursor-1",
                savedAtEpochMillis = 1_700_000_400_000L,
            ),
        )
        database.documentTreeApprovalDao().upsert(
            DocumentTreeApprovalEntity(
                sourceId = "clear-source",
                treeUri = "content://com.memora.poc/tree/clear",
                approvedAtEpochMillis = 1_700_000_400_100L,
            ),
        )
    }

    private fun probeStandardSqlite(databaseName: String): Boolean {
        val file = context.getDatabasePath(databaseName)
        if (!file.exists()) {
            return false
        }
        return try {
            SQLiteDatabase.openDatabase(
                file.path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).use { database ->
                database.rawQuery("SELECT 1", null).use { cursor ->
                    cursor.moveToFirst()
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
