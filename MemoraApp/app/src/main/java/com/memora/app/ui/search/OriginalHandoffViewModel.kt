package com.memora.app.ui.search

import androidx.lifecycle.ViewModel
import com.memora.app.application.handoff.OpenOriginalInAnotherApp
import com.memora.app.application.handoff.OpenOriginalOutcome
import com.memora.app.application.handoff.OriginalHandoffRequest
import com.memora.app.application.handoff.PrepareOriginalHandoff
import com.memora.app.application.handoff.PreparedOriginalHandoff
import com.memora.app.application.handoff.ShareOriginalChooser
import com.memora.app.application.handoff.ShareOriginalOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * The two handoffs a person can tap on an opened original: the share sheet and
 * Open in another app. Both resolve the same read-only stored URI.
 */
@HiltViewModel
class OriginalHandoffViewModel @Inject constructor(
    private val prepareOriginalHandoff: PrepareOriginalHandoff,
    private val shareOriginalChooser: ShareOriginalChooser,
    private val openOriginalInAnotherApp: OpenOriginalInAnotherApp,
) : ViewModel() {
    suspend fun share(request: OriginalHandoffRequest): ShareOriginalOutcome =
        when (val prepared = prepareOriginalHandoff(request)) {
            is PreparedOriginalHandoff.Ready -> if (
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
            PreparedOriginalHandoff.SourceUnavailable -> ShareOriginalOutcome.SourceUnavailable
            PreparedOriginalHandoff.CouldNotHandOff -> ShareOriginalOutcome.CouldNotShare
        }

    suspend fun open(request: OriginalHandoffRequest): OpenOriginalOutcome =
        openOriginalInAnotherApp(request)
}
