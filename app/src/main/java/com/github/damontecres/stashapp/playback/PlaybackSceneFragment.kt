package com.github.damontecres.stashapp.playback

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SceneDetailsFragment
import com.github.damontecres.stashapp.SearchForActivity
import com.github.damontecres.stashapp.SearchForFragment
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.data.ThrottledLiveData
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.animateToInvisible
import com.github.damontecres.stashapp.util.animateToVisible
import com.github.damontecres.stashapp.views.ListPopupWindowBuilder
import com.github.damontecres.stashapp.views.durationToString
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackSceneFragment : PlaybackFragment() {
    lateinit var scene: Scene

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    // Track whether the video is playing before calling the resultLauncher
    private var wasPlayingBeforeResultLauncher: Boolean? = null
    override val previewsEnabled: Boolean
        get() = true

    private val viewModel: VideoFilterViewModel by activityViewModels()

    private fun applyEffects(exoPlayer: ExoPlayer) {
        if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_video_filters), false)
        ) {
            Log.v(TAG, "Initializing video effects")
            exoPlayer.setVideoEffects(listOf())

            val filterContainer = requireActivity().findViewById<View>(R.id.video_filter_container)
            videoView.setControllerVisibilityListener(
                PlayerView.ControllerVisibilityListener {
                    filterContainer.visibility = it
                },
            )

            ThrottledLiveData(viewModel.videoFilter, 500L).observe(
                viewLifecycleOwner,
            ) { vf ->
                Log.v(TAG, "Got new VideoFilter: $vf")
                val effectList = vf?.createEffectList().orEmpty()
                Log.d(TAG, "Applying ${effectList.size} effects")
                player?.setVideoEffects(effectList)
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun initializePlayer(): ExoPlayer {
        val position =
            if (playbackPosition >= 0) {
                playbackPosition
            } else {
                requireActivity().intent.getLongExtra(SceneDetailsFragment.POSITION_ARG, -1)
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
        return StashExoPlayer.getInstance(requireContext())
            .also { exoPlayer ->
                maybeAddActivityTracking(exoPlayer)
            }.also { exoPlayer ->
                val finishedBehavior =
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
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
                applyEffects(exoPlayer)
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
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                ResultCallback(),
            )
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        currentScene = scene

        val position = requireActivity().intent.getLongExtra(SceneDetailsFragment.POSITION_ARG, -1)
        Log.d(TAG, "scene=${scene.id}, ${SceneDetailsFragment.POSITION_ARG}=$position")

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val saveFilters =
            preferences.getBoolean(
                getString(R.string.pref_key_playback_save_effects),
                true,
            ) && preferences.getBoolean(getString(R.string.pref_key_video_filters), false)
        viewModel.initialize(
            StashServer.getCurrentStashServer(requireContext())!!,
            scene.id,
            saveFilters,
        )

        moreOptionsButton.setOnClickListener {
            val previousControllerShowTimeoutMs = videoView.controllerShowTimeoutMs
            val debugToggleText =
                if (debugView.isVisible) "Hide transcode info" else "Show transcode info"
            val applyFiltersText =
                if (preferences.getBoolean(
                        getString(R.string.pref_key_video_filters),
                        false,
                    )
                ) {
                    "Apply Video Filters"
                } else {
                    null
                }
            ListPopupWindowBuilder(
                moreOptionsButton,
                listOfNotNull(
                    "Create Marker",
                    debugToggleText,
                    applyFiltersText,
                ),
            ) { position ->
                if (position == 0) {
                    // Save current playback state
                    player?.let { exoPlayer ->
                        playbackPosition = exoPlayer.currentPosition
                        wasPlayingBeforeResultLauncher = exoPlayer.isPlaying
                    }
                    val intent = Intent(requireActivity(), SearchForActivity::class.java)
                    intent.putExtra(SearchForFragment.TITLE_KEY, "for primary tag for scene marker")
                    intent.putExtra("dataType", DataType.TAG.name)
                    intent.putExtra(SearchForFragment.ID_KEY, StashAction.CREATE_MARKER.id)
                    resultLauncher.launch(intent)
                } else if (position == 1) {
                    if (debugView.isVisible) {
                        debugView.animateToInvisible(View.GONE)
                    } else {
                        debugView.animateToVisible()
                    }
                } else if (position == 2) {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.video_filter_container,
                            PlaybackFilterFragment(),
                            PlaybackFilterFragment.TAG,
                        )
                        .commitNow()
                }
            }.onShowListener {
                // Prevent the controller from hiding
                videoView.controllerShowTimeoutMs = -1
            }.onDismissListener {
                // Restore previous controller timeout
                videoView.controllerShowTimeoutMs = previousControllerShowTimeoutMs
            }.build()
                .show()
        }
    }

    private inner class ResultCallback : ActivityResultCallback<ActivityResult> {
        override fun onActivityResult(result: ActivityResult) {
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val id = data!!.getLongExtra(SearchForFragment.ID_KEY, -1)
                if (id == StashAction.CREATE_MARKER.id) {
                    val videoPos = currentVideoPosition
                    playbackPosition = videoPos
                    viewLifecycleOwner.lifecycleScope.launch(
                        CoroutineExceptionHandler { _, ex ->
                            Log.e(TAG, "Exception creating marker", ex)
                            Toast.makeText(
                                requireContext(),
                                "Failed to create marker: ${ex.message}",
                                Toast.LENGTH_LONG,
                            ).show()
                        },
                    ) {
                        val tagId =
                            data.getStringExtra(SearchForFragment.RESULT_ID_KEY)!!
                        Log.d(
                            TAG,
                            "Adding marker at $videoPos with tagId=$tagId to scene ${scene.id}",
                        )
                        val newMarker =
                            MutationEngine(requireContext()).createMarker(
                                scene.id,
                                videoPos,
                                tagId,
                            )!!
                        val dur = durationToString(newMarker.seconds)
                        Toast.makeText(
                            requireContext(),
                            "Created a new marker at $dur with primary tag '${newMarker.primary_tag.tagData.name}'",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "PlaybackExoFragment"

        const val DB_NAME = "playback_db"
    }
}
