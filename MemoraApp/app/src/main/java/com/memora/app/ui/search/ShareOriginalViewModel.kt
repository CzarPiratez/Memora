package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import com.memora.app.application.share.PrepareShareOriginal
import com.memora.app.application.share.PreparedShareOriginal
import com.memora.app.application.share.ShareOriginalChooser
import com.memora.app.application.share.ShareOriginalOutcome
import com.memora.app.application.share.ShareOriginalRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ShareOriginalViewModel @Inject constructor(
    private val prepareShareOriginal: PrepareShareOriginal,
    private val shareOriginalChooser: ShareOriginalChooser,
) : ViewModel() {
    suspend fun share(request: ShareOriginalRequest): ShareOriginalOutcome =
        when (val prepared = prepareShareOriginal(request)) {
            is PreparedShareOriginal.Ready -> if (
                shareOriginalChooser.present(
                    uri = prepared.uri,
                    mimeType = prepared.mimeType,
                    label = prepared.label,
                )
            ) {
                ShareOriginalOutcome.Presented
            } else {
                ShareOriginalOutcome.CouldNotShare
            }
            PreparedShareOriginal.SourceUnavailable -> ShareOriginalOutcome.SourceUnavailable
            PreparedShareOriginal.CouldNotShare -> ShareOriginalOutcome.CouldNotShare
        }
}
