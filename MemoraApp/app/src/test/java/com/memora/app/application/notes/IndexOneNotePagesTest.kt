package com.memora.app.application.notes

import com.memora.app.application.discovery.DiscoverSourcePage
import com.memora.app.application.discovery.PersistDiscoveryPage
import com.memora.app.application.discovery.ProcessDiscoveryResult
import com.memora.app.data.notes.InMemoryNotesProviderTokenVault
import com.memora.app.data.notes.OneNotePageSummary
import com.memora.app.data.notes.OneNotePagesDiscoverySource
import com.memora.app.data.notes.OneNotePagesGraphGateway
import com.memora.app.data.notes.OneNotePagesGraphResult
import com.memora.app.data.notes.OneNotePagesListResponse
import com.memora.app.data.notes.OneNoteSectionsGraphResult
import com.memora.app.data.notes.OneNoteSectionsListResponse
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.discovery.DiscoveryCheckpointRepository
import com.memora.app.domain.discovery.DiscoveryCursor
import com.memora.app.domain.discovery.DiscoveryPage
import com.memora.app.domain.discovery.DiscoveryPageStore
import com.memora.app.domain.notes.NotesProviderSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexOneNotePagesTest {

    @Test
    fun persistsOneBoundedPageAndReportsTotals() = runBlocking {
        val vault = InMemoryNotesProviderTokenVault().apply {
            writeSession(
                NotesProviderSession(
                    providerId = NotesProviderSession.PROVIDER_MICROSOFT_ONENOTE,
                    accountId = "a",
                    accountDisplayLabel = "u@example.com",
                    accessToken = "t",
                    accessTokenExpiresAtEpochMs = 9_999_999_999_999L,
                ),
            )
        }
        val store = RecordingStore()
        val checkpoints = InMemoryCheckpoints()
        val source = OneNotePagesDiscoverySource(
            tokenVault = vault,
            graphGateway = object : OneNotePagesGraphGateway {
                override suspend fun listSections(
                    accessToken: String,
                    requestUrl: String,
                ): OneNoteSectionsGraphResult = OneNoteSectionsGraphResult.Ok(
                    OneNoteSectionsListResponse(
                        sectionIds = listOf("sec1"),
                        nextLink = null,
                    ),
                )

                override suspend fun listPages(
                    accessToken: String,
                    requestUrl: String,
                ): OneNotePagesGraphResult = OneNotePagesGraphResult.Ok(
                    OneNotePagesListResponse(
                        pages = listOf(
                            OneNotePageSummary(
                                id = "n1",
                                title = "Note",
                                contentUrl = "https://example.com/n1",
                                createdDateTime = null,
                                lastModifiedDateTime = "2024-01-01T00:00:00Z",
                            ),
                        ),
                        nextLink =
                            "https://graph.microsoft.com/v1.0/me/onenote/sections/sec1/pages?skiptoken=1",
                    ),
                )
            },
        )
        val assets = CountingAssetRepository()
        val useCase = IndexOneNotePages(
            oneNoteSource = source,
            discoverSourcePage = DiscoverSourcePage(
                checkpointRepository = checkpoints,
                processDiscoveryResult = ProcessDiscoveryResult(
                    persistDiscoveryPage = PersistDiscoveryPage(store),
                ),
            ),
            assetRepository = assets,
            oneNoteAuth = object : OneNoteInteractiveAuth {
                override suspend fun connect(activity: android.app.Activity) =
                    OneNoteAuthOutcome.RegistrationRequired
                override suspend fun disconnect() = Unit
                override suspend fun restoreAccountLabel(): String? = "u@example.com"
                override suspend fun ensureSession(): NotesProviderSession? =
                    vault.readSession()
            },
        )

        val outcome = useCase(batchSize = 5) as OneNoteDiscoveryOutcome.Discovered
        assertEquals(1, outcome.pageAssetCount)
        assertEquals(1, outcome.totalNoteAssets)
        assertTrue(outcome.hasMore)
        assertEquals(1, store.savedPage?.assets?.size)
    }

    private class RecordingStore : DiscoveryPageStore {
        var savedPage: DiscoveryPage? = null
        override suspend fun save(page: DiscoveryPage) {
            savedPage = page
        }
    }

    private class InMemoryCheckpoints : DiscoveryCheckpointRepository {
        private val values = mutableMapOf<SourceId, DiscoveryCursor>()
        override suspend fun find(sourceId: SourceId): DiscoveryCursor? = values[sourceId]
        override suspend fun save(cursor: DiscoveryCursor) {
            values[cursor.sourceId] = cursor
        }
    }

    private class CountingAssetRepository : AssetRepository {
        override suspend fun save(record: AssetIndexRecord) = Unit
        override suspend fun find(identity: AssetIdentity): AssetIndexRecord? = null
        override suspend fun findFirstBySourceAndType(sourceId: SourceId, type: AssetType): Asset? =
            null
        override suspend fun countBySourceAndType(sourceId: SourceId, type: AssetType): Int =
            if (sourceId == OneNotePagesDiscoverySource.SOURCE_ID && type == AssetType.NOTE) 1 else 0
        override suspend fun findNextPdfPendingLocalReading(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null
        override suspend fun findNextImagePendingExifExtract(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null
        override suspend fun findNextScreenshotPendingOcrExtract(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null
        override suspend fun findNextPhotoPendingOcrExtract(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null
    }
}
