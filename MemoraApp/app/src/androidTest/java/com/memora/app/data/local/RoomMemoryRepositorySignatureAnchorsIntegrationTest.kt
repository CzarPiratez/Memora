package com.memora.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.domain.memory.MemoryAnchorKind
import com.memora.app.domain.memory.MemoryEvidenceId
import com.memora.app.domain.memory.MemoryRevisionId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression: [RoomMemoryRepository.findSignatureAnchors] must load cited evidence
 * ids from [memory_anchor_evidence]. Empty evidence sets violate [MemoryAnchor]
 * and crashed Find-by-meaning for typical ≥4-char queries (advisory TOPIC).
 */
@RunWith(AndroidJUnit4::class)
class RoomMemoryRepositorySignatureAnchorsIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: MemoraDatabase
    private lateinit var repository: RoomMemoryRepository

    private val revisionId = MemoryRevisionId("rev-anchor-integration")
    private val evidenceId = MemoryEvidenceId("e-topic-1")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, MemoraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomMemoryRepository(database = { database })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun findSignatureAnchors_loads_cited_evidence_for_valid_rows() = runBlocking {
        seedMemoryWithTopicAnchor(
            anchorId = "anchor-topic",
            anchorKind = MemoryAnchorKind.TOPIC.name,
            anchorText = "memora-open-5page.pdf",
            linkEvidence = true,
        )

        val anchors = repository.findSignatureAnchors(listOf(revisionId))

        val topic = anchors.getValue(revisionId).single()
        assertEquals(MemoryAnchorKind.TOPIC, topic.kind)
        assertEquals("memora-open-5page.pdf", topic.text.value)
        assertEquals(setOf(evidenceId), topic.evidenceIds)
    }

    @Test
    fun findSignatureAnchors_omits_anchors_without_evidence_rows_instead_of_throwing() =
        runBlocking {
            seedMemoryWithTopicAnchor(
                anchorId = "anchor-orphan",
                anchorKind = MemoryAnchorKind.TOPIC.name,
                anchorText = "orphan topic",
                linkEvidence = false,
            )

            val anchors = repository.findSignatureAnchors(listOf(revisionId))

            assertTrue(anchors.isEmpty())
        }

    @Test
    fun findSignatureAnchors_skips_unknown_anchor_kinds() = runBlocking {
        seedMemoryWithTopicAnchor(
            anchorId = "anchor-bad-kind",
            anchorKind = "NOT_A_REAL_KIND",
            anchorText = "ignored",
            linkEvidence = true,
        )

        val anchors = repository.findSignatureAnchors(listOf(revisionId))

        assertTrue(anchors.isEmpty())
    }

    private suspend fun seedMemoryWithTopicAnchor(
        anchorId: String,
        anchorKind: String,
        anchorText: String,
        linkEvidence: Boolean,
    ) {
        val revision = revisionId.value
        database.memoryDao().insertHeader(
            MemoryEntity(
                revisionId = revision,
                memoryId = "mem-anchor-integration",
                sourceId = "saf-pdf-test",
                sourceAssetKey = "tree:invoice.pdf",
                fingerprint = "fp-anchor-integration",
                assemblySchemaVersion = "asset-memory-facts-v4",
                integrityState = "READY",
                summaryText = "Invoice summary for integration fixture",
                createdAtEpochMillis = 1_720_000_000_000L,
                updatedAtEpochMillis = 1_720_000_000_000L,
            ),
        )
        database.memoryDao().insertEvidence(
            listOf(
                MemoryEvidenceEntity(
                    revisionId = revision,
                    evidenceId = evidenceId.value,
                    evidenceKind = "DOCUMENT_TEXT",
                    evidenceClass = "DIRECT",
                    locator = "pdf:title",
                    excerpt = "Title mentions invoice",
                ),
            ),
        )
        database.memoryDao().insertAnchors(
            listOf(
                MemoryAnchorEntity(
                    revisionId = revision,
                    anchorId = anchorId,
                    anchorKind = anchorKind,
                    anchorText = anchorText,
                ),
            ),
        )
        if (linkEvidence) {
            database.memoryDao().insertAnchorEvidence(
                listOf(
                    MemoryAnchorEvidenceEntity(
                        revisionId = revision,
                        anchorId = anchorId,
                        evidenceId = evidenceId.value,
                    ),
                ),
            )
        }
    }
}
