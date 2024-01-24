package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.StashPreviewLoader
import com.github.rubensousa.previewseekbar.PreviewBar
import com.github.rubensousa.previewseekbar.media3.PreviewTimeBar

@OptIn(UnstableApi::class)
class PlaybackExoFragment :
    Fragment(R.layout.video_playback),
    PlaybackActivity.StashVideoPlayer {
    private var player: ExoPlayer? = null
    private lateinit var scene: Scene
    private lateinit var videoView: PlayerView

    private var playbackPosition = -1L

    override val currentVideoPosition get() = player?.currentPosition ?: playbackPosition

    override fun hideControlsIfVisible(): Boolean {
        if (videoView.isControllerFullyVisible) {
            videoView.hideController()
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
                }.also { exoPlayer ->
                    var mediaItem: MediaItem? = null
                    var streamUrl = scene.streams.get("Direct stream")
                    if (streamUrl != null && scene.videoCodec != "av1" && !forceTranscode) {
                        mediaItem = MediaItem.fromUri(streamUrl)
                    } else {
                        val streamChoice =
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                                .getString("stream_choice", "MP4")
                        streamUrl = scene.streams.get(streamChoice)
                        val mimeType =
                            if (streamChoice == "DASH") {
                                MimeTypes.APPLICATION_MPD
                            } else if (streamChoice == "HLS") {
                                MimeTypes.APPLICATION_M3U8
                            } else if (streamChoice == "MP4") {
                                MimeTypes.VIDEO_MP4
                            } else {
                                MimeTypes.VIDEO_WEBM
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
                    videoView.hideController()
                    exoPlayer.addListener(
                        object : Player.Listener {
                            private var initialSeek = true

                            override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
                                if (initialSeek && position > 0 && Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM in availableCommands) {
                                    exoPlayer.seekTo(position)
                                    initialSeek = false
                                }
                            }
                        },
                    )
                }
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        scene = requireActivity().intent.getParcelableExtra(DetailsActivity.MOVIE) as Scene?
            ?: throw RuntimeException()

        val position = requireActivity().intent.getLongExtra(VideoDetailsFragment.POSITION_ARG, -1)
        Log.d(TAG, "scene=${scene.id}, ${VideoDetailsFragment.POSITION_ARG}=$position")

        videoView = view.findViewById(R.id.video_view)
        videoView.requestFocus()
        videoView.controllerShowTimeoutMs = 2000
        videoView.hideController()

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
            )
        buttons.forEach {
            view.findViewById<View>(it)?.onFocusChangeListener = onFocusChangeListener
        }

        val previewImageView = view.findViewById<ImageView>(R.id.video_preview_image_view)
        val previewTimeBar = view.findViewById<PreviewTimeBar>(R.id.exo_progress)
        previewTimeBar.isPreviewEnabled = true
        previewTimeBar.setPreviewLoader(StashPreviewLoader(previewImageView, scene))
        previewTimeBar.addOnScrubListener(
            object : PreviewBar.OnScrubListener {
                override fun onScrubStart(previewBar: PreviewBar) {
                    player!!.playWhenReady = false
                }

                override fun onScrubMove(
                    previewBar: PreviewBar,
                    progress: Int,
                    fromUser: Boolean,
                ) {}

                override fun onScrubStop(previewBar: PreviewBar) {
                    player!!.playWhenReady = true
                }
            },
        )

//        val timeBar: DefaultTimeBar =
//            view.findViewById(androidx.media3.ui.R.id.exo_progress)
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

    companion object {
        const val TAG = "PlaybackExoFragment"
    }
}
