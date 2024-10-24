package com.github.damontecres.stashapp.playback

import android.app.Activity
import android.content.Intent
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
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerControlView
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.SearchForActivity
import com.github.damontecres.stashapp.SearchForFragment
import com.github.damontecres.stashapp.actions.StashAction
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.data.ThrottledLiveData
import com.github.damontecres.stashapp.presenters.PopupOnLongClickListener
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashPreviewLoader
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.animateToInvisible
import com.github.damontecres.stashapp.util.animateToVisible
import com.github.damontecres.stashapp.views.ListPopupWindowBuilder
import com.github.damontecres.stashapp.views.durationToString
import com.github.rubensousa.previewseekbar.PreviewBar
import com.github.rubensousa.previewseekbar.media3.PreviewTimeBar
import kotlinx.coroutines.CoroutineExceptionHandler
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
    protected val filterViewModel: VideoFilterViewModel by activityViewModels()

    protected var trackActivityListener: TrackActivityPlaybackListener? = null
    protected val controllerVisibilityListener = ControllerVisibilityListenerList()

    /**
     * Whether to show video previews when scrubbing
     */
    protected abstract val previewsEnabled: Boolean

    /**
     * Configuration for what options to show for the [moreOptionsButton]
     */
    abstract val optionsButtonOptions: OptionsButtonOptions

    /**
     * Initialize an [ExoPlayer]. Users should start with [StashExoPlayer]!
     */
    protected abstract fun initializePlayer(): ExoPlayer

    var player: ExoPlayer? = null
        private set
    protected lateinit var videoView: StashPlayerView
    protected lateinit var previewImageView: ImageView
    protected lateinit var previewTimeBar: PreviewTimeBar
    protected lateinit var exoCenterControls: View
    protected lateinit var titleText: TextView
    protected lateinit var dateText: TextView
    protected lateinit var debugView: View
    protected lateinit var debugSceneId: TextView
    protected lateinit var debugPlaybackTextView: TextView
    protected lateinit var debugVideoTextView: TextView
    protected lateinit var debugAudioTextView: TextView
    protected lateinit var debugContainerTextView: TextView
    protected lateinit var debugPlaylistTextView: TextView
    protected lateinit var oCounterButton: ImageButton
    protected lateinit var oCounterText: TextView
    private lateinit var moreOptionsButton: ImageButton

    // Track whether the video is playing before calling the resultLauncher
    protected var wasPlayingBeforeResultLauncher: Boolean? = null
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private val videoFilterFragment = PlaybackVideoFiltersFragment()

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

    protected val server = StashServer.requireCurrentServer()

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
        trackActivityListener?.release(currentVideoPosition)
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
        debugSceneId.text = scene.id
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
        val mutationEngine = MutationEngine(server)
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
                val newCounter = mutationEngine.incrementOCounter(scene.id)
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
        filterViewModel.maybeGetSavedFilter(scene.id)
    }

    /**
     * If video filter preference is enabled, this will setup for applying effects
     *
     * Should be called before [Player.prepare]
     */
    protected fun maybeSetupVideoEffects(exoPlayer: ExoPlayer) {
        if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_video_filters), false)
        ) {
            Log.v(PlaybackSceneFragment.TAG, "Initializing video effects")
            exoPlayer.setVideoEffects(listOf())

            controllerVisibilityListener.addListener {
                if (it == View.VISIBLE) {
                    showVideoFilterFragment()
                } else {
                    hideVideoFilterFragment()
                }
            }

            ThrottledLiveData(filterViewModel.videoFilter, 500L).observe(
                viewLifecycleOwner,
            ) { vf ->
                Log.v(TAG, "Got new VideoFilter: $vf")
                val effectList = vf?.createEffectList().orEmpty()
                Log.d(TAG, "Applying ${effectList.size} effects")
                player?.setVideoEffects(effectList)
            }
        }
    }

    private fun hideVideoFilterFragment() {
        childFragmentManager.commit {
            hide(videoFilterFragment)
        }
    }

    private fun showVideoFilterFragment() {
        childFragmentManager.commit {
            show(videoFilterFragment)
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
        val manager = PreferenceManager.getDefaultSharedPreferences(requireContext())

        titleText = view.findViewById(R.id.playback_title)
        dateText = view.findViewById(R.id.playback_date)
        exoCenterControls = view.findViewById(androidx.media3.ui.R.id.exo_center_controls)

        debugView = view.findViewById(R.id.playback_debug_info)
        debugSceneId = view.findViewById(R.id.debug_scene_id)
        debugPlaybackTextView = view.findViewById(R.id.debug_playback)
        debugVideoTextView = view.findViewById(R.id.debug_video)
        debugAudioTextView = view.findViewById(R.id.debug_audio)
        debugContainerTextView = view.findViewById(R.id.debug_container_format)
        debugPlaylistTextView = view.findViewById(R.id.debug_playlist)

        if (manager.getBoolean(getString(R.string.pref_key_show_playback_debug_info), false)) {
            debugView.visibility = View.VISIBLE
        }

        videoView = view.findViewById(R.id.video_view)
        videoView.controllerShowTimeoutMs =
            manager.getInt("controllerShowTimeoutMs", PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS)
        videoView.setControllerVisibilityListener(controllerVisibilityListener)

        val backCallback =
            requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), false) {
                hideControlsIfVisible()
            }
        controllerVisibilityListener.addListener { vis ->
            backCallback.isEnabled = vis == View.VISIBLE
        }
        controllerVisibilityListener.addListener { _ ->
            if (!exoCenterControls.isVisible) {
                hideControlsIfVisible()
            }
        }

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

        setupMoreOptions()
    }

    private fun setupMoreOptions() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        var addedVideoFilter = false

        moreOptionsButton.setOnClickListener {
            val callbacks = mutableMapOf<Int, () -> Unit>()
            val options =
                buildList {
                    if (optionsButtonOptions.isPlayList) {
                        add("Show Playlist")
                        callbacks[size - 1] = {
                            // Kind of hacky
                            val fragment = this@PlaybackFragment as PlaylistFragment<*, *, *>
                            fragment.showPlaylist()
                        }
                    }

                    if (optionsButtonOptions.dataType == DataType.SCENE && !optionsButtonOptions.isPlayList) {
                        add("Create Marker")
                        callbacks[size - 1] = {
                            // Save current playback state
                            player?.let { exoPlayer ->
                                playbackPosition = exoPlayer.currentPosition
                                wasPlayingBeforeResultLauncher = exoPlayer.isPlaying
                            }
                            val intent = Intent(requireActivity(), SearchForActivity::class.java)
                            intent.putExtra(
                                SearchForFragment.TITLE_KEY,
                                "for primary tag for scene marker",
                            )
                            intent.putExtra("dataType", DataType.TAG.name)
                            intent.putExtra(SearchForFragment.ID_KEY, StashAction.CREATE_MARKER.id)
                            resultLauncher.launch(intent)
                        }
                    }

                    if (preferences.getBoolean(getString(R.string.pref_key_video_filters), false)) {
                        if (addedVideoFilter) {
                            add("Hide Video Filters")
                            callbacks[size - 1] = {
                                childFragmentManager.commitNow {
                                    remove(videoFilterFragment)
                                }
                                addedVideoFilter = !addedVideoFilter
                            }
                        } else {
                            add("Show Video Filters")
                            callbacks[size - 1] = {
                                videoView.showController()
                                childFragmentManager.commitNow {
                                    add(R.id.video_overlay, videoFilterFragment)
                                }
                                videoFilterFragment.requireView().requestFocus()
                                addedVideoFilter = !addedVideoFilter
                            }
                        }
                    }

                    val debugToggleText =
                        if (debugView.isVisible) "Hide transcode info" else "Show transcode info"
                    add(debugToggleText)
                    callbacks[size - 1] = {
                        if (debugView.isVisible) {
                            debugView.animateToInvisible(View.GONE)
                        } else {
                            debugView.animateToVisible()
                        }
                    }
                }
            val previousControllerShowTimeoutMs = videoView.controllerShowTimeoutMs
            ListPopupWindowBuilder(
                moreOptionsButton,
                options,
            ) { position ->
                callbacks[position]?.let { it() }
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

    private fun updatePreviewLoader(scene: Scene) {
        if (previewsEnabled && scene.spriteUrl != null) {
            // Usually even if not null, there may not be sprites and the server will return a 404
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                withContext(Dispatchers.IO) {
                    val client = StashClient.getHttpClient(server)
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

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
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
        val server = StashServer.requireCurrentServer()
        if (server.serverPreferences.trackActivity && currentScene != null) {
            Log.v(TAG, "Adding TrackActivityPlaybackListener")
            trackActivityListener =
                TrackActivityPlaybackListener(
                    context = requireContext(),
                    mutationEngine = MutationEngine(server),
                    scene = currentScene!!,
                    getCurrentPosition = ::currentVideoPosition,
                )
            exoPlayer.addListener(trackActivityListener!!)
        }
    }

    data class OptionsButtonOptions(val dataType: DataType, val isPlayList: Boolean)

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
                            Log.e(PlaybackSceneFragment.TAG, "Exception creating marker", ex)
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
                            PlaybackSceneFragment.TAG,
                            "Adding marker at $videoPos with tagId=$tagId to scene ${currentScene!!.id}",
                        )
                        val newMarker =
                            MutationEngine(server).createMarker(
                                currentScene!!.id,
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
        private const val TAG = "PlaybackFragment"
    }
}
