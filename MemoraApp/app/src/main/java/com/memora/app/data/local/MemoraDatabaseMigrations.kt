package com.memora.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Explicit, non-destructive schema changes for durable Memora-owned data. */
object MemoraDatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
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
}
