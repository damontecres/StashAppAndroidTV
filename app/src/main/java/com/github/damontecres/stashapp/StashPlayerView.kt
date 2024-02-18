package com.github.damontecres.stashapp

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.fragment.app.findFragment
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

class StashPlayerView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : PlayerView(context, attrs, defStyleAttr) {
    constructor(context: Context, attrs: AttributeSet? = null) : this(context, attrs, 0)

    constructor(context: Context) : this(context, null)

    @OptIn(UnstableApi::class)
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (player != null && !findFragment<PlaybackExoFragment>().isControllerVisible &&
            (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT)
        ) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    player!!.seekForward()
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    player!!.seekBack()
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
