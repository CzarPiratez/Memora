package com.memora.app.application.asset

import com.memora.app.domain.asset.FindHiddenItem
import com.memora.app.domain.asset.FindHiddenStore
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import java.time.Clock
import javax.inject.Inject

/**
 * Hides one already-ranked Find identity. Memory stays READY.
 */
class HideFromFind(
    private val store: FindHiddenStore,
    private val clock: Clock = Clock.systemUTC(),
) {
    @Inject
    constructor(store: FindHiddenStore) : this(store, Clock.systemUTC())

    suspend operator fun invoke(
        sourceId: String,
        sourceAssetKey: String,
        label: String,
    ) {
        store.hide(
            FindHiddenItem(
                sourceId = SourceId(sourceId),
                sourceAssetKey = SourceAssetKey(sourceAssetKey),
                label = label,
                hiddenAt = clock.instant(),
            ),
        )
    }
}
