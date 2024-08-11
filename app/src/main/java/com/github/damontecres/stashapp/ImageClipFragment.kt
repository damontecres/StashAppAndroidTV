package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.playback.CodecSupport
import com.github.damontecres.stashapp.playback.PlaybackFragment

@OptIn(UnstableApi::class)
class ImageClipFragment(private val imageData: ImageData) : PlaybackFragment(), ImageActivity.StashImageFragment {
    override val previewsEnabled: Boolean
        get() = false

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val videoFile = imageData.visual_files.firstOrNull()?.onVideoFile

        val supportedCodecs = CodecSupport.getSupportedCodecs(requireContext())
        val videoCodec = videoFile?.video_codec
        val audioCodec = videoFile?.audio_codec
        val videoSupported = supportedCodecs.isVideoSupported(videoCodec)
        val audioSupported = supportedCodecs.isAudioSupported(audioCodec)
        val formatSupported = supportedCodecs.isContainerFormatSupported(videoFile?.format)

        val unsupportedStr = getString(R.string.unsupported)
        debugPlaybackTextView.text = getString(R.string.force_direct)
        debugVideoTextView.text =
            if (videoSupported) videoCodec else "$videoCodec ($unsupportedStr)"
        debugAudioTextView.text =
            if (audioSupported) audioCodec else "$audioCodec ($unsupportedStr)"
        debugContainerTextView.text =
            if (formatSupported) videoFile?.format else "${videoFile?.format} ($unsupportedStr)"

        titleText.text = imageData.title
        dateText.text = imageData.date

//        val previousButton = exoCenterControls.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_prev)
//        val nextButton = exoCenterControls.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_next)
        moreOptionsButton.visibility = View.GONE

        viewCreated = true
    }

    override fun initializePlayer(): ExoPlayer {
        val skipForward =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("skip_forward_time", 30)
        val skipBack =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("skip_back_time", 10)
        // TODO maybe refactor so the player is tied to the context's (Activity) lifecycle, not the fragment's?
        return StashExoPlayer.createInstance(requireContext(), skipForward * 1000L, skipBack * 1000L)
            .also { exoPlayer ->
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                val mediaItem =
                    MediaItem.Builder()
                        .setUri(imageData.paths.image)
                        .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            }
    }

    override var viewCreated: Boolean = false

    override fun isOverlayVisible(): Boolean = isControllerVisible

    override fun hideOverlay() {
        hideControlsIfVisible()
    }

    override fun isImageZoomedIn(): Boolean = false

    override fun resetImageZoom() {
        // no-op
    }
}
