package com.memora.app.data.notes

import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * HttpURLConnection Graph client for OneNote section/page metadata only.
 * JSON parsing uses Gson so JVM unit tests are not blocked by Android org.json stubs.
 */
class HttpOneNotePagesGraphGateway(
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
) : OneNotePagesGraphGateway {
    override suspend fun listSections(
        accessToken: String,
        requestUrl: String,
    ): OneNoteSectionsGraphResult = withContext(Dispatchers.IO) {
        when (val raw = get(accessToken, requestUrl, accept = "application/json")) {
            GraphHttp.Unauthorized -> OneNoteSectionsGraphResult.Unauthorized
            is GraphHttp.Failed -> OneNoteSectionsGraphResult.Failed(raw.message)
            is GraphHttp.Ok -> OneNoteSectionsGraphResult.Ok(parseSectionsResponse(raw.body))
        }
    }

    override suspend fun listPages(
        accessToken: String,
        requestUrl: String,
    ): OneNotePagesGraphResult = withContext(Dispatchers.IO) {
        when (val raw = get(accessToken, requestUrl, accept = "application/json")) {
            GraphHttp.Unauthorized -> OneNotePagesGraphResult.Unauthorized
            is GraphHttp.Failed -> OneNotePagesGraphResult.Failed(raw.message)
            is GraphHttp.Ok -> OneNotePagesGraphResult.Ok(parseListResponse(raw.body))
        }
    }

    override suspend fun fetchPageContent(
        accessToken: String,
        contentUrl: String,
    ): OneNotePageContentGraphResult = withContext(Dispatchers.IO) {
        when (
            val raw = get(
                accessToken,
                contentUrl,
                accept = "text/html",
            )
        ) {
            GraphHttp.Unauthorized -> OneNotePageContentGraphResult.Unauthorized
            is GraphHttp.Failed -> OneNotePageContentGraphResult.Failed(raw.message)
            is GraphHttp.Ok -> OneNotePageContentGraphResult.Ok(raw.body)
        }
    }

    private fun get(accessToken: String, requestUrl: String, accept: String): GraphHttp {
        return try {
            val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Accept", accept)
            }
            try {
                when (val code = connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val body = connection.inputStream.use { stream ->
                            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))
                                .readText()
                        }
                        GraphHttp.Ok(body)
                    }
                    HttpURLConnection.HTTP_UNAUTHORIZED,
                    HttpURLConnection.HTTP_FORBIDDEN,
                    -> GraphHttp.Unauthorized
                    else -> {
                        val errorBody = connection.errorStream?.use { stream ->
                            BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8))
                                .readText()
                        }
                        GraphHttp.Failed(
                            "Graph OneNote list failed (HTTP $code)" +
                                (errorBody?.take(180)?.let { ": $it" } ?: "."),
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        } catch (error: Exception) {
            GraphHttp.Failed(
                error.message?.takeIf { it.isNotBlank() }
                    ?: "Could not reach Microsoft Graph. Check your network and try again.",
            )
        }
    }

    private sealed interface GraphHttp {
        data class Ok(val body: String) : GraphHttp
        data object Unauthorized : GraphHttp
        data class Failed(val message: String) : GraphHttp
    }

    companion object {
        fun parseSectionsResponse(json: String): OneNoteSectionsListResponse {
            val root = JsonParser.parseString(json).asJsonObject
            val values = root.getAsJsonArray("value")
            val ids = buildList {
                if (values != null) {
                    for (element in values) {
                        val id = element.asJsonObject.get("id")?.asString?.trim().orEmpty()
                        if (id.isNotEmpty()) add(id)
                    }
                }
            }
            val next = root.get("@odata.nextLink")?.asString?.takeIf { it.isNotBlank() }
            return OneNoteSectionsListResponse(sectionIds = ids, nextLink = next)
        }

        fun parseListResponse(json: String): OneNotePagesListResponse {
            val root = JsonParser.parseString(json).asJsonObject
            val values = root.getAsJsonArray("value")
            val pages = buildList {
                if (values != null) {
                    for (element in values) {
                        val item = element.asJsonObject
                        val id = item.get("id")?.asString?.trim().orEmpty()
                        if (id.isEmpty()) continue
                        add(
                            OneNotePageSummary(
                                id = id,
                                title = item.get("title")?.asString?.takeIf { it.isNotBlank() },
                                contentUrl = item.get("contentUrl")?.asString
                                    ?.takeIf { it.isNotBlank() },
                                createdDateTime = item.get("createdDateTime")?.asString
                                    ?.takeIf { it.isNotBlank() },
                                lastModifiedDateTime = item.get("lastModifiedDateTime")?.asString
                                    ?.takeIf { it.isNotBlank() },
                            ),
                        )
                    }
                }
            }
            val next = root.get("@odata.nextLink")?.asString?.takeIf { it.isNotBlank() }
            return OneNotePagesListResponse(pages = pages, nextLink = next)
        }
    }
}
