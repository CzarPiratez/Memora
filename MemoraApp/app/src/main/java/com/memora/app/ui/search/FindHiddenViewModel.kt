package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memora.app.application.asset.HideFromFind
import com.memora.app.application.asset.LoadFindHidden
import com.memora.app.application.asset.ShowAgainOnFind
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.FindHiddenItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FindHiddenHost(
    val hidden: List<FindHiddenItem>,
    val onHide: (FindOpenTarget, String) -> Unit,
    val onShowAgain: (FindHiddenItem) -> Unit,
) {
    fun hiddenIdentities(): Set<AssetIdentity> = hidden.map { it.asIdentity() }.toSet()
}

/**
 * Shared Hide-from-Find preference across keyword and meaning Find.
 *
 * Ranking and Memory integrity stay untouched. Show again restores the
 * identity to the already-ranked list.
 */
@HiltViewModel
class FindHiddenViewModel @Inject constructor(
    private val loadFindHidden: LoadFindHidden,
    private val hideFromFind: HideFromFind,
    private val showAgainOnFind: ShowAgainOnFind,
) : ViewModel() {
    private val mutableHidden = MutableStateFlow<List<FindHiddenItem>>(emptyList())
    val hidden: StateFlow<List<FindHiddenItem>> = mutableHidden.asStateFlow()
    private val mutablePendingUndo = MutableStateFlow<FindHiddenItem?>(null)
    val pendingUndo: StateFlow<FindHiddenItem?> = mutablePendingUndo.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            mutableHidden.value = loadFindHidden()
        }
    }

    fun hide(target: FindOpenTarget, label: String) {
        viewModelScope.launch {
            hideFromFind(target.sourceId, target.sourceAssetKey, label)
            val hiddenNow = loadFindHidden()
            mutableHidden.value = hiddenNow
            mutablePendingUndo.value = hiddenNow.firstOrNull {
                it.sourceId.value == target.sourceId &&
                    it.sourceAssetKey.value == target.sourceAssetKey
            }
        }
    }

    fun showAgain(item: FindHiddenItem) {
        viewModelScope.launch {
            showAgainOnFind(item.sourceId.value, item.sourceAssetKey.value)
            mutableHidden.value = loadFindHidden()
            if (mutablePendingUndo.value?.asIdentity() == item.asIdentity()) {
                mutablePendingUndo.value = null
            }
        }
    }

    fun clearPendingUndo() {
        mutablePendingUndo.value = null
    }
}
