package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
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

@OptIn(UnstableApi::class)
class PlaybackExoFragment :
    Fragment(R.layout.video_playback),
    PlaybackActivity.StashVideoPlayer {
    var player: ExoPlayer? = null
    private lateinit var scene: Scene
    lateinit var videoView: PlayerView

    private var playWhenReady = true
    private var mediaItemIndex = 0
    private var playbackPosition = 0L

    override val currentVideoPosition get() = player!!.currentPosition

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
            mediaItemIndex = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        player = null
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer(position: Long) {
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
                dataSource.setRequestProperty(Constants.STASH_API_HEADER, apiKey!!)
                dataSource.setRequestProperty("apikey", apiKey!!)
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
                    if (streamUrl != null) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val scene = requireActivity().intent.getParcelableExtra(DetailsActivity.MOVIE) as Scene?
        if (scene == null) {
            throw RuntimeException()
        }
        this.scene = scene
        val position = requireActivity().intent.getLongExtra(VideoDetailsFragment.POSITION_ARG, -1)
        Log.d(TAG, "scene=${scene?.id}")
        Log.d(TAG, "${VideoDetailsFragment.POSITION_ARG}=$position")

        videoView = view.findViewById<PlayerView>(R.id.video_view)
        videoView.requestFocus()
        videoView.controllerShowTimeoutMs = 2000
        videoView.hideController()

        val callback = ControlsListener(videoView)
        videoView.setControllerVisibilityListener(callback)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            callback,
        )
    }

    fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return videoView.dispatchKeyEvent(event!!)
    }

    @OptIn(UnstableApi::class)
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            val position =
                requireActivity().intent.getLongExtra(VideoDetailsFragment.POSITION_ARG, -1)
            initializePlayer(position)
        }
    }

    @OptIn(UnstableApi::class)
    override fun onResume() {
        super.onResume()
//        hideSystemUi()
        if ((Util.SDK_INT <= 23 || player == null)) {
            val position =
                requireActivity().intent.getLongExtra(VideoDetailsFragment.POSITION_ARG, -1)
            initializePlayer(position)
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

    class ControlsListener(private val view: PlayerView) : PlayerView.ControllerVisibilityListener,
        OnBackPressedCallback(
            view.isControllerFullyVisible,
        ) {
        init {
//            Log.d(TAG, "ControlsListener initial isEnabled=$isEnabled")
        }

        override fun onVisibilityChanged(visibility: Int) {
            when (visibility) {
                View.VISIBLE -> isEnabled = true
                View.GONE -> isEnabled = false
            }
//            Log.d(TAG, "ControlsListener isEnabled=$isEnabled")
        }

        @OptIn(UnstableApi::class)
        override fun handleOnBackPressed() {
//            Log.d(TAG, "ControlsListener handleOnBackPressed, isEnabled=$isEnabled")
            view.hideController()
        }
    }
}
