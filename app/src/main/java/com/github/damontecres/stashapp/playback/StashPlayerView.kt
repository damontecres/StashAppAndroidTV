package com.github.damontecres.stashapp.playback

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.media3.common.Player.Listener
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.views.SkipIndicator

/**
 * A [PlayerView] which overrides button presses
 */
class StashPlayerView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : PlayerView(context, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)

    constructor(context: Context) : this(context, null)

    private val dPadSkipEnabled: Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean("skipWithDpad", true)

    var skipIndicator: SkipIndicator? = null

    @OptIn(UnstableApi::class)
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val fragment = findFragment<Fragment>() as PlaybackFragment
        if (player != null &&
            (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        ) {
            if (event.action == KeyEvent.ACTION_UP) {
                val isPaused = !player!!.isPlaying
                if (isPaused) {
                    // If paused and user presses play, resume playback without showing controls
                    if (!fragment.isControllerVisible) {
                        // If the controller is already visible, don't change its behavior
                        useController = false
                        player!!.addListener(
                            object : Listener {
                                override fun onIsPlayingChanged(isPlaying: Boolean) {
                                    if (isPlaying) {
                                        useController = true
                                        player!!.removeListener(this)
                                    }
                                }
                            },
                        )
                    }
                    when (keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
                            Util.handlePlayPauseButtonAction(
                                player,
                                true,
                            )

                        KeyEvent.KEYCODE_MEDIA_PLAY -> Util.handlePlayButtonAction(player)
                    }
                    return true
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    player!!.pause()
                    showController()
                    return true
                } else {
                    // Not paused, so allow normal handling
                    return super.dispatchKeyEvent(event)
                }
            } else {
                return true
            }
        } else if (player != null &&
            !fragment.isControllerVisible &&
            (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
        ) {
            if (event.action == KeyEvent.ACTION_UP) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (dPadSkipEnabled) {
                        player!!.seekForward()
                        skipIndicator?.update(player!!.seekForwardIncrement)
                    } else {
                        fragment.showAndFocusSeekBar()
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (dPadSkipEnabled) {
                        player!!.seekBack()
                        skipIndicator?.update(-1 * player!!.seekBackIncrement)
                    } else {
                        fragment.showAndFocusSeekBar()
                    }
                }
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    companion object {
        const val TAG = "StashPlayerView"
    }
}
