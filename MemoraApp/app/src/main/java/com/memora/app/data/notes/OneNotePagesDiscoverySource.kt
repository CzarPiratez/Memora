package com.memora.app.data.notes

import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAccessModel
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceCapability
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.AssetDiscoverySource
import com.memora.app.domain.discovery.DiscoveryCursor
import com.memora.app.domain.discovery.DiscoveryFailure
import com.memora.app.domain.discovery.DiscoveryPage
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.SourceAccessState
import com.memora.app.domain.notes.NotesProviderSession
import com.memora.app.domain.notes.NotesProviderTokenVault
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Bounded, read-only OneNote page discovery via Microsoft Graph.
 * Walks sections then pages-per-section (avoids global `/pages` error 20266).
 * Creates NOTE Asset placeholders only — no page HTML extract (N4).
 */
class OneNotePagesDiscoverySource(
    private val tokenVault: NotesProviderTokenVault,
    private val graphGateway: OneNotePagesGraphGateway,
    private val clock: () -> Instant = { Instant.now() },
) : AssetDiscoverySource {
    override val capability: SourceCapability = SourceCapability(
        sourceId = SOURCE_ID,
        supportedAssetTypes = setOf(AssetType.NOTE),
        accessModel = SourceAccessModel.PROVIDER_AUTHORIZATION,
        supportsIncrementalDiscovery = true,
        supportsBackgroundIndexing = false,
        isReadOnly = true,
    )

    override suspend fun accessState(): SourceAccessState {
        val session = tokenVault.readSession() ?: return SourceAccessState.ACCESS_REQUIRED
        if (session.providerId != NotesProviderSession.PROVIDER_MICROSOFT_ONENOTE) {
            return SourceAccessState.UNAVAILABLE
        }
        return SourceAccessState.GRANTED
    }

    override suspend fun discover(request: DiscoveryRequest): DiscoveryResult {
        val session = tokenVault.readSession()
            ?: return DiscoveryResult.AccessRequired
        if (session.providerId != NotesProviderSession.PROVIDER_MICROSOFT_ONENOTE) {
            return DiscoveryResult.Failed(
                DiscoveryFailure(
                    code = "onenote_unexpected_provider",
                    message = "UNFYND expected a Microsoft OneNote session.",
                ),
            )
        }

        val cursorValue = request.cursor?.value
        if (cursorValue == COMPLETED_CURSOR) {
            return emptyCompletePage()
        }

        val batchSize = request.batchSize.coerceIn(
            DiscoveryRequest.MIN_BATCH_SIZE,
            DiscoveryRequest.MAX_BATCH_SIZE,
        )
        val walk = when {
            cursorValue == null -> WalkState(emptyList(), null, null)
            // Pre-fix global /pages nextLinks are not resumable; restart section walk.
            cursorValue.startsWith("http://") || cursorValue.startsWith("https://") ->
                WalkState(emptyList(), null, null)
            else -> parseWalkCursor(cursorValue)
                ?: return DiscoveryResult.Failed(
                    DiscoveryFailure(
                        code = "onenote_cursor_invalid",
                        message = "UNFYND could not resume OneNote discovery from this checkpoint.",
                    ),
                )
        }

        return advance(session.accessToken, walk, batchSize)
    }

    private suspend fun advance(
        accessToken: String,
        start: WalkState,
        batchSize: Int,
    ): DiscoveryResult {
        var state = start
        // Bounded internal steps so one Discover tap stays responsive.
        repeat(MAX_INTERNAL_STEPS) {
            if (state.pendingSectionIds.isEmpty()) {
                val sectionsUrl = state.sectionsNextUrl ?: initialSectionsUrl()
                when (val sections = graphGateway.listSections(accessToken, sectionsUrl)) {
                    OneNoteSectionsGraphResult.Unauthorized -> return DiscoveryResult.AccessRevoked
                    is OneNoteSectionsGraphResult.Failed -> return DiscoveryResult.Failed(
                        DiscoveryFailure(code = "onenote_graph_failed", message = sections.message),
                    )
                    is OneNoteSectionsGraphResult.Ok -> {
                        if (sections.response.sectionIds.isEmpty() &&
                            sections.response.nextLink.isNullOrBlank()
                        ) {
                            return emptyCompletePage()
                        }
                        if (sections.response.sectionIds.isEmpty()) {
                            state = WalkState(
                                pendingSectionIds = emptyList(),
                                sectionsNextUrl = sections.response.nextLink,
                                pagesNextUrl = null,
                            )
                            return@repeat
                        }
                        state = WalkState(
                            pendingSectionIds = sections.response.sectionIds,
                            sectionsNextUrl = sections.response.nextLink,
                            pagesNextUrl = FIRST_PAGES,
                        )
                    }
                }
            }

            val sectionId = state.pendingSectionIds.firstOrNull()
                ?: return@repeat
            val pagesUrl = when (val next = state.pagesNextUrl) {
                null, FIRST_PAGES -> sectionPagesUrl(sectionId, batchSize)
                else -> next
            }
            return when (val pages = graphGateway.listPages(accessToken, pagesUrl)) {
                OneNotePagesGraphResult.Unauthorized -> DiscoveryResult.AccessRevoked
                is OneNotePagesGraphResult.Failed -> DiscoveryResult.Failed(
                    DiscoveryFailure(code = "onenote_graph_failed", message = pages.message),
                )
                is OneNotePagesGraphResult.Ok -> {
                    val assets = pages.response.pages.mapNotNull { toAsset(it) }
                    val pageNext = pages.response.nextLink
                    val nextState = when {
                        !pageNext.isNullOrBlank() -> state.copy(pagesNextUrl = pageNext)
                        state.pendingSectionIds.size > 1 -> WalkState(
                            pendingSectionIds = state.pendingSectionIds.drop(1),
                            sectionsNextUrl = state.sectionsNextUrl,
                            pagesNextUrl = FIRST_PAGES,
                        )
                        !state.sectionsNextUrl.isNullOrBlank() -> WalkState(
                            pendingSectionIds = emptyList(),
                            sectionsNextUrl = state.sectionsNextUrl,
                            pagesNextUrl = null,
                        )
                        else -> null
                    }
                    // Skip empty sections inside this Discover tap when more remain.
                    if (assets.isEmpty() && nextState != null) {
                        state = nextState
                        return@repeat
                    }
                    val hasMore = nextState != null
                    DiscoveryResult.Page(
                        DiscoveryPage(
                            sourceId = SOURCE_ID,
                            assets = assets,
                            checkpoint = DiscoveryCursor(
                                sourceId = SOURCE_ID,
                                value = if (hasMore) {
                                    encodeWalkCursor(nextState!!)
                                } else {
                                    COMPLETED_CURSOR
                                },
                            ),
                            hasMore = hasMore,
                        ),
                    )
                }
            }
        }
        return DiscoveryResult.Failed(
            DiscoveryFailure(
                code = "onenote_walk_stuck",
                message = "UNFYND could not make progress listing OneNote sections. Try Discover again.",
            ),
        )
    }

    private fun emptyCompletePage(): DiscoveryResult.Page = DiscoveryResult.Page(
        DiscoveryPage(
            sourceId = SOURCE_ID,
            assets = emptyList(),
            checkpoint = DiscoveryCursor(SOURCE_ID, COMPLETED_CURSOR),
            hasMore = false,
        ),
    )

    private fun toAsset(page: OneNotePageSummary): Asset? {
        val id = page.id.trim()
        if (id.isEmpty()) return null
        val location = page.contentUrl?.takeIf { it.isNotBlank() }
            ?: "https://graph.microsoft.com/v1.0/me/onenote/pages/$id"
        val modified = page.lastModifiedDateTime?.let(::parseInstant)
        val fingerprint = AssetFingerprint(
            "$id:${page.lastModifiedDateTime?.trim().orEmpty().ifBlank { "unknown" }}",
        )
        return Asset(
            identity = AssetIdentity(
                sourceId = SOURCE_ID,
                sourceAssetKey = SourceAssetKey(id),
            ),
            type = AssetType.NOTE,
            location = AssetLocation(location),
            fingerprint = fingerprint,
            discoveredAt = clock(),
            displayName = page.title?.trim()?.takeIf { it.isNotEmpty() },
            sourceModifiedAt = modified,
        )
    }

    private fun parseInstant(value: String): Instant? = try {
        Instant.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }

    private data class WalkState(
        val pendingSectionIds: List<String>,
        val sectionsNextUrl: String?,
        val pagesNextUrl: String?,
    )

    companion object {
        val SOURCE_ID = SourceId(NotesProviderSession.PROVIDER_MICROSOFT_ONENOTE)
        const val COMPLETED_CURSOR = "onenote-pages:complete"
        private const val CURSOR_PREFIX = "onenote-walk-v1|"
        private const val FIRST_PAGES = "FIRST"
        private const val CURSOR_EMPTY = "-"
        private const val MAX_INTERNAL_STEPS = 8
        private const val GRAPH_SECTIONS_BASE =
            "https://graph.microsoft.com/v1.0/me/onenote/sections"

        fun initialSectionsUrl(): String =
            "$GRAPH_SECTIONS_BASE?\$select=id&\$top=20&\$orderby=createdDateTime"

        fun sectionPagesUrl(sectionId: String, batchSize: Int): String {
            val top = batchSize.coerceIn(
                DiscoveryRequest.MIN_BATCH_SIZE,
                DiscoveryRequest.MAX_BATCH_SIZE,
            )
            return "$GRAPH_SECTIONS_BASE/$sectionId/pages" +
                "?\$top=$top" +
                "&\$select=id,title,createdDateTime,lastModifiedDateTime,contentUrl" +
                "&\$orderby=createdDateTime"
        }

        private fun encodeWalkCursor(state: WalkState): String {
            val ids = state.pendingSectionIds.joinToString(",")
            val sectionsNext = state.sectionsNextUrl?.takeIf { it.isNotBlank() } ?: CURSOR_EMPTY
            val pagesNext = state.pagesNextUrl?.takeIf { it.isNotBlank() } ?: CURSOR_EMPTY
            return CURSOR_PREFIX + ids + "|" + sectionsNext + "|" + pagesNext
        }

        private fun parseWalkCursor(raw: String): WalkState? {
            if (!raw.startsWith(CURSOR_PREFIX)) return null
            val body = raw.removePrefix(CURSOR_PREFIX)
            val parts = body.split("|", limit = 3)
            if (parts.size != 3) return null
            val ids = parts[0].split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val sectionsNext = parts[1].takeUnless { it == CURSOR_EMPTY || it.isBlank() }
            val pagesNext = parts[2].takeUnless { it == CURSOR_EMPTY || it.isBlank() }
            if (ids.isEmpty() && sectionsNext == null && pagesNext == null) return null
            return WalkState(
                pendingSectionIds = ids,
                sectionsNextUrl = sectionsNext,
                pagesNextUrl = pagesNext,
            )
        }
    }
}
