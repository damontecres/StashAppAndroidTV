package com.github.damontecres.stashapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager

/** Loads [PlaybackVideoFragment]. */
class PlaybackActivity : SecureFragmentActivity() {
    private lateinit var fragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val useExo =
                PreferenceManager.getDefaultSharedPreferences(this).getBoolean("playerChoice", true)
            fragment =
                if (useExo) {
                    PlaybackExoFragment()
                } else {
                    PlaybackVideoFragment()
                }
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit()
        }
    }

    @OptIn(UnstableApi::class)
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // TODO: deprecated, so use https://stackoverflow.com/a/72634975/608317 eventually
        if (!(fragment as StashVideoPlayer).hideControlsIfVisible()) {
            returnPosition()
            super.onBackPressed()
        }
    }

    override fun onStop() {
        returnPosition() // Not sure if it's actually needed here, but doesn't hurt
        super.onStop()
    }

    /**
     * Return the video's current position to the previous Activity
     */
    private fun returnPosition() {
        val intent = Intent()
        intent.putExtra("position", (fragment as StashVideoPlayer).currentVideoPosition)
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
}
