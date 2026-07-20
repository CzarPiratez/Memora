package com.memora.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.data.saf.SafDocumentTreeSource
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDocumentTreeApprovalRepositoryTest {
    private lateinit var database: MemoraDatabase
    private lateinit var repository: RoomDocumentTreeApprovalRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MemoraDatabase::class.java,
        ).build()
        repository = RoomDocumentTreeApprovalRepository(database.documentTreeApprovalDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun savesAndRestoresAnApprovedDocumentTreeReference() = runBlocking {
        val approval = SafDocumentTreeSource.approvalFor(
            persistedTreeUri = "content://example/tree/documents",
            approvedAt = Instant.parse("2026-07-20T12:00:00Z"),
        )

        repository.save(approval)

        assertEquals(listOf(approval), repository.findAll())
    }

    @Test
    fun savesDifferentApprovedTreesAsIndependentSources() = runBlocking {
        val first = SafDocumentTreeSource.approvalFor(
            persistedTreeUri = "content://example/tree/first",
            approvedAt = Instant.parse("2026-07-20T12:00:00Z"),
        )
        val second = SafDocumentTreeSource.approvalFor(
            persistedTreeUri = "content://example/tree/second",
            approvedAt = Instant.parse("2026-07-20T12:01:00Z"),
        )

        repository.save(first)
        repository.save(second)

        val approvals = repository.findAll()
        assertEquals(listOf(first, second), approvals)
        assertTrue(approvals.all { it.sourceId.value.startsWith("android-saf-document-tree:") })
    }
}
