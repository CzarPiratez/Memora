package com.memora.app.data.notes

import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.SourceAccessState
import com.memora.app.domain.notes.NotePageOpenTarget
import com.memora.app.domain.notes.NotePageOpenTargetRepository
import com.memora.app.domain.notes.NotesProviderSession
import com.memora.app.domain.notes.NotesProviderTokenVault
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OneNotePagesDiscoverySourceTest {

    @Test
    fun accessRequiredWithoutSession() = runBlocking {
        val source = OneNotePagesDiscoverySource(
            tokenVault = InMemoryNotesProviderTokenVault(),
            graphGateway = FakeGraphGateway(),
        )
        assertEquals(SourceAccessState.ACCESS_REQUIRED, source.accessState())
        assertEquals(DiscoveryResult.AccessRequired, source.discover(DiscoveryRequest(batchSize = 5)))
    }

    /**
     * I5. Before this, Open original resolved these two URLs through Graph on
     * every tap: measured at 6.1–7.0s each time. They ride along on the page
     * resource discovery already reads, so listing is where they get learned.
     */
    @Test
    fun listingAPageRecordsWhereItOpens() = runBlocking {
        val openTargets = RecordingOpenTargets()
        val source = OneNotePagesDiscoverySource(
            tokenVault = InMemoryNotesProviderTokenVault().apply { writeSession(session()) },
            graphGateway = FakeGraphGateway(
                sections = OneNoteSectionsGraphResult.Ok(
                    OneNoteSectionsListResponse(sectionIds = listOf("sec1"), nextLink = null),
                ),
                pages = OneNotePagesGraphResult.Ok(
                    OneNotePagesListResponse(
                        pages = listOf(
                            pageSummary(
                                id = "p1",
                                links = OneNotePageLinks(
                                    oneNoteWebUrl = "https://example/web",
                                    oneNoteClientUrl = "onenote:https://example/client",
                                ),
                            ),
                            // No links: nothing to record, and no reason to fail.
                            pageSummary(id = "p2", links = null),
                        ),
                        nextLink = null,
                    ),
                ),
            ),
            openTargetRepository = openTargets,
            clock = { Instant.parse("2024-03-01T00:00:00Z") },
        )

        val result = source.discover(DiscoveryRequest(batchSize = 10)) as DiscoveryResult.Page

        assertEquals(2, result.value.assets.size)
        assertEquals(
            NotePageOpenTarget(
                sourceId = OneNotePagesDiscoverySource.SOURCE_ID.value,
                sourceAssetKey = "p1",
                clientUrl = "onenote:https://example/client",
                webUrl = "https://example/web",
            ),
            openTargets.find(OneNotePagesDiscoverySource.SOURCE_ID.value, "p1"),
        )
        assertNull(openTargets.find(OneNotePagesDiscoverySource.SOURCE_ID.value, "p2"))
    }

    /** Listing Assets is the job; failing to note an open target must not stop it. */
    @Test
    fun aFailureToRecordAnOpenTargetDoesNotFailTheScan() = runBlocking {
        val source = OneNotePagesDiscoverySource(
            tokenVault = InMemoryNotesProviderTokenVault().apply { writeSession(session()) },
            graphGateway = FakeGraphGateway(
                sections = OneNoteSectionsGraphResult.Ok(
                    OneNoteSectionsListResponse(sectionIds = listOf("sec1"), nextLink = null),
                ),
                pages = OneNotePagesGraphResult.Ok(
                    OneNotePagesListResponse(
                        pages = listOf(
                            pageSummary(
                                id = "p1",
                                links = OneNotePageLinks(
                                    oneNoteWebUrl = "https://example/web",
                                    oneNoteClientUrl = null,
                                ),
                            ),
                        ),
                        nextLink = null,
                    ),
                ),
            ),
            openTargetRepository = RecordingOpenTargets(failOnSave = true),
            clock = { Instant.parse("2024-03-01T00:00:00Z") },
        )

        val result = source.discover(DiscoveryRequest(batchSize = 10)) as DiscoveryResult.Page
        assertEquals(1, result.value.assets.size)
    }

    /** The links have to be asked for, or Graph does not send them. */
    @Test
    fun theSectionPagesRequestSelectsTheOpenLinks() {
        assertTrue(
            OneNotePagesDiscoverySource.sectionPagesUrl("sec1", 10).contains(",links"),
        )
    }

    private fun pageSummary(id: String, links: OneNotePageLinks?) = OneNotePageSummary(
        id = id,
        title = "Ideas",
        contentUrl = "https://example.com/$id",
        createdDateTime = "2024-01-01T00:00:00Z",
        lastModifiedDateTime = "2024-01-02T00:00:00Z",
        links = links,
    )

    private class RecordingOpenTargets(
        private val failOnSave: Boolean = false,
    ) : NotePageOpenTargetRepository {
        private val rows = mutableMapOf<Pair<String, String>, NotePageOpenTarget>()

        override suspend fun find(sourceId: String, sourceAssetKey: String): NotePageOpenTarget? =
            rows[sourceId to sourceAssetKey]

        override suspend fun save(target: NotePageOpenTarget) {
            if (failOnSave) error("store unavailable")
            rows[target.sourceId to target.sourceAssetKey] = target
        }
    }

    @Test
    fun mapsSectionPagesToNotePlaceholders() = runBlocking {
        val vault = InMemoryNotesProviderTokenVault().apply {
            writeSession(session())
        }
        val gateway = FakeGraphGateway(
            sections = OneNoteSectionsGraphResult.Ok(
                OneNoteSectionsListResponse(sectionIds = listOf("sec1"), nextLink = null),
            ),
            pages = OneNotePagesGraphResult.Ok(
                OneNotePagesListResponse(
                    pages = listOf(
                        OneNotePageSummary(
                            id = "p1",
                            title = "Ideas",
                            contentUrl = "https://example.com/p1",
                            createdDateTime = "2024-01-01T00:00:00Z",
                            lastModifiedDateTime = "2024-01-02T00:00:00Z",
                        ),
                    ),
                    nextLink = null,
                ),
            ),
        )
        val source = OneNotePagesDiscoverySource(
            tokenVault = vault,
            graphGateway = gateway,
            clock = { Instant.parse("2024-03-01T00:00:00Z") },
        )

        val result = source.discover(DiscoveryRequest(batchSize = 10)) as DiscoveryResult.Page
        assertEquals(1, result.value.assets.size)
        assertEquals(AssetType.NOTE, result.value.assets[0].type)
        assertEquals("p1", result.value.assets[0].identity.sourceAssetKey.value)
        assertEquals("Ideas", result.value.assets[0].displayName)
        assertFalse(result.value.hasMore)
        assertEquals(
            OneNotePagesDiscoverySource.COMPLETED_CURSOR,
            result.value.checkpoint.value,
        )
        assertTrue(gateway.lastSectionsUrl!!.contains("/sections"))
        assertTrue(gateway.lastPagesUrl!!.contains("/sections/sec1/pages"))
        assertTrue(gateway.lastPagesUrl!!.contains("\$top=10"))
        assertFalse(gateway.lastPagesUrl!!.contains("/me/onenote/pages?"))
    }

    @Test
    fun unauthorizedBecomesAccessRevoked() = runBlocking {
        val vault = InMemoryNotesProviderTokenVault().apply { writeSession(session()) }
        val source = OneNotePagesDiscoverySource(
            tokenVault = vault,
            graphGateway = FakeGraphGateway(
                sections = OneNoteSectionsGraphResult.Unauthorized,
            ),
        )
        assertEquals(DiscoveryResult.AccessRevoked, source.discover(DiscoveryRequest(batchSize = 1)))
    }

    private fun session() = NotesProviderSession(
        providerId = NotesProviderSession.PROVIDER_MICROSOFT_ONENOTE,
        accountId = "acct",
        accountDisplayLabel = "user@example.com",
        accessToken = "token",
        accessTokenExpiresAtEpochMs = 9_999_999_999_999L,
    )

    private class FakeGraphGateway(
        private val sections: OneNoteSectionsGraphResult =
            OneNoteSectionsGraphResult.Failed("unused"),
        private val pages: OneNotePagesGraphResult = OneNotePagesGraphResult.Failed("unused"),
    ) : OneNotePagesGraphGateway {
        var lastSectionsUrl: String? = null
        var lastPagesUrl: String? = null

        override suspend fun listSections(
            accessToken: String,
            requestUrl: String,
        ): OneNoteSectionsGraphResult {
            lastSectionsUrl = requestUrl
            return sections
        }

        override suspend fun listPages(
            accessToken: String,
            requestUrl: String,
        ): OneNotePagesGraphResult {
            lastPagesUrl = requestUrl
            return pages
        }

        override suspend fun fetchPageContent(
            accessToken: String,
            contentUrl: String,
        ): OneNotePageContentGraphResult = OneNotePageContentGraphResult.Failed("unused")

        override suspend fun getPageLinks(
            accessToken: String,
            pageId: String,
        ): OneNotePageLinksGraphResult = OneNotePageLinksGraphResult.Failed("unused")
    }
}
