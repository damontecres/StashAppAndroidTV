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
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.toMilliseconds

class PlaybackActivity : FragmentActivity() {
    private val fragment = PlaybackExoFragment()
    private var maxPlayPercent = 98

    private lateinit var scene: Scene

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scene = intent.getParcelableExtra(VideoDetailsActivity.MOVIE) as Scene?
            ?: throw RuntimeException()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        maxPlayPercent =
            PreferenceManager.getDefaultSharedPreferences(this).getInt("maxPlayPercent", 98)
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
        val sceneDuration = scene.duration ?: Double.MIN_VALUE
        val position = fragment.currentVideoPosition

        val playedPercent = (position.toMilliseconds / sceneDuration) * 100
        val positionToSave =
            if (playedPercent >= maxPlayPercent) {
                Log.v(
                    PlaybackExoFragment.TAG,
                    "Setting position to 0 since $playedPercent >= $maxPlayPercent",
                )
                0L
            } else {
                position
            }
        Log.d(
            TAG,
            "Video playback ending, currentVideoPosition=$position, positionToSave=$positionToSave",
        )

        val intent = Intent()
        intent.putExtra(VideoDetailsFragment.POSITION_RESULT_ARG, positionToSave)
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
