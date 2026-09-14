package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import com.memora.app.application.find.FindThumbnailRequest
import com.memora.app.application.find.FindThumbnailResult
import com.memora.app.application.find.LoadFindResultThumbnail
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FindThumbnailViewModel @Inject constructor(
    private val loadFindResultThumbnail: LoadFindResultThumbnail,
) : ViewModel() {
    suspend fun load(request: FindThumbnailRequest): FindThumbnailResult =
        loadFindResultThumbnail(request)
}
