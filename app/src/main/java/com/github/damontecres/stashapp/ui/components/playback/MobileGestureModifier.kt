package com.github.damontecres.stashapp.ui.components.playback

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import kotlin.math.abs

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
    val transformState =
        rememberTransformableState { zoomChange, offsetChange, _ ->
            zoomFactor = (zoomFactor * zoomChange).coerceIn(1f, GESTURE_MAX_ZOOM)
            if (zoomFactor > 1f) {
                val maxX = surfaceWidth * (zoomFactor - 1f) / 2f
                val maxY = surfaceHeight * (zoomFactor - 1f) / 2f
                panX = (panX + offsetChange.x * zoomFactor).coerceIn(-maxX, maxX)
                panY = (panY + offsetChange.y * zoomFactor).coerceIn(-maxY, maxY)
            } else {
                panX = 0f
                panY = 0f
            }
        }

    return Modifier
        .onSizeChanged {
            surfaceWidth = it.width
            surfaceHeight = it.height
        }.graphicsLayer {
            scaleX = zoomFactor
            scaleY = zoomFactor
            translationX = panX
            translationY = panY
        }.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    tryAwaitRelease()
                    if (gestureSpeedActive) {
                        player.setPlaybackParameters(PlaybackParameters(1f))
                        gestureSpeedActive = false
                    }
                },
                onTap = {
                    controllerViewState.toggleControls()
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
                    val speed =
                        when {
                            offset.x < size.width / 3f -> GESTURE_SLOW_SPEED
                            offset.x > size.width * 2f / 3f -> GESTURE_FAST_SPEED
                            else -> null
                        }
                    if (speed != null) {
                        player.setPlaybackParameters(PlaybackParameters(speed))
                        gestureSpeedActive = true
                    }
                },
            )
        }
        // Swipe left/right to go to next/previous item. Uses PointerEventPass.Initial to
        // observe move events before transformable() consumes them in the Main pass, and
        // requireUnconsumed=false so detectTapGestures' down.consume() doesn't block us.
        .pointerInput(zoomFactor > 1f) {
            if (zoomFactor <= 1f) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragX = 0f
                    var dragY = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        dragX += change.position.x - change.previousPosition.x
                        dragY += change.position.y - change.previousPosition.y
                    }
                    if (abs(dragX) > SWIPE_NEXT_PREV_THRESHOLD_PX && abs(dragX) > abs(dragY)) {
                        if (dragX < 0 && player.hasNextMediaItem()) {
                            player.seekToNext()
                        } else if (dragX > 0 && player.hasPreviousMediaItem()) {
                            player.seekToPrevious()
                        }
                    }
                }
            }
        }.transformable(transformState, lockRotationOnZoomPan = true)
}
