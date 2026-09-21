package com.memora.app.application.asset

import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.FindHiddenStore
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import javax.inject.Inject

/** Restores a hidden identity to Find. Memory was never deleted. */
class ShowAgainOnFind @Inject constructor(
    private val store: FindHiddenStore,
) {
    suspend operator fun invoke(sourceId: String, sourceAssetKey: String) {
        store.showAgain(
            AssetIdentity(SourceId(sourceId), SourceAssetKey(sourceAssetKey)),
        )
    }
}
