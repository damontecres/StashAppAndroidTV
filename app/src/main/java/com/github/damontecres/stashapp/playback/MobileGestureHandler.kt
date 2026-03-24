package com.github.damontecres.stashapp.playback

import android.content.Context
import android.graphics.Matrix
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.View
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import com.github.damontecres.stashapp.views.SkipIndicator
import kotlin.math.abs

/**
 * Handles mobile touch gestures for video playback:
 * - Pinch to zoom + double-tap center to toggle zoom
 * - Drag to pan while zoomed
 * - Double-tap left/right edges to skip back/forward
 * - Long-press left/right edges to slow down (0.5x) / speed up (2x)
 * - Horizontal fling to go to next/previous item in queue
 *
 * Must be assigned to [StashPlayerView.gestureHandler] after the view is created.
 * Reads [StashPlayerView.player] lazily so it is safe to construct before the player is attached.
 */
@UnstableApi
class MobileGestureHandler(
    private val context: Context,
    private val playerView: StashPlayerView,
    private val skipIndicator: SkipIndicator?,
    private val speedOverlay: TextView?,
) : GestureDetector.SimpleOnGestureListener(),
    ScaleGestureDetector.OnScaleGestureListener {

    private val gestureDetector = GestureDetectorCompat(context, this)
    private val scaleDetector = ScaleGestureDetector(context, this)

    // Zoom / pan state
    private var currentScale = 1f
    private var translateX = 0f
    private var translateY = 0f

    private var isSpeedModified = false

    companion object {
        private const val TAG = "MobileGestureHandler"
        private const val MAX_SCALE = 5f
        private const val DOUBLE_TAP_ZOOM_SCALE = 2f
        // Minimum fling velocity in dp/s to trigger track change
        private const val FLING_VELOCITY_DP = 1000f
    }

    private val flingThresholdPx: Float by lazy {
        FLING_VELOCITY_DP * context.resources.displayMetrics.density
    }

    // ── Tap zone helpers ─────────────────────────────────────────────────────

    private enum class TapZone { LEFT, MIDDLE, RIGHT }

    private fun getTapZone(x: Float): TapZone {
        val third = playerView.width / 3f
        return when {
            x < third       -> TapZone.LEFT
            x < third * 2f  -> TapZone.MIDDLE
            else             -> TapZone.RIGHT
        }
    }

    // ── Transform helpers ────────────────────────────────────────────────────

    private fun clampTranslation() {
        val maxX = playerView.width * (currentScale - 1f) / 2f
        val maxY = playerView.height * (currentScale - 1f) / 2f
        translateX = translateX.coerceIn(-maxX, maxX)
        translateY = translateY.coerceIn(-maxY, maxY)
    }

    private fun applyTransform() {
        val textureView = playerView.videoSurfaceView as? TextureView
        if (textureView == null) {
            Log.w(TAG, "applyTransform: videoSurfaceView is not a TextureView " +
                    "(actual type: ${playerView.videoSurfaceView?.javaClass?.simpleName}). " +
                    "Check that surface_type=\"texture_view\" is set in video_playback.xml.")
            return
        }
        val matrix = Matrix()
        matrix.postScale(
            currentScale, currentScale,
            playerView.width / 2f,
            playerView.height / 2f,
        )
        matrix.postTranslate(translateX, translateY)
        textureView.setTransform(matrix)
    }

    private fun resetZoom() {
        currentScale = 1f
        translateX = 0f
        translateY = 0f
        applyTransform()
    }

    // ── ScaleGestureDetector: pinch to zoom ──────────────────────────────────

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = true

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        currentScale = (currentScale * detector.scaleFactor).coerceIn(1f, MAX_SCALE)
        if (currentScale == 1f) {
            translateX = 0f
            translateY = 0f
        }
        clampTranslation()
        applyTransform()
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {}

    // ── GestureDetector: double-tap ──────────────────────────────────────────

    override fun onDoubleTap(e: MotionEvent): Boolean {
        when (getTapZone(e.x)) {
            TapZone.MIDDLE -> {
                if (currentScale > 1f) resetZoom() else {
                    currentScale = DOUBLE_TAP_ZOOM_SCALE
                    applyTransform()
                }
            }
            TapZone.LEFT -> {
                val player = playerView.player ?: return true
                player.seekBack()
                skipIndicator?.update(-player.seekBackIncrement)
            }
            TapZone.RIGHT -> {
                val player = playerView.player ?: return true
                player.seekForward()
                skipIndicator?.update(player.seekForwardIncrement)
            }
        }
        return true
    }

    // ── GestureDetector: drag to pan while zoomed ────────────────────────────

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float,
    ): Boolean {
        if (currentScale <= 1f) return false
        translateX -= distanceX
        translateY -= distanceY
        clampTranslation()
        applyTransform()
        return true
    }

    // ── GestureDetector: long-press to change speed ──────────────────────────

    override fun onLongPress(e: MotionEvent) {
        val zone = getTapZone(e.x)
        val speed = when (zone) {
            TapZone.LEFT  -> 0.5f
            TapZone.RIGHT -> 2.0f
            TapZone.MIDDLE -> return
        }
        val player = playerView.player ?: return
        player.setPlaybackParameters(PlaybackParameters(speed))
        speedOverlay?.text = "${speed}x"
        speedOverlay?.visibility = View.VISIBLE
        isSpeedModified = true
    }

    // ── GestureDetector: fling to skip to next/previous ─────────────────────

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        if (abs(velocityX) < flingThresholdPx) return false
        val player = playerView.player ?: return false
        return when {
            velocityX < 0 && player.hasNextMediaItem() -> { player.seekToNext(); true }
            velocityX > 0 && player.hasPreviousMediaItem() -> { player.seekToPrevious(); true }
            else -> false
        }
    }

    // ── Main touch entry point (called from StashPlayerView.dispatchTouchEvent) ──

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            if (isSpeedModified) {
                playerView.player?.setPlaybackParameters(PlaybackParameters(1.0f))
                speedOverlay?.visibility = View.GONE
                isSpeedModified = false
            }
        }
        scaleDetector.onTouchEvent(event)
        return gestureDetector.onTouchEvent(event)
    }

    /**
     * Call from [PlaybackFragment.onDestroyView] to ensure playback speed is restored
     * if the app is backgrounded mid-long-press.
     */
    fun release() {
        if (isSpeedModified) {
            playerView.player?.setPlaybackParameters(PlaybackParameters(1.0f))
            speedOverlay?.visibility = View.GONE
            isSpeedModified = false
        }
        resetZoom()
    }
}
