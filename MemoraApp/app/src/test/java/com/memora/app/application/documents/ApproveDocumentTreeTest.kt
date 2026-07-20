package com.memora.app.application.documents

import com.memora.app.domain.discovery.DocumentTreeApproval
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ApproveDocumentTreeTest {
    @Test
    fun createsAndSavesOnePrivateApprovalAfterThePlatformGrant() = runBlocking {
        val repository = RecordingRepository()
        val useCase = ApproveDocumentTree(
            approvalRepository = repository,
            clock = Clock.fixed(Instant.parse("2026-07-20T12:00:00Z"), ZoneOffset.UTC),
        )

        val approval = useCase("content://example/tree/documents")

        assertEquals(approval, repository.savedApproval)
        assertEquals("content://example/tree/documents", approval.treeUri)
        assertEquals("2026-07-20T12:00:00Z", approval.approvedAt.toString())
    }

    private class RecordingRepository : DocumentTreeApprovalRepository {
        var savedApproval: DocumentTreeApproval? = null

        override suspend fun save(approval: DocumentTreeApproval) {
            savedApproval = approval
        }

        override suspend fun findAll(): List<DocumentTreeApproval> = emptyList()
    }
}
