package com.github.damontecres.stashapp.playback

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SceneDetailsFragment
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getParcelable
import com.github.damontecres.stashapp.util.toMilliseconds
import kotlinx.coroutines.launch

class PlaybackActivity : FragmentActivity() {
    private var fragment: PlaybackSceneFragment? = null
    private var maxPlayPercent = 98

    private lateinit var scene: Scene

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_playback)

        onBackPressedDispatcher.addCallback(this, true) {
            setResultAndFinish()
        }

        val scene = intent.getParcelable(Constants.SCENE_ARG, Scene::class)
        if (scene != null) {
            this.scene = scene
            if (savedInstanceState == null) {
                fragment = PlaybackSceneFragment(scene)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.playback_container, fragment!!)
                    .commit()
            }
        } else {
            // TODO remove?
            lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val sceneId = intent.getStringExtra(Constants.SCENE_ID_ARG)!!
                val fullScene =
                    QueryEngine(StashServer.requireCurrentServer())
                        .getScene(sceneId)!!
                val scene = Scene.fromFullSceneData(fullScene)
                this@PlaybackActivity.scene = scene
                if (savedInstanceState == null) {
                    fragment = PlaybackSceneFragment(scene)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.playback_container, fragment!!)
                        .commit()
                }
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        maxPlayPercent =
            PreferenceManager.getDefaultSharedPreferences(this).getInt("maxPlayPercent", 98)
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

    private fun setResultAndFinish() {
        // Return the video's current position to the previous Activity
        val sceneDuration = scene.duration
        val position = fragment!!.currentVideoPosition

        val positionToSave =
            if (sceneDuration == null || (position.toMilliseconds / sceneDuration) * 100 < maxPlayPercent) {
                position
            } else {
                Log.v(
                    PlaybackSceneFragment.TAG,
                    "Setting position to 0 since played percent (${(position.toMilliseconds / sceneDuration) * 100} >= $maxPlayPercent",
                )
                0L
            }

        Log.d(
            TAG,
            "Video playback ending, currentVideoPosition=$position, positionToSave=$positionToSave",
        )

        val intent = Intent()
        intent.putExtra(SceneDetailsFragment.POSITION_RESULT_ARG, positionToSave)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    companion object {
        private const val TAG = "PlaybackActivity"
    }
}
