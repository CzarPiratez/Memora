package com.memora.app.data.saf

import com.memora.app.domain.discovery.AssetDiscoverySource
import com.memora.app.domain.discovery.DocumentTreeAccessValidator
import com.memora.app.domain.discovery.DocumentTreeApproval
import com.memora.app.domain.discovery.PdfFolderDiscoverySourceFactory
import javax.inject.Inject

/** Android SAF implementation of the source-neutral approved-PDF-folder factory. */
class SafPdfDiscoverySourceFactory @Inject constructor(
    private val accessValidator: DocumentTreeAccessValidator,
    private val catalog: SafDocumentTreeCatalog,
) : PdfFolderDiscoverySourceFactory {
    override fun create(approval: DocumentTreeApproval): AssetDiscoverySource =
        SafPdfDiscoverySource(approval, accessValidator, catalog)
}
