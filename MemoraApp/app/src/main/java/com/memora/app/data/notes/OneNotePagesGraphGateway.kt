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
