package com.github.damontecres.stashapp.views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.fragment.app.findFragment
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
        if (player != null && !fragment.isControllerVisible &&
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
