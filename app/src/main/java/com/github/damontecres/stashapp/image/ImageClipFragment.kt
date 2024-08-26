package com.github.damontecres.stashapp.image

import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.playback.CodecSupport
import com.github.damontecres.stashapp.playback.PlaybackFragment
import com.github.damontecres.stashapp.util.isImageClip

@OptIn(UnstableApi::class)
class ImageClipFragment() : PlaybackFragment() {
    private val viewModel: ImageViewModel by activityViewModels<ImageViewModel>()

    override val previewsEnabled: Boolean
        get() = false

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        moreOptionsButton.visibility = View.GONE

        viewModel.image.observe(viewLifecycleOwner) { imageData ->
            if (imageData.isImageClip) {
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

                val mediaItem =
                    MediaItem.Builder()
                        .setUri(imageData.paths.image)
                        .build()
                player?.setMediaItem(mediaItem)
                player?.play()
            }
        }
    }

    override fun initializePlayer(): ExoPlayer {
        val skipForward =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("skip_forward_time", 30)
        val skipBack =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("skip_back_time", 10)
        // TODO maybe refactor so the player is tied to the context's (Activity) lifecycle, not the fragment's?
        return StashExoPlayer.createInstance(
            requireContext(),
            skipForward * 1000L,
            skipBack * 1000L,
        ).also { exoPlayer ->
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }
}
