package com.github.damontecres.stashapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi

/** Loads [PlaybackVideoFragment]. */
class PlaybackActivity : FragmentActivity() {
    private val fragment = PlaybackExoFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit()
        }
    }

    @OptIn(UnstableApi::class)
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // TODO: deprecated, so use https://stackoverflow.com/a/72634975/608317 eventually
        if (!fragment.hideControlsIfVisible()) {
            returnPosition()
            super.onBackPressed()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return fragment.videoView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
    }

    /**
     * Return the video's current position to the previous Activity
     */
    private fun returnPosition() {
        val intent = Intent()
        val position = fragment.currentVideoPosition
        Log.d(TAG, "Video playback ending, currentVideoPosition=$position")
        intent.putExtra("position", position)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    interface StashVideoPlayer {
        val currentVideoPosition: Long

        /**
         * Hide the controls if needed
         *
         * @return true if the controls needed to be hidden
         */
        fun hideControlsIfVisible(): Boolean
    }

    companion object {
        const val TAG = "PlaybackActivity"
    }
}
