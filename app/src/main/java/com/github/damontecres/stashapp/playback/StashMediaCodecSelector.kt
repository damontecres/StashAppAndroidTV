package com.github.damontecres.stashapp.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

@UnstableApi
class StashMediaCodecSelector(
    private val softwareMimeTypes: Set<String>,
) : MediaCodecSelector {
    private val software by lazy { MediaCodecSelector.PREFER_SOFTWARE }

    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean,
    ): MutableList<MediaCodecInfo> =
        if (mimeType in softwareMimeTypes) {
            software.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
        } else {
            MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
        }
}
