package com.memora.app.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.memora.app.application.find.MediaStoreImageUriResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidMediaStoreImageUriResolver @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : MediaStoreImageUriResolver {
    override fun candidates(storedUri: Uri, displayName: String): List<Uri> {
        val ordered = LinkedHashSet<Uri>()
        ordered.add(storedUri)
        val mediaId = storedUri.lastPathSegment?.toLongOrNull()
        if (mediaId != null) {
            ordered.add(
                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ordered.add(
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                        mediaId,
                    ),
                )
                ordered.add(
                    ContentUris.withAppendedId(
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                        mediaId,
                    ),
                )
            }
        }
        resolveCurrentUriByDisplayName(displayName)?.let { ordered.add(it) }
        return ordered.toList()
    }

    private fun resolveCurrentUriByDisplayName(displayName: String): Uri? {
        if (displayName.isBlank()) return null
        val resolver = context.applicationContext.contentResolver
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val args = arrayOf(displayName)
        val bases = buildList {
            add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL))
                add(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY))
            }
        }
        for (base in bases) {
            val uri = try {
                resolver.query(base, projection, selection, args, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    ContentUris.withAppendedId(base, cursor.getLong(0))
                }
            } catch (error: Exception) {
                Log.w(TAG, "Display-name query failed on $base: ${error.javaClass.simpleName}")
                null
            }
            if (uri != null) return uri
        }
        return null
    }

    private companion object {
        const val TAG = "MemoraImageUri"
    }
}
