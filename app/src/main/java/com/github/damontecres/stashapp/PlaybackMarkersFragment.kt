package com.github.damontecres.stashapp

import android.os.Build
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
import androidx.annotation.OptIn
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.api.CountMarkersQuery
import com.github.damontecres.stashapp.api.FindMarkersQuery
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.data.AppFilter
import com.github.damontecres.stashapp.data.FilterType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.presenters.PopupOnLongClickListener
import com.github.damontecres.stashapp.suppliers.MarkerDataSupplier
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.CodecSupport
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.util.toFindFilterType
import com.github.damontecres.stashapp.views.StashPlayerView
import com.github.rubensousa.previewseekbar.PreviewBar
import com.github.rubensousa.previewseekbar.media3.PreviewTimeBar
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(UnstableApi::class)
class PlaybackMarkersFragment() :
    Fragment(R.layout.video_playback),
    PlaybackActivity.StashVideoPlayer {
    private lateinit var sourceFilter: StashFilter

    private var player: ExoPlayer? = null
    lateinit var videoView: StashPlayerView
    lateinit var previewTimeBar: PreviewTimeBar
    private lateinit var queryEngine: QueryEngine
    private lateinit var pagingSource: StashPagingSource<FindMarkersQuery.Data, MarkerData, CountMarkersQuery.Data>
    private lateinit var exoCenterControls: View
    private lateinit var debugView: View
    private lateinit var debugPlaybackTextView: TextView
    private lateinit var debugVideoTextView: TextView
    private lateinit var debugAudioTextView: TextView
    private lateinit var debugContainerTextView: TextView
    private lateinit var titleText: TextView
    private lateinit var dateText: TextView
    private lateinit var oCounterText: TextView
    private lateinit var oCounterButton: ImageButton

    private var playbackPosition = -1L

    override val currentVideoPosition get() = player?.currentPosition ?: playbackPosition

    override val isControllerVisible get() = videoView.isControllerFullyVisible || previewTimeBar.isShown

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

    private fun updateDebugInfo(
        streamChoice: StreamChoice,
        scene: Scene,
    ) {
        when (streamChoice.transcodeDecision) {
            TranscodeDecision.TRANSCODE -> debugPlaybackTextView.text = "Transcode"
            TranscodeDecision.FORCED_TRANSCODE -> debugPlaybackTextView.text = "Force transcode"
            TranscodeDecision.DIRECT_PLAY -> debugPlaybackTextView.text = "Direct"
            TranscodeDecision.FORCED_DIRECT_PLAY -> debugPlaybackTextView.text = "Force direct"
        }
        debugVideoTextView.text =
            if (streamChoice.videoSupported) scene.videoCodec else "${scene.videoCodec} (unsupported)"
        debugAudioTextView.text =
            if (streamChoice.audioSupported) scene.audioCodec else "${scene.audioCodec} (unsupported)"
        debugContainerTextView.text =
            if (streamChoice.containerSupported) scene.format else "${scene.format} (unsupported)"
    }

    private fun updateUi(marker: MarkerData) {
        val streamChoice = chooseStream(marker, 0L, false, false)
        val scene = Scene.fromVideoSceneData(marker.scene.videoSceneData)
        updateDebugInfo(streamChoice, scene)

        Toast.makeText(requireContext(), scene.title, Toast.LENGTH_SHORT).show()

        val showTitle = PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("exoShowTitle", true)
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
            oCounterText.text = "0"
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
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        player =
            StashExoPlayer.getInstance(requireContext())
                .also { exoPlayer ->
                    videoView.player = exoPlayer
                    StashExoPlayer.addListener(AmbientPlaybackListener())
                }.also { exoPlayer ->
                    StashExoPlayer.addListener(
                        object : Listener {
                            override fun onPlayerError(error: PlaybackException) {
                                val marker =
                                    exoPlayer.currentMediaItem?.localConfiguration?.tag as MarkerData?
                                Toast.makeText(
                                    requireContext(),
                                    "Playback error: ${marker?.scene?.videoSceneData?.titleOrFilename}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                exoPlayer.seekToNext()
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
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
                    exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        buildPlaylist()
                    }

                    if (videoView.controllerShowTimeoutMs > 0) {
                        videoView.hideController()
                    }
                }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceFilter = requireActivity().intent.getParcelableExtra(INTENT_FILTER_ID)!!
        queryEngine = QueryEngine(requireContext())
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val manager = PreferenceManager.getDefaultSharedPreferences(requireContext())

        exoCenterControls = view.findViewById(androidx.media3.ui.R.id.exo_center_controls)

        debugView = view.findViewById(R.id.playback_debug_info)
        debugPlaybackTextView = view.findViewById(R.id.debug_playback)
        debugVideoTextView = view.findViewById(R.id.debug_video)
        debugAudioTextView = view.findViewById(R.id.debug_audio)
        debugContainerTextView = view.findViewById(R.id.debug_container_format)

        titleText = view.findViewById<TextView>(R.id.playback_title)
        dateText = view.findViewById<TextView>(R.id.playback_date)

        if (manager.getBoolean(getString(R.string.pref_key_show_playback_debug_info), false)) {
            debugView.visibility = View.VISIBLE
        }

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

        oCounterText = view.findViewById<TextView>(R.id.controls_o_counter_text)
        oCounterButton = view.findViewById<ImageButton>(R.id.controls_o_counter_button)
        oCounterButton.onFocusChangeListener = onFocusChangeListener

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

        previewTimeBar.isPreviewEnabled = false

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

            val debugToggleText =
                if (debugView.isVisible) "Hide transcode info" else "Show transcode info"

            val adapter =
                ArrayAdapter(
                    view.context,
                    R.layout.popup_item,
                    listOf(debugToggleText),
                )
            listPopUp.setAdapter(adapter)

            listPopUp.setOnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
                if (position == 0) {
                    if (debugView.isVisible) {
                        debugView.visibility = View.GONE
                    } else {
                        debugView.visibility = View.VISIBLE
                    }
                }
                listPopUp.dismiss()
            }

            listPopUp.show()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onResume() {
        super.onResume()
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer()
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

    private suspend fun buildPlaylist() {
        val filter = sourceFilter
        val savedFilter =
            when (filter.filterType) {
                FilterType.APP_FILTER -> {
                    filter as AppFilter
                    filter.toSavedFilterData(requireContext())
                }

                FilterType.SAVED_FILTER -> {
                    filter as StashSavedFilter
                    queryEngine.getSavedFilter(filter.savedFilterId)
                }

                FilterType.CUSTOM_FILTER -> {
                    filter as StashCustomFilter
                    filter.toSavedFilterData()
                }
            }
        if (savedFilter != null) {
            val duration = requireActivity().intent.getLongExtra(INTENT_DURATION_ID, 15_000L)
            val filterParser = FilterParser(ServerPreferences(requireContext()).serverVersion)
            val objectFilter = filterParser.convertMarkerObjectFilter(savedFilter.object_filter)
            val dataSupplier =
                MarkerDataSupplier(savedFilter.find_filter?.toFindFilterType(), objectFilter)
            pagingSource = StashPagingSource(requireContext(), 25, dataSupplier)
            addPageToPlaylist(1, duration)
            player!!.addListener(PlaylistListener(duration))
            player!!.prepare()
            player!!.volume = 1f
            player!!.playWhenReady = true
        } else {
            Log.w(TAG, "savedFilter is null")
            Toast.makeText(requireContext(), "Could not determine filter", Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun addPageToPlaylist(
        page: Int,
        duration: Long,
    ): Boolean {
        Log.v(TAG, "Fetching page #$page")
        val newMarkers = pagingSource.fetchPage(page, 25)
        val mediaItems =
            newMarkers.map { chooseStream(it, duration, false, false).mediaItem }
        Log.v(TAG, "Got ${mediaItems.size} media items")
        if (mediaItems.isNotEmpty()) {
            updateUi(newMarkers[0])
            player!!.addMediaItems(mediaItems)
            return true
        } else {
            return false
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

    private inner class PlaylistListener(val duration: Long) :
        Listener {
        private var hasMorePages = true

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            player!!.currentMediaItem?.let {
                val marker = it.localConfiguration!!.tag!! as MarkerData
                updateUi(marker)
            }
        }

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int,
        ) {
            if (mediaItem != null) {
                val marker = mediaItem.localConfiguration!!.tag!! as MarkerData
                Log.v(TAG, "Starting playback of marker ${marker.id}")
                updateUi(marker)
            }
            if (hasMorePages) {
                val count = player!!.mediaItemCount
                // TODO: https://medium.com/@nicholas.rose/exoplayer-playlist-diffing-f8fcd4b2ab7c
                if (count - player!!.currentMediaItemIndex <= 5) {
                    Log.v(TAG, "Too few items in playlist")
                    val nextPage = count / 25 + 2
                    // TODO race condition
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        if (!addPageToPlaylist(nextPage, duration)) {
                            Log.v(TAG, "No more markers")
                            hasMorePages = false
                        }
                    }
                }
            }
        }
    }

    enum class TranscodeDecision {
        DIRECT_PLAY,
        FORCED_DIRECT_PLAY,
        TRANSCODE,
        FORCED_TRANSCODE,
    }

    data class StreamChoice(
        val transcodeDecision: TranscodeDecision,
        val videoSupported: Boolean,
        val audioSupported: Boolean,
        val containerSupported: Boolean,
        val mediaItem: MediaItem,
    )

    private fun buildMediaItems(
        url: String,
        format: String?,
        marker: MarkerData,
        clipConfig: MediaItem.ClippingConfiguration,
    ): MediaItem {
        val mimeType =
            when (format?.lowercase()) {
                // As recommended by https://developer.android.com/media/media3/exoplayer/hls#using-mediaitem
                // Specify the mimetype for HLS & DASH streams
                "hls" -> MimeTypes.APPLICATION_M3U8
                "dash" -> MimeTypes.APPLICATION_MPD
                else -> null
            }
        return MediaItem.Builder()
            .setUri(url)
            .setMimeType(mimeType)
            .setClippingConfiguration(clipConfig)
            .setTag(marker)
            .build()
    }

    private fun chooseStream(
        marker: MarkerData,
        duration: Long,
        forceTranscode: Boolean,
        forceDirectPlay: Boolean,
    ): StreamChoice {
        val startPos = (marker.seconds * 1000).toLong()
        val clipConfig =
            ClippingConfiguration.Builder()
                .setStartPositionMs(startPos)
                .setEndPositionMs(startPos + duration)
                .build()

        val scene = Scene.fromVideoSceneData(marker.scene.videoSceneData)
        val supportedCodecs = CodecSupport.getSupportedCodecs(requireContext())
        val videoSupported = supportedCodecs.isVideoSupported(scene.videoCodec)
        val audioSupported = supportedCodecs.isAudioSupported(scene.audioCodec)
        val containerSupported = supportedCodecs.isContainerFormatSupported(scene.format)
        if (
            !forceTranscode &&
            videoSupported &&
            audioSupported &&
            containerSupported &&
            scene.streamUrl != null
        ) {
            Log.v(
                TAG,
                "Video (${scene.videoCodec}), audio (${scene.audioCodec}), & container (${scene.format}) supported",
            )
            return StreamChoice(
                TranscodeDecision.DIRECT_PLAY,
                videoSupported,
                audioSupported,
                containerSupported,
                buildMediaItems(scene.streamUrl!!, scene.format, marker, clipConfig),
            )
        } else if (forceDirectPlay) {
            Log.v(
                TAG,
                "Forcing direct play for video (${scene.videoCodec}), audio (${scene.audioCodec}), & container (${scene.format})",
            )
            return StreamChoice(
                TranscodeDecision.FORCED_DIRECT_PLAY,
                videoSupported,
                audioSupported,
                containerSupported,
                buildMediaItems(scene.streamUrl!!, scene.format, marker, clipConfig),
            )
        } else {
            val streamChoice =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("stream_choice", "HLS")
            val streamUrl = scene.streams[streamChoice]
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
            Log.v(
                TAG,
                "Transcoding video (${scene.videoCodec}), audio (${scene.audioCodec}), & container (${scene.format}) using $streamChoice",
            )
            return StreamChoice(
                if (forceTranscode) TranscodeDecision.FORCED_TRANSCODE else TranscodeDecision.TRANSCODE,
                videoSupported,
                audioSupported,
                containerSupported,
                MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMimeType(mimeType)
                    .setTag(marker)
                    .setClippingConfiguration(clipConfig)
                    .build(),
            )
        }
    }

    override fun showAndFocusSeekBar() {
        videoView.showController()
        previewTimeBar.showPreview()
        previewTimeBar.requestFocus()
    }

    companion object {
        const val TAG = "PlaybackMarkersFragment"
        const val INTENT_FILTER_ID = "$TAG.filter"
        const val INTENT_DURATION_ID = "$TAG.duration"
    }
}
