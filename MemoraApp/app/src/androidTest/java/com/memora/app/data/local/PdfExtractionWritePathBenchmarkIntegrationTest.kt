package com.memora.app.data.local

import android.content.Context
import android.os.BatteryManager
import android.os.Debug
import android.os.SystemClock
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.security.ConversionElapsedBuckets
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.extraction.ExtractionSchemaVersion
import com.memora.app.domain.extraction.PdfExtractionIntegrity
import com.memora.app.domain.extraction.PdfExtractionPersistenceDecision
import com.memora.app.domain.extraction.PdfExtractionPersistenceFacts
import com.memora.app.domain.extraction.PdfExtractionPersistenceKey
import com.memora.app.domain.extraction.PdfExtractionPersistenceWriteOutcome
import com.memora.app.domain.extraction.PdfExtractionPersistenceWriteRequest
import com.memora.app.domain.extraction.PdfExtractionRecord
import com.memora.app.domain.extraction.PdfExtractionRequest
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
 * Content-free write-path timing/size buckets for durable PDF extraction rows.
 * Synthetic fixtures only; never opens a real PDF or logs page text.
 */
@RunWith(AndroidJUnit4::class)
class PdfExtractionWritePathBenchmarkIntegrationTest {
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
    fun no_text_bucket_persists_within_budget() = runBlocking {
        measureBucket(
            bucketId = "no_text",
            pageCount = 4,
            charsPerPage = 0,
            coverage = PdfTextCoverage.NoExtractableText,
            persistableCoverage = PersistablePdfTextCoverage.NO_EXTRACTABLE_TEXT,
            metadata = emptyMap(),
        )
    }

    @Test
    fun blank_pages_bucket_persists_within_budget() = runBlocking {
        measureBucket(
            bucketId = "blank_pages",
            pageCount = 8,
            charsPerPage = 0,
            coverage = PdfTextCoverage.Complete,
            persistableCoverage = PersistablePdfTextCoverage.COMPLETE,
            metadata = mapOf("Producer" to "MemoraFixture"),
        )
    }

    @Test
    fun dense_text_bucket_persists_within_budget() = runBlocking {
        measureBucket(
            bucketId = "dense_text",
            pageCount = 8,
            charsPerPage = 4_096,
            coverage = PdfTextCoverage.Complete,
            persistableCoverage = PersistablePdfTextCoverage.COMPLETE,
            metadata = mapOf("Author" to "Fixture", "Subject" to "Benchmark"),
        )
    }

    @Test
    fun max_envelope_bucket_persists_within_budget() = runBlocking {
        measureBucket(
            bucketId = "max_envelope",
            pageCount = PdfExtractionWriteBudgets.MAX_PAGE_COUNT,
            charsPerPage = PdfExtractionWriteBudgets.MAX_TOTAL_CHARS /
                PdfExtractionWriteBudgets.MAX_PAGE_COUNT,
            coverage = PdfTextCoverage.Complete,
            persistableCoverage = PersistablePdfTextCoverage.COMPLETE,
            metadata = (1..PdfExtractionWriteBudgets.MAX_METADATA_ENTRIES).associate {
                "k$it" to "v".repeat(16)
            },
        )
    }

    @Test
    fun over_budget_page_count_fails_safely_without_rows() = runBlocking {
        val request = buildWriteRequest(
            fingerprint = "overflow-pages",
            pageCount = PdfExtractionWriteBudgets.MAX_PAGE_COUNT + 1,
            charsPerPage = 1,
            coverage = PdfTextCoverage.Complete,
            persistableCoverage = PersistablePdfTextCoverage.COMPLETE,
            metadata = emptyMap(),
        )
        assertEquals(
            PdfExtractionPersistenceWriteOutcome.FailedSafely,
            port.persist(request),
        )
        assertEquals(0, database.pdfExtractionDao().countForAsset(SOURCE_ID, ASSET_KEY))
    }

    private suspend fun measureBucket(
        bucketId: String,
        pageCount: Int,
        charsPerPage: Int,
        coverage: PdfTextCoverage,
        persistableCoverage: PersistablePdfTextCoverage,
        metadata: Map<String, String>,
    ) {
        val beforeBytes = databaseFileLength()
        val heapBefore = Debug.getNativeHeapAllocatedSize()
        val chargeBefore = readChargeCounterUah()
        val startedAt = SystemClock.elapsedRealtime()

        val outcome = port.persist(
            buildWriteRequest(
                fingerprint = "fp-$bucketId",
                pageCount = pageCount,
                charsPerPage = charsPerPage,
                coverage = coverage,
                persistableCoverage = persistableCoverage,
                metadata = metadata,
            ),
        )
        val elapsedMs = SystemClock.elapsedRealtime() - startedAt
        val afterBytes = databaseFileLength()
        val heapAfter = Debug.getNativeHeapAllocatedSize()
        val chargeAfter = readChargeCounterUah()
        val chargeDelta = when {
            chargeBefore == null || chargeAfter == null -> null
            else -> chargeBefore - chargeAfter
        }

        assertEquals(PdfExtractionPersistenceWriteOutcome.Persisted, outcome)
        assertTrue(
            "Bucket $bucketId elapsed ${elapsedMs}ms exceeds ceiling.",
            elapsedMs <= PdfExtractionWriteBudgets.MAX_PERSIST_ELAPSED_MS,
        )

        Log.i(
            LOG_TAG,
            "bucket=$bucketId; pages=$pageCount; chars_per_page=$charsPerPage; " +
                "metadata_entries=${metadata.size}; " +
                "db_bytes_before=$beforeBytes; db_bytes_after=$afterBytes; " +
                "db_growth_bytes=${afterBytes - beforeBytes}; " +
                "elapsed_ms=$elapsedMs; " +
                "elapsed_bucket=${ConversionElapsedBuckets.forMillis(elapsedMs)}; " +
                "native_heap_delta_bytes=${heapAfter - heapBefore}; " +
                "charge_counter_delta_uah=${chargeDelta?.toString() ?: "unavailable"}",
        )
    }

    private fun buildWriteRequest(
        fingerprint: String,
        pageCount: Int,
        charsPerPage: Int,
        coverage: PdfTextCoverage,
        persistableCoverage: PersistablePdfTextCoverage,
        metadata: Map<String, String>,
    ): PdfExtractionPersistenceWriteRequest {
        val asset = Asset(
            identity = AssetIdentity(SourceId(SOURCE_ID), SourceAssetKey(ASSET_KEY)),
            type = AssetType.PDF,
            location = AssetLocation("content://com.memora.bench/pdf/write-budget"),
            fingerprint = AssetFingerprint(fingerprint),
            discoveredAt = Instant.ofEpochMilli(1_700_000_000_000L),
            displayName = "budget-fixture.pdf",
            sourceModifiedAt = Instant.ofEpochMilli(1_700_000_000_000L),
        )
        val extractionRequest = PdfExtractionRequest(
            asset = asset,
            schemaVersion = ExtractionSchemaVersion(SCHEMA),
        )
        val pages = if (coverage is PdfTextCoverage.NoExtractableText) {
            emptyList()
        } else {
            (1..pageCount).map { pageNumber ->
                PdfPageText(
                    pageNumber = pageNumber,
                    text = "a".repeat(charsPerPage),
                )
            }
        }
        val record = PdfExtractionRecord.forRequest(
            request = extractionRequest,
            title = "BudgetFixture",
            pageCount = pageCount,
            metadata = metadata,
            pages = pages,
            textCoverage = coverage,
            extractedAt = Instant.ofEpochMilli(1_700_000_000_100L),
        )
        val decision = PdfExtractionPersistenceDecision.EligibleForAtomicWrite(
            PdfExtractionPersistenceFacts(
                key = PdfExtractionPersistenceKey.from(extractionRequest),
                pageCount = pageCount,
                coverage = persistableCoverage,
                integrity = PdfExtractionIntegrity.VERIFIED,
                retry = PdfExtractionRetryDirective.NO_RETRY_REQUIRED,
            ),
        )
        return PdfExtractionPersistenceWriteRequest.from(decision, record)
    }

    private fun databaseFileLength(): Long =
        context.getDatabasePath(TEST_DATABASE_NAME).length()

    private fun readChargeCounterUah(): Long? {
        val batteryManager = context.getSystemService(BatteryManager::class.java) ?: return null
        val value = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        return value.takeIf { it >= 0L }
    }

    companion object {
        private const val LOG_TAG = "MemoraPdfWriteBenchmark"
        private const val TEST_DATABASE_NAME = "memora_pdf_write_budget_bench.db"
        private const val SOURCE_ID = "bench-write-source"
        private const val ASSET_KEY = "budget-doc"
        private const val SCHEMA = "pdf-extraction-v1"
    }
}
