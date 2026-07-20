package com.memora.app.domain.discovery

/** Creates the source adapter for one already approved PDF folder. */
interface PdfFolderDiscoverySourceFactory {
    fun create(approval: DocumentTreeApproval): AssetDiscoverySource
}
