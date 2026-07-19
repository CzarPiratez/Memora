package com.memora.app.data.mediastore

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.memora.app.domain.asset.Asset
import com.memora.app.domain.asset.AssetType
import com.memora.app.domain.asset.SourceAccessModel
import com.memora.app.domain.asset.SourceCapability
import com.memora.app.domain.discovery.DiscoveryFailure
import com.memora.app.domain.discovery.DiscoveryPage
import com.memora.app.domain.discovery.DiscoveryRequest
import com.memora.app.domain.discovery.DiscoveryResult
import com.memora.app.domain.discovery.ImageLibraryAccessScope
import com.memora.app.domain.discovery.ImageLibraryDiscoverySource
import com.memora.app.domain.discovery.SourceAccessState
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Read-only adapter for Android's shared-image catalogue.
 *
 * This class reads only MediaStore metadata. It never opens an image URI, reads image
 * bytes, creates thumbnails, writes to MediaStore, or starts a scan on its own.
 */
class MediaStoreImageDiscoverySource(
    context: Context,
    private val clock: Clock = Clock.systemUTC(),
) : ImageLibraryDiscoverySource {
    private val appContext = context.applicationContext
    private val resolver: ContentResolver = appContext.contentResolver

    override val capability = SourceCapability(
        sourceId = MediaStoreImageMapper.sourceId,
        supportedAssetTypes = setOf(AssetType.PHOTO, AssetType.SCREENSHOT),
        accessModel = SourceAccessModel.ANDROID_MEDIA_PERMISSION,
        supportsIncrementalDiscovery = true,
        supportsBackgroundIndexing = true,
    )

    override suspend fun accessState(): SourceAccessState = if (accessScope() == null) {
        SourceAccessState.ACCESS_REQUIRED
    } else {
        SourceAccessState.GRANTED
    }

    override suspend fun accessScope(): ImageLibraryAccessScope? = when (mediaStoreImageAccess(appContext)) {
        MediaStoreAccess.FULL -> ImageLibraryAccessScope.FULL_LIBRARY
        MediaStoreAccess.SELECTED -> ImageLibraryAccessScope.SELECTED_PHOTOS
        MediaStoreAccess.REQUIRED -> null
    }

    override suspend fun discover(request: DiscoveryRequest): DiscoveryResult = withContext(Dispatchers.IO) {
        if (accessState() != SourceAccessState.GRANTED) {
            return@withContext DiscoveryResult.AccessRequired
        }

        try {
            val version = currentStoreVersion()
            val checkpoint = checkpointFor(request, version)
            val rows = readRows(checkpoint, request.batchSize + 1)
            val returnedRows = rows.take(request.batchSize)
            val assets = returnedRows.map { MediaStoreImageMapper.toAsset(it, Instant.now(clock)) }
            val nextCheckpoint = returnedRows.lastOrNull()?.let { row ->
                MediaStoreDiscoveryCheckpoint(
                    storeVersion = version,
                    watermark = row.generationModified,
                    lastMediaId = row.mediaId,
                )
            } ?: checkpoint

            DiscoveryResult.Page(
                DiscoveryPage(
                    sourceId = capability.sourceId,
                    assets = assets,
                    checkpoint = nextCheckpoint.toCursor(),
                    hasMore = rows.size > request.batchSize,
                )
            )
        } catch (_: SecurityException) {
            DiscoveryResult.AccessRevoked
        } catch (_: IllegalArgumentException) {
            DiscoveryResult.Failed(
                DiscoveryFailure(
                    code = "INVALID_MEDIASTORE_CHECKPOINT",
                    message = "Memora could not safely resume the MediaStore scan.",
                )
            )
        } catch (_: Exception) {
            DiscoveryResult.Failed(
                DiscoveryFailure(
                    code = "MEDIASTORE_QUERY_FAILED",
                    message = "Memora could not read the MediaStore image catalogue. You can retry later.",
                )
            )
        }
    }

    private fun checkpointFor(
        request: DiscoveryRequest,
        currentVersion: String,
    ): MediaStoreDiscoveryCheckpoint {
        val requested = request.cursor?.let(MediaStoreDiscoveryCheckpoint::from)
            ?: return MediaStoreDiscoveryCheckpoint.initial(currentVersion)
        return requested.takeIf { it.storeVersion == currentVersion }
            ?: MediaStoreDiscoveryCheckpoint.initial(currentVersion)
    }

    private fun currentStoreVersion(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        MediaStore.getVersion(appContext)
    } else {
        LEGACY_STORE_VERSION
    }

    private fun readRows(
        checkpoint: MediaStoreDiscoveryCheckpoint,
        limit: Int,
    ): List<MediaStoreImageRow> {
        val versionColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.MediaColumns.GENERATION_MODIFIED
        } else {
            MediaStore.MediaColumns.DATE_MODIFIED
        }
        val selectionParts = mutableListOf(
            "($versionColumn > ? OR ($versionColumn = ? AND ${MediaStore.MediaColumns._ID} > ?))"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selectionParts += "${MediaStore.MediaColumns.IS_PENDING} = 0"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            selectionParts += "${MediaStore.MediaColumns.IS_TRASHED} = 0"
        }

        val queryArgs = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selectionParts.joinToString(" AND "))
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf(checkpoint.watermark.toString(), checkpoint.watermark.toString(), checkpoint.lastMediaId.toString()),
            )
            putString(
                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                "$versionColumn ASC, ${MediaStore.MediaColumns._ID} ASC",
            )
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        }

        return resolver.query(imageCollection(), projection(), queryArgs, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toRow())
            }
        } ?: emptyList()
    }

    private fun imageCollection() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    private fun projection(): Array<String> = buildList {
        add(MediaStore.MediaColumns._ID)
        add(MediaStore.MediaColumns.DISPLAY_NAME)
        add(MediaStore.MediaColumns.SIZE)
        add(MediaStore.MediaColumns.DATE_MODIFIED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaStore.MediaColumns.VOLUME_NAME)
            add(MediaStore.MediaColumns.RELATIVE_PATH)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(MediaStore.MediaColumns.GENERATION_MODIFIED)
        }
    }.toTypedArray()

    private fun android.database.Cursor.toRow(): MediaStoreImageRow = MediaStoreImageRow(
        volumeName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getString(getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME))
        } else {
            LEGACY_EXTERNAL_VOLUME
        },
        mediaId = getLong(getColumnIndexOrThrow(MediaStore.MediaColumns._ID)),
        displayName = getString(getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)),
        relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getString(getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH))
        } else {
            null
        },
        sizeBytes = getLong(getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
        generationModified = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getLong(getColumnIndexOrThrow(MediaStore.MediaColumns.GENERATION_MODIFIED))
        } else {
            getLong(getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
        },
        dateModifiedSeconds = getLong(getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)),
    )

    private companion object {
        const val LEGACY_STORE_VERSION = "legacy-media-store"
        const val LEGACY_EXTERNAL_VOLUME = "external"
    }
}
