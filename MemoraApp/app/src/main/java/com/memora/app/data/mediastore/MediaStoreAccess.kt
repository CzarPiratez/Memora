package com.memora.app.data.mediastore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class MediaStoreAccess { FULL, SELECTED, REQUIRED }

/** Checks the live Android grant; no permission state is cached. */
fun mediaStoreImageAccess(context: Context): MediaStoreAccess = when {
    Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_MEDIA_IMAGES
    ) == PackageManager.PERMISSION_GRANTED -> MediaStoreAccess.FULL
    Build.VERSION.SDK_INT >= 34 && ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    ) == PackageManager.PERMISSION_GRANTED -> MediaStoreAccess.SELECTED
    Build.VERSION.SDK_INT <= 32 && ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED -> MediaStoreAccess.FULL
    else -> MediaStoreAccess.REQUIRED
}
