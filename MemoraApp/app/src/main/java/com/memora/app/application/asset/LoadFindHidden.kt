package com.memora.app.application.asset

import com.memora.app.domain.asset.FindHiddenItem
import com.memora.app.domain.asset.FindHiddenStore
import javax.inject.Inject

/** Loads the durable Hide-from-Find list for restore UI. */
class LoadFindHidden @Inject constructor(
    private val store: FindHiddenStore,
) {
    suspend operator fun invoke(): List<FindHiddenItem> = store.listAll()
}
