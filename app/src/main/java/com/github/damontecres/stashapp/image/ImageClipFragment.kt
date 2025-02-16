package com.github.damontecres.stashapp.image

import android.os.Bundle
import android.view.View
import androidx.annotation.OptIn
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.playback.CodecSupport
import com.github.damontecres.stashapp.playback.PlaybackFragment
import com.github.damontecres.stashapp.util.isImageClip
import com.github.damontecres.stashapp.views.models.ImageViewModel

/**
 * Playback for an image clip (a video)
 */
@OptIn(UnstableApi::class)
class ImageClipFragment :
    PlaybackFragment(),
    VideoController {
    private val imageViewModel: ImageViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override val previewsEnabled: Boolean
        get() = false

    override val optionsButtonOptions: OptionsButtonOptions
        get() = OptionsButtonOptions(DataType.IMAGE, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        videoView.useController = false
        hideControlsIfVisible()

        imageViewModel.videoController = this

        imageViewModel.image.observe(viewLifecycleOwner) { imageData ->
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
                    MediaItem
                        .Builder()
                        .setUri(imageData.paths.image)
                        .build()
                player?.setMediaItem(mediaItem)
                player?.prepare()
                player?.play()
            } else {
                player?.stop()
            }
        }
    }

    override fun Player.setupPlayer() {
        // no-op
    }

    override fun Player.postSetupPlayer() {
        repeatMode = Player.REPEAT_MODE_ONE
        prepare()
        play()
    }

    override fun play() {
        player?.play()
    }

    override fun pause() {
        player?.pause()
    }
}
