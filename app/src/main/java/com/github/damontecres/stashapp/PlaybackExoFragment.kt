package com.github.damontecres.stashapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.presenters.PopupOnLongClickListener
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashPreviewLoader
import com.github.damontecres.stashapp.util.createOkHttpClient
import com.github.damontecres.stashapp.util.toMilliseconds
import com.github.damontecres.stashapp.views.StashPlayerView
import com.github.rubensousa.previewseekbar.PreviewBar
import com.github.rubensousa.previewseekbar.media3.PreviewTimeBar
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(UnstableApi::class)
class PlaybackExoFragment :
    Fragment(R.layout.video_playback),
    PlaybackActivity.StashVideoPlayer {
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private var player: ExoPlayer? = null
    private var trackActivityListener: PlaybackListener? = null
    private lateinit var scene: Scene
    lateinit var videoView: StashPlayerView
    lateinit var previewTimeBar: PreviewTimeBar
    private lateinit var exoCenterControls: View

    private var playbackPosition = -1L

    // Track whether the video is playing before calling the resultLauncher
    private var wasPlayingBeforeResultLauncher: Boolean? = null

    override val currentVideoPosition get() = player?.currentPosition ?: playbackPosition

    val isControllerVisible get() = videoView.isControllerFullyVisible || previewTimeBar.isShown

    override fun hideControlsIfVisible(): Boolean {
        if (isControllerVisible) {
            videoView.hideController()
            previewTimeBar.hidePreview()
            previewTimeBar.hideScrubber(250L)
            return true
        }
        return false
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            exoPlayer.release()
        }
        trackActivityListener?.release(playbackPosition)
        trackActivityListener = null
        player = null
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer(
        position: Long,
        forceTranscode: Boolean,
    ) {
        player =
            StashExoPlayer.getInstance(requireContext())
                .also { exoPlayer ->
                    videoView.player = exoPlayer
                    StashExoPlayer.addListener(AmbientPlaybackListener())
                    if (ServerPreferences(requireContext()).trackActivity) {
                        trackActivityListener = PlaybackListener()
                        StashExoPlayer.addListener(trackActivityListener!!)
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
                                                VideoDetailsFragment.POSITION_RESULT_ARG,
                                                0L,
                                            )
                                            requireActivity().setResult(Activity.RESULT_OK, result)
                                            requireActivity().finish()
                                        }
                                    }
                                },
                            )

                        getString(R.string.playback_finished_do_nothing) -> {
                            // no-op
                        }

                        else -> Log.w(TAG, "Unknown playbackFinishedBehavior: $finishedBehavior")
                    }
                }.also { exoPlayer ->
                    var mediaItem: MediaItem? = null
                    var streamUrl = scene.streams["Direct stream"]
                    if (streamUrl != null && !forceTranscode) {
                        mediaItem = MediaItem.fromUri(streamUrl)
                    } else {
                        val streamChoice =
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                                .getString("stream_choice", "HLS")
                        streamUrl = scene.streams[streamChoice]
                        val mimeType =
                            when (streamChoice) {
                                "DASH" -> {
                                    MimeTypes.APPLICATION_MPD
                                }

                                "HLS" -> {
                                    MimeTypes.APPLICATION_M3U8
                                }

                                "MP4" -> {
                                    MimeTypes.VIDEO_MP4
                                }

                                else -> {
                                    MimeTypes.VIDEO_WEBM
                                }
                            }

                        mediaItem =
                            MediaItem.Builder()
                                .setUri(streamUrl)
                                .setMimeType(mimeType)
                                .build()
                    }
                    exoPlayer.setMediaItem(mediaItem, if (position > 0) position else C.TIME_UNSET)
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

        exoCenterControls = view.findViewById(androidx.media3.ui.R.id.exo_center_controls)

        scene = requireActivity().intent.getParcelableExtra(VideoDetailsActivity.MOVIE) as Scene?
            ?: throw RuntimeException()

        val showTitle =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean("exoShowTitle", true)
        val titleText = view.findViewById<TextView>(R.id.playback_title)
        if (showTitle) {
            titleText.text = scene.title
        } else {
            titleText.visibility = View.GONE
        }

        val position = requireActivity().intent.getLongExtra(VideoDetailsFragment.POSITION_ARG, -1)
        Log.d(TAG, "scene=${scene.id}, ${VideoDetailsFragment.POSITION_ARG}=$position")

        val controllerShowTimeoutMs =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("controllerShowTimeoutMs", PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS)

        videoView = view.findViewById(R.id.video_view)
        videoView.controllerShowTimeoutMs = controllerShowTimeoutMs
        videoView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener {
                if (!exoCenterControls.isVisible) {
                    hideControlsIfVisible()
                }
            },
        )
        val controller =
            videoView.findViewById<PlayerControlView>(androidx.media3.ui.R.id.exo_controller)

        val mFocusedZoom =
            requireContext().resources.getFraction(
                androidx.leanback.R.fraction.lb_focus_zoom_factor_large,
                1,
                1,
            )
        val onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus ->
                val zoom = if (hasFocus) mFocusedZoom else 1f
                v.animate().scaleX(zoom).scaleY(zoom).setDuration(150.toLong()).start()
            }

        val buttons =
            listOf(
                androidx.media3.ui.R.id.exo_rew_with_amount,
                androidx.media3.ui.R.id.exo_ffwd_with_amount,
                androidx.media3.ui.R.id.exo_settings,
                androidx.media3.ui.R.id.exo_prev,
                androidx.media3.ui.R.id.exo_play_pause,
                androidx.media3.ui.R.id.exo_next,
                androidx.media3.ui.R.id.exo_rew,
                androidx.media3.ui.R.id.exo_ffwd,
            )
        buttons.forEach {
            view.findViewById<View>(it)?.onFocusChangeListener = onFocusChangeListener
        }

        val oCounterText = view.findViewById<TextView>(R.id.controls_o_counter_text)
        if (scene.oCounter != null) {
            oCounterText.text = scene.oCounter.toString()
        } else {
            oCounterText.text = "0"
        }

        val oCounterButton = view.findViewById<ImageButton>(R.id.controls_o_counter_button)
        oCounterButton.onFocusChangeListener = onFocusChangeListener
        oCounterButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler(
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed_o_counter),
                        Toast.LENGTH_SHORT,
                    ),
                ),
            ) {
                val newCounter =
                    MutationEngine(requireContext()).incrementOCounter(scene.id)
                oCounterText.text = newCounter.count.toString()
            }
        }
        oCounterButton.setOnLongClickListener(
            PopupOnLongClickListener(
                listOf(
                    "Decrement",
                    "Reset",
                ),
            ) { _: AdapterView<*>, _: View, popUpItemPosition: Int, id: Long ->
                val mutationEngine = MutationEngine(requireContext())
                viewLifecycleOwner.lifecycleScope.launch(
                    StashCoroutineExceptionHandler(
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.failed_o_counter),
                            Toast.LENGTH_SHORT,
                        ),
                    ),
                ) {
                    when (popUpItemPosition) {
                        0 -> {
                            // Decrement
                            val newCount = mutationEngine.decrementOCounter(scene.id)
                            if (newCount.count > 0) {
                                oCounterText.text = newCount.count.toString()
                            } else {
                                oCounterText.text = "0"
                            }
                        }

                        1 -> {
                            // Reset
                            mutationEngine.resetOCounter(scene.id)
                            oCounterText.text = "0"
                        }

                        else ->
                            Log.w(
                                TAG,
                                "Unknown position for oCounterButton.setOnLongClickListener: $popUpItemPosition",
                            )
                    }
                }
            },
        )

        val previewImageView = view.findViewById<ImageView>(R.id.video_preview_image_view)
        previewTimeBar = view.findViewById(R.id.exo_progress)

        previewTimeBar.isPreviewEnabled = false
        previewTimeBar.addOnScrubListener(
            object : PreviewBar.OnScrubListener {
                override fun onScrubStart(previewBar: PreviewBar) {
                    player!!.playWhenReady = false
                    previewTimeBar.showPreview()
                }

                override fun onScrubMove(
                    previewBar: PreviewBar,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                }

                override fun onScrubStop(previewBar: PreviewBar) {
                    player!!.playWhenReady = true
                }
            },
        )

        if (scene.spriteUrl != null) {
            // Usually even if not null, there may not be sprites and the server will return a 404
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                withContext(Dispatchers.IO) {
                    val client = createOkHttpClient(requireContext())
                    val request = Request.Builder().url(scene.spriteUrl!!).get().build()
                    client.newCall(request).execute().use {
                        Log.d(
                            TAG,
                            "Sprite URL check isSuccessful=${it.isSuccessful}, code=${it.code}",
                        )
                        if (it.isSuccessful) {
                            previewTimeBar.isPreviewEnabled = true
                            previewTimeBar.setPreviewLoader(
                                StashPreviewLoader(
                                    requireContext(),
                                    previewImageView,
                                    scene,
                                ),
                            )
                        }
                    }
                }
            }
        }

        val moreOptionsButton = view.findViewById<ImageButton>(R.id.more_options_button)
        moreOptionsButton.onFocusChangeListener = onFocusChangeListener
        moreOptionsButton.setOnClickListener {
            val listPopUp =
                ListPopupWindow(
                    view.context,
                    null,
                    android.R.attr.listPopupWindowStyle,
                )
            listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
            listPopUp.anchorView = moreOptionsButton
            listPopUp.width = 300
            listPopUp.isModal = true

            val adapter =
                ArrayAdapter(
                    view.context,
                    R.layout.popup_item,
                    listOf("Create Marker"),
                )
            listPopUp.setAdapter(adapter)

            listPopUp.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
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
                    listPopUp.dismiss()
                }
            }

            listPopUp.show()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            val position =
                if (playbackPosition >= 0) {
                    playbackPosition
                } else {
                    requireActivity().intent.getLongExtra(VideoDetailsFragment.POSITION_ARG, -1)
                }
            val forceTranscode =
                requireActivity().intent.getBooleanExtra(
                    VideoDetailsFragment.FORCE_TRANSCODE,
                    false,
                )
            initializePlayer(position, forceTranscode)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onResume() {
        super.onResume()
//        hideSystemUi()
        if ((Util.SDK_INT <= 23 || player == null)) {
            val position =
                if (playbackPosition >= 0) {
                    playbackPosition
                } else {
                    requireActivity().intent.getLongExtra(VideoDetailsFragment.POSITION_ARG, -1)
                }
            val forceTranscode =
                requireActivity().intent.getBooleanExtra(
                    VideoDetailsFragment.FORCE_TRANSCODE,
                    false,
                )
            initializePlayer(position, forceTranscode)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
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

    private inner class AmbientPlaybackListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.v(TAG, "Keep screen on: $isPlaying")
            if (isPlaying) {
                requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
                        val dur = Constants.durationToString(newMarker.seconds)
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

    fun showAndFocusSeekBar() {
        videoView.showController()
        previewTimeBar.showPreview()
        previewTimeBar.requestFocus()
    }

    companion object {
        const val TAG = "PlaybackExoFragment"
    }
}
