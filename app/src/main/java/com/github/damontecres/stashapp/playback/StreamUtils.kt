package com.github.damontecres.stashapp.playback

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.Scene
import kotlinx.serialization.Serializable
import java.util.Locale

private const val TAG = "StreamUtils"

@Serializable
enum class PlaybackMode {
    FORCED_DIRECT_PLAY,
    FORCED_TRANSCODE,
    CHOOSE,
}

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
    streamDecision: StreamDecision,
    scene: Scene,
    builderCallback: (MediaItem.Builder.() -> Unit)? = null,
): MediaItem {
    if (streamDecision.sceneId != scene.id) {
        throw IllegalArgumentException("Scene IDs do not match: streamSupport.sceneId=${streamDecision.sceneId}, scene.id=${scene.id}")
    }
    val format =
        when (streamDecision.transcodeDecision) {
            TranscodeDecision.TRANSCODE, TranscodeDecision.FORCED_TRANSCODE -> {
                PreferenceManager
                    .getDefaultSharedPreferences(context)
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
        MediaItem
            .Builder()
            .setUri(url)
            .setMimeType(mimeType)
            .setMediaId(scene.id)

    if (scene.hasCaptions) {
        val baseUrl = Uri.parse(scene.captionUrl)
        val subtitles =
            scene.captions.map {
                val uri =
                    baseUrl
                        .buildUpon()
                        .appendQueryParameter("lang", it.lang)
                        .appendQueryParameter("type", it.type)
                        .build()
                val languageName =
                    try {
                        if (it.lang != "00") {
                            Locale(it.lang).displayLanguage
                        } else {
                            context.getString(R.string.stashapp_display_mode_unknown)
                        }
                    } catch (ex: Exception) {
                        Log.w(TAG, "Error in locale for '${it.lang}'", ex)
                        it.lang.uppercase()
                    }
                MediaItem.SubtitleConfiguration
                    .Builder(uri)
                    // The server always provides subtitles as VTT: https://github.com/stashapp/stash/blob/v0.26.2/internal/api/routes_scene.go#L439
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLabel("$languageName (${it.type})")
                    .setSelectionFlags(C.SELECTION_FLAG_AUTOSELECT)
                    .build()
            }
        Log.v(TAG, "Got ${subtitles.size} subtitle options for scene ${scene.id}")
        builder.setSubtitleConfigurations(subtitles)
    }
    if (builderCallback != null) {
        builderCallback(builder)
    }
    return builder.build()
}

fun getStreamDecision(
    context: Context,
    scene: Scene,
    mode: PlaybackMode,
): StreamDecision {
    val supportedCodecs = CodecSupport.getSupportedCodecs(context)
    val videoSupported = supportedCodecs.isVideoSupported(scene.videoCodec)
    val audioSupported = supportedCodecs.isAudioSupported(scene.audioCodec)
    val containerSupported = supportedCodecs.isContainerFormatSupported(scene.format)
    if (
        mode == PlaybackMode.CHOOSE &&
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
            true,
            true,
            true,
        )
    } else if (mode == PlaybackMode.FORCED_DIRECT_PLAY) {
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
            if (mode == PlaybackMode.FORCED_TRANSCODE) TranscodeDecision.FORCED_TRANSCODE else TranscodeDecision.TRANSCODE,
            videoSupported,
            audioSupported,
            containerSupported,
        )
    }
}
