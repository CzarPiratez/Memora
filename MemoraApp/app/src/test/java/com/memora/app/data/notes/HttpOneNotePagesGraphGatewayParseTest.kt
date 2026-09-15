package com.memora.app.data.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HttpOneNotePagesGraphGatewayParseTest {

    @Test
    fun parsesPagesAndNextLink() {
        val json = """
            {
              "value": [
                {
                  "id": "page-1",
                  "title": "Meeting notes",
                  "contentUrl": "https://graph.microsoft.com/v1.0/me/onenote/pages/page-1/content",
                  "createdDateTime": "2024-01-01T00:00:00Z",
                  "lastModifiedDateTime": "2024-02-01T00:00:00Z"
                }
              ],
              "@odata.nextLink": "https://graph.microsoft.com/v1.0/me/onenote/pages?${'$'}skiptoken=abc"
            }
        """.trimIndent()

        val parsed = HttpOneNotePagesGraphGateway.parseListResponse(json)
        assertEquals(1, parsed.pages.size)
        assertEquals("page-1", parsed.pages[0].id)
        assertEquals("Meeting notes", parsed.pages[0].title)
        assertEquals(
            "https://graph.microsoft.com/v1.0/me/onenote/pages?\$skiptoken=abc",
            parsed.nextLink,
        )
    }

    /**
     * I5: the same `links` object Graph returns for a single page also comes
     * back on a list item, so discovery can learn where every page opens
     * without a second request.
     */
    @Test
    fun parsesTheOpenLinksCarriedOnAListedPage() {
        val json = """
            {
              "value": [
                {
                  "id": "page-1",
                  "title": "Meeting notes",
                  "links": {
                    "oneNoteClientUrl": { "href": "onenote:https://example/client" },
                    "oneNoteWebUrl": { "href": "https://example/web" }
                  }
                }
              ]
            }
        """.trimIndent()

        val links = HttpOneNotePagesGraphGateway.parseListResponse(json).pages.single().links
        assertEquals("https://example/web", links?.webUrlOrNull)
        assertEquals("onenote:https://example/client", links?.clientUrlOrNull)
    }

    /** An older cursor, or a request that did not select `links`, has none. */
    @Test
    fun aListedPageWithoutLinksHasNoOpenTarget() {
        val json = """{"value":[{"id":"page-1","title":"Meeting notes"}]}"""
        assertNull(HttpOneNotePagesGraphGateway.parseListResponse(json).pages.single().links)
    }

    /** Graph can return the object with neither href; that opens nothing. */
    @Test
    fun aLinksObjectWithNoHrefIsNotAnOpenTarget() {
        val json = """{"value":[{"id":"page-1","links":{}}]}"""
        assertNull(HttpOneNotePagesGraphGateway.parseListResponse(json).pages.single().links)
    }

    @Test
    fun emptyValueMeansNoPagesAndNoNextLink() {
        val parsed = HttpOneNotePagesGraphGateway.parseListResponse("""{"value":[]}""")
        assertEquals(0, parsed.pages.size)
        assertNull(parsed.nextLink)
    }

    @Test
    fun parsesOneNoteWebAndClientLinks() {
        val json = """
            {
              "links": {
                "oneNoteClientUrl": { "href": "onenote:https://example/client" },
                "oneNoteWebUrl": { "href": "https://example/web" }
              }
            }
        """.trimIndent()
        val links = HttpOneNotePagesGraphGateway.parsePageLinksResponse(json)
        assertEquals("https://example/web", links.oneNoteWebUrl)
        assertEquals("onenote:https://example/client", links.oneNoteClientUrl)
        assertEquals("https://example/web", links.preferredOpenUrl())
    }

    @Test
    fun pageLinksRequestUrlEncodesPageId() {
        val url = HttpOneNotePagesGraphGateway.pageLinksRequestUrl("page id/1")
        assertEquals(
            "https://graph.microsoft.com/v1.0/me/onenote/pages/page%20id%2F1?\$select=links",
            url,
        )
    }
}
