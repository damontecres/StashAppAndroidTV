package com.github.damontecres.stashapp.playback

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SceneDetailsFragment
import com.github.damontecres.stashapp.SearchForFragment
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.StashServer

@OptIn(UnstableApi::class)
class PlaybackSceneFragment() : PlaybackFragment() {
    constructor(scene: Scene) : this() {
        this.scene = scene
    }

    private lateinit var scene: Scene

    override val previewsEnabled: Boolean
        get() = true
    override val optionsButtonOptions: OptionsButtonOptions
        get() = OptionsButtonOptions(DataType.SCENE, false)

    @OptIn(UnstableApi::class)
    override fun initializePlayer(): ExoPlayer {
        val position =
            if (playbackPosition >= 0) {
                playbackPosition
            } else {
                requireActivity().intent.getLongExtra(Constants.POSITION_ARG, -1)
            }
        val forceTranscode =
            requireActivity().intent.getBooleanExtra(
                SceneDetailsFragment.FORCE_TRANSCODE,
                false,
            )
        val forceDirectPlay =
            requireActivity().intent.getBooleanExtra(
                SceneDetailsFragment.FORCE_DIRECT_PLAY,
                false,
            )
        val streamDecision =
            getStreamDecision(requireContext(), scene, forceTranscode, forceDirectPlay)
        updateDebugInfo(streamDecision, scene)
        return StashExoPlayer
            .getInstance(requireContext(), StashServer.requireCurrentServer())
            .also { exoPlayer ->
                maybeAddActivityTracking(exoPlayer)
            }.also { exoPlayer ->
                val finishedBehavior =
                    PreferenceManager
                        .getDefaultSharedPreferences(requireContext())
                        .getString(
                            "playbackFinishedBehavior",
                            getString(R.string.playback_finished_do_nothing),
                        )
                when (finishedBehavior) {
                    getString(R.string.playback_finished_repeat) -> {
                        StashExoPlayer.addListener(
                            object :
                                Player.Listener {
                                override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
                                    if (Player.COMMAND_SET_REPEAT_MODE in availableCommands) {
                                        Log.v(
                                            TAG,
                                            "Listener setting repeatMode to REPEAT_MODE_ONE",
                                        )
                                        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                                        exoPlayer.removeListener(this)
                                    }
                                }
                            },
                        )
                    }

                    getString(R.string.playback_finished_return) ->
                        StashExoPlayer.addListener(
                            object :
                                Player.Listener {
                                override fun onPlaybackStateChanged(playbackState: Int) {
                                    if (playbackState == Player.STATE_ENDED) {
                                        Log.v(TAG, "Finishing activity")
                                        val result = Intent()
                                        result.putExtra(
                                            SearchForFragment.ID_KEY,
                                            -1L,
                                        )
                                        result.putExtra(
                                            SceneDetailsFragment.POSITION_RESULT_ARG,
                                            0L,
                                        )
                                        requireActivity().setResult(Activity.RESULT_OK, result)
                                        requireActivity().finish()
                                    }
                                }
                            },
                        )

                    getString(R.string.playback_finished_do_nothing) -> {
                        StashExoPlayer.addListener(
                            object :
                                Player.Listener {
                                override fun onPlaybackStateChanged(playbackState: Int) {
                                    if (playbackState == Player.STATE_ENDED) {
                                        videoView.showController()
                                    }
                                }
                            },
                        )
                    }

                    else -> Log.w(TAG, "Unknown playbackFinishedBehavior: $finishedBehavior")
                }
            }.also { exoPlayer ->
                if (scene.streams.isNotEmpty()) {
                    maybeSetupVideoEffects(exoPlayer)
                    exoPlayer.setMediaItem(
                        buildMediaItem(requireContext(), streamDecision, scene),
                        if (position > 0) position else C.TIME_UNSET,
                    )

                    Log.v(TAG, "Preparing playback")
                    exoPlayer.prepare()
                    // Unless the video was paused before called the result launcher, play immediately
                    exoPlayer.playWhenReady = wasPlayingBeforeResultLauncher ?: true
                    exoPlayer.volume = 1f
                    if (videoView.controllerShowTimeoutMs > 0) {
                        videoView.hideController()
                    }
                } else {
                    videoView.useController = false
                    Toast
                        .makeText(
                            requireContext(),
                            "This scene has no video files to play",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        currentScene = scene

        val position = requireActivity().intent.getLongExtra(Constants.POSITION_ARG, -1)
        Log.d(TAG, "scene=${scene.id}, ${Constants.POSITION_ARG}=$position")
    }

    companion object {
        const val TAG = "PlaybackSceneFragment"
    }
}
