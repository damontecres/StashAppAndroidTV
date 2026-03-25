package com.github.damontecres.stashapp.ui.components.playback

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player

private const val SWIPE_NEXT_PREV_THRESHOLD_PX = 200f
private const val GESTURE_MAX_ZOOM = 5f
private const val GESTURE_DOUBLE_TAP_ZOOM = 2f
private const val GESTURE_SLOW_SPEED = 0.5f
private const val GESTURE_FAST_SPEED = 2.0f

/**
 * Returns a [Modifier] that adds mobile touch gestures to a video surface:
 * - Pinch-to-zoom and drag-to-pan while zoomed
 * - Double-tap left/right to skip back/forward; center to toggle zoom
 * - Long-press left/right for 0.5x/2x speed (resets on release)
 * - Horizontal swipe to navigate to the previous/next media item
 *
 * Requires the surface to use [androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW]
 * so that [graphicsLayer] transforms are applied correctly.
 */
@Composable
fun rememberMobileGestureModifier(
    scaledModifier: Modifier,
    player: Player,
    controllerViewState: ControllerViewState,
    updateSkipIndicator: (Long) -> Unit,
): Modifier {
    var zoomFactor by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var surfaceWidth by remember { mutableIntStateOf(0) }
    var surfaceHeight by remember { mutableIntStateOf(0) }
    var gestureSpeedActive by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        onDispose {
            if (gestureSpeedActive) {
                player.setPlaybackParameters(PlaybackParameters(1f))
                gestureSpeedActive = false
            }
        }
    }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        zoomFactor = (zoomFactor * zoomChange).coerceIn(1f, GESTURE_MAX_ZOOM)
        if (zoomFactor > 1f) {
            val maxX = surfaceWidth * (zoomFactor - 1f) / 2f
            val maxY = surfaceHeight * (zoomFactor - 1f) / 2f
            panX = (panX + offsetChange.x).coerceIn(-maxX, maxX)
            panY = (panY + offsetChange.y).coerceIn(-maxY, maxY)
        } else {
            panX = 0f
            panY = 0f
        }
    }

    return scaledModifier
        .onSizeChanged { surfaceWidth = it.width; surfaceHeight = it.height }
        .graphicsLayer {
            scaleX = zoomFactor
            scaleY = zoomFactor
            translationX = panX
            translationY = panY
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    tryAwaitRelease()
                    if (gestureSpeedActive) {
                        player.setPlaybackParameters(PlaybackParameters(1f))
                        gestureSpeedActive = false
                    }
                },
                onTap = {
                    if (controllerViewState.controlsVisible) {
                        controllerViewState.hideControls()
                    } else {
                        controllerViewState.showControls()
                    }
                },
                onDoubleTap = { offset ->
                    when {
                        offset.x < size.width / 3f -> {
                            player.seekBack()
                            updateSkipIndicator(-player.seekBackIncrement)
                        }
                        offset.x > size.width * 2f / 3f -> {
                            player.seekForward()
                            updateSkipIndicator(player.seekForwardIncrement)
                        }
                        else -> {
                            if (zoomFactor > 1f) {
                                zoomFactor = 1f
                                panX = 0f
                                panY = 0f
                            } else {
                                zoomFactor = GESTURE_DOUBLE_TAP_ZOOM
                            }
                        }
                    }
                },
                onLongPress = { offset ->
                    val speed = when {
                        offset.x < size.width / 3f -> GESTURE_SLOW_SPEED
                        offset.x > size.width * 2f / 3f -> GESTURE_FAST_SPEED
                        else -> return@detectTapGestures
                    }
                    player.setPlaybackParameters(PlaybackParameters(speed))
                    gestureSpeedActive = true
                },
            )
        }
        .pointerInput(zoomFactor) {
            if (zoomFactor <= 1f) {
                var dragX = 0f
                detectDragGestures(
                    onDragStart = { dragX = 0f },
                    onDragCancel = { dragX = 0f },
                    onDragEnd = {
                        if (dragX < -SWIPE_NEXT_PREV_THRESHOLD_PX && player.hasNextMediaItem()) {
                            player.seekToNext()
                        } else if (dragX > SWIPE_NEXT_PREV_THRESHOLD_PX && player.hasPreviousMediaItem()) {
                            player.seekToPrevious()
                        }
                        dragX = 0f
                    },
                ) { change, dragAmount ->
                    dragX += dragAmount.x
                    change.consume()
                }
            }
        }
        .transformable(transformState, lockRotationOnZoomPan = true)
}
