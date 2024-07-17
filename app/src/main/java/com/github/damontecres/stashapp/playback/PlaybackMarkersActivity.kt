package com.github.damontecres.stashapp.playback

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi

class PlaybackMarkersActivity : FragmentActivity() {
    private var fragment: PlaybackMarkersFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            fragment = PlaybackMarkersFragment()
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment!!)
                .commit()
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @OptIn(UnstableApi::class)
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // TODO: deprecated, so use https://stackoverflow.com/a/72634975/608317 eventually
        if (fragment == null) {
            super.onBackPressed()
        } else if (!fragment!!.hideControlsIfVisible()) {
            super.onBackPressed()
        }
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return if (fragment != null) {
            fragment!!.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
        } else {
            super.dispatchKeyEvent(event)
        }
    }

    companion object {
        const val TAG = "PlaybackActivity"
    }
}
