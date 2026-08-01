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

    @Test
    fun emptyValueMeansNoPagesAndNoNextLink() {
        val parsed = HttpOneNotePagesGraphGateway.parseListResponse("""{"value":[]}""")
        assertEquals(0, parsed.pages.size)
        assertNull(parsed.nextLink)
    }
}
