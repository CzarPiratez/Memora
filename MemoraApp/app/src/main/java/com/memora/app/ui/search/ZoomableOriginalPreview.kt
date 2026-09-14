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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
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
import com.memora.app.application.handoff.OpenOriginalOutcome
import com.memora.app.application.handoff.OriginalHandoffRequest
import com.memora.app.application.handoff.ShareOriginalOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun interface OriginalPreviewReloader {
    suspend fun reload(
        request: OriginalPreviewReloadRequest,
        maxEdgePx: Int,
    ): OriginalPreviewReloadResult
}

val LocalOriginalPreviewReloader = compositionLocalOf<OriginalPreviewReloader> {
    OriginalPreviewReloader { _, _ -> OriginalPreviewReloadResult.Unavailable }
}

fun interface OriginalShareLauncher {
    suspend fun share(request: OriginalHandoffRequest): ShareOriginalOutcome
}

val LocalOriginalShareLauncher = compositionLocalOf<OriginalShareLauncher> {
    OriginalShareLauncher { ShareOriginalOutcome.CouldNotShare }
}

fun interface OriginalOpenLauncher {
    suspend fun open(request: OriginalHandoffRequest): OpenOriginalOutcome
}

val LocalOriginalOpenLauncher = compositionLocalOf<OriginalOpenLauncher> {
    OriginalOpenLauncher { OpenOriginalOutcome.CouldNotOpen }
}

fun OriginalPreviewReloadRequest.toHandoffRequest(): OriginalHandoffRequest = when (this) {
    is OriginalPreviewReloadRequest.Pdf -> OriginalHandoffRequest.Pdf(
        sourceId = sourceId,
        sourceAssetKey = sourceAssetKey,
        label = documentLabel,
    )
    is OriginalPreviewReloadRequest.Photo -> OriginalHandoffRequest.Photo(
        sourceId = sourceId,
        sourceAssetKey = sourceAssetKey,
        label = photoLabel,
    )
    is OriginalPreviewReloadRequest.Screenshot -> OriginalHandoffRequest.Screenshot(
        sourceId = sourceId,
        sourceAssetKey = sourceAssetKey,
        label = screenshotLabel,
    )
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
    val shareLauncher = LocalOriginalShareLauncher.current
    val openLauncher = LocalOriginalOpenLauncher.current
    val handoffScope = rememberCoroutineScope()
    var handingOff by remember(reloadRequest) { mutableStateOf(false) }
    var shareOutcome by remember(reloadRequest) {
        mutableStateOf<ShareOriginalOutcome?>(null)
    }
    var openOutcome by remember(reloadRequest) {
        mutableStateOf<OpenOriginalOutcome?>(null)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 32.dp)
            .padding(top = 24.dp, bottom = 16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onClose,
                modifier = Modifier.weight(1f),
            ) {
                Text(closeLabel)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    handoffScope.launch {
                        handingOff = true
                        shareOutcome = null
                        openOutcome = null
                        try {
                            shareOutcome = shareLauncher.share(reloadRequest.toHandoffRequest())
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            shareOutcome = ShareOriginalOutcome.CouldNotShare
                        } finally {
                            handingOff = false
                        }
                    }
                },
                enabled = !handingOff,
                modifier = Modifier.weight(1f),
            ) {
                Text(OriginalHandoffCopy.SHARE_LABEL)
            }
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
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                handoffScope.launch {
                    handingOff = true
                    shareOutcome = null
                    openOutcome = null
                    try {
                        openOutcome = openLauncher.open(reloadRequest.toHandoffRequest())
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        openOutcome = OpenOriginalOutcome.CouldNotOpen
                    } finally {
                        handingOff = false
                    }
                }
            },
            enabled = !handingOff,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(OriginalHandoffCopy.OPEN_LABEL)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = OriginalHandoffCopy.HINT_BODY,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = OriginalPreviewZoomCopy.HINT_BODY,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HandoffFeedback(shareOutcome = shareOutcome, openOutcome = openOutcome)
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

/** One place for both handoff results, so only the last tap speaks. */
@Composable
private fun HandoffFeedback(
    shareOutcome: ShareOriginalOutcome?,
    openOutcome: OpenOriginalOutcome?,
) {
    val message = when {
        openOutcome == OpenOriginalOutcome.NoAppAvailable -> OriginalHandoffCopy.NO_APP_BODY
        openOutcome == OpenOriginalOutcome.SourceUnavailable ->
            OriginalHandoffCopy.SOURCE_UNAVAILABLE_BODY
        openOutcome == OpenOriginalOutcome.CouldNotOpen ->
            OriginalHandoffCopy.COULD_NOT_OPEN_BODY
        shareOutcome == ShareOriginalOutcome.SourceUnavailable ->
            OriginalHandoffCopy.SOURCE_UNAVAILABLE_BODY
        shareOutcome == ShareOriginalOutcome.CouldNotShare ->
            OriginalHandoffCopy.COULD_NOT_SHARE_BODY
        else -> null
    } ?: return
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
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
    val initialImage = remember(initialPixels) { initialPixels.toImageBitmapOrNull() }
    var scale by remember(reloadRequest) { mutableFloatStateOf(PreviewZoomPolicy.MIN_SCALE) }
    var offset by remember(reloadRequest) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var image by remember(initialImage) { mutableStateOf(initialImage) }
    var sharpening by remember(reloadRequest) { mutableStateOf(false) }
    var couldNotSharpen by remember(reloadRequest) { mutableStateOf(false) }
    val targetEdge = PreviewZoomPolicy.renderEdgePx(scale, initialEdgePx)

    LaunchedEffect(reloadRequest, targetEdge, initialImage) {
        if (!PreviewZoomPolicy.needsReload(targetEdge, initialEdgePx)) {
            image = initialImage
            sharpening = false
            couldNotSharpen = false
            return@LaunchedEffect
        }
        sharpening = true
        delay(PreviewZoomPolicy.RERENDER_DEBOUNCE_MS)
        // Decoding and the ARGB copy both happen off the composition so a phone
        // that cannot spare the memory degrades to honest copy, not a crash.
        val sharper = when (val loaded = reloader.reload(reloadRequest, targetEdge)) {
            is OriginalPreviewReloadResult.Ready -> withContext(Dispatchers.Default) {
                PreviewPixels(loaded.widthPx, loaded.heightPx, loaded.argb8888)
                    .toImageBitmapOrNull()
            }
            OriginalPreviewReloadResult.Unavailable -> null
        }
        if (sharper == null) {
            couldNotSharpen = true
        } else {
            image = sharper
            couldNotSharpen = false
        }
        sharpening = false
    }

    val imageWidthPx = image?.width ?: 0
    val imageHeightPx = image?.height ?: 0
    val fittedHeight = if (imageWidthPx > 0 && viewport.width > 0) {
        viewport.width.toFloat() * imageHeightPx.toFloat() / imageWidthPx.toFloat()
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
        } else if (couldNotSharpen) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = OriginalPreviewZoomCopy.COULD_NOT_SHARPEN_BODY,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
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
                        val currentWidthPx = image?.width ?: 0
                        val currentHeightPx = image?.height ?: 0
                        val (x, y) = PreviewZoomPolicy.constrainOffset(
                            offsetX = offset.x + pan.x,
                            offsetY = offset.y + pan.y,
                            scale = scale,
                            viewportWidthPx = size.width.toFloat(),
                            viewportHeightPx = size.height.toFloat(),
                            fittedHeightPx = if (currentWidthPx > 0) {
                                size.width.toFloat() * currentHeightPx.toFloat() /
                                    currentWidthPx.toFloat()
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
            image?.let { shown ->
                Image(
                    bitmap = shown,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.FillWidth,
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
}

/**
 * Never recycles: Compose may still be drawing the previous frame when a sharper
 * read swaps the image. Returns null instead of throwing when the phone cannot
 * spare the ARGB copy.
 */
private fun PreviewPixels.toImageBitmapOrNull(): ImageBitmap? =
    if (widthPx <= 0 || heightPx <= 0 || argb8888.size != widthPx * heightPx) {
        null
    } else {
        runCatching {
            Bitmap.createBitmap(argb8888, widthPx, heightPx, Bitmap.Config.ARGB_8888)
                .asImageBitmap()
        }.getOrNull()
    }
