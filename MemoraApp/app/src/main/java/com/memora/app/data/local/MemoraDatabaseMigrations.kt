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

    val MIGRATION_7_8: Migration = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `memories` (
                    `revision_id` TEXT NOT NULL,
                    `memory_id` TEXT NOT NULL,
                    `source_id` TEXT NOT NULL,
                    `source_asset_key` TEXT NOT NULL,
                    `fingerprint` TEXT NOT NULL,
                    `assembly_schema_version` TEXT NOT NULL,
                    `integrity_state` TEXT NOT NULL,
                    `summary_text` TEXT NOT NULL,
                    `created_at_epoch_millis` INTEGER NOT NULL,
                    `updated_at_epoch_millis` INTEGER NOT NULL,
                    PRIMARY KEY(`revision_id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_memory_id` ON `memories` (`memory_id`)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_memories_source_id_source_asset_key_fingerprint_assembly_schema_version` " +
                    "ON `memories` (`source_id`, `source_asset_key`, `fingerprint`, `assembly_schema_version`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_memories_source_id_source_asset_key` " +
                    "ON `memories` (`source_id`, `source_asset_key`)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_fingerprint` ON `memories` (`fingerprint`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `memory_extraction_schemas` (
                    `revision_id` TEXT NOT NULL,
                    `schema_version` TEXT NOT NULL,
                    PRIMARY KEY(`revision_id`, `schema_version`),
                    FOREIGN KEY(`revision_id`) REFERENCES `memories`(`revision_id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_memory_extraction_schemas_revision_id` " +
                    "ON `memory_extraction_schemas` (`revision_id`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `memory_evidence` (
                    `revision_id` TEXT NOT NULL,
                    `evidence_id` TEXT NOT NULL,
                    `evidence_kind` TEXT NOT NULL,
                    `locator` TEXT NOT NULL,
                    `excerpt` TEXT NOT NULL,
                    PRIMARY KEY(`revision_id`, `evidence_id`),
                    FOREIGN KEY(`revision_id`) REFERENCES `memories`(`revision_id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_memory_evidence_revision_id` " +
                    "ON `memory_evidence` (`revision_id`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `memory_anchors` (
                    `revision_id` TEXT NOT NULL,
                    `anchor_id` TEXT NOT NULL,
                    `anchor_kind` TEXT NOT NULL,
                    `anchor_text` TEXT NOT NULL,
                    PRIMARY KEY(`revision_id`, `anchor_id`),
                    FOREIGN KEY(`revision_id`) REFERENCES `memories`(`revision_id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_memory_anchors_revision_id` " +
                    "ON `memory_anchors` (`revision_id`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `memory_anchor_evidence` (
                    `revision_id` TEXT NOT NULL,
                    `anchor_id` TEXT NOT NULL,
                    `evidence_id` TEXT NOT NULL,
                    PRIMARY KEY(`revision_id`, `anchor_id`, `evidence_id`),
                    FOREIGN KEY(`revision_id`, `anchor_id`)
                        REFERENCES `memory_anchors`(`revision_id`, `anchor_id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`revision_id`, `evidence_id`)
                        REFERENCES `memory_evidence`(`revision_id`, `evidence_id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_memory_anchor_evidence_revision_id_anchor_id` " +
                    "ON `memory_anchor_evidence` (`revision_id`, `anchor_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_memory_anchor_evidence_revision_id_evidence_id` " +
                    "ON `memory_anchor_evidence` (`revision_id`, `evidence_id`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `memory_summary_evidence` (
                    `revision_id` TEXT NOT NULL,
                    `evidence_id` TEXT NOT NULL,
                    PRIMARY KEY(`revision_id`, `evidence_id`),
                    FOREIGN KEY(`revision_id`) REFERENCES `memories`(`revision_id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`revision_id`, `evidence_id`)
                        REFERENCES `memory_evidence`(`revision_id`, `evidence_id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_memory_summary_evidence_revision_id` " +
                    "ON `memory_summary_evidence` (`revision_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_memory_summary_evidence_revision_id_evidence_id` " +
                    "ON `memory_summary_evidence` (`revision_id`, `evidence_id`)",
            )
        }
    }

    val MIGRATION_8_9: Migration = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `note_page_extractions` (
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
                "CREATE INDEX IF NOT EXISTS `index_note_page_extractions_source_id_source_asset_key` " +
                    "ON `note_page_extractions` (`source_id`, `source_asset_key`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_note_page_extractions_fingerprint` " +
                    "ON `note_page_extractions` (`fingerprint`)",
            )
        }
    }

    val MIGRATION_9_10: Migration = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ai_pack_install_ledger` (
                    `pack_id` TEXT NOT NULL,
                    `capability` TEXT NOT NULL,
                    `installation_state` TEXT NOT NULL,
                    `model_id` TEXT,
                    `model_version` TEXT,
                    `compatible_app_versions` TEXT,
                    `compatible_schema_versions` TEXT,
                    `download_size_bytes` INTEGER,
                    `storage_requirement_bytes` INTEGER,
                    `license` TEXT,
                    `verified_integrity_hash` TEXT,
                    `disclosure_acknowledged_at_epoch_ms` INTEGER,
                    `failure_reason` TEXT,
                    `updated_at_epoch_ms` INTEGER NOT NULL,
                    PRIMARY KEY(`pack_id`)
                )
                """.trimIndent(),
            )
        }
    }

    val MIGRATION_10_11: Migration = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `memory_embeddings` (
                    `revision_id` TEXT NOT NULL,
                    `memory_id` TEXT NOT NULL,
                    `model_id` TEXT NOT NULL,
                    `model_version` TEXT NOT NULL,
                    `dimensions` INTEGER NOT NULL,
                    `vector_blob` BLOB NOT NULL,
                    `source_text_fingerprint` TEXT NOT NULL,
                    `created_at_epoch_ms` INTEGER NOT NULL,
                    PRIMARY KEY(`revision_id`, `model_id`, `model_version`),
                    FOREIGN KEY(`revision_id`) REFERENCES `memories`(`revision_id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_memory_embeddings_memory_id` " +
                    "ON `memory_embeddings` (`memory_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_memory_embeddings_model_id_model_version` " +
                    "ON `memory_embeddings` (`model_id`, `model_version`)",
            )
        }
    }
}
