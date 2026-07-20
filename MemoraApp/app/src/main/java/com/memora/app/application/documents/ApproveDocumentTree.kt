package com.memora.app.application.documents

import com.memora.app.domain.discovery.DocumentTreeApproval
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
import com.memora.app.domain.discovery.DocumentTreeSource
import java.time.Clock
import javax.inject.Inject

/** Application boundary for saving a SAF tree after Android has persisted read access. */
interface DocumentTreeApprover {
    suspend operator fun invoke(persistedTreeUri: String): DocumentTreeApproval
}

class ApproveDocumentTree internal constructor(
    private val approvalRepository: DocumentTreeApprovalRepository,
    private val clock: Clock,
) : DocumentTreeApprover {
    @Inject
    constructor(approvalRepository: DocumentTreeApprovalRepository) : this(
        approvalRepository = approvalRepository,
        clock = Clock.systemUTC(),
    )

    override suspend fun invoke(persistedTreeUri: String): DocumentTreeApproval {
        val approval = DocumentTreeSource.approvalFor(
            persistedTreeUri = persistedTreeUri,
            approvedAt = clock.instant(),
        )
        approvalRepository.save(approval)
        return approval
    }
}
