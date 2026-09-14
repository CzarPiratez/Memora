package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import com.memora.app.application.preview.OriginalPreviewReloadRequest
import com.memora.app.application.preview.OriginalPreviewReloadResult
import com.memora.app.application.preview.ReloadOriginalPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OriginalPreviewReloadViewModel @Inject constructor(
    private val reloadOriginalPreview: ReloadOriginalPreview,
) : ViewModel() {
    suspend fun reload(
        request: OriginalPreviewReloadRequest,
        maxEdgePx: Int,
    ): OriginalPreviewReloadResult = reloadOriginalPreview(request, maxEdgePx)
}
