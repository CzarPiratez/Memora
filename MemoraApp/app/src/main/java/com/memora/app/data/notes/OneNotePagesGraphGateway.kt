package com.memora.app.data.notes

/**
 * One Graph page summary used only for discovery placeholders (N3).
 * Does not include HTML body content — that is N4.
 */
data class OneNotePageSummary(
    val id: String,
    val title: String?,
    val contentUrl: String?,
    val createdDateTime: String?,
    val lastModifiedDateTime: String?,
    /**
     * Where this page opens. Graph carries `links` on the same page resource
     * discovery already `$select`s, so reading it here costs no extra request
     * and spares Open original a 6–7s round trip per tap (I5).
     *
     * Null when a page was listed by a request that did not select `links`.
     */
    val links: OneNotePageLinks? = null,
)

data class OneNotePagesListResponse(
    val pages: List<OneNotePageSummary>,
    val nextLink: String?,
)

data class OneNoteSectionsListResponse(
    val sectionIds: List<String>,
    val nextLink: String?,
)

/**
 * Read-only Microsoft Graph OneNote list APIs.
 * Implementations must not call write/update/delete Graph APIs.
 *
 * Pages are listed **per section** (not `/me/onenote/pages`) so accounts with
 * many sections do not hit Graph error 20266.
 */
interface OneNotePagesGraphGateway {
    suspend fun listSections(accessToken: String, requestUrl: String): OneNoteSectionsGraphResult

    suspend fun listPages(accessToken: String, requestUrl: String): OneNotePagesGraphResult

    /** GET page HTML body (read-only). [contentUrl] is typically `…/pages/{id}/content`. */
    suspend fun fetchPageContent(
        accessToken: String,
        contentUrl: String,
    ): OneNotePageContentGraphResult

    /**
     * GET page `links` only (read-only). Used to open the original in OneNote/browser.
     * Never treat [contentUrl] / Asset.location as a browser URL.
     */
    suspend fun getPageLinks(
        accessToken: String,
        pageId: String,
    ): OneNotePageLinksGraphResult
}

data class OneNotePageLinks(
    val oneNoteWebUrl: String?,
    val oneNoteClientUrl: String?,
) {
    val webUrlOrNull: String? = oneNoteWebUrl?.takeIf { it.isNotBlank() }
    val clientUrlOrNull: String? = oneNoteClientUrl?.takeIf { it.isNotBlank() }

    /** Graph can return a `links` object with neither href populated. */
    fun hasAnyUrl(): Boolean = webUrlOrNull != null || clientUrlOrNull != null

    /** Fallback when no package manager is available: web first, then client. */
    fun preferredOpenUrl(): String? = webUrlOrNull ?: clientUrlOrNull
}

sealed interface OneNotePageLinksGraphResult {
    data class Ok(val links: OneNotePageLinks) : OneNotePageLinksGraphResult

    data object Unauthorized : OneNotePageLinksGraphResult

    data class Failed(val message: String) : OneNotePageLinksGraphResult
}

sealed interface OneNotePageContentGraphResult {
    data class Ok(val html: String) : OneNotePageContentGraphResult

    data object Unauthorized : OneNotePageContentGraphResult

    data class Failed(val message: String) : OneNotePageContentGraphResult
}

sealed interface OneNoteSectionsGraphResult {
    data class Ok(val response: OneNoteSectionsListResponse) : OneNoteSectionsGraphResult

    data object Unauthorized : OneNoteSectionsGraphResult

    data class Failed(val message: String) : OneNoteSectionsGraphResult
}

sealed interface OneNotePagesGraphResult {
    data class Ok(val response: OneNotePagesListResponse) : OneNotePagesGraphResult

    data object Unauthorized : OneNotePagesGraphResult

    data class Failed(val message: String) : OneNotePagesGraphResult
}
