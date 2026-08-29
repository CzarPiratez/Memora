package com.memora.app.application.intelligence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.local.MemoraDatabase
import com.memora.app.data.local.MemoryEntity
import com.memora.app.data.local.MemoryEvidenceEntity
import com.memora.app.data.local.RoomMemoryEvidenceEmbeddingStore
import com.memora.app.data.local.RoomPdfPageEmbeddingStore
import com.memora.app.domain.intelligence.CapabilityAvailability
import com.memora.app.domain.intelligence.CapabilityLimits
import com.memora.app.domain.intelligence.EmbeddingEncodeResult
import com.memora.app.domain.intelligence.EmbeddingEngine
import com.memora.app.domain.intelligence.EmbeddingVector
import com.memora.app.domain.intelligence.MemoryEvidenceEmbeddingStore
import com.memora.app.domain.intelligence.ModelVersionIdentity
import com.memora.app.domain.intelligence.PdfPageEmbeddingStore
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryId
import com.memora.app.domain.memory.MemoryRevisionId
import com.memora.app.domain.memory.PdfPageEvidenceLocator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MIG-05 pre-step-3 gate: device/Room proof that [IndexPdfPageEmbeddings]
 * dual-writes into [MemoryEvidenceEmbeddingStore] with a real evidence id
 * (`e{n}`), while still writing [PdfPageEmbeddingStore].
 *
 * Does **not** cut over Search (still on PdfPageEmbedding path). No Room bump.
 */
@RunWith(AndroidJUnit4::class)
class IndexPdfPageEmbeddingsDualWriteInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: MemoraDatabase
    private lateinit var pageStore: PdfPageEmbeddingStore
    private lateinit var evidenceStore: MemoryEvidenceEmbeddingStore

    private val revisionId = MemoryRevisionId("rev-dual-write-e2e")
    private val memoryId = MemoryId("mem-dual-write-e2e")
    private val evidenceId = MemoryEvidenceId("e3")
    private val pageNumber = 2
    private val pageText = "invoice total due on page two"
    private val fixedVector = EmbeddingVector(floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f))
    private val model = ModelVersionIdentity(modelId = "dual-write-test-embed", version = "0.0.1")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, MemoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        assertEquals(14, database.openHelper.readableDatabase.version)
        pageStore = RoomPdfPageEmbeddingStore(dao = { database.pdfPageEmbeddingDao() })
        evidenceStore = RoomMemoryEvidenceEmbeddingStore(
            dao = { database.memoryEvidenceEmbeddingDao() },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun dual_write_persists_page_and_evidence_rows_with_real_evidence_id() = runBlocking {
        seedMemoryWithPdfPageEvidence(evidenceId = evidenceId.value, locatorPage = pageNumber)

        val engine = FixedAvailableEmbeddingEngine(model = model, vector = fixedVector)
        val useCase = IndexPdfPageEmbeddings(engine, pageStore, evidenceStore)

        val result = useCase(
            candidates = listOf(
                PdfPageEmbeddingCandidate(
                    revisionId = revisionId,
                    memoryId = memoryId,
                    pageNumber = pageNumber,
                    pageText = pageText,
                    evidenceId = evidenceId,
                ),
            ),
            nowEpochMs = 1_720_000_000_000L,
        )

        assertTrue(result is IndexPdfPageEmbeddingsResult.Completed)
        val completed = result as IndexPdfPageEmbeddingsResult.Completed
        assertEquals(1, completed.indexed)
        assertEquals(0, completed.unresolvedEvidence)
        assertEquals(1, completed.evidenceDualWrites)

        val page = pageStore.find(revisionId, pageNumber, model)
        assertNotNull(page)
        assertEquals(memoryId, page!!.memoryId)
        assertEquals(fixedVector.values.toList(), page.vector.values.toList())

        val evidence = evidenceStore.find(revisionId, evidenceId, model)
        assertNotNull(evidence)
        assertEquals(evidenceId, evidence!!.evidenceId)
        assertEquals("e3", evidence.evidenceId.value)
        assertNull(PdfPageEvidenceLocator.parsePageNumber(evidence.evidenceId.value))
        assertEquals(page.vector.values.toList(), evidence.vector.values.toList())
        assertEquals(page.sourceTextFingerprint, evidence.sourceTextFingerprint)
        assertEquals(page.model, evidence.model)

        // Direct Room DAO proof (same schema tables Search does not yet read).
        assertEquals(1, database.pdfPageEmbeddingDao().countForModel(model.modelId, model.version))
        assertEquals(
            1,
            database.memoryEvidenceEmbeddingDao().countForModel(model.modelId, model.version),
        )
        val evidenceEntity = database.memoryEvidenceEmbeddingDao().find(
            revisionId = revisionId.value,
            evidenceId = evidenceId.value,
            modelId = model.modelId,
            modelVersion = model.version,
        )
        assertNotNull(evidenceEntity)
        assertEquals("e3", evidenceEntity!!.evidenceId)
        assertFalse(evidenceEntity.evidenceId.startsWith("pdf:page:"))
    }

    @Test
    fun unresolved_evidence_id_writes_page_store_only() = runBlocking {
        seedMemoryWithPdfPageEvidence(evidenceId = "e9", locatorPage = 9)

        val engine = FixedAvailableEmbeddingEngine(model = model, vector = fixedVector)
        val useCase = IndexPdfPageEmbeddings(engine, pageStore, evidenceStore)

        val result = useCase(
            candidates = listOf(
                PdfPageEmbeddingCandidate(
                    revisionId = revisionId,
                    memoryId = memoryId,
                    pageNumber = 9,
                    pageText = "orphan page without resolved id",
                    evidenceId = null,
                ),
            ),
            nowEpochMs = 1_720_000_000_100L,
        )

        assertTrue(result is IndexPdfPageEmbeddingsResult.Completed)
        val completed = result as IndexPdfPageEmbeddingsResult.Completed
        assertEquals(1, completed.indexed)
        assertEquals(1, completed.unresolvedEvidence)
        assertEquals(0, completed.evidenceDualWrites)

        assertNotNull(pageStore.find(revisionId, pageNumber = 9, model = model))
        assertEquals(1, pageStore.countForModel(model))
        assertEquals(0, evidenceStore.countForModel(model))
        assertNull(evidenceStore.find(revisionId, MemoryEvidenceId("e9"), model))
        assertEquals(
            0,
            database.memoryEvidenceEmbeddingDao().countForModel(model.modelId, model.version),
        )
    }

    @Test
    fun search_by_meaning_constructor_still_depends_on_pdf_page_store_not_evidence_store() {
        // Pre-step-3 gate: Search must remain on PdfPageEmbedding path (no cutover).
        val constructors = SearchAssetMemoriesByMeaning::class.java.declaredConstructors
        assertTrue(constructors.isNotEmpty())
        val parameterTypes = constructors.flatMap { it.parameterTypes.toList() }.toSet()
        assertTrue(
            "SearchAssetMemoriesByMeaning must still take PdfPageEmbeddingStore",
            parameterTypes.any { PdfPageEmbeddingStore::class.java.isAssignableFrom(it) },
        )
        assertFalse(
            "SearchAssetMemoriesByMeaning must not yet take MemoryEvidenceEmbeddingStore",
            parameterTypes.any { MemoryEvidenceEmbeddingStore::class.java.isAssignableFrom(it) },
        )
    }

    private suspend fun seedMemoryWithPdfPageEvidence(
        evidenceId: String,
        locatorPage: Int,
    ) {
        database.memoryDao().insertHeader(
            MemoryEntity(
                revisionId = revisionId.value,
                memoryId = memoryId.value,
                sourceId = "saf-pdf-test",
                sourceAssetKey = "tree:dual-write.pdf",
                fingerprint = "fp-dual-write-1",
                assemblySchemaVersion = "asset-memory-facts-v4",
                integrityState = "READY",
                summaryText = "Dual-write device fixture",
                createdAtEpochMillis = 1_720_000_000_000L,
                updatedAtEpochMillis = 1_720_000_000_000L,
            ),
        )
        database.memoryDao().insertEvidence(
            listOf(
                MemoryEvidenceEntity(
                    revisionId = revisionId.value,
                    evidenceId = evidenceId,
                    evidenceKind = "DOCUMENT_TEXT",
                    evidenceClass = "DIRECT",
                    locator = PdfPageEvidenceLocator.formatLocator(locatorPage),
                    excerpt = pageText,
                ),
            ),
        )
        val seeded = database.memoryDao().findEvidence(revisionId.value).single()
        assertEquals(evidenceId, seeded.evidenceId)
        assertEquals(PdfPageEvidenceLocator.formatLocator(locatorPage), seeded.locator)
        assertNull(PdfPageEvidenceLocator.parsePageNumber(seeded.evidenceId))
    }

    private class FixedAvailableEmbeddingEngine(
        private val model: ModelVersionIdentity,
        private val vector: EmbeddingVector,
    ) : EmbeddingEngine {
        override fun availability(): CapabilityAvailability =
            CapabilityAvailability.Available(model)

        override fun limits(): CapabilityLimits? = null

        override fun embedText(text: String): EmbeddingEncodeResult {
            require(text.isNotBlank())
            return EmbeddingEncodeResult.Success(vector, model)
        }
    }
}
