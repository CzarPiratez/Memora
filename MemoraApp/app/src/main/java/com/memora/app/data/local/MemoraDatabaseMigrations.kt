package com.memora.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Explicit, non-destructive schema changes for durable Memora-owned data. */
object MemoraDatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `discovery_checkpoints` (
                    `source_id` TEXT NOT NULL,
                    `cursor_value` TEXT NOT NULL,
                    `saved_at_epoch_millis` INTEGER NOT NULL,
                    PRIMARY KEY(`source_id`)
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `document_tree_approvals` (
                    `source_id` TEXT NOT NULL,
                    `tree_uri` TEXT NOT NULL,
                    `approved_at_epoch_millis` INTEGER NOT NULL,
                    PRIMARY KEY(`source_id`)
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pdf_extractions` (
                    `source_id` TEXT NOT NULL,
                    `source_asset_key` TEXT NOT NULL,
                    `fingerprint` TEXT NOT NULL,
                    `schema_version` TEXT NOT NULL,
                    `page_count` INTEGER NOT NULL,
                    `text_coverage` TEXT NOT NULL,
                    `title` TEXT,
                    `extracted_at_epoch_millis` INTEGER NOT NULL,
                    `created_at_epoch_millis` INTEGER NOT NULL,
                    `integrity` TEXT NOT NULL,
                    PRIMARY KEY(
                        `source_id`,
                        `source_asset_key`,
                        `fingerprint`,
                        `schema_version`
                    )
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_pdf_extractions_source_id_source_asset_key` " +
                    "ON `pdf_extractions` (`source_id`, `source_asset_key`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_pdf_extractions_fingerprint` " +
                    "ON `pdf_extractions` (`fingerprint`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pdf_extraction_pages` (
                    `source_id` TEXT NOT NULL,
                    `source_asset_key` TEXT NOT NULL,
                    `fingerprint` TEXT NOT NULL,
                    `schema_version` TEXT NOT NULL,
                    `page_number` INTEGER NOT NULL,
                    `page_text` TEXT NOT NULL,
                    PRIMARY KEY(
                        `source_id`,
                        `source_asset_key`,
                        `fingerprint`,
                        `schema_version`,
                        `page_number`
                    ),
                    FOREIGN KEY(
                        `source_id`,
                        `source_asset_key`,
                        `fingerprint`,
                        `schema_version`
                    ) REFERENCES `pdf_extractions` (
                        `source_id`,
                        `source_asset_key`,
                        `fingerprint`,
                        `schema_version`
                    ) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_pdf_extraction_pages_source_id_source_asset_key_fingerprint_schema_version` " +
                    "ON `pdf_extraction_pages` " +
                    "(`source_id`, `source_asset_key`, `fingerprint`, `schema_version`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `pdf_extraction_metadata` (
                    `source_id` TEXT NOT NULL,
                    `source_asset_key` TEXT NOT NULL,
                    `fingerprint` TEXT NOT NULL,
                    `schema_version` TEXT NOT NULL,
                    `metadata_name` TEXT NOT NULL,
                    `metadata_value` TEXT NOT NULL,
                    PRIMARY KEY(
                        `source_id`,
                        `source_asset_key`,
                        `fingerprint`,
                        `schema_version`,
                        `metadata_name`
                    ),
                    FOREIGN KEY(
                        `source_id`,
                        `source_asset_key`,
                        `fingerprint`,
                        `schema_version`
                    ) REFERENCES `pdf_extractions` (
                        `source_id`,
                        `source_asset_key`,
                        `fingerprint`,
                        `schema_version`
                    ) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_pdf_extraction_metadata_source_id_source_asset_key_fingerprint_schema_version` " +
                    "ON `pdf_extraction_metadata` " +
                    "(`source_id`, `source_asset_key`, `fingerprint`, `schema_version`)",
            )
        }
    }

    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `image_exif_extractions` (
                    `source_id` TEXT NOT NULL,
                    `source_asset_key` TEXT NOT NULL,
                    `fingerprint` TEXT NOT NULL,
                    `schema_version` TEXT NOT NULL,
                    `asset_kind` TEXT NOT NULL,
                    `datetime_original` TEXT,
                    `image_width` INTEGER,
                    `image_height` INTEGER,
                    `orientation` INTEGER,
                    `make` TEXT,
                    `model` TEXT,
                    `extracted_at_epoch_millis` INTEGER NOT NULL,
                    `created_at_epoch_millis` INTEGER NOT NULL,
                    `integrity` TEXT NOT NULL,
                    PRIMARY KEY(
                        `source_id`,
                        `source_asset_key`,
                        `fingerprint`,
                        `schema_version`
                    )
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_image_exif_extractions_source_id_source_asset_key` " +
                    "ON `image_exif_extractions` (`source_id`, `source_asset_key`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_image_exif_extractions_fingerprint` " +
                    "ON `image_exif_extractions` (`fingerprint`)",
            )
        }
    }

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `screenshot_ocr_extractions` (
                    `source_id` TEXT NOT NULL,
                    `source_asset_key` TEXT NOT NULL,
                    `fingerprint` TEXT NOT NULL,
                    `schema_version` TEXT NOT NULL,
                    `full_text` TEXT NOT NULL,
                    `text_truncated` INTEGER NOT NULL,
                    `engine_id` TEXT NOT NULL,
                    `engine_version` TEXT NOT NULL,
                    `extracted_at_epoch_millis` INTEGER NOT NULL,
                    `created_at_epoch_millis` INTEGER NOT NULL,
                    `integrity` TEXT NOT NULL,
                    PRIMARY KEY(
                        `source_id`,
                        `source_asset_key`,
                        `fingerprint`,
                        `schema_version`
                    )
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_screenshot_ocr_extractions_source_id_source_asset_key` " +
                    "ON `screenshot_ocr_extractions` (`source_id`, `source_asset_key`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_screenshot_ocr_extractions_fingerprint` " +
                    "ON `screenshot_ocr_extractions` (`fingerprint`)",
            )
        }
    }

    val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `photo_ocr_extractions` (
                    `source_id` TEXT NOT NULL,
                    `source_asset_key` TEXT NOT NULL,
                    `fingerprint` TEXT NOT NULL,
                    `schema_version` TEXT NOT NULL,
                    `full_text` TEXT NOT NULL,
                    `text_truncated` INTEGER NOT NULL,
                    `engine_id` TEXT NOT NULL,
                    `engine_version` TEXT NOT NULL,
                    `extracted_at_epoch_millis` INTEGER NOT NULL,
                    `created_at_epoch_millis` INTEGER NOT NULL,
                    `integrity` TEXT NOT NULL,
                    PRIMARY KEY(
                        `source_id`,
                        `source_asset_key`,
                        `fingerprint`,
                        `schema_version`
                    )
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_photo_ocr_extractions_source_id_source_asset_key` " +
                    "ON `photo_ocr_extractions` (`source_id`, `source_asset_key`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_photo_ocr_extractions_fingerprint` " +
                    "ON `photo_ocr_extractions` (`fingerprint`)",
            )
        }
    }
}
