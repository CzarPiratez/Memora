package com.memora.app.ui.search

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.memora.app.R
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

/** Near-black so the document is the subject, as in any phone photo viewer. */
private val PreviewCanvas = Color(0xFF0B0B0D)

/** Chrome sits in translucent circles so it survives a white PDF page. */
private val ChromeScrim = Color(0x99000000)

private val ChromeGradient = Color(0x8C000000)

private val StatusSurface = Color(0xE61A1A1E)

private val StatusError = Color(0xFFFFB4AB)

private enum class SharpenStatus { IDLE, SHARPENING, FAILED }

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

/**
 * An opened original, full-bleed.
 *
 * The picture takes the whole screen; Back, Share, and info are icons floating
 * over it and fade away while a gesture is in progress. Zoom is pinch and
 * double-tap only — no visible buttons — with `Zoom in` / `Zoom out` /
 * `Fit to screen` kept as TalkBack custom actions so a screen-reader user who
 * cannot pinch keeps the same capability
 * (`CHANGE_CONTROL_OPEN_ORIGINAL_PINCH_ZOOM` acceptance).
 *
 * [scopeBody] is a trust promise, not decoration: it says search used saved
 * text rather than a live re-read. It moves behind the info icon, where a
 * person can reach it on purpose, instead of sitting above the evidence.
 *
 * [caption] should be null when a document has only one page; "Page 1 of 1"
 * tells nobody anything.
 */
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
    var sharpenStatus by remember(reloadRequest) { mutableStateOf(SharpenStatus.IDLE) }
    var chromeVisible by remember(reloadRequest) { mutableStateOf(true) }
    var infoVisible by remember(reloadRequest) { mutableStateOf(false) }

    DarkSystemBarsWhileVisible()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PreviewCanvas),
    ) {
        ZoomableOriginalImage(
            reloadRequest = reloadRequest,
            initialEdgePx = initialEdgePx,
            initialWidthPx = widthPx,
            initialHeightPx = heightPx,
            initialArgb8888 = argb8888,
            contentDescription = contentDescription,
            onSharpenStatus = { sharpenStatus = it },
            onGesture = { chromeVisible = false },
            onTap = { chromeVisible = !chromeVisible },
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            PreviewTopChrome(
                closeLabel = closeLabel,
                caption = caption,
                shareEnabled = !handingOff,
                onClose = onClose,
                onInfo = { infoVisible = true },
                onShare = {
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
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Outside the fade: a failure must not be hidden by the gesture
            // that caused it.
            PreviewStatus(
                shareOutcome = shareOutcome,
                openOutcome = openOutcome,
                sharpenStatus = sharpenStatus,
            )
            AnimatedVisibility(visible = chromeVisible, enter = fadeIn(), exit = fadeOut()) {
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
                    shape = CircleShape,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text(
                        text = OriginalHandoffCopy.OPEN_LABEL,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (infoVisible) {
            PreviewInfoDialog(
                title = title,
                label = CanonicalRecallWhyCopy.friendlyDisplayLabel(subtitle),
                caption = caption,
                scopeBody = scopeBody,
                onDismiss = { infoVisible = false },
            )
        }
    }
}

/**
 * The rest of the app is light, so the system bars ask for dark icons. Over this
 * screen's near-black canvas that leaves an unreadable clock and a pale
 * navigation scrim. Flip both to light icons while the preview is on screen and
 * restore the app's choice on the way out.
 */
@Composable
private fun DarkSystemBarsWhileVisible() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        val wasLightStatusBars = controller.isAppearanceLightStatusBars
        val wasLightNavigationBars = controller.isAppearanceLightNavigationBars
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = wasLightStatusBars
            controller.isAppearanceLightNavigationBars = wasLightNavigationBars
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
private fun PreviewTopChrome(
    closeLabel: String,
    caption: String?,
    shareEnabled: Boolean,
    onClose: () -> Unit,
    onInfo: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(ChromeGradient, Color.Transparent)),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChromeIconButton(
                iconRes = R.drawable.ic_preview_back,
                description = closeLabel,
                onClick = onClose,
            )
            if (caption != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(color = ChromeScrim, shape = CircleShape) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            ChromeIconButton(
                iconRes = R.drawable.ic_preview_share,
                description = OriginalHandoffCopy.SHARE_LABEL,
                onClick = onShare,
                enabled = shareEnabled,
            )
            Spacer(modifier = Modifier.width(4.dp))
            ChromeIconButton(
                iconRes = R.drawable.ic_preview_info,
                description = OriginalPreviewChromeCopy.INFO_LABEL,
                onClick = onInfo,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** 24dp glyph on a 48dp target, in a translucent circle so it reads on white paper. */
@Composable
private fun ChromeIconButton(
    iconRes: Int,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .background(color = ChromeScrim, shape = CircleShape),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = description,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * One line for both handoff results and the sharper read, so only the latest
 * thing that happened speaks. A tapped action outranks a background re-render.
 */
@Composable
private fun PreviewStatus(
    shareOutcome: ShareOriginalOutcome?,
    openOutcome: OpenOriginalOutcome?,
    sharpenStatus: SharpenStatus,
) {
    val handoffMessage = when {
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
    }
    val message = handoffMessage
        ?: when (sharpenStatus) {
            SharpenStatus.FAILED -> OriginalPreviewZoomCopy.COULD_NOT_SHARPEN_BODY
            SharpenStatus.SHARPENING -> OriginalPreviewZoomCopy.SHARPENING_BODY
            SharpenStatus.IDLE -> null
        }
        ?: return
    val isProblem = handoffMessage != null || sharpenStatus == SharpenStatus.FAILED
    Surface(color = StatusSurface, shape = RoundedCornerShape(12.dp)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = if (isProblem) StatusError else Color.White,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/**
 * Everything the old screen printed above the picture. Reachable on purpose,
 * so the trust promise is kept without spending the evidence's space.
 */
@Composable
private fun PreviewInfoDialog(
    title: String,
    label: String,
    caption: String?,
    scopeBody: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (caption != null) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(text = scopeBody, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = OriginalHandoffCopy.HINT_BODY,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = OriginalPreviewZoomCopy.HINT_BODY,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(OriginalPreviewChromeCopy.INFO_DISMISS_LABEL)
            }
        },
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
    onSharpenStatus: (SharpenStatus) -> Unit,
    onGesture: () -> Unit,
    onTap: () -> Unit,
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
    val targetEdge = PreviewZoomPolicy.renderEdgePx(scale, initialEdgePx)
    val reportStatus by rememberUpdatedState(onSharpenStatus)

    LaunchedEffect(reloadRequest, targetEdge, initialImage) {
        if (!PreviewZoomPolicy.needsReload(targetEdge, initialEdgePx)) {
            image = initialImage
            reportStatus(SharpenStatus.IDLE)
            return@LaunchedEffect
        }
        reportStatus(SharpenStatus.SHARPENING)
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
            reportStatus(SharpenStatus.FAILED)
        } else {
            image = sharper
            reportStatus(SharpenStatus.IDLE)
        }
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

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { viewport = it }
            .pointerInput(reloadRequest) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onGesture()
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
                    onTap = { onTap() },
                    onDoubleTap = {
                        scale = PreviewZoomPolicy.toggleDoubleTap(scale)
                        if (scale <= PreviewZoomPolicy.MIN_SCALE) {
                            offset = Offset.Zero
                        } else {
                            applyOffset(offset)
                        }
                    },
                )
            }
            // Pinch is unavailable to a screen-reader user, so the three zoom
            // steps stay as custom actions instead of visible buttons.
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                customActions = listOf(
                    CustomAccessibilityAction(OriginalPreviewZoomCopy.ZOOM_IN_LABEL) {
                        if (!PreviewZoomPolicy.canZoomIn(scale)) {
                            false
                        } else {
                            scale = PreviewZoomPolicy.zoomIn(scale)
                            true
                        }
                    },
                    CustomAccessibilityAction(OriginalPreviewZoomCopy.ZOOM_OUT_LABEL) {
                        if (!PreviewZoomPolicy.canZoomOut(scale)) {
                            false
                        } else {
                            scale = PreviewZoomPolicy.zoomOut(scale)
                            if (!PreviewZoomPolicy.canZoomOut(scale)) offset = Offset.Zero
                            else applyOffset(offset)
                            true
                        }
                    },
                    CustomAccessibilityAction(OriginalPreviewZoomCopy.FIT_LABEL) {
                        if (!PreviewZoomPolicy.canZoomOut(scale)) {
                            false
                        } else {
                            scale = PreviewZoomPolicy.MIN_SCALE
                            offset = Offset.Zero
                            true
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        image?.let { shown ->
            Image(
                bitmap = shown,
                contentDescription = null,
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
