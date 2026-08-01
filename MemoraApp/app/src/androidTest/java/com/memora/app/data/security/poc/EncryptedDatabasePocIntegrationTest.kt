package com.memora.app.data.security.poc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.di.PersistenceModule
import com.memora.app.data.local.AssetEntity
import com.memora.app.data.local.DiscoveryCheckpointEntity
import com.memora.app.data.local.DocumentTreeApprovalEntity
import com.memora.app.data.security.DatabaseSecretFailureCategory
import com.memora.app.data.security.KeystoreDatabasePassphraseStore
import java.security.SecureRandom
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic encrypted-database PoC corpus from docs/ENCRYPTED_DATABASE_POC_PLAN.md.
 * Uses only repository-owned fixture rows and a separately named database file.
 * It does not convert production memora.db, open user sources, or change UI.
 */
@RunWith(AndroidJUnit4::class)
class EncryptedDatabasePocIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var passphraseStore: KeystoreDatabasePassphraseStore

    @Before
    fun setUp() {
        passphraseStore = KeystoreDatabasePassphraseStore(context)
        passphraseStore.clearForTest()
        EncryptedPocDatabaseFactory.deleteDatabaseFiles(context)
    }

    @After
    fun tearDown() {
        passphraseStore.clearForTest()
        EncryptedPocDatabaseFactory.deleteDatabaseFiles(context)
    }

    @Test
    fun native_sqlcipher_library_loads_on_device() {
        EncryptedPocDatabaseFactory.loadNativeLibrary()
    }

    @Test
    fun release_app_declares_internet_for_notes_connector_not_for_database_poc() {
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.toList()
            .orEmpty()

        assertTrue(
            "Notes N2b adds INTERNET for Microsoft OneNote source access; " +
                "the encrypted-database PoC still must not perform network I/O.",
            Manifest.permission.INTERNET in requested,
        )
    }

    @Test
    fun production_persistence_module_targets_memora_db_name() {
        assertEquals("memora.db", PersistenceModule.DATABASE_NAME)
        assertFalse(
            EncryptedPocDatabaseFactory.DATABASE_NAME == "memora.db",
        )
    }

    @Test
    fun create_reopen_and_round_trip_synthetic_schema_v3_rows() = runBlocking {
        val opened = EncryptedPocDatabaseFactory.createNew(context, passphraseStore)
        val database = (opened as EncryptedPocOpenResult.Opened).database

        val asset = syntheticAsset()
        val checkpoint = DiscoveryCheckpointEntity(
            sourceId = "poc-source",
            cursorValue = "cursor-1",
            savedAtEpochMillis = 1_700_000_000_000L,
        )
        val approval = DocumentTreeApprovalEntity(
            sourceId = "poc-source",
            treeUri = "content://com.memora.poc/tree/fixture",
            approvedAtEpochMillis = 1_700_000_000_100L,
        )

        database.assetDao().upsert(asset)
        database.discoveryCheckpointDao().upsert(checkpoint)
        database.documentTreeApprovalDao().upsert(approval)
        database.close()

        val reopened = EncryptedPocDatabaseFactory.reopenExisting(context, passphraseStore)
        val restored = (reopened as EncryptedPocOpenResult.Opened).database
        try {
            assertEquals(asset, restored.assetDao().find(asset.sourceId, asset.sourceAssetKey))
            assertEquals(checkpoint, restored.discoveryCheckpointDao().find(checkpoint.sourceId))
            assertEquals(approval, restored.documentTreeApprovalDao().find(approval.sourceId))
        } finally {
            restored.close()
        }
    }

    @Test
    fun wrong_passphrase_fails_without_deleting_database_files() = runBlocking {
        val opened = EncryptedPocDatabaseFactory.createNew(context, passphraseStore)
        val database = (opened as EncryptedPocOpenResult.Opened).database
        database.assetDao().upsert(syntheticAsset())
        database.close()

        assertTrue(EncryptedPocDatabaseFactory.databaseFile(context).exists())

        val wrongPassphrase = ByteArray(32).also(SecureRandom()::nextBytes)
        val denied = EncryptedPocDatabaseFactory.openWithRawPassphrase(context, wrongPassphrase)

        assertEquals(
            EncryptedPocOpenResult.Denied(DatabaseSecretFailureCategory.DATABASE_AUTH_FAILED),
            denied,
        )
        assertTrue(
            "Wrong-passphrase denial must retain the encrypted database file.",
            EncryptedPocDatabaseFactory.databaseFile(context).exists(),
        )
    }

    @Test
    fun tampered_wrapper_denies_access_without_creating_a_replacement_database() {
        val opened = EncryptedPocDatabaseFactory.createNew(context, passphraseStore)
        (opened as EncryptedPocOpenResult.Opened).database.close()
        val existingLength = EncryptedPocDatabaseFactory.databaseFile(context).length()
        assertTrue(existingLength > 0L)

        passphraseStore.tamperWrapperForTest()
        val denied = EncryptedPocDatabaseFactory.reopenExisting(context, passphraseStore)

        assertEquals(
            EncryptedPocOpenResult.Denied(DatabaseSecretFailureCategory.WRAPPER_INVALID),
            denied,
        )
        assertTrue(EncryptedPocDatabaseFactory.databaseFile(context).exists())
        assertEquals(existingLength, EncryptedPocDatabaseFactory.databaseFile(context).length())
    }

    @Test
    fun standard_sqlite_probe_cannot_read_encrypted_fixture_marker() = runBlocking {
        val opened = EncryptedPocDatabaseFactory.createNew(context, passphraseStore)
        val database = (opened as EncryptedPocOpenResult.Opened).database
        database.assetDao().upsert(syntheticAsset())
        database.close()

        val probe = EncryptedPocDatabaseFactory.probeWithStandardSqlite(context)
        assertTrue(probe.fileExistedBeforeProbe)
        assertFalse(
            "Standard SQLite must not open the encrypted PoC database without the passphrase.",
            probe.openedWithoutPassphrase,
        )
        assertTrue(
            "A read-only plaintext probe must not delete the encrypted database.",
            probe.fileExistsAfterProbe,
        )
        assertEquals(probe.fileLengthBeforeProbe, probe.fileLengthAfterProbe)
        assertFalse(
            "Fixture marker must not appear in clear-text database artifacts.",
            EncryptedPocDatabaseFactory.containsFixtureMarkerInCleartextArtifacts(context),
        )
    }

    private fun syntheticAsset(): AssetEntity = AssetEntity(
        sourceId = "poc-source",
        sourceAssetKey = "poc-asset-1",
        assetType = "PDF",
        location = "content://com.memora.poc/document/1",
        fingerprint = EncryptedPocDatabaseFactory.FIXTURE_MARKER,
        discoveredAtEpochMillis = 1_700_000_000_000L,
        displayName = "poc-fixture.pdf",
        sourceModifiedAtEpochMillis = 1_700_000_000_050L,
        indexingStatus = "DISCOVERED",
        indexingAttemptCount = 0,
        failureCode = null,
        failureMessage = null,
    )
}
