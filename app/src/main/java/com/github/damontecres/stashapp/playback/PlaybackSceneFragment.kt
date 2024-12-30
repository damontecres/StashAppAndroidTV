package com.github.damontecres.stashapp.playback

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.getDestination

@OptIn(UnstableApi::class)
class PlaybackSceneFragment : PlaybackFragment() {
    override val previewsEnabled: Boolean
        get() = true
    override val optionsButtonOptions: OptionsButtonOptions
        get() = OptionsButtonOptions(DataType.SCENE, false)

    override fun createPlayer(): ExoPlayer = StashExoPlayer.createInstance(requireContext(), serverViewModel.requireServer())

    @OptIn(UnstableApi::class)
    override fun postCreatePlayer(player: Player) {
        maybeAddActivityTracking(player)

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
                                player.repeatMode = Player.REPEAT_MODE_ONE
                                player.removeListener(this)
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
                                setFragmentResult(
                                    Constants.POSITION_REQUEST_KEY,
                                    bundleOf(Constants.POSITION_REQUEST_KEY to 0L),
                                )
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
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val playback = requireArguments().getDestination<Destination.Playback>()
        viewModel.setScene(playback.sceneId)

        viewModel.scene.observe(viewLifecycleOwner) { scene ->
            currentScene = scene

            if (scene == null) {
                return@observe
            }
            val position =
                if (playbackPosition >= 0) {
                    playbackPosition
                } else {
                    playback.position
                }
            Log.d(TAG, "playbackPosition=$playbackPosition, playback.position=${playback.position}")
            val forceTranscode = playback.mode == PlaybackMode.FORCED_TRANSCODE
            val forceDirectPlay = playback.mode == PlaybackMode.FORCED_DIRECT_PLAY
            val streamDecision =
                getStreamDecision(requireContext(), scene, forceTranscode, forceDirectPlay)
            updateDebugInfo(streamDecision, scene)

            player!!.also { exoPlayer ->
                if (scene.streams.isNotEmpty()) {
                    maybeSetupVideoEffects(exoPlayer)
                    exoPlayer.setMediaItem(
                        buildMediaItem(requireContext(), streamDecision, scene),
                        if (position > 0) position else C.TIME_UNSET,
                    )

                    exoPlayer.volume = 1f
                    if (videoView.controllerShowTimeoutMs > 0) {
                        videoView.hideController()
                    }
                    exoPlayer.prepare()
                    // Unless the video was paused before called the result launcher, play immediately
                    exoPlayer.playWhenReady = wasPlayingBeforeResultLauncher ?: true
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
    }

    companion object {
        const val TAG = "PlaybackSceneFragment"
    }
}
