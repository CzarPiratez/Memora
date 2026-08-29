package com.memora.app.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        context.deleteDatabase(VERSION_TWELVE_EVIDENCE_DATABASE_NAME)
        context.deleteDatabase(VERSION_THIRTEEN_DUAL_STORE_DATABASE_NAME)
        context.deleteDatabase(VERSION_FOURTEEN_RETIRE_PAGE_DATABASE_NAME)
    }

    @Test
    fun migratesVersionOneAssetsWithoutDestructiveReset() = runBlocking {
        createVersionOneDatabaseWithAnAsset()

        val migratedDatabase = Room.databaseBuilder(
            context,
            MemoraDatabase::class.java,
            TEST_DATABASE_NAME,
        ).addMigrations(
            MemoraDatabaseMigrations.MIGRATION_1_2,
            MemoraDatabaseMigrations.MIGRATION_2_3,
            MemoraDatabaseMigrations.MIGRATION_3_4,
            MemoraDatabaseMigrations.MIGRATION_4_5,
            MemoraDatabaseMigrations.MIGRATION_5_6,
            MemoraDatabaseMigrations.MIGRATION_6_7,
            MemoraDatabaseMigrations.MIGRATION_7_8,
            MemoraDatabaseMigrations.MIGRATION_8_9,
            MemoraDatabaseMigrations.MIGRATION_9_10,
            MemoraDatabaseMigrations.MIGRATION_10_11,
            MemoraDatabaseMigrations.MIGRATION_11_12,
            MemoraDatabaseMigrations.MIGRATION_12_13,
            MemoraDatabaseMigrations.MIGRATION_13_14,
            MemoraDatabaseMigrations.MIGRATION_14_15,
        ).build()

        try {
            val preservedAsset = migratedDatabase.assetDao().find(
                "android-media-store-images",
                "external_primary:42",
            )

            assertEquals("lake.jpg", preservedAsset?.displayName)
            assertEquals(null, migratedDatabase.discoveryCheckpointDao().find("android-media-store-images"))
            assertTrue(migratedDatabase.documentTreeApprovalDao().findAll().isEmpty())
            assertEquals(15, migratedDatabase.openHelper.readableDatabase.version)
            assertEquals(0, migratedDatabase.aiPackInstallLedgerDao().count())
            assertEquals(
                0,
                migratedDatabase.memoryEmbeddingDao().countForModel("none", "none"),
            )
            assertFalse(tableExists(migratedDatabase, "pdf_page_embeddings"))
            assertEquals(
                0,
                migratedDatabase.memoryEvidenceEmbeddingDao().countForModel("none", "none"),
            )
            assertEquals(
                0,
                migratedDatabase.pdfExtractionDao().countForAsset(
                    "android-media-store-images",
                    "external_primary:42",
                ),
            )
            assertEquals(
                0,
                migratedDatabase.photoOcrExtractionDao()
                    .countCurrentSearchablePhotos("photo-ocr-v1"),
            )
            assertEquals(
                0,
                migratedDatabase.memoryDao().countCurrentReady("asset-memory-facts-v2"),
            )
        } finally {
            migratedDatabase.close()
        }
    }

    @Test
    fun backfillsDirectEvidenceClassOnExistingMemoryEvidenceRow() = runBlocking {
        createVersionTwelveDatabaseWithOneEvidenceRow()

        val migratedDatabase = Room.databaseBuilder(
            context,
            MemoraDatabase::class.java,
            VERSION_TWELVE_EVIDENCE_DATABASE_NAME,
        ).addMigrations(
            MemoraDatabaseMigrations.MIGRATION_12_13,
            MemoraDatabaseMigrations.MIGRATION_13_14,
            MemoraDatabaseMigrations.MIGRATION_14_15,
        ).build()

        try {
            assertEquals(15, migratedDatabase.openHelper.readableDatabase.version)
            val evidence = migratedDatabase.memoryDao().findEvidence(LEGACY_REVISION_ID)
            assertEquals(1, evidence.size)
            assertEquals("DIRECT", evidence.single().evidenceClass)
            assertEquals("e1", evidence.single().evidenceId)
            assertEquals("OCR_TEXT", evidence.single().evidenceKind)
            assertEquals("image:whole", evidence.single().locator)
            assertEquals("Legacy excerpt", evidence.single().excerpt)
        } finally {
            migratedDatabase.close()
        }
    }

    @Test
    fun migratesVersionThirteenCreatesEvidenceEmbeddingTableThenDropsPdfPageOnFifteen() =
        runBlocking {
            createVersionThirteenDatabaseWithPdfPageEmbeddingRow()

            val migratedDatabase = Room.databaseBuilder(
                context,
                MemoraDatabase::class.java,
                VERSION_THIRTEEN_DUAL_STORE_DATABASE_NAME,
            ).addMigrations(
                MemoraDatabaseMigrations.MIGRATION_13_14,
                MemoraDatabaseMigrations.MIGRATION_14_15,
            ).build()

            try {
                assertEquals(15, migratedDatabase.openHelper.readableDatabase.version)
                assertFalse(tableExists(migratedDatabase, "pdf_page_embeddings"))
                assertEquals(
                    0,
                    migratedDatabase.memoryEvidenceEmbeddingDao().countForModel("none", "none"),
                )
                // Prove the evidence table is queryable after CREATE (13→14) + DROP (14→15).
                assertTrue(
                    migratedDatabase.memoryEvidenceEmbeddingDao()
                        .listForModel("none", "none")
                        .isEmpty(),
                )
            } finally {
                migratedDatabase.close()
            }
        }

    @Test
    fun migratesVersionFourteenDropsPdfPageEmbeddingsPreservesEvidenceEmbeddings() = runBlocking {
        createVersionFourteenDatabaseWithPageAndEvidenceEmbeddings()

        val migratedDatabase = Room.databaseBuilder(
            context,
            MemoraDatabase::class.java,
            VERSION_FOURTEEN_RETIRE_PAGE_DATABASE_NAME,
        ).addMigrations(
            MemoraDatabaseMigrations.MIGRATION_14_15,
        ).build()

        try {
            assertEquals(15, migratedDatabase.openHelper.readableDatabase.version)
            assertFalse(tableExists(migratedDatabase, "pdf_page_embeddings"))
            assertEquals(
                1,
                migratedDatabase.memoryEvidenceEmbeddingDao().countForModel("use-test", "1.0"),
            )
            val preserved = migratedDatabase.memoryEvidenceEmbeddingDao().find(
                revisionId = LEGACY_REVISION_ID_V14,
                evidenceId = "e3",
                modelId = "use-test",
                modelVersion = "1.0",
            )
            assertEquals(LEGACY_REVISION_ID_V14, preserved?.revisionId)
            assertEquals("e3", preserved?.evidenceId)
            assertEquals("fp-evidence-e3", preserved?.sourceTextFingerprint)
        } finally {
            migratedDatabase.close()
        }
    }

    private fun tableExists(database: MemoraDatabase, tableName: String): Boolean {
        database.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName),
        ).use { cursor ->
            return cursor.moveToFirst()
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

    /**
     * Schema 12.json / MIGRATION_7_8 created `memories` and `memory_evidence` without
     * `evidence_class`. Other v12 tables are empty so Room can open the upgraded file.
     */
    private fun createVersionTwelveDatabaseWithOneEvidenceRow() {
        val databaseFile = context.getDatabasePath(VERSION_TWELVE_EVIDENCE_DATABASE_NAME)
        databaseFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            VERSION_TWELVE_SCHEMA_SQL.forEach(database::execSQL)
            database.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            database.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)",
                arrayOf(VERSION_TWELVE_IDENTITY_HASH),
            )
            database.execSQL(
                """
                INSERT INTO `memories` (
                    `revision_id`, `memory_id`, `source_id`, `source_asset_key`, `fingerprint`,
                    `assembly_schema_version`, `integrity_state`, `summary_text`,
                    `created_at_epoch_millis`, `updated_at_epoch_millis`
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    LEGACY_REVISION_ID,
                    "mem-mig01",
                    "source",
                    "asset",
                    "fp-1",
                    "asset-memory-facts-v2",
                    "READY",
                    "Legacy excerpt",
                    1_720_000_000_000L,
                    1_720_000_000_000L,
                ),
            )
            database.execSQL(
                """
                INSERT INTO `memory_evidence` (
                    `revision_id`, `evidence_id`, `evidence_kind`, `locator`, `excerpt`
                ) VALUES (?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    LEGACY_REVISION_ID,
                    "e1",
                    "OCR_TEXT",
                    "image:whole",
                    "Legacy excerpt",
                ),
            )
            database.version = 12
        }
    }

    /**
     * Schema 13.json: same as v12 plus `evidence_class` on `memory_evidence`.
     * Seeds one `pdf_page_embeddings` row so MIG-05 dual-store interim is proven.
     */
    private fun createVersionThirteenDatabaseWithPdfPageEmbeddingRow() {
        val databaseFile = context.getDatabasePath(VERSION_THIRTEEN_DUAL_STORE_DATABASE_NAME)
        databaseFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            VERSION_THIRTEEN_SCHEMA_SQL.forEach(database::execSQL)
            database.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            database.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)",
                arrayOf(VERSION_THIRTEEN_IDENTITY_HASH),
            )
            database.execSQL(
                """
                INSERT INTO `memories` (
                    `revision_id`, `memory_id`, `source_id`, `source_asset_key`, `fingerprint`,
                    `assembly_schema_version`, `integrity_state`, `summary_text`,
                    `created_at_epoch_millis`, `updated_at_epoch_millis`
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    LEGACY_REVISION_ID_V13,
                    "mem-mig05",
                    "source",
                    "asset-pdf",
                    "fp-pdf-1",
                    "asset-memory-facts-v4",
                    "READY",
                    "PDF summary",
                    1_720_000_000_000L,
                    1_720_000_000_000L,
                ),
            )
            database.execSQL(
                """
                INSERT INTO `pdf_page_embeddings` (
                    `revision_id`, `memory_id`, `page_number`, `model_id`, `model_version`,
                    `dimensions`, `vector_blob`, `source_text_fingerprint`, `created_at_epoch_ms`
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    LEGACY_REVISION_ID_V13,
                    "mem-mig05",
                    2,
                    "use-test",
                    "1.0",
                    1,
                    byteArrayOf(0, 0, 0x80.toByte(), 0x3F), // float 1.0 little-endian
                    "fp-page-2",
                    1_720_000_000_000L,
                ),
            )
            database.version = 13
        }
    }

    /**
     * Schema 14.json: v13 + memory_evidence_embeddings. Seeds both a page
     * embedding row (to be DROPped) and an evidence embedding row (preserved).
     */
    private fun createVersionFourteenDatabaseWithPageAndEvidenceEmbeddings() {
        val databaseFile = context.getDatabasePath(VERSION_FOURTEEN_RETIRE_PAGE_DATABASE_NAME)
        databaseFile.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            VERSION_FOURTEEN_SCHEMA_SQL.forEach(database::execSQL)
            database.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            database.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)",
                arrayOf(VERSION_FOURTEEN_IDENTITY_HASH),
            )
            database.execSQL(
                """
                INSERT INTO `memories` (
                    `revision_id`, `memory_id`, `source_id`, `source_asset_key`, `fingerprint`,
                    `assembly_schema_version`, `integrity_state`, `summary_text`,
                    `created_at_epoch_millis`, `updated_at_epoch_millis`
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    LEGACY_REVISION_ID_V14,
                    "mem-mig05-retire",
                    "source",
                    "asset-pdf",
                    "fp-pdf-14",
                    "asset-memory-facts-v4",
                    "READY",
                    "PDF summary",
                    1_720_000_000_000L,
                    1_720_000_000_000L,
                ),
            )
            database.execSQL(
                """
                INSERT INTO `pdf_page_embeddings` (
                    `revision_id`, `memory_id`, `page_number`, `model_id`, `model_version`,
                    `dimensions`, `vector_blob`, `source_text_fingerprint`, `created_at_epoch_ms`
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    LEGACY_REVISION_ID_V14,
                    "mem-mig05-retire",
                    2,
                    "use-test",
                    "1.0",
                    1,
                    byteArrayOf(0, 0, 0x80.toByte(), 0x3F),
                    "fp-page-2",
                    1_720_000_000_000L,
                ),
            )
            database.execSQL(
                """
                INSERT INTO `memory_evidence_embeddings` (
                    `revision_id`, `memory_id`, `evidence_id`, `model_id`, `model_version`,
                    `dimensions`, `vector_blob`, `source_text_fingerprint`, `created_at_epoch_ms`
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    LEGACY_REVISION_ID_V14,
                    "mem-mig05-retire",
                    "e3",
                    "use-test",
                    "1.0",
                    1,
                    byteArrayOf(0, 0, 0x80.toByte(), 0x3F),
                    "fp-evidence-e3",
                    1_720_000_000_000L,
                ),
            )
            database.version = 14
        }
    }

    private companion object {
        const val TEST_DATABASE_NAME = "memora-migration-test"
        const val VERSION_TWELVE_EVIDENCE_DATABASE_NAME = "memora-migration-v12-evidence"
        const val VERSION_THIRTEEN_DUAL_STORE_DATABASE_NAME = "memora-migration-v13-dual-store"
        const val VERSION_FOURTEEN_RETIRE_PAGE_DATABASE_NAME = "memora-migration-v14-retire-page"
        const val LEGACY_REVISION_ID = "rev-mig01"
        const val LEGACY_REVISION_ID_V13 = "rev-mig05"
        const val LEGACY_REVISION_ID_V14 = "rev-mig05-retire"
        const val VERSION_ONE_IDENTITY_HASH = "0c50310c2cead329a76dfbe22c8241c6"
        const val VERSION_TWELVE_IDENTITY_HASH = "982059560c315dcbca422ad3cdc12b6d"
        const val VERSION_THIRTEEN_IDENTITY_HASH = "d6343d5397ea12646194c479ab6255fc"
        const val VERSION_FOURTEEN_IDENTITY_HASH = "9637e604f34fc817e9c6a0ce655c3a4d"

        /**
         * Exact v12 CREATE TABLE / INDEX SQL from
         * `app/schemas/com.memora.app.data.local.MemoraDatabase/12.json`.
         * `memory_evidence` has no `evidence_class` column.
         */
        val VERSION_TWELVE_SCHEMA_SQL = listOf(
            "CREATE TABLE IF NOT EXISTS `assets` (`source_id` TEXT NOT NULL, `source_asset_key` TEXT NOT NULL, `asset_type` TEXT NOT NULL, `location` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `discovered_at_epoch_millis` INTEGER NOT NULL, `display_name` TEXT, `source_modified_at_epoch_millis` INTEGER, `indexing_status` TEXT NOT NULL, `indexing_attempt_count` INTEGER NOT NULL, `failure_code` TEXT, `failure_message` TEXT, PRIMARY KEY(`source_id`, `source_asset_key`))",
            "CREATE INDEX IF NOT EXISTS `index_assets_fingerprint` ON `assets` (`fingerprint`)",
            "CREATE INDEX IF NOT EXISTS `index_assets_indexing_status` ON `assets` (`indexing_status`)",
            "CREATE TABLE IF NOT EXISTS `discovery_checkpoints` (`source_id` TEXT NOT NULL, `cursor_value` TEXT NOT NULL, `saved_at_epoch_millis` INTEGER NOT NULL, PRIMARY KEY(`source_id`))",
            "CREATE TABLE IF NOT EXISTS `document_tree_approvals` (`source_id` TEXT NOT NULL, `tree_uri` TEXT NOT NULL, `approved_at_epoch_millis` INTEGER NOT NULL, PRIMARY KEY(`source_id`))",
            "CREATE TABLE IF NOT EXISTS `pdf_extractions` (`source_id` TEXT NOT NULL, `source_asset_key` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `schema_version` TEXT NOT NULL, `page_count` INTEGER NOT NULL, `text_coverage` TEXT NOT NULL, `title` TEXT, `extracted_at_epoch_millis` INTEGER NOT NULL, `created_at_epoch_millis` INTEGER NOT NULL, `integrity` TEXT NOT NULL, PRIMARY KEY(`source_id`, `source_asset_key`, `fingerprint`, `schema_version`))",
            "CREATE INDEX IF NOT EXISTS `index_pdf_extractions_source_id_source_asset_key` ON `pdf_extractions` (`source_id`, `source_asset_key`)",
            "CREATE INDEX IF NOT EXISTS `index_pdf_extractions_fingerprint` ON `pdf_extractions` (`fingerprint`)",
            "CREATE TABLE IF NOT EXISTS `pdf_extraction_pages` (`source_id` TEXT NOT NULL, `source_asset_key` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `schema_version` TEXT NOT NULL, `page_number` INTEGER NOT NULL, `page_text` TEXT NOT NULL, PRIMARY KEY(`source_id`, `source_asset_key`, `fingerprint`, `schema_version`, `page_number`), FOREIGN KEY(`source_id`, `source_asset_key`, `fingerprint`, `schema_version`) REFERENCES `pdf_extractions`(`source_id`, `source_asset_key`, `fingerprint`, `schema_version`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_pdf_extraction_pages_source_id_source_asset_key_fingerprint_schema_version` ON `pdf_extraction_pages` (`source_id`, `source_asset_key`, `fingerprint`, `schema_version`)",
            "CREATE TABLE IF NOT EXISTS `pdf_extraction_metadata` (`source_id` TEXT NOT NULL, `source_asset_key` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `schema_version` TEXT NOT NULL, `metadata_name` TEXT NOT NULL, `metadata_value` TEXT NOT NULL, PRIMARY KEY(`source_id`, `source_asset_key`, `fingerprint`, `schema_version`, `metadata_name`), FOREIGN KEY(`source_id`, `source_asset_key`, `fingerprint`, `schema_version`) REFERENCES `pdf_extractions`(`source_id`, `source_asset_key`, `fingerprint`, `schema_version`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_pdf_extraction_metadata_source_id_source_asset_key_fingerprint_schema_version` ON `pdf_extraction_metadata` (`source_id`, `source_asset_key`, `fingerprint`, `schema_version`)",
            "CREATE TABLE IF NOT EXISTS `image_exif_extractions` (`source_id` TEXT NOT NULL, `source_asset_key` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `schema_version` TEXT NOT NULL, `asset_kind` TEXT NOT NULL, `datetime_original` TEXT, `image_width` INTEGER, `image_height` INTEGER, `orientation` INTEGER, `make` TEXT, `model` TEXT, `extracted_at_epoch_millis` INTEGER NOT NULL, `created_at_epoch_millis` INTEGER NOT NULL, `integrity` TEXT NOT NULL, PRIMARY KEY(`source_id`, `source_asset_key`, `fingerprint`, `schema_version`))",
            "CREATE INDEX IF NOT EXISTS `index_image_exif_extractions_source_id_source_asset_key` ON `image_exif_extractions` (`source_id`, `source_asset_key`)",
            "CREATE INDEX IF NOT EXISTS `index_image_exif_extractions_fingerprint` ON `image_exif_extractions` (`fingerprint`)",
            "CREATE TABLE IF NOT EXISTS `screenshot_ocr_extractions` (`source_id` TEXT NOT NULL, `source_asset_key` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `schema_version` TEXT NOT NULL, `full_text` TEXT NOT NULL, `text_truncated` INTEGER NOT NULL, `engine_id` TEXT NOT NULL, `engine_version` TEXT NOT NULL, `extracted_at_epoch_millis` INTEGER NOT NULL, `created_at_epoch_millis` INTEGER NOT NULL, `integrity` TEXT NOT NULL, PRIMARY KEY(`source_id`, `source_asset_key`, `fingerprint`, `schema_version`))",
            "CREATE INDEX IF NOT EXISTS `index_screenshot_ocr_extractions_source_id_source_asset_key` ON `screenshot_ocr_extractions` (`source_id`, `source_asset_key`)",
            "CREATE INDEX IF NOT EXISTS `index_screenshot_ocr_extractions_fingerprint` ON `screenshot_ocr_extractions` (`fingerprint`)",
            "CREATE TABLE IF NOT EXISTS `photo_ocr_extractions` (`source_id` TEXT NOT NULL, `source_asset_key` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `schema_version` TEXT NOT NULL, `full_text` TEXT NOT NULL, `text_truncated` INTEGER NOT NULL, `engine_id` TEXT NOT NULL, `engine_version` TEXT NOT NULL, `extracted_at_epoch_millis` INTEGER NOT NULL, `created_at_epoch_millis` INTEGER NOT NULL, `integrity` TEXT NOT NULL, PRIMARY KEY(`source_id`, `source_asset_key`, `fingerprint`, `schema_version`))",
            "CREATE INDEX IF NOT EXISTS `index_photo_ocr_extractions_source_id_source_asset_key` ON `photo_ocr_extractions` (`source_id`, `source_asset_key`)",
            "CREATE INDEX IF NOT EXISTS `index_photo_ocr_extractions_fingerprint` ON `photo_ocr_extractions` (`fingerprint`)",
            "CREATE TABLE IF NOT EXISTS `note_page_extractions` (`source_id` TEXT NOT NULL, `source_asset_key` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `schema_version` TEXT NOT NULL, `full_text` TEXT NOT NULL, `text_truncated` INTEGER NOT NULL, `engine_id` TEXT NOT NULL, `engine_version` TEXT NOT NULL, `extracted_at_epoch_millis` INTEGER NOT NULL, `created_at_epoch_millis` INTEGER NOT NULL, `integrity` TEXT NOT NULL, PRIMARY KEY(`source_id`, `source_asset_key`, `fingerprint`, `schema_version`))",
            "CREATE INDEX IF NOT EXISTS `index_note_page_extractions_source_id_source_asset_key` ON `note_page_extractions` (`source_id`, `source_asset_key`)",
            "CREATE INDEX IF NOT EXISTS `index_note_page_extractions_fingerprint` ON `note_page_extractions` (`fingerprint`)",
            "CREATE TABLE IF NOT EXISTS `memories` (`revision_id` TEXT NOT NULL, `memory_id` TEXT NOT NULL, `source_id` TEXT NOT NULL, `source_asset_key` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `assembly_schema_version` TEXT NOT NULL, `integrity_state` TEXT NOT NULL, `summary_text` TEXT NOT NULL, `created_at_epoch_millis` INTEGER NOT NULL, `updated_at_epoch_millis` INTEGER NOT NULL, PRIMARY KEY(`revision_id`))",
            "CREATE INDEX IF NOT EXISTS `index_memories_memory_id` ON `memories` (`memory_id`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_memories_source_id_source_asset_key_fingerprint_assembly_schema_version` ON `memories` (`source_id`, `source_asset_key`, `fingerprint`, `assembly_schema_version`)",
            "CREATE INDEX IF NOT EXISTS `index_memories_source_id_source_asset_key` ON `memories` (`source_id`, `source_asset_key`)",
            "CREATE INDEX IF NOT EXISTS `index_memories_fingerprint` ON `memories` (`fingerprint`)",
            "CREATE TABLE IF NOT EXISTS `memory_extraction_schemas` (`revision_id` TEXT NOT NULL, `schema_version` TEXT NOT NULL, PRIMARY KEY(`revision_id`, `schema_version`), FOREIGN KEY(`revision_id`) REFERENCES `memories`(`revision_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_memory_extraction_schemas_revision_id` ON `memory_extraction_schemas` (`revision_id`)",
            "CREATE TABLE IF NOT EXISTS `memory_evidence` (`revision_id` TEXT NOT NULL, `evidence_id` TEXT NOT NULL, `evidence_kind` TEXT NOT NULL, `locator` TEXT NOT NULL, `excerpt` TEXT NOT NULL, PRIMARY KEY(`revision_id`, `evidence_id`), FOREIGN KEY(`revision_id`) REFERENCES `memories`(`revision_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_memory_evidence_revision_id` ON `memory_evidence` (`revision_id`)",
            "CREATE TABLE IF NOT EXISTS `memory_anchors` (`revision_id` TEXT NOT NULL, `anchor_id` TEXT NOT NULL, `anchor_kind` TEXT NOT NULL, `anchor_text` TEXT NOT NULL, PRIMARY KEY(`revision_id`, `anchor_id`), FOREIGN KEY(`revision_id`) REFERENCES `memories`(`revision_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_memory_anchors_revision_id` ON `memory_anchors` (`revision_id`)",
            "CREATE TABLE IF NOT EXISTS `memory_anchor_evidence` (`revision_id` TEXT NOT NULL, `anchor_id` TEXT NOT NULL, `evidence_id` TEXT NOT NULL, PRIMARY KEY(`revision_id`, `anchor_id`, `evidence_id`), FOREIGN KEY(`revision_id`, `anchor_id`) REFERENCES `memory_anchors`(`revision_id`, `anchor_id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`revision_id`, `evidence_id`) REFERENCES `memory_evidence`(`revision_id`, `evidence_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_memory_anchor_evidence_revision_id_anchor_id` ON `memory_anchor_evidence` (`revision_id`, `anchor_id`)",
            "CREATE INDEX IF NOT EXISTS `index_memory_anchor_evidence_revision_id_evidence_id` ON `memory_anchor_evidence` (`revision_id`, `evidence_id`)",
            "CREATE TABLE IF NOT EXISTS `memory_summary_evidence` (`revision_id` TEXT NOT NULL, `evidence_id` TEXT NOT NULL, PRIMARY KEY(`revision_id`, `evidence_id`), FOREIGN KEY(`revision_id`) REFERENCES `memories`(`revision_id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`revision_id`, `evidence_id`) REFERENCES `memory_evidence`(`revision_id`, `evidence_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_memory_summary_evidence_revision_id` ON `memory_summary_evidence` (`revision_id`)",
            "CREATE INDEX IF NOT EXISTS `index_memory_summary_evidence_revision_id_evidence_id` ON `memory_summary_evidence` (`revision_id`, `evidence_id`)",
            "CREATE TABLE IF NOT EXISTS `ai_pack_install_ledger` (`pack_id` TEXT NOT NULL, `capability` TEXT NOT NULL, `installation_state` TEXT NOT NULL, `model_id` TEXT, `model_version` TEXT, `compatible_app_versions` TEXT, `compatible_schema_versions` TEXT, `download_size_bytes` INTEGER, `storage_requirement_bytes` INTEGER, `license` TEXT, `verified_integrity_hash` TEXT, `disclosure_acknowledged_at_epoch_ms` INTEGER, `failure_reason` TEXT, `updated_at_epoch_ms` INTEGER NOT NULL, PRIMARY KEY(`pack_id`))",
            "CREATE TABLE IF NOT EXISTS `memory_embeddings` (`revision_id` TEXT NOT NULL, `memory_id` TEXT NOT NULL, `model_id` TEXT NOT NULL, `model_version` TEXT NOT NULL, `dimensions` INTEGER NOT NULL, `vector_blob` BLOB NOT NULL, `source_text_fingerprint` TEXT NOT NULL, `created_at_epoch_ms` INTEGER NOT NULL, PRIMARY KEY(`revision_id`, `model_id`, `model_version`), FOREIGN KEY(`revision_id`) REFERENCES `memories`(`revision_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_memory_embeddings_memory_id` ON `memory_embeddings` (`memory_id`)",
            "CREATE INDEX IF NOT EXISTS `index_memory_embeddings_model_id_model_version` ON `memory_embeddings` (`model_id`, `model_version`)",
            "CREATE TABLE IF NOT EXISTS `pdf_page_embeddings` (`revision_id` TEXT NOT NULL, `memory_id` TEXT NOT NULL, `page_number` INTEGER NOT NULL, `model_id` TEXT NOT NULL, `model_version` TEXT NOT NULL, `dimensions` INTEGER NOT NULL, `vector_blob` BLOB NOT NULL, `source_text_fingerprint` TEXT NOT NULL, `created_at_epoch_ms` INTEGER NOT NULL, PRIMARY KEY(`revision_id`, `page_number`, `model_id`, `model_version`), FOREIGN KEY(`revision_id`) REFERENCES `memories`(`revision_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_pdf_page_embeddings_memory_id` ON `pdf_page_embeddings` (`memory_id`)",
            "CREATE INDEX IF NOT EXISTS `index_pdf_page_embeddings_model_id_model_version` ON `pdf_page_embeddings` (`model_id`, `model_version`)",
        )

        /**
         * v13 = v12 + `evidence_class` on `memory_evidence` (identity from 13.json).
         */
        val VERSION_THIRTEEN_SCHEMA_SQL = VERSION_TWELVE_SCHEMA_SQL.map { sql ->
            if (sql.startsWith("CREATE TABLE IF NOT EXISTS `memory_evidence`")) {
                "CREATE TABLE IF NOT EXISTS `memory_evidence` (`revision_id` TEXT NOT NULL, `evidence_id` TEXT NOT NULL, `evidence_kind` TEXT NOT NULL, `evidence_class` TEXT NOT NULL, `locator` TEXT NOT NULL, `excerpt` TEXT NOT NULL, PRIMARY KEY(`revision_id`, `evidence_id`), FOREIGN KEY(`revision_id`) REFERENCES `memories`(`revision_id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            } else {
                sql
            }
        }

        /**
         * v14 = v13 + memory_evidence_embeddings (identity from 14.json).
         */
        val VERSION_FOURTEEN_SCHEMA_SQL = VERSION_THIRTEEN_SCHEMA_SQL + listOf(
            "CREATE TABLE IF NOT EXISTS `memory_evidence_embeddings` (`revision_id` TEXT NOT NULL, `memory_id` TEXT NOT NULL, `evidence_id` TEXT NOT NULL, `model_id` TEXT NOT NULL, `model_version` TEXT NOT NULL, `dimensions` INTEGER NOT NULL, `vector_blob` BLOB NOT NULL, `source_text_fingerprint` TEXT NOT NULL, `created_at_epoch_ms` INTEGER NOT NULL, PRIMARY KEY(`revision_id`, `evidence_id`, `model_id`, `model_version`), FOREIGN KEY(`revision_id`) REFERENCES `memories`(`revision_id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_memory_evidence_embeddings_memory_id` ON `memory_evidence_embeddings` (`memory_id`)",
            "CREATE INDEX IF NOT EXISTS `index_memory_evidence_embeddings_model_id_model_version` ON `memory_evidence_embeddings` (`model_id`, `model_version`)",
            "CREATE INDEX IF NOT EXISTS `index_memory_evidence_embeddings_revision_id_evidence_id` ON `memory_evidence_embeddings` (`revision_id`, `evidence_id`)",
        )
    }
}
