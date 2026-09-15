package com.memora.app.application.notes

import android.util.Log
import com.memora.app.data.notes.OneNotePageLinksGraphResult
import com.memora.app.data.notes.OneNotePagesGraphGateway
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.notes.NotePageOpenTarget
import com.memora.app.domain.notes.NotePageOpenTargetRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Resolves external OneNote/browser URLs for a saved NOTE hit.
 *
 * Prefers the open targets discovery already saved, so the common path needs
 * neither a session nor a network. Falls back to a Graph `links` lookup for
 * pages indexed before those were stored, and records what it learns. Never
 * treats Asset.location / contentUrl as a browser URL.
 */
class OpenPersistedNotePageInOneNote @Inject constructor(
    private val assetRepository: AssetRepository,
    private val oneNoteAuth: OneNoteInteractiveAuth,
    private val graphGateway: OneNotePagesGraphGateway,
    private val openTargetRepository: NotePageOpenTargetRepository,
) {
    suspend operator fun invoke(
        sourceId: String,
        sourceAssetKey: String,
    ): OpenPersistedNotePageResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(OPEN_TIMEOUT_MS) {
                open(sourceId, sourceAssetKey)
            }
        } catch (_: TimeoutCancellationException) {
            OpenPersistedNotePageResult.CouldNotOpen
        }
    }

    private suspend fun open(
        sourceId: String,
        sourceAssetKey: String,
    ): OpenPersistedNotePageResult {
        val identity = AssetIdentity(
            sourceId = SourceId(sourceId),
            sourceAssetKey = SourceAssetKey(sourceAssetKey),
        )
        val asset = assetRepository.find(identity)?.asset
            ?: return OpenPersistedNotePageResult.SourceUnavailable
        if (asset.type != AssetType.NOTE) {
            return OpenPersistedNotePageResult.SourceUnavailable
        }

        // I5: discovery reads the open targets off the same Graph page resource
        // it already selects, so the common path is a local row and needs
        // neither a session nor a network.
        openTargetRepository.find(sourceId, sourceAssetKey)?.let { target ->
            return OpenPersistedNotePageResult.Ready(
                webUrl = target.webUrl,
                clientUrl = target.clientUrl,
            )
        }

        // Fallback for pages indexed before I5, and for a saved link that has
        // gone stale. Measured at 6.1–7.0s, which is why it is no longer the
        // path every tap takes.
        val startedAtMs = System.currentTimeMillis()
        var session = oneNoteAuth.ensureSession()
            ?: return OpenPersistedNotePageResult.SourceUnavailable
        val sessionReadyAtMs = System.currentTimeMillis()

        val first = fetchLinks(session.accessToken, sourceAssetKey)
        if (first !is OpenPersistedNotePageResult.SourceUnavailable) {
            logTimings(startedAtMs, sessionReadyAtMs, refreshed = false)
            return first.alsoRemember(sourceId, sourceAssetKey)
        }

        session = oneNoteAuth.ensureSession(forceRefresh = true)
            ?: return OpenPersistedNotePageResult.SourceUnavailable
        return fetchLinks(session.accessToken, sourceAssetKey)
            .also { logTimings(startedAtMs, sessionReadyAtMs, refreshed = true) }
            .alsoRemember(sourceId, sourceAssetKey)
    }

    /**
     * Heals a corpus indexed before I5: the first tap pays Graph once, and
     * every tap after it is local. Never lets a write failure turn an open the
     * person can already make into an error.
     */
    private suspend fun OpenPersistedNotePageResult.alsoRemember(
        sourceId: String,
        sourceAssetKey: String,
    ): OpenPersistedNotePageResult {
        if (this !is OpenPersistedNotePageResult.Ready) return this
        try {
            openTargetRepository.save(
                NotePageOpenTarget(
                    sourceId = sourceId,
                    sourceAssetKey = sourceAssetKey,
                    clientUrl = clientUrl,
                    webUrl = webUrl,
                ),
            )
        } catch (_: Exception) {
            // The open still works; the next tap just pays Graph again.
        }
        return this
    }

    private fun logTimings(startedAtMs: Long, sessionReadyAtMs: Long, refreshed: Boolean) {
        Log.d(
            TAG,
            "note open: session ${sessionReadyAtMs - startedAtMs}ms, " +
                "links ${System.currentTimeMillis() - sessionReadyAtMs}ms, " +
                "forced refresh $refreshed",
        )
    }

    private suspend fun fetchLinks(
        accessToken: String,
        pageId: String,
    ): OpenPersistedNotePageResult {
        return when (val linksResult = graphGateway.getPageLinks(accessToken, pageId)) {
            OneNotePageLinksGraphResult.Unauthorized ->
                OpenPersistedNotePageResult.SourceUnavailable
            is OneNotePageLinksGraphResult.Failed ->
                OpenPersistedNotePageResult.CouldNotOpen
            is OneNotePageLinksGraphResult.Ok -> {
                val web = linksResult.links.webUrlOrNull
                val client = linksResult.links.clientUrlOrNull
                if (web == null && client == null) {
                    OpenPersistedNotePageResult.CouldNotOpen
                } else {
                    OpenPersistedNotePageResult.Ready(
                        webUrl = web,
                        clientUrl = client,
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "OpenNotePage"
        private const val OPEN_TIMEOUT_MS = 25_000L
    }
}

sealed interface OpenPersistedNotePageResult {
    /**
     * Graph-provided open targets. The launcher prefers the OneNote app deep link
     * when something on the device can handle it; otherwise it uses the web URL.
     */
    data class Ready(
        val webUrl: String?,
        val clientUrl: String?,
    ) : OpenPersistedNotePageResult {
        init {
            require(!webUrl.isNullOrBlank() || !clientUrl.isNullOrBlank()) {
                "Ready open needs a web or client URL."
            }
        }
    }

    data object SourceUnavailable : OpenPersistedNotePageResult

    data object CouldNotOpen : OpenPersistedNotePageResult
}
