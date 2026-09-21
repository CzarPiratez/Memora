package com.memora.app.ui.search

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.memora.app.application.find.FindThumbnailGlyph
import com.memora.app.application.find.FindThumbnailRequest
import com.memora.app.application.find.FindThumbnailResult
import com.memora.app.domain.asset.AssetType

fun interface FindThumbnailLoader {
    suspend fun load(request: FindThumbnailRequest): FindThumbnailResult
}

val LocalFindThumbnailLoader = compositionLocalOf<FindThumbnailLoader> {
    FindThumbnailLoader { request ->
        FindThumbnailResult.Glyph(
            if (request.assetType == AssetType.NOTE) {
                FindThumbnailGlyph.NOTE
            } else {
                FindThumbnailGlyph.UNAVAILABLE
            },
        )
    }
}

private val ThumbnailShape = RoundedCornerShape(12.dp)
private val ThumbnailSize = 72.dp

@Composable
fun FindResultThumbnail(
    request: FindThumbnailRequest,
    modifier: Modifier = Modifier,
    onLoaded: (FindThumbnailResult) -> Unit = {},
) {
    val loader = LocalFindThumbnailLoader.current
    var result by remember(request.cacheKey) { mutableStateOf<FindThumbnailResult?>(null) }
    LaunchedEffect(request.cacheKey) {
        val loaded = loader.load(request)
        result = loaded
        onLoaded(loaded)
    }
    Box(
        modifier = modifier
            .size(ThumbnailSize)
            .clip(ThumbnailShape)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        when (val loaded = result) {
            null -> Unit
            is FindThumbnailResult.Ready -> ThumbnailImage(request, loaded)
            is FindThumbnailResult.Glyph -> ThumbnailGlyph(request, loaded)
        }
    }
}

@Composable
private fun ThumbnailImage(
    request: FindThumbnailRequest,
    ready: FindThumbnailResult.Ready,
) {
    val bitmap = remember(ready) {
        Bitmap.createBitmap(
            ready.argb8888,
            ready.widthPx,
            ready.heightPx,
            Bitmap.Config.ARGB_8888,
        ).asImageBitmap()
    }
    val spoken = FindThumbnailCopy.spokenDescription(request, ready)
    Image(
        bitmap = bitmap,
        contentDescription = spoken,
        modifier = Modifier.size(ThumbnailSize),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun ThumbnailGlyph(
    request: FindThumbnailRequest,
    glyph: FindThumbnailResult.Glyph,
) {
    val spoken = FindThumbnailCopy.spokenDescription(request, glyph)
    Text(
        text = FindThumbnailCopy.glyphLabel(glyph.kind, request.assetType),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = if (spoken != null) {
            Modifier.semantics { contentDescription = spoken }
        } else {
            Modifier
        },
    )
}
