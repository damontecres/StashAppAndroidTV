package com.github.damontecres.stashapp.image

import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.playback.StashPlayerView
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.util.keepScreenOn
import com.github.damontecres.stashapp.views.models.ImageViewModel
import kotlin.properties.Delegates

/**
 * Playback for an image clip (a video)
 */
@OptIn(UnstableApi::class)
class ImageClipFragment :
    Fragment(R.layout.image_clip_playback),
    VideoController,
    Player.Listener {
    private val imageViewModel: ImageViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private lateinit var videoView: StashPlayerView
    private var player: Player? = null

    val isPlaying: Boolean get() = player?.isPlaying == true

    private var delay by Delegates.notNull<Long>()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        delay =
            PreferenceManager
                .getDefaultSharedPreferences(StashApplication.getApplication())
                .getInt(
                    requireContext().getString(R.string.pref_key_slideshow_duration_image_clip),
                    resources.getInteger(R.integer.pref_key_slideshow_duration_default_image_clip),
                ).toLong()

        videoView = view.findViewById(R.id.video_view)
        videoView.useController = false

        imageViewModel.videoController = this

        imageViewModel.image.observe(viewLifecycleOwner) { imageData ->
            if (imageData.isImageClip) {
                val mediaItem =
                    MediaItem
                        .Builder()
                        .setUri(imageData.paths.image)
                        .build()
                player?.setMediaItem(mediaItem)
                player?.prepare()
                player?.play()

                if (imageViewModel.slideshow.value!!) {
                    player?.repeatMode = Player.REPEAT_MODE_OFF
                } else {
                    player?.repeatMode = Player.REPEAT_MODE_ONE
                }
                imageViewModel.pulseSlideshow(Long.MAX_VALUE)
            } else {
                player?.stop()
            }
        }

        imageViewModel.slideshow.observe(viewLifecycleOwner) { slideshow ->
            player?.repeatMode =
                if (slideshow) {
                    Player.REPEAT_MODE_OFF
                } else {
                    Player.REPEAT_MODE_ONE
                }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onStart() {
        // Always release the player and recreate
        StashExoPlayer.releasePlayer()
        player =
            StashExoPlayer
                .getInstance(
                    requireContext(),
                    StashServer.requireCurrentServer(),
                ).also {
                    videoView.player = it
                    it.repeatMode =
                        if (imageViewModel.slideshow.value == true) {
                            Player.REPEAT_MODE_OFF
                        } else {
                            Player.REPEAT_MODE_ONE
                        }
                }
        StashExoPlayer.addListener(this)
        super.onStart()
    }

    override fun play() {
        player?.play()
    }

    override fun pause() {
        player?.pause()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            imageViewModel.pulseSlideshow(delay)
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (imageViewModel.slideshow.value == false) {
            // Only adjust if not in slideshow mode
            keepScreenOn(isPlaying)
        }
    }

    override fun onStop() {
        super.onStop()
        StashExoPlayer.releasePlayer()
        videoView.player = null
        player = null
    }

    companion object {
        private const val TAG = "ImageClipFragment"
    }
}
