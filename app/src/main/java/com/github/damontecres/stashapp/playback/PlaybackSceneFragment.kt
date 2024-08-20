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
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.toMilliseconds
import com.github.damontecres.stashapp.views.durationToString
import com.github.damontecres.stashapp.views.showSimpleListPopupWindow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(UnstableApi::class)
class PlaybackSceneFragment : PlaybackFragment() {
    lateinit var scene: Scene

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private var trackActivityListener: PlaybackListener? = null

    // Track whether the video is playing before calling the resultLauncher
    private var wasPlayingBeforeResultLauncher: Boolean? = null
    override val previewsEnabled: Boolean
        get() = true

    private val viewModel: VideoFilterViewModel by activityViewModels()

    private fun applyEffects(exoPlayer: ExoPlayer) {
        if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_experimental_features), false)
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
                if (ServerPreferences(requireContext()).trackActivity) {
                    trackActivityListener = PlaybackListener()
                    exoPlayer.addListener(trackActivityListener!!)
                }
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
            ) && preferences.getBoolean(getString(R.string.pref_key_experimental_features), false)
        viewModel.initialize(
            StashServer.getCurrentStashServer(requireContext())!!,
            scene.id,
            saveFilters,
        )

        moreOptionsButton.setOnClickListener {
            val debugToggleText =
                if (debugView.isVisible) "Hide transcode info" else "Show transcode info"
            val applyFiltersText =
                if (preferences.getBoolean(
                        getString(R.string.pref_key_experimental_features),
                        false,
                    )
                ) {
                    "Apply Filters"
                } else {
                    null
                }
            showSimpleListPopupWindow(
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
                        debugView.visibility = View.GONE
                    } else {
                        debugView.visibility = View.VISIBLE
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
            }
        }
    }

    override fun releasePlayer() {
        super.releasePlayer()
        trackActivityListener?.release(playbackPosition)
        trackActivityListener = null
    }

    private inner class PlaybackListener : Player.Listener {
        val timer: Timer
        private val mutationEngine = MutationEngine(requireContext())
        private val minimumPlayPercent = ServerPreferences(requireContext()).minimumPlayPercent
        private val maxPlayPercent: Int

        private var totalPlayDurationSeconds = AtomicInteger(0)
        private var currentDurationSeconds = AtomicInteger(0)
        private var isPlaying = AtomicBoolean(false)
        private var incrementedPlayCount = AtomicBoolean(false)

        init {
            val manager = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val playbackDurationInterval = manager.getInt("playbackDurationInterval", 1)
            val saveActivityInterval = manager.getInt("saveActivityInterval", 10)
            maxPlayPercent = manager.getInt("maxPlayPercent", 98)

            val delay =
                playbackDurationInterval.toDuration(DurationUnit.SECONDS).inWholeMilliseconds
            // Every x seconds, check if the video is playing
            timer =
                kotlin.concurrent.timer(
                    name = "playbackTrackerTimer",
                    initialDelay = delay,
                    period = delay,
                ) {
                    try {
                        if (isPlaying.get()) {
                            // If it is playing, add the interval to currently tracked duration
                            val current = currentDurationSeconds.addAndGet(playbackDurationInterval)
                            // TODO currentDuration.getAndUpdate would be better, but requires API 24+
                            if (current >= saveActivityInterval) {
                                // If the accumulated currently tracked duration > threshold, reset it and save activity
                                currentDurationSeconds.set(0)
                                totalPlayDurationSeconds.addAndGet(current)
                                saveSceneActivity(-1L, current)
                            }
                        }
                    } catch (ex: Exception) {
                        Log.w(TAG, "Exception during track activity timer", ex)
                    }
                }
        }

        fun release(position: Long) {
            timer.cancel()
            saveSceneActivity(position)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this.isPlaying.set(isPlaying)
            if (!isPlaying) {
                val diff = currentDurationSeconds.getAndSet(0)
                if (diff > 0) {
                    totalPlayDurationSeconds.addAndGet(diff)
                    saveSceneActivity(currentVideoPosition, diff)
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                Log.v(TAG, "onPlaybackStateChanged STATE_ENDED")
                val sceneDuration = scene.durationPosition
                val diff = currentDurationSeconds.getAndSet(0)
                totalPlayDurationSeconds.addAndGet(diff)
                if (sceneDuration != null) {
                    saveSceneActivity(sceneDuration, diff)
                }
            }
        }

        fun saveSceneActivity(position: Long) {
            val duration = currentDurationSeconds.getAndSet(0)
            saveSceneActivity(position, duration)
        }

        fun saveSceneActivity(
            position: Long,
            duration: Int,
        ) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main + StashCoroutineExceptionHandler()) {
                val sceneDuration = scene.duration
                val totalDuration = totalPlayDurationSeconds.get()
                val calcPosition = if (position >= 0) position else currentVideoPosition
                Log.v(
                    TAG,
                    "saveSceneActivity: position=$position, duration=$duration, calcPosition=$calcPosition, totalDuration=$totalDuration",
                )
                if (sceneDuration != null) {
                    val playedPercent = (calcPosition.toMilliseconds / sceneDuration) * 100
                    val positionToSave =
                        if (playedPercent >= maxPlayPercent) {
                            Log.v(
                                TAG,
                                "Setting position to 0 since $playedPercent >= $maxPlayPercent",
                            )
                            0L
                        } else {
                            calcPosition
                        }
                    mutationEngine.saveSceneActivity(scene.id, positionToSave, duration)
                    val totalPlayPercent = (totalDuration / sceneDuration) * 100
                    Log.v(
                        TAG,
                        "totalPlayPercent=$totalPlayPercent, minimumPlayPercent=$minimumPlayPercent",
                    )
                    if (totalPlayPercent >= minimumPlayPercent) {
                        // If the current session hasn't incremented the play count yet, do it
                        val shouldIncrement = !incrementedPlayCount.getAndSet(true)
                        if (shouldIncrement) {
                            Log.v(TAG, "Incrementing play count for ${scene.id}")
                            mutationEngine.incrementPlayCount(scene.id)
                        }
                    }
                } else {
                    // No scene duration
                    mutationEngine.saveSceneActivity(scene.id, calcPosition, duration)
                }
            }
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
