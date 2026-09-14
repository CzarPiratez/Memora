package com.memora.app.ui.search

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.memora.app.application.preview.OriginalPreviewReloadRequest
import com.memora.app.application.preview.OriginalPreviewReloadResult
import com.memora.app.application.preview.PreviewZoomPolicy
import kotlinx.coroutines.delay

fun interface OriginalPreviewReloader {
    suspend fun reload(
        request: OriginalPreviewReloadRequest,
        maxEdgePx: Int,
    ): OriginalPreviewReloadResult
}

val LocalOriginalPreviewReloader = compositionLocalOf<OriginalPreviewReloader> {
    OriginalPreviewReloader { _, _ -> OriginalPreviewReloadResult.Unavailable }
}

private data class PreviewPixels(
    val widthPx: Int,
    val heightPx: Int,
    val argb8888: IntArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PreviewPixels) return false
        return widthPx == other.widthPx &&
            heightPx == other.heightPx &&
            argb8888.contentEquals(other.argb8888)
    }

    override fun hashCode(): Int {
        var result = widthPx
        result = 31 * result + heightPx
        result = 31 * result + argb8888.contentHashCode()
        return result
    }
}

@Composable
fun OriginalPreviewScaffold(
    onClose: () -> Unit,
    closeLabel: String,
    title: String,
    subtitle: String,
    scopeBody: String,
    contentDescription: String,
    reloadRequest: OriginalPreviewReloadRequest,
    initialEdgePx: Int,
    widthPx: Int,
    heightPx: Int,
    argb8888: IntArray,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp)
            .padding(top = 24.dp, bottom = 16.dp),
    ) {
        Button(onClick = onClose) {
            Text(closeLabel)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (caption != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = scopeBody,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = OriginalPreviewZoomCopy.HINT_BODY,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        ZoomableOriginalImage(
            reloadRequest = reloadRequest,
            initialEdgePx = initialEdgePx,
            initialWidthPx = widthPx,
            initialHeightPx = heightPx,
            initialArgb8888 = argb8888,
            contentDescription = contentDescription,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun ZoomableOriginalImage(
    reloadRequest: OriginalPreviewReloadRequest,
    initialEdgePx: Int,
    initialWidthPx: Int,
    initialHeightPx: Int,
    initialArgb8888: IntArray,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val reloader = LocalOriginalPreviewReloader.current
    val initialPixels = remember(initialWidthPx, initialHeightPx, initialArgb8888) {
        PreviewPixels(initialWidthPx, initialHeightPx, initialArgb8888)
    }
    var scale by remember(reloadRequest) { mutableFloatStateOf(PreviewZoomPolicy.MIN_SCALE) }
    var offset by remember(reloadRequest) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var pixels by remember(initialPixels) { mutableStateOf(initialPixels) }
    var sharpening by remember { mutableStateOf(false) }
    val targetEdge = PreviewZoomPolicy.renderEdgePx(scale, initialEdgePx)

    LaunchedEffect(reloadRequest, targetEdge, initialPixels) {
        if (!PreviewZoomPolicy.needsReload(targetEdge, initialEdgePx)) {
            pixels = initialPixels
            sharpening = false
            return@LaunchedEffect
        }
        sharpening = true
        delay(PreviewZoomPolicy.RERENDER_DEBOUNCE_MS)
        when (val loaded = reloader.reload(reloadRequest, targetEdge)) {
            is OriginalPreviewReloadResult.Ready -> {
                pixels = PreviewPixels(loaded.widthPx, loaded.heightPx, loaded.argb8888)
            }
            OriginalPreviewReloadResult.Unavailable -> Unit
        }
        sharpening = false
    }

    val fittedHeight = if (pixels.widthPx > 0 && viewport.width > 0) {
        viewport.width.toFloat() * pixels.heightPx.toFloat() / pixels.widthPx.toFloat()
    } else {
        0f
    }

    fun applyOffset(next: Offset) {
        val (x, y) = PreviewZoomPolicy.constrainOffset(
            offsetX = next.x,
            offsetY = next.y,
            scale = scale,
            viewportWidthPx = viewport.width.toFloat(),
            viewportHeightPx = viewport.height.toFloat(),
            fittedHeightPx = fittedHeight,
        )
        offset = Offset(x, y)
    }

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    scale = PreviewZoomPolicy.zoomOut(scale)
                    if (!PreviewZoomPolicy.canZoomOut(scale)) offset = Offset.Zero
                    else applyOffset(offset)
                },
                enabled = PreviewZoomPolicy.canZoomOut(scale),
                modifier = Modifier.weight(1f),
            ) {
                Text(OriginalPreviewZoomCopy.ZOOM_OUT_LABEL)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { scale = PreviewZoomPolicy.zoomIn(scale) },
                enabled = PreviewZoomPolicy.canZoomIn(scale),
                modifier = Modifier.weight(1f),
            ) {
                Text(OriginalPreviewZoomCopy.ZOOM_IN_LABEL)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    scale = PreviewZoomPolicy.MIN_SCALE
                    offset = Offset.Zero
                },
                enabled = PreviewZoomPolicy.canZoomOut(scale),
                modifier = Modifier.weight(1f),
            ) {
                Text(OriginalPreviewZoomCopy.FIT_LABEL)
            }
        }
        if (sharpening) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = OriginalPreviewZoomCopy.SHARPENING_BODY,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { viewport = it }
                .pointerInput(reloadRequest) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = PreviewZoomPolicy.clampScale(scale * zoom)
                        val (x, y) = PreviewZoomPolicy.constrainOffset(
                            offsetX = offset.x + pan.x,
                            offsetY = offset.y + pan.y,
                            scale = scale,
                            viewportWidthPx = size.width.toFloat(),
                            viewportHeightPx = size.height.toFloat(),
                            fittedHeightPx = if (pixels.widthPx > 0) {
                                size.width.toFloat() * pixels.heightPx.toFloat() /
                                    pixels.widthPx.toFloat()
                            } else {
                                0f
                            },
                        )
                        offset = Offset(x, y)
                    }
                }
                .pointerInput(reloadRequest) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = PreviewZoomPolicy.toggleDoubleTap(scale)
                            if (scale <= PreviewZoomPolicy.MIN_SCALE) {
                                offset = Offset.Zero
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            PreviewBitmapImage(
                pixels = pixels,
                contentDescription = contentDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }
    }
}

@Composable
private fun PreviewBitmapImage(
    pixels: PreviewPixels,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(pixels) {
        Bitmap.createBitmap(
            pixels.argb8888,
            pixels.widthPx,
            pixels.heightPx,
            Bitmap.Config.ARGB_8888,
        )
    }
    DisposableEffect(bitmap) {
        onDispose { bitmap.recycle() }
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.FillWidth,
    )
}
