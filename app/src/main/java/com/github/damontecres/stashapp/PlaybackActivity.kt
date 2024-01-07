package com.github.damontecres.stashapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/** Loads [PlaybackVideoFragment]. */
class PlaybackActivity : FragmentActivity() {
    private val fragment: PlaybackVideoFragment = PlaybackVideoFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // TODO: deprecated, so use https://stackoverflow.com/a/72634975/608317 eventually
        returnPosition()
        super.onBackPressed()
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
        intent.putExtra("position", fragment.currentVideoPosition)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}
