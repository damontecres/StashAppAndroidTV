package com.github.damontecres.stashapp.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.fragment.app.findFragment
import androidx.media3.common.Player.Listener
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.PlaybackExoFragment

class StashPlayerView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    PlayerView(context, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)

    constructor(context: Context) : this(context, null)

    private val dPadSkipEnabled: Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean("skipWithDpad", true)

    @OptIn(UnstableApi::class)
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val fragment = findFragment<PlaybackExoFragment>()
        if (player != null &&
            (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        ) {
            val isPaused = !player!!.isPlaying
            if (event.action == KeyEvent.ACTION_DOWN && isPaused) {
                // If paused and user presses play, resume playback without showing controls
                Log.v(TAG, "Resuming")
                if (!fragment.isControllerVisible) {
                    // If the controller is visible, we don't want to change its behavior
                    controllerAutoShow = false
                    player!!.addListener(
                        object : Listener {
                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                Log.v(
                                    TAG,
                                    "onIsPlayingChanged: controllerAutoShow=$controllerAutoShow, useController=$useController",
                                )
                                if (isPlaying) {
                                    controllerAutoShow = true
                                    player!!.removeListener(this)
                                }
                            }
                        },
                    )
                }
                player!!.play()
                return true
            } else if (event.action == KeyEvent.ACTION_DOWN && !isPaused) {
                return super.dispatchKeyEvent(event)
            } else {
                return true
            }
        } else if (player != null && !fragment.isControllerVisible &&
            (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
        ) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (dPadSkipEnabled) {
                        player!!.seekForward()
                    } else {
                        fragment.showAndFocusSeekBar()
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (dPadSkipEnabled) {
                        player!!.seekBack()
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
