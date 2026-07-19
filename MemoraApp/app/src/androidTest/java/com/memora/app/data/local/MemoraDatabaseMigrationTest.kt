package com.memora.app.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uses a real version-1 SQLite fixture so the migration can be verified without the
 * incompatible serialization runtime loaded by Room's MigrationTestHelper.
 */
@RunWith(AndroidJUnit4::class)
class MemoraDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    @After
    fun deleteTestDatabase() {
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @Test
    fun migratesVersionOneAssetsWithoutDestructiveReset() = runBlocking {
        createVersionOneDatabaseWithAnAsset()

        val migratedDatabase = Room.databaseBuilder(
            context,
            MemoraDatabase::class.java,
            TEST_DATABASE_NAME,
        ).addMigrations(MemoraDatabaseMigrations.MIGRATION_1_2).build()

        try {
            val preservedAsset = migratedDatabase.assetDao().find(
                "android-media-store-images",
                "external_primary:42",
            )

            assertEquals("lake.jpg", preservedAsset?.displayName)
            assertEquals(null, migratedDatabase.discoveryCheckpointDao().find("android-media-store-images"))
        } finally {
            migratedDatabase.close()
        }
    }

    private fun createVersionOneDatabaseWithAnAsset() {
        val databaseFile = context.getDatabasePath(TEST_DATABASE_NAME)
        databaseFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            database.execSQL(
                """
                CREATE TABLE `assets` (
                    `source_id` TEXT NOT NULL,
                    `source_asset_key` TEXT NOT NULL,
                    `asset_type` TEXT NOT NULL,
                    `location` TEXT NOT NULL,
                    `fingerprint` TEXT NOT NULL,
                    `discovered_at_epoch_millis` INTEGER NOT NULL,
                    `display_name` TEXT,
                    `source_modified_at_epoch_millis` INTEGER,
                    `indexing_status` TEXT NOT NULL,
                    `indexing_attempt_count` INTEGER NOT NULL,
                    `failure_code` TEXT,
                    `failure_message` TEXT,
                    PRIMARY KEY(`source_id`, `source_asset_key`)
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX `index_assets_fingerprint` ON `assets` (`fingerprint`)")
            database.execSQL("CREATE INDEX `index_assets_indexing_status` ON `assets` (`indexing_status`)")
            database.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            database.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)",
                arrayOf(VERSION_ONE_IDENTITY_HASH),
            )
            database.execSQL(
                """
                INSERT INTO assets (
                    source_id, source_asset_key, asset_type, location, fingerprint,
                    discovered_at_epoch_millis, display_name, source_modified_at_epoch_millis,
                    indexing_status, indexing_attempt_count, failure_code, failure_message
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "android-media-store-images",
                    "external_primary:42",
                    "PHOTO",
                    "content://media/external_primary/images/media/42",
                    "external_primary:42:9:2048",
                    1_720_000_000_000L,
                    "lake.jpg",
                    1_720_000_000_000L,
                    "DISCOVERED",
                    0,
                    null,
                    null,
                ),
            )
            database.version = 1
        }
    }

    private companion object {
        const val TEST_DATABASE_NAME = "memora-migration-test"
        const val VERSION_ONE_IDENTITY_HASH = "0c50310c2cead329a76dfbe22c8241c6"
    }
}
