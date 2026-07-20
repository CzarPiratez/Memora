package com.memora.app.application.documents

import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DocumentTreeApprovalRepository
import javax.inject.Inject

/** Finds the most recently user-approved PDF folder without reading its contents. */
interface PdfFolderConnectionFinder {
    suspend operator fun invoke(): SourceId?
}

class FindMostRecentPdfFolder @Inject constructor(
    private val approvalRepository: DocumentTreeApprovalRepository,
) : PdfFolderConnectionFinder {
    override suspend operator fun invoke(): SourceId? = approvalRepository.findAll()
        .maxByOrNull { approval -> approval.approvedAt }
        ?.sourceId
}
