package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.presenters.PopupOnLongClickListener
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashPreviewLoader
import com.github.damontecres.stashapp.util.createOkHttpClient
import com.github.rubensousa.previewseekbar.PreviewBar
import com.github.rubensousa.previewseekbar.media3.PreviewTimeBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(UnstableApi::class)
class PlaybackExoFragment :
    Fragment(R.layout.video_playback),
    PlaybackActivity.StashVideoPlayer {
    private var player: ExoPlayer? = null
    private lateinit var scene: Scene
    private lateinit var videoView: PlayerView
    private lateinit var previewTimeBar: PreviewTimeBar
    private lateinit var exoCenterControls: View

    private var playbackPosition = -1L

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
        player = null
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer(
        position: Long,
        forceTranscode: Boolean,
    ) {
        val apiKey =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("stashApiKey", "")
        val skipForward =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("skip_forward_time", 30)
        val skipBack =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("skip_back_time", 10)

        val dataSourceFactory =
            DataSource.Factory {
                val dataSource = DefaultHttpDataSource.Factory().createDataSource()
                if (!apiKey.isNullOrBlank()) {
                    dataSource.setRequestProperty(Constants.STASH_API_HEADER, apiKey)
                    dataSource.setRequestProperty(Constants.STASH_API_HEADER.lowercase(), apiKey)
                }
                dataSource
            }

        player =
            ExoPlayer.Builder(requireContext())
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(requireContext()).setDataSourceFactory(
                        dataSourceFactory,
                    ),
                )
                .setSeekBackIncrementMs(skipBack * 1000L)
                .setSeekForwardIncrementMs(skipForward * 1000L)
                .build()
                .also { exoPlayer ->
                    videoView.player = exoPlayer
                    exoPlayer.addListener(AmbientPlaybackListener())
                    if (ServerPreferences(requireContext()).trackActivity) {
                        exoPlayer.addListener(PlaybackListener())
                    }
                }.also { exoPlayer ->
                    var mediaItem: MediaItem? = null
                    var streamUrl = scene.streams["Direct stream"]
                    if (streamUrl != null && scene.videoCodec != "av1" && !forceTranscode) {
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

                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.playWhenReady = true
                    exoPlayer.prepare()
                    if (videoView.controllerShowTimeoutMs > 0) {
                        videoView.hideController()
                    }
                    if (position > 0) {
                        exoPlayer.addListener(
                            object : Player.Listener {
                                override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
                                    if (Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM in availableCommands) {
                                        exoPlayer.seekTo(position)
                                        exoPlayer.removeListener(this)
                                    }
                                }
                            },
                        )
                    }
                }
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

                if (hasFocus) {
                    v.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.selected_background,
                        ),
                    )
                } else {
                    v.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            android.R.color.transparent,
                        ),
                    )
                }
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
                    MutationEngine(requireContext()).incrementOCounter(scene.id.toInt())
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
                            val newCount = mutationEngine.decrementOCounter(scene.id.toInt())
                            if (newCount.count > 0) {
                                oCounterText.text = newCount.count.toString()
                            } else {
                                oCounterText.text = "0"
                            }
                        }

                        1 -> {
                            // Reset
                            mutationEngine.resetOCounter(scene.id.toInt())
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
    }

    @OptIn(UnstableApi::class)
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            val position =
                requireActivity().intent.getLongExtra(VideoDetailsFragment.POSITION_ARG, -1)
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
                requireActivity().intent.getLongExtra(VideoDetailsFragment.POSITION_ARG, -1)
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
        private val job: Job
        private val mutationEngine = MutationEngine(requireContext())
        private val minimumPlayPercent = ServerPreferences(requireContext()).minimumPlayPercent

        private var totalPlayDuration = 0L
        private var previousPosition = 0L
        private var isEnded = AtomicBoolean(false)
        private var incrementedPlayCount = AtomicBoolean(false)

        init {
            job =
                launch {
                    while (true) {
                        if (!isEnded.get()) {
                            delay(10.toDuration(DurationUnit.SECONDS))
                            Log.v(TAG, "Timer saveSceneActivity")
                            saveSceneActivity(currentVideoPosition)
                        }
                    }
                }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.v(TAG, "onIsPlayingChanged isPlaying=$isPlaying")
            if (isPlaying) {
                isEnded.set(false)
            }
            if (!isEnded.get()) {
                launch {
                    saveSceneActivity(currentVideoPosition)
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                Log.v(TAG, "onPlaybackStateChanged STATE_ENDED")
                isEnded.set(true)
                playbackPosition = 0 // Needed?
                launch {
                    saveSceneActivity(0)
                }
            }
        }

        private suspend fun saveSceneActivity(position: Long) {
            mutationEngine.saveSceneActivity(scene.id, position, getDuration())
            if (scene.duration != null &&
                totalPlayDuration >= minimumPlayPercent / 100.0 * scene.duration!! &&
                !incrementedPlayCount.get()
            ) {
                mutationEngine.incrementPlayCount(scene.id)
                incrementedPlayCount.set(true)
            }
        }

        private fun getDuration(): Long {
            val currentPosition = currentVideoPosition
            val duration = currentPosition - previousPosition
            previousPosition = currentPosition
            totalPlayDuration += duration
            return duration
        }

        private fun launch(block: suspend () -> Unit): Job {
            return viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                block.invoke()
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

    companion object {
        const val TAG = "PlaybackExoFragment"
    }
}
