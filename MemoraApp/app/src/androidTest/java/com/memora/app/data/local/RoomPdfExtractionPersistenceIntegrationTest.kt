package com.memora.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionPersistenceDecision
import com.memora.app.domain.extraction.PdfExtractionPersistenceFacts
import com.memora.app.domain.extraction.PdfExtractionPersistenceKey
import com.memora.app.domain.extraction.PdfExtractionIntegrity
import com.memora.app.domain.extraction.PdfExtractionPersistenceWriteOutcome
import com.memora.app.domain.extraction.PdfExtractionPersistenceWriteRequest
import com.memora.app.domain.extraction.PdfExtractionRecord
import com.memora.app.domain.extraction.PdfExtractionRetryDirective
import com.memora.app.domain.extraction.PdfPageText
import com.memora.app.domain.extraction.PdfTextCoverage
import com.memora.app.domain.extraction.PersistablePdfTextCoverage
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic-only atomic PDF extraction Room writes (ADR-022 provenance retained).
 * Does not open real PDFs, bind the parser, or touch production UI/workers.
 */
@RunWith(AndroidJUnit4::class)
class RoomPdfExtractionPersistenceIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: MemoraDatabase
    private lateinit var port: RoomPdfExtractionPersistencePort

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DATABASE_NAME)
        database = Room.databaseBuilder(
            context,
            MemoraDatabase::class.java,
            TEST_DATABASE_NAME,
        ).build()
        port = RoomPdfExtractionPersistencePort(database)
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @Test
    fun persists_complete_extraction_atomically_and_is_idempotent() = runBlocking {
        val request = writeRequest(fingerprint = "fp-v1", pageText = "hello page")
        assertEquals(
            PdfExtractionPersistenceWriteOutcome.Persisted,
            port.persist(request),
        )
        assertEquals(
            PdfExtractionPersistenceWriteOutcome.Persisted,
            port.persist(request),
        )

        val header = database.pdfExtractionDao().findHeader(
            SOURCE_ID,
            ASSET_KEY,
            "fp-v1",
            SCHEMA,
        )
        assertEquals(1, header?.pageCount)
        assertEquals(RoomPdfExtractionPersistencePort.COVERAGE_COMPLETE, header?.textCoverage)
        val pages = database.pdfExtractionDao().findPages(SOURCE_ID, ASSET_KEY, "fp-v1", SCHEMA)
        assertEquals(listOf("hello page"), pages.map { it.pageText })
    }

    @Test
    fun retains_superseded_fingerprint_rows_per_adr_022() = runBlocking {
        assertEquals(
            PdfExtractionPersistenceWriteOutcome.Persisted,
            port.persist(writeRequest(fingerprint = "fp-old", pageText = "old")),
        )
        assertEquals(
            PdfExtractionPersistenceWriteOutcome.Persisted,
            port.persist(writeRequest(fingerprint = "fp-new", pageText = "new")),
        )

        assertEquals(2, database.pdfExtractionDao().countForAsset(SOURCE_ID, ASSET_KEY))
        assertEquals(
            "old",
            database.pdfExtractionDao().findPages(SOURCE_ID, ASSET_KEY, "fp-old", SCHEMA)
                .single().pageText,
        )
        assertEquals(
            "new",
            database.pdfExtractionDao().findPages(SOURCE_ID, ASSET_KEY, "fp-new", SCHEMA)
                .single().pageText,
        )
    }

    @Test
    fun conflicting_payload_for_same_key_fails_safely_without_overwrite() = runBlocking {
        val first = writeRequest(fingerprint = "fp-same", pageText = "original")
        assertEquals(PdfExtractionPersistenceWriteOutcome.Persisted, port.persist(first))

        val conflict = writeRequest(fingerprint = "fp-same", pageText = "tampered")
        assertEquals(
            PdfExtractionPersistenceWriteOutcome.FailedSafely,
            port.persist(conflict),
        )
        assertEquals(
            "original",
            database.pdfExtractionDao().findPages(SOURCE_ID, ASSET_KEY, "fp-same", SCHEMA)
                .single().pageText,
        )
    }

    @Test
    fun delete_all_clears_header_pages_and_metadata() = runBlocking {
        assertEquals(
            PdfExtractionPersistenceWriteOutcome.Persisted,
            port.persist(
                writeRequest(
                    fingerprint = "fp-clear",
                    pageText = "body",
                    metadata = mapOf("Author" to "Fixture"),
                ),
            ),
        )
        assertTrue(
            database.pdfExtractionDao().findMetadata(SOURCE_ID, ASSET_KEY, "fp-clear", SCHEMA)
                .isNotEmpty(),
        )

        database.pdfExtractionDao().deleteAll()
        assertEquals(0, database.pdfExtractionDao().countForAsset(SOURCE_ID, ASSET_KEY))
        assertTrue(
            database.pdfExtractionDao().findPages(SOURCE_ID, ASSET_KEY, "fp-clear", SCHEMA).isEmpty(),
        )
        assertTrue(
            database.pdfExtractionDao()
                .findMetadata(SOURCE_ID, ASSET_KEY, "fp-clear", SCHEMA)
                .isEmpty(),
        )
    }

    private fun writeRequest(
        fingerprint: String,
        pageText: String,
        metadata: Map<String, String> = emptyMap(),
    ): PdfExtractionPersistenceWriteRequest {
        val asset = Asset(
            identity = AssetIdentity(SourceId(SOURCE_ID), SourceAssetKey(ASSET_KEY)),
            type = AssetType.PDF,
            location = AssetLocation("content://com.memora.bench/pdf/1"),
            fingerprint = AssetFingerprint(fingerprint),
            discoveredAt = Instant.ofEpochMilli(1_700_000_000_000L),
            displayName = "fixture.pdf",
            sourceModifiedAt = Instant.ofEpochMilli(1_700_000_000_000L),
        )
        val extractionRequest = com.memora.app.domain.extraction.PdfExtractionRequest(
            asset = asset,
            schemaVersion = ExtractionSchemaVersion(SCHEMA),
        )
        val record = PdfExtractionRecord.forRequest(
            request = extractionRequest,
            title = "Fixture",
            pageCount = 1,
            metadata = metadata,
            pages = listOf(PdfPageText(pageNumber = 1, text = pageText)),
            textCoverage = PdfTextCoverage.Complete,
            extractedAt = Instant.ofEpochMilli(1_700_000_000_100L),
        )
        val decision = PdfExtractionPersistenceDecision.EligibleForAtomicWrite(
            PdfExtractionPersistenceFacts(
                key = PdfExtractionPersistenceKey.from(extractionRequest),
                pageCount = 1,
                coverage = PersistablePdfTextCoverage.COMPLETE,
                integrity = PdfExtractionIntegrity.VERIFIED,
                retry = PdfExtractionRetryDirective.NO_RETRY_REQUIRED,
            ),
        )
        return PdfExtractionPersistenceWriteRequest.from(decision, record)
    }

    companion object {
        private const val TEST_DATABASE_NAME = "memora_pdf_extraction_persistence_test.db"
        private const val SOURCE_ID = "bench-pdf-source"
        private const val ASSET_KEY = "doc-1"
        private const val SCHEMA = "pdf-extraction-v1"
    }
}
