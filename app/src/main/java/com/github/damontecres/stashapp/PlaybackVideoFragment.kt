package com.github.damontecres.stashapp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackControlGlue.ACTION_FAST_FORWARD
import androidx.leanback.media.PlaybackControlGlue.ACTION_PLAY_PAUSE
import androidx.leanback.media.PlaybackControlGlue.ACTION_REWIND
import androidx.leanback.media.PlaybackGlue
import androidx.leanback.media.PlaybackGlue.PlayerCallback
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.VideoDetailsFragment.Companion.POSITION_ARG
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment(), PlaybackActivity.StashVideoPlayer {
    private lateinit var mTransportControlGlue: BasicTransportControlsGlue
    private lateinit var playerAdapter: BasicMediaPlayerAdapter

    override val currentVideoPosition get() = playerAdapter.currentPosition

    override fun hideControlsIfVisible(): Boolean {
        return false
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val scene =
            requireActivity().intent.getParcelableExtra(VideoDetailsActivity.MOVIE) as Scene?
        val position = requireActivity().intent.getLongExtra(POSITION_ARG, -1)
        Log.d(TAG, "scene=${scene?.id}")
        Log.d(TAG, "$POSITION_ARG=$position")

        val glueHost = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)
        val skipForward =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("skip_forward_time", 30)
        val skipBack =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("skip_back_time", 10)
        val forceTranscode =
            requireActivity().intent.getBooleanExtra(
                VideoDetailsFragment.FORCE_TRANSCODE,
                false,
            )

        val serverPreferences = ServerPreferences(requireContext())

        playerAdapter =
            BasicMediaPlayerAdapter(
                requireActivity(),
                viewLifecycleOwner.lifecycleScope,
                skipForward,
                skipBack,
                scene?.duration,
                scene!!.id,
                serverPreferences.trackActivity,
            )

        if (serverPreferences.trackActivity) {
            val mutationEngine = MutationEngine(requireContext(), false)
            viewLifecycleOwner.lifecycleScope.launch(coroutineExceptionHandler) {
                while (true) {
                    delay(30.toDuration(DurationUnit.SECONDS))
                    mutationEngine.saveSceneActivity(scene.id, playerAdapter.currentPosition)
                }
            }
        }

        mTransportControlGlue = BasicTransportControlsGlue(activity, playerAdapter)
        mTransportControlGlue.host = glueHost
        mTransportControlGlue.title = scene.title
        mTransportControlGlue.subtitle = scene.details
        mTransportControlGlue.playWhenPrepared()

        var streamUrl = scene.streams["Direct stream"]
        if (streamUrl == null || forceTranscode) {
            val streamChoice =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("stream_choice", "MP4")
            streamUrl = scene.streams[streamChoice]
        }

        if (streamUrl != null) {
            playerAdapter.setDataSource(Uri.parse(streamUrl))

            mTransportControlGlue.addPlayerCallback(
                object : PlayerCallback() {
                    override fun onPlayStateChanged(glue: PlaybackGlue) {
                        Log.v(TAG, "Keep screen on: ${glue.isPlaying}")
                        if (glue.isPlaying) {
                            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                },
            )

            if (position > 0) {
                // If a position was provided, resume playback from there
                mTransportControlGlue.addPlayerCallback(
                    object : PlayerCallback() {
                        override fun onPlayStateChanged(glue: PlaybackGlue) {
                            if (glue.isPlaying) {
                                // Remove the callback so it's only run once
                                glue.removePlayerCallback(this)
                                playerAdapter.seekTo(position)
                            }
                        }
                    },
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mTransportControlGlue.pause()
    }

    class BasicMediaPlayerAdapter(
        context: Context,
        private val lifecycleScope: LifecycleCoroutineScope,
        private var skipForward: Int,
        private var skipBack: Int,
        private val duration: Double?,
        private val sceneId: Long,
        private val trackActivity: Boolean,
    ) :
        MediaPlayerAdapter(context) {
        private val mutationEngine = MutationEngine(context, false)

        override fun fastForward() = seekTo(currentPosition + skipForward * 1000)

        override fun rewind() = seekTo(currentPosition - skipBack * 1000)

        override fun seekTo(newPosition: Long) {
            super.seekTo(newPosition)
            if (trackActivity) {
                lifecycleScope.launch(coroutineExceptionHandler) {
                    mutationEngine.saveSceneActivity(sceneId, newPosition)
                }
            }
        }

        override fun getDuration(): Long {
            val dur = super.getDuration()
            return if (dur < 1) {
                duration?.times(1000)?.toLong() ?: -1
            } else {
                dur
            }
        }

        override fun getSupportedActions(): Long {
            return (
                ACTION_REWIND xor
                    ACTION_PLAY_PAUSE xor
                    ACTION_FAST_FORWARD
            ).toLong()
        }
    }

    class BasicTransportControlsGlue(
        context: Context?,
        playerAdapter: BasicMediaPlayerAdapter,
    ) : PlaybackTransportControlGlue<BasicMediaPlayerAdapter>(context, playerAdapter) {
        // Primary actions
        private val forwardAction = PlaybackControlsRow.FastForwardAction(context)
        private val rewindAction = PlaybackControlsRow.RewindAction(context)

        init {
            isSeekEnabled = true // Enables scrubbing on the seekbar
        }

        override fun onCreatePrimaryActions(primaryActionsAdapter: ArrayObjectAdapter) {
            primaryActionsAdapter.add(rewindAction)
            super.onCreatePrimaryActions(primaryActionsAdapter) // Adds play/pause action
            primaryActionsAdapter.add(forwardAction)
        }

        override fun onActionClicked(action: Action?) {
            when (action) {
                forwardAction -> playerAdapter.fastForward()
                rewindAction -> playerAdapter.rewind()
                else -> super.onActionClicked(action)
            }
            onUpdateProgress() // Updates seekbar progress
        }

        override fun onKey(
            v: View?,
            keyCode: Int,
            event: KeyEvent,
        ): Boolean {
            // TODO: left/right pauses
            // TODO: if visible, fast forward button doesn't work
            if (host.isControlsOverlayVisible || event.repeatCount > 0) {
                return super.onKey(v, keyCode, event)
            }
            if (event.action != KeyEvent.ACTION_DOWN) {
                return when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                    KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD, KeyEvent.KEYCODE_MEDIA_NEXT,
                    -> {
                        onActionClicked(forwardAction)
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND,
                    KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD, KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                    -> {
                        onActionClicked(rewindAction)
                        true
                    }

                    else -> super.onKey(v, keyCode, event)
                }
            }
            return false
        }
    }

    companion object {
        private const val TAG = "PlaybackVideoFragment"

        val coroutineExceptionHandler =
            CoroutineExceptionHandler { _, ex ->
                // Just log and ignore the error because it's not big deal to not save the resume time
                Log.w(TAG, "Error during corountine", ex)
            }
    }
}
