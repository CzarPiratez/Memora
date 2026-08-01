package com.memora.app.data.notes

import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.SourceAccessState
import com.memora.app.domain.notes.NotesProviderSession
import com.memora.app.domain.notes.NotesProviderTokenVault
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
