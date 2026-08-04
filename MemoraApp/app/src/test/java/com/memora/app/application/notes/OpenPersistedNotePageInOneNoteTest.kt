package com.memora.app.application.notes

import com.memora.app.data.notes.OneNotePageLinks
import com.memora.app.data.notes.OneNotePageLinksGraphResult
import com.memora.app.data.notes.OneNotePagesGraphGateway
import com.memora.app.data.notes.OneNotePagesGraphResult
import com.memora.app.data.notes.OneNotePageContentGraphResult
import com.memora.app.data.notes.OneNoteSectionsGraphResult
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetFingerprint
import com.memora.app.domain.asset.AssetIdentity
import com.memora.app.domain.asset.AssetIndexRecord
import com.memora.app.domain.asset.AssetLocation
import com.memora.app.domain.asset.AssetRepository
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.IndexingState
import com.memora.app.domain.asset.SourceAssetKey
import com.memora.app.domain.asset.SourceId
import com.memora.app.domain.notes.NotesProviderSession
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenPersistedNotePageInOneNoteTest {
    @Test
    fun prefersWebUrlAndNeverUsesContentLocation() = runTest {
        val useCase = OpenPersistedNotePageInOneNote(
            assetRepository = FakeAssetRepository(noteAsset()),
            oneNoteAuth = FakeAuth(session = sampleSession()),
            graphGateway = FakeGateway(
                links = OneNotePageLinks(
                    oneNoteWebUrl = "https://onenote.example/web",
                    oneNoteClientUrl = "onenote:https://onenote.example/client",
                ),
            ),
        )

        val result = useCase("microsoft.onenote", "page-1")
        assertEquals(
            OpenPersistedNotePageResult.Ready(
                webUrl = "https://onenote.example/web",
                clientUrl = "onenote:https://onenote.example/client",
            ),
            result,
        )
    }

    @Test
    fun missingSessionIsSourceUnavailable() = runTest {
        val useCase = OpenPersistedNotePageInOneNote(
            assetRepository = FakeAssetRepository(noteAsset()),
            oneNoteAuth = FakeAuth(session = null),
            graphGateway = FakeGateway(
                links = OneNotePageLinks(
                    oneNoteWebUrl = "https://onenote.example/web",
                    oneNoteClientUrl = null,
                ),
            ),
        )
        assertEquals(
            OpenPersistedNotePageResult.SourceUnavailable,
            useCase("microsoft.onenote", "page-1"),
        )
    }

    @Test
    fun emptyLinksIsCouldNotOpen() = runTest {
        val useCase = OpenPersistedNotePageInOneNote(
            assetRepository = FakeAssetRepository(noteAsset()),
            oneNoteAuth = FakeAuth(session = sampleSession()),
            graphGateway = FakeGateway(
                links = OneNotePageLinks(oneNoteWebUrl = null, oneNoteClientUrl = null),
            ),
        )
        assertEquals(
            OpenPersistedNotePageResult.CouldNotOpen,
            useCase("microsoft.onenote", "page-1"),
        )
    }

    @Test
    fun unauthorizedIsSourceUnavailable() = runTest {
        val useCase = OpenPersistedNotePageInOneNote(
            assetRepository = FakeAssetRepository(noteAsset()),
            oneNoteAuth = FakeAuth(session = sampleSession()),
            graphGateway = FakeGateway(unauthorized = true),
        )
        assertEquals(
            OpenPersistedNotePageResult.SourceUnavailable,
            useCase("microsoft.onenote", "page-1"),
        )
    }

    @Test
    fun unauthorizedRetriesOnceAfterForcedSilentRefresh() = runTest {
        val auth = FakeAuth(session = sampleSession())
        val gateway = FakeGateway(
            unauthorizedFirstCalls = 1,
            links = OneNotePageLinks(
                oneNoteWebUrl = "https://onenote.example/web",
                oneNoteClientUrl = null,
            ),
        )
        val useCase = OpenPersistedNotePageInOneNote(
            assetRepository = FakeAssetRepository(noteAsset()),
            oneNoteAuth = auth,
            graphGateway = gateway,
        )
        assertEquals(
            OpenPersistedNotePageResult.Ready(
                webUrl = "https://onenote.example/web",
                clientUrl = null,
            ),
            useCase("microsoft.onenote", "page-1"),
        )
        assertEquals(1, auth.forceRefreshCalls)
        assertEquals(2, gateway.getPageLinksCalls)
    }

    private fun noteAsset() = Asset(
        identity = AssetIdentity(SourceId("microsoft.onenote"), SourceAssetKey("page-1")),
        type = AssetType.NOTE,
        location = AssetLocation(
            "https://graph.microsoft.com/v1.0/me/onenote/pages/page-1/content",
        ),
        fingerprint = AssetFingerprint("fp-1"),
        discoveredAt = Instant.parse("2026-08-02T00:00:00Z"),
        displayName = "Ideas",
    )

    private fun sampleSession() = NotesProviderSession(
        providerId = NotesProviderSession.PROVIDER_MICROSOFT_ONENOTE,
        accountId = "acct-1",
        accountDisplayLabel = "mir.m@outlook.com",
        accessToken = "token",
        accessTokenExpiresAtEpochMs = 1_775_000_000_000L,
    )

    private class FakeAuth(
        private val session: NotesProviderSession?,
    ) : OneNoteInteractiveAuth {
        var forceRefreshCalls: Int = 0
            private set

        override suspend fun connect(activity: android.app.Activity) =
            error("not used")
        override suspend fun disconnect() = Unit
        override suspend fun restoreAccountLabel(): String? = session?.accountDisplayLabel
        override suspend fun ensureSession(forceRefresh: Boolean): NotesProviderSession? {
            if (forceRefresh) forceRefreshCalls += 1
            return session
        }
    }

    private class FakeGateway(
        private val links: OneNotePageLinks = OneNotePageLinks(null, null),
        private val unauthorized: Boolean = false,
        private val unauthorizedFirstCalls: Int = 0,
    ) : OneNotePagesGraphGateway {
        var getPageLinksCalls: Int = 0
            private set

        override suspend fun listSections(accessToken: String, requestUrl: String) =
            OneNoteSectionsGraphResult.Failed("unused")
        override suspend fun listPages(accessToken: String, requestUrl: String) =
            OneNotePagesGraphResult.Failed("unused")
        override suspend fun fetchPageContent(accessToken: String, contentUrl: String) =
            OneNotePageContentGraphResult.Failed("unused")
        override suspend fun getPageLinks(accessToken: String, pageId: String): OneNotePageLinksGraphResult {
            assertTrue(pageId == "page-1")
            getPageLinksCalls += 1
            if (unauthorized) return OneNotePageLinksGraphResult.Unauthorized
            if (getPageLinksCalls <= unauthorizedFirstCalls) {
                return OneNotePageLinksGraphResult.Unauthorized
            }
            return OneNotePageLinksGraphResult.Ok(links)
        }
    }

    private class FakeAssetRepository(asset: Asset) : AssetRepository {
        private val records = mutableMapOf(asset.identity to AssetIndexRecord(asset, IndexingState.discovered))
        override suspend fun save(record: AssetIndexRecord) { records[record.asset.identity] = record }
        override suspend fun find(identity: AssetIdentity): AssetIndexRecord? = records[identity]
        override suspend fun findFirstBySourceAndType(sourceId: SourceId, type: AssetType): Asset? = null
        override suspend fun countBySourceAndType(sourceId: SourceId, type: AssetType): Int = 0
        override suspend fun findNextPdfPendingLocalReading(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null
    override suspend fun countPdfPendingLocalReading(
        sourceId: SourceId,
        schemaVersion: String,
    ): Int = 0
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
        override suspend fun findNextNotePendingPageExtract(
            sourceId: SourceId,
            schemaVersion: String,
            afterSourceAssetKey: String?,
        ): Asset? = null
    }
}
