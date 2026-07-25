package com.memora.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.application.documents.PdfKeywordSearchOutcome
import com.memora.app.application.documents.PdfKeywordSearchSupport
import com.memora.app.application.documents.SearchPersistedPdfPageText
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.IndexingState
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionIntegrity
import com.memora.app.domain.extraction.PdfExtractionPersistenceDecision
import com.memora.app.domain.extraction.PdfExtractionPersistenceFacts
import com.memora.app.domain.extraction.PdfExtractionPersistenceKey
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

@RunWith(AndroidJUnit4::class)
class PdfExtractionKeywordSearchIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: MemoraDatabase

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DATABASE_NAME)
        database = Room.databaseBuilder(
            context,
            MemoraDatabase::class.java,
            TEST_DATABASE_NAME,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(TEST_DATABASE_NAME)
    }

    @Test
    fun finds_current_fingerprint_page_and_ignores_superseded() = runBlocking {
        val currentFingerprint = "fp-current"
        val oldFingerprint = "fp-old"
        RoomAssetRepository { database.assetDao() }.save(
            AssetIndexRecord(
                asset = Asset(
                    identity = AssetIdentity(SourceId(SOURCE_ID), SourceAssetKey(ASSET_KEY)),
                    type = AssetType.PDF,
                    location = AssetLocation("content://tree/doc"),
                    fingerprint = AssetFingerprint(currentFingerprint),
                    discoveredAt = Instant.EPOCH,
                    displayName = "Receipt.pdf",
                ),
                indexingState = IndexingState.discovered,
            ),
        )

        val port = RoomPdfExtractionPersistencePort(database)
        port.persist(writeRequest(fingerprint = oldFingerprint, pageText = "old cafe receipt"))
        port.persist(writeRequest(fingerprint = currentFingerprint, pageText = "current cafe receipt"))

        val outcome = SearchPersistedPdfPageText(database)("cafe")
        require(outcome is PdfKeywordSearchOutcome.Matches)
        assertEquals(1, outcome.hits.size)
        assertEquals("Receipt.pdf", outcome.hits.single().label)
        assertEquals(1, outcome.hits.single().pageNumber)
        assertTrue(outcome.hits.single().excerpt.contains("cafe", ignoreCase = true))
        assertTrue(outcome.hits.single().excerpt.contains("current", ignoreCase = true))
    }

    @Test
    fun blank_query_is_rejected() = runBlocking {
        assertEquals(
            PdfKeywordSearchOutcome.BlankQuery,
            SearchPersistedPdfPageText(database)("   "),
        )
    }

    private fun writeRequest(fingerprint: String, pageText: String) =
        PdfExtractionPersistenceWriteRequest.from(
            decision = PdfExtractionPersistenceDecision.EligibleForAtomicWrite(
                PdfExtractionPersistenceFacts(
                    key = PdfExtractionPersistenceKey(
                        assetIdentity = AssetIdentity(SourceId(SOURCE_ID), SourceAssetKey(ASSET_KEY)),
                        assetFingerprint = AssetFingerprint(fingerprint),
                        schemaVersion = ExtractionSchemaVersion(PdfKeywordSearchSupport.SCHEMA_VERSION),
                    ),
                    pageCount = 1,
                    coverage = PersistablePdfTextCoverage.COMPLETE,
                    integrity = PdfExtractionIntegrity.VERIFIED,
                    retry = PdfExtractionRetryDirective.NO_RETRY_REQUIRED,
                ),
            ),
            record = PdfExtractionRecord.forRequest(
                request = com.memora.app.domain.extraction.PdfExtractionRequest(
                    asset = Asset(
                        identity = AssetIdentity(SourceId(SOURCE_ID), SourceAssetKey(ASSET_KEY)),
                        type = AssetType.PDF,
                        location = AssetLocation("content://tree/doc"),
                        fingerprint = AssetFingerprint(fingerprint),
                        discoveredAt = Instant.EPOCH,
                    ),
                    schemaVersion = ExtractionSchemaVersion(PdfKeywordSearchSupport.SCHEMA_VERSION),
                ),
                pageCount = 1,
                pages = listOf(PdfPageText(pageNumber = 1, text = pageText)),
                textCoverage = PdfTextCoverage.Complete,
                extractedAt = Instant.EPOCH,
            ),
        )

    private companion object {
        const val TEST_DATABASE_NAME = "memora-pdf-keyword-search-test.db"
        const val SOURCE_ID = "saf-pdf-folder"
        const val ASSET_KEY = "doc-1"
    }
}
