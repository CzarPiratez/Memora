package com.memora.app.application.handoff

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens the whole stored original in another app on this phone.
 *
 * The in-app preview shows the page that justified the hit; this hands the
 * person the entire file where they normally read it, so they can scroll it,
 * search inside it, and come back. Same read-only grant as the share sheet
 * (ADR-053) and the same resolved URI — a different verb, not a new power.
 *
 * Not Act: UNFYND starts a viewer the person asked for and changes nothing.
 */
@Singleton
class OpenOriginalInAnotherApp(
    private val prepare: suspend (OriginalHandoffRequest) -> PreparedOriginalHandoff,
    private val viewer: ExternalOriginalViewer,
) {
    @Inject
    constructor(
        prepare: PrepareOriginalHandoff,
        viewer: ExternalOriginalViewer,
    ) : this(
        prepare = { request -> prepare(request) },
        viewer = viewer,
    )

    suspend operator fun invoke(request: OriginalHandoffRequest): OpenOriginalOutcome =
        when (val prepared = prepare(request)) {
            is PreparedOriginalHandoff.Ready -> when (
                viewer.view(
                    uri = prepared.uri,
                    mimeType = prepared.mimeType,
                    label = prepared.label,
                )
            ) {
                ExternalViewOutcome.Opened -> OpenOriginalOutcome.Opened
                ExternalViewOutcome.NoAppAvailable -> OpenOriginalOutcome.NoAppAvailable
                ExternalViewOutcome.CouldNotOpen -> OpenOriginalOutcome.CouldNotOpen
            }
            PreparedOriginalHandoff.SourceUnavailable -> OpenOriginalOutcome.SourceUnavailable
            PreparedOriginalHandoff.CouldNotHandOff -> OpenOriginalOutcome.CouldNotOpen
        }
}
