package com.github.damontecres.stashapp.playback

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.presenters.PopupOnLongClickListener
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashPreviewLoader
import com.github.rubensousa.previewseekbar.PreviewBar
import com.github.rubensousa.previewseekbar.media3.PreviewTimeBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Parent [Fragment] for playing videos
 */
@UnstableApi
abstract class PlaybackFragment(
    @LayoutRes layoutId: Int = R.layout.video_playback,
) : Fragment(layoutId) {
    protected var trackActivityListener: TrackActivityPlaybackListener? = null

    /**
     * Whether to show video previews when scrubbing
     */
    protected abstract val previewsEnabled: Boolean

    /**
     * Initialize an [ExoPlayer]. Users should start with [StashExoPlayer]!
     */
    protected abstract fun initializePlayer(): ExoPlayer

    protected var player: ExoPlayer? = null
        private set
    protected lateinit var videoView: StashPlayerView
    protected lateinit var previewImageView: ImageView
    protected lateinit var previewTimeBar: PreviewTimeBar
    protected lateinit var exoCenterControls: View
    protected lateinit var titleText: TextView
    protected lateinit var dateText: TextView
    protected lateinit var debugView: View
    protected lateinit var debugPlaybackTextView: TextView
    protected lateinit var debugVideoTextView: TextView
    protected lateinit var debugAudioTextView: TextView
    protected lateinit var debugContainerTextView: TextView
    protected lateinit var oCounterButton: ImageButton
    protected lateinit var oCounterText: TextView
    protected lateinit var moreOptionsButton: ImageButton

    protected var playbackPosition = -1L
    val currentVideoPosition get() = player?.currentPosition ?: playbackPosition
    var currentScene: Scene? = null
        set(newScene) {
            field = newScene
            if (newScene != null) {
                updateUI(newScene)
            }
        }

    val isControllerVisible get() = videoView.isControllerFullyVisible || previewTimeBar.isShown

    fun hideControlsIfVisible(): Boolean {
        if (isControllerVisible) {
            videoView.hideController()
            previewTimeBar.hidePreview()
            previewTimeBar.hideScrubber(250L)
            return true
        }
        return false
    }

    protected open fun releasePlayer() {
        trackActivityListener?.release(playbackPosition)
        trackActivityListener = null
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            exoPlayer.release()
        }
        player = null
    }

    private fun createPlayer(): ExoPlayer {
        return initializePlayer().also { exoPlayer ->
            exoPlayer.addListener(
                object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Toast.makeText(
                            requireContext(),
                            "Playback error: ${error.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                    }

                    override fun onPlayerErrorChanged(error: PlaybackException?) {
                        if (error != null) {
                            Toast.makeText(
                                requireContext(),
                                "Playback error: ${error.message}",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                },
            )
        }.also { exoPlayer ->
            videoView.player = exoPlayer
            exoPlayer.addListener(AmbientPlaybackListener())
        }
    }

    protected fun updateDebugInfo(
        streamDecision: StreamDecision,
        scene: Scene,
    ) {
        when (streamDecision.transcodeDecision) {
            TranscodeDecision.TRANSCODE ->
                debugPlaybackTextView.text =
                    getString(R.string.transcode)

            TranscodeDecision.FORCED_TRANSCODE ->
                debugPlaybackTextView.text =
                    getString(R.string.force_transcode)

            TranscodeDecision.DIRECT_PLAY ->
                debugPlaybackTextView.text = getString(R.string.direct)

            TranscodeDecision.FORCED_DIRECT_PLAY ->
                debugPlaybackTextView.text =
                    getString(R.string.force_direct)
        }
        debugVideoTextView.text =
            if (streamDecision.videoSupported) scene.videoCodec else "${scene.videoCodec} (unsupported)"
        debugAudioTextView.text =
            if (streamDecision.audioSupported) scene.audioCodec else "${scene.audioCodec} (unsupported)"
        debugContainerTextView.text =
            if (streamDecision.containerSupported) scene.format else "${scene.format} (unsupported)"
    }

    protected open fun updateUI(scene: Scene) {
        val showTitle =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean("exoShowTitle", true)
        if (showTitle) {
            titleText.text = scene.title
            dateText.text =
                if (scene.date != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
                        val date = LocalDate.parse(scene.date, DateTimeFormatter.ISO_DATE)
                        date.format(dateFormatter)
                    } catch (ex: DateTimeParseException) {
                        scene.date
                    }
                } else {
                    scene.date
                }
            if (dateText.text.isNullOrBlank()) {
                dateText.visibility = View.GONE
            }
        } else {
            titleText.visibility = View.GONE
            dateText.visibility = View.GONE
        }

        if (scene.oCounter != null) {
            oCounterText.text = scene.oCounter.toString()
        } else {
            oCounterText.text = getString(R.string.zero)
        }

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
                                oCounterText.text = getString(R.string.zero)
                            }
                        }

                        1 -> {
                            // Reset
                            mutationEngine.resetOCounter(scene.id)
                            oCounterText.text = getString(R.string.zero)
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

        updatePreviewLoader(scene)
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val manager = PreferenceManager.getDefaultSharedPreferences(requireContext())

        titleText = view.findViewById(R.id.playback_title)
        dateText = view.findViewById(R.id.playback_date)
        exoCenterControls = view.findViewById(androidx.media3.ui.R.id.exo_center_controls)

        debugView = view.findViewById(R.id.playback_debug_info)
        debugPlaybackTextView = view.findViewById(R.id.debug_playback)
        debugVideoTextView = view.findViewById(R.id.debug_video)
        debugAudioTextView = view.findViewById(R.id.debug_audio)
        debugContainerTextView = view.findViewById(R.id.debug_container_format)

        if (manager.getBoolean(getString(R.string.pref_key_show_playback_debug_info), false)) {
            debugView.visibility = View.VISIBLE
        }

        videoView = view.findViewById(R.id.video_view)
        videoView.controllerShowTimeoutMs =
            manager.getInt("controllerShowTimeoutMs", PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS)
        videoView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener {
                if (!exoCenterControls.isVisible) {
                    hideControlsIfVisible()
                }
            },
        )

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

        oCounterText = view.findViewById(R.id.controls_o_counter_text)
        oCounterButton = view.findViewById(R.id.controls_o_counter_button)
        oCounterButton.onFocusChangeListener = onFocusChangeListener

        previewImageView = view.findViewById(R.id.video_preview_image_view)
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

        moreOptionsButton = view.findViewById(R.id.more_options_button)
        moreOptionsButton.onFocusChangeListener = onFocusChangeListener
    }

    private fun updatePreviewLoader(scene: Scene) {
        if (previewsEnabled && scene.spriteUrl != null) {
            // Usually even if not null, there may not be sprites and the server will return a 404
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                withContext(Dispatchers.IO) {
                    val client = StashClient.getHttpClient(requireContext())
                    val request = Request.Builder().url(scene.spriteUrl).get().build()
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
            player = createPlayer()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onResume() {
        super.onResume()
        if ((Util.SDK_INT <= 23 || player == null)) {
            player = createPlayer()
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

    fun showAndFocusSeekBar() {
        videoView.showController()
        previewTimeBar.showPreview()
        previewTimeBar.requestFocus()
    }

    open fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return videoView.dispatchKeyEvent(event)
    }

    inner class AmbientPlaybackListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.v(TAG, "Keep screen on: $isPlaying")
            if (isPlaying) {
                requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    protected fun maybeAddActivityTracking(exoPlayer: ExoPlayer) {
        if (ServerPreferences(requireContext()).trackActivity) {
            Log.v(TAG, "Adding TrackActivityPlaybackListener")
            trackActivityListener = TrackActivityPlaybackListener(this)
            exoPlayer.addListener(trackActivityListener!!)
        }
    }

    companion object {
        private const val TAG = "PlaybackFragment"
    }
}
