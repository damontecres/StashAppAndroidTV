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
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.toMilliseconds
import kotlinx.coroutines.launch

class PlaybackActivity : FragmentActivity() {
    private var fragment: PlaybackSceneFragment? = null
    private var maxPlayPercent = 98

    private lateinit var scene: Scene

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scene = intent.getParcelableExtra(VideoDetailsActivity.MOVIE) as Scene?
        if (scene != null) {
            this.scene = scene
            if (savedInstanceState == null) {
                fragment = PlaybackSceneFragment()
                fragment!!.scene = scene
                supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, fragment!!)
                    .commit()
            }
        } else {
            lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val sceneId = intent.getStringExtra(VideoDetailsActivity.MOVIE_ID)!!
                val fullScene = QueryEngine(this@PlaybackActivity).getScene(sceneId)!!
                val scene = Scene.fromFullSceneData(fullScene)
                this@PlaybackActivity.scene = scene
                if (savedInstanceState == null) {
                    fragment = PlaybackSceneFragment()
                    fragment!!.scene = scene
                    supportFragmentManager.beginTransaction()
                        .replace(android.R.id.content, fragment!!)
                        .commit()
                }
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        maxPlayPercent =
            PreferenceManager.getDefaultSharedPreferences(this).getInt("maxPlayPercent", 98)
    }

    @OptIn(UnstableApi::class)
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // TODO: deprecated, so use https://stackoverflow.com/a/72634975/608317 eventually
        if (fragment == null) {
            super.onBackPressed()
        } else if (!fragment!!.hideControlsIfVisible()) {
            returnPosition()
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

    /**
     * Return the video's current position to the previous Activity
     */
    private fun returnPosition() {
        val sceneDuration = scene.duration ?: Double.MIN_VALUE
        val position = fragment!!.currentVideoPosition

        val playedPercent = (position.toMilliseconds / sceneDuration) * 100
        val positionToSave =
            if (playedPercent >= maxPlayPercent) {
                Log.v(
                    PlaybackSceneFragment.TAG,
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
        val isControllerVisible: Boolean
        val currentVideoPosition: Long

        /**
         * Hide the controls if needed
         *
         * @return true if the controls needed to be hidden
         */
        fun hideControlsIfVisible(): Boolean

        fun showAndFocusSeekBar()
    }

    companion object {
        const val TAG = "PlaybackActivity"
    }
}
