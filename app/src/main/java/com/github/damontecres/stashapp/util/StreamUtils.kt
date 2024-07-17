package com.github.damontecres.stashapp.util

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.PlaybackSceneFragment
import com.github.damontecres.stashapp.data.Scene

enum class TranscodeDecision {
    DIRECT_PLAY,
    FORCED_DIRECT_PLAY,
    TRANSCODE,
    FORCED_TRANSCODE,
}

data class StreamDecision(
    val sceneId: String,
    val transcodeDecision: TranscodeDecision,
    val videoSupported: Boolean,
    val audioSupported: Boolean,
    val containerSupported: Boolean,
)

fun buildMediaItem(
    context: Context,
    scene: Scene,
): MediaItem {
    val decision = getStreamDecision(context, scene)
    return buildMediaItem(context, decision, scene)
}

fun buildMediaItem(
    context: Context,
    streamDecision: StreamDecision,
    scene: Scene,
    builderCallback: ((MediaItem.Builder) -> Unit)? = null,
): MediaItem {
    if (streamDecision.sceneId != scene.id) {
        throw IllegalArgumentException("Scene IDs do not match: streamSupport.sceneId=${streamDecision.sceneId}, scene.id=${scene.id}")
    }
    val format =
        when (streamDecision.transcodeDecision) {
            TranscodeDecision.TRANSCODE, TranscodeDecision.FORCED_TRANSCODE -> {
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("stream_choice", "HLS")
            }
            TranscodeDecision.DIRECT_PLAY, TranscodeDecision.FORCED_DIRECT_PLAY -> {
                scene.format
            }
        }
    val url =
        when (streamDecision.transcodeDecision) {
            TranscodeDecision.TRANSCODE, TranscodeDecision.FORCED_TRANSCODE -> {
                scene.streams[format]!!
            }
            TranscodeDecision.DIRECT_PLAY, TranscodeDecision.FORCED_DIRECT_PLAY -> {
                scene.streamUrl!!
            }
        }

    val mimeType =
        when (format?.lowercase()) {
            // As recommended by https://developer.android.com/media/media3/exoplayer/hls#using-mediaitem
            // Specify the mimetype for HLS & DASH streams
            "hls" -> MimeTypes.APPLICATION_M3U8
            "dash" -> MimeTypes.APPLICATION_MPD
            "mp4" -> MimeTypes.APPLICATION_MP4
            else -> null
        }
    val builder =
        MediaItem.Builder()
            .setUri(url)
            .setMimeType(mimeType)
    if (builderCallback != null) {
        builderCallback(builder)
    }
    return builder.build()
}

fun getStreamDecision(
    context: Context,
    scene: Scene,
    forceTranscode: Boolean = false,
    forceDirectPlay: Boolean = false,
): StreamDecision {
    val supportedCodecs = CodecSupport.getSupportedCodecs(context)
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
            PlaybackSceneFragment.TAG,
            "Video (${scene.videoCodec}), audio (${scene.audioCodec}), & container (${scene.format}) supported",
        )
        return StreamDecision(
            scene.id,
            TranscodeDecision.DIRECT_PLAY,
            videoSupported,
            audioSupported,
            containerSupported,
        )
    } else if (forceDirectPlay) {
        Log.v(
            PlaybackSceneFragment.TAG,
            "Forcing direct play for video (${scene.videoCodec}), audio (${scene.audioCodec}), & container (${scene.format})",
        )
        return StreamDecision(
            scene.id,
            TranscodeDecision.FORCED_DIRECT_PLAY,
            videoSupported,
            audioSupported,
            containerSupported,
        )
    } else {
        // Transcode
        Log.v(
            PlaybackSceneFragment.TAG,
            "Transcoding video (${scene.videoCodec}), audio (${scene.audioCodec}), & container (${scene.format})",
        )
        return StreamDecision(
            scene.id,
            if (forceTranscode) TranscodeDecision.FORCED_TRANSCODE else TranscodeDecision.TRANSCODE,
            videoSupported,
            audioSupported,
            containerSupported,
        )
    }
}
