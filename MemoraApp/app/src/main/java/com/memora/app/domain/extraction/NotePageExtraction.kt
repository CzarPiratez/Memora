package com.memora.app.domain.extraction

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetType

/** Schema id for deterministic OneNote page plain text (not keyword search). */
@JvmInline
value class NotePageSchemaVersion(val value: String) {
    init {
        require(value.isNotBlank()) { "Note page schema version cannot be blank." }
    }

    companion object {
        val V1 = NotePageSchemaVersion("onenote-page-text-v1")
    }
}

/**
 * One durable plain-text record for a discovered NOTE page.
 *
 * Empty [fullText] is a valid completed extract (no readable text found).
 * Does not claim keyword search or Memory ranking (N5+).
 */
data class NotePageExtractionRecord(
    val asset: Asset,
    val schemaVersion: NotePageSchemaVersion,
    val fullText: String,
    val textTruncated: Boolean,
    val engineId: String,
    val engineVersion: String,
    val extractedAtEpochMillis: Long,
    val integrity: String = INTEGRITY_VERIFIED,
) {
    init {
        require(asset.type == AssetType.NOTE) {
            "Note page extract only applies to NOTE assets."
        }
        require(engineId.isNotBlank())
        require(engineVersion.isNotBlank())
        require(integrity.isNotBlank())
        require(fullText.length <= MAX_STORED_CHARS) {
            "Stored note text must already be truncated to $MAX_STORED_CHARS characters."
        }
    }

    companion object {
        const val INTEGRITY_VERIFIED = "VERIFIED"
        const val MAX_STORED_CHARS = 50_000
        const val ENGINE_GRAPH_HTML_PLAINTEXT = "onenote-graph-html-plaintext"
        const val ENGINE_VERSION_V1 = "v1"
    }
}

/** Outcome of fetching OneNote page HTML for plain-text extract. */
sealed interface NotePageReadResult {
    data class Text(
        val fullText: String,
        val textTruncated: Boolean,
        val engineId: String,
        val engineVersion: String,
    ) : NotePageReadResult

    data object AccessStopped : NotePageReadResult

    data object RetryableFailure : NotePageReadResult
}

/** Persistence boundary for durable note page extraction rows. */
interface NotePageExtractionPersistence {
    suspend fun findHeader(record: NotePageExtractionRecord): NotePageExtractionRecord?

    suspend fun insert(record: NotePageExtractionRecord)

    suspend fun countCurrentForSource(sourceId: String, schemaVersion: String): Int
}
