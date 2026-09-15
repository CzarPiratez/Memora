package com.memora.app.application.notes

import android.util.Log
import com.memora.app.data.notes.OneNotePageLinksGraphResult
import com.memora.app.data.notes.OneNotePagesGraphGateway
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Resolves external OneNote/browser URLs for a saved NOTE hit.
 *
 * Uses vaulted Graph session + page `links` only. Never treats Asset.location /
 * contentUrl as a browser URL. Search stays offline; this path may need network.
 */
class OpenPersistedNotePageInOneNote @Inject constructor(
    private val assetRepository: AssetRepository,
    private val oneNoteAuth: OneNoteInteractiveAuth,
    private val graphGateway: OneNotePagesGraphGateway,
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

        // D-18 remainder / backlog I5: every tap pays for a session check and a
        // Graph request because the page `links` are not persisted at index
        // time. Time the two halves so the fix is chosen against a number.
        // Durations and branch only — never a page id, token, or URL.
        val startedAtMs = System.currentTimeMillis()
        var session = oneNoteAuth.ensureSession()
            ?: return OpenPersistedNotePageResult.SourceUnavailable
        val sessionReadyAtMs = System.currentTimeMillis()

        val first = fetchLinks(session.accessToken, sourceAssetKey)
        if (first !is OpenPersistedNotePageResult.SourceUnavailable) {
            logTimings(startedAtMs, sessionReadyAtMs, refreshed = false)
            return first
        }

        session = oneNoteAuth.ensureSession(forceRefresh = true)
            ?: return OpenPersistedNotePageResult.SourceUnavailable
        return fetchLinks(session.accessToken, sourceAssetKey).also {
            logTimings(startedAtMs, sessionReadyAtMs, refreshed = true)
        }
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
