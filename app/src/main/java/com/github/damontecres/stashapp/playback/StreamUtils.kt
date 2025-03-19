package com.github.damontecres.stashapp.playback

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.Caption
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.getPreference
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.serialization.Serializable
import java.util.Locale

private const val TAG = "StreamUtils"

data class StreamLabel(
    val readableName: String,
    val label: String,
)

@Serializable
sealed interface PlaybackMode {
    @Serializable
    data object ForcedDirectPlay : PlaybackMode

    @Serializable
    data class ForcedTranscode(
        val streamLabel: String,
    ) : PlaybackMode

    @Serializable
    data object Choose : PlaybackMode
}

sealed interface TranscodeDecision {
    data object DirectPlay : TranscodeDecision

    data object ForcedDirectPlay : TranscodeDecision

    data object Transcode : TranscodeDecision

    data class ForcedTranscode(
        val streamLabel: String,
    ) : TranscodeDecision
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
            TranscodeDecision.Transcode, is TranscodeDecision.ForcedTranscode -> {
                PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getString("stream_choice", "HLS")
            }

            TranscodeDecision.DirectPlay, TranscodeDecision.ForcedDirectPlay -> {
                scene.format
            }
        }
    val url =
        when (val decision = streamDecision.transcodeDecision) {
            TranscodeDecision.Transcode -> {
                scene.streams[format]!!
            }

            is TranscodeDecision.ForcedTranscode -> {
                val key = decision.streamLabel
                if (key in scene.streams) {
                    scene.streams[key]!!
                } else {
                    Log.w(TAG, "Couldn't find $key for scene ${scene.id}")
                    scene.streams[format]!!
                }
            }

            TranscodeDecision.DirectPlay, TranscodeDecision.ForcedDirectPlay -> {
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
                        .appendQueryParameter("lang", it.language_code)
                        .appendQueryParameter("type", it.caption_type)
                        .build()
                MediaItem.SubtitleConfiguration
                    .Builder(uri)
                    // The server always provides subtitles as VTT: https://github.com/stashapp/stash/blob/v0.26.2/internal/api/routes_scene.go#L439
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLabel(it.displayString(context))
                    .setSelectionFlags(C.SELECTION_FLAG_AUTOSELECT)
                    .build()
            }
        Log.v(TAG, "Got ${subtitles.size} subtitle options for scene ${scene.id}")
        builder.setSubtitleConfigurations(subtitles)
    }
    builderCallback?.invoke(builder)

    return builder.build()
}

fun getStreamDecision(
    context: Context,
    scene: Scene,
    mode: PlaybackMode,
): StreamDecision {
    Log.d(
        TAG,
        "getStreamDecision: mode=$mode, " +
            "sceneId=${scene.id}, " +
            "videoCodec=${scene.videoCodec}, " +
            "resolution=${scene.videoResolution}, " +
            "audioCodec=${scene.audioCodec}, " +
            "format=${scene.format}",
    )
    val supportedCodecs = CodecSupport.getSupportedCodecs(context)
    val videoSupported = supportedCodecs.isVideoSupported(scene.videoCodec)
    val audioSupported = supportedCodecs.isAudioSupported(scene.audioCodec)
    val containerSupported = supportedCodecs.isContainerFormatSupported(scene.format)
    if (
        mode == PlaybackMode.Choose &&
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
            TranscodeDecision.DirectPlay,
            true,
            true,
            true,
        )
    } else if (mode == PlaybackMode.ForcedDirectPlay) {
        Log.v(
            PlaybackSceneFragment.TAG,
            "Forcing direct play for video (${scene.videoCodec}), audio (${scene.audioCodec}), & container (${scene.format})",
        )
        return StreamDecision(
            scene.id,
            TranscodeDecision.ForcedDirectPlay,
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
            if (mode is PlaybackMode.ForcedTranscode) TranscodeDecision.ForcedTranscode(mode.streamLabel) else TranscodeDecision.Transcode,
            videoSupported,
            audioSupported,
            containerSupported,
        )
    }
}

fun Caption.displayString(context: Context): String {
    val languageName =
        try {
            if (language_code != "00") {
                Locale(language_code).displayLanguage
            } else {
                context.getString(R.string.stashapp_display_mode_unknown)
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Error in locale for '${language_code}'", ex)
            language_code.uppercase()
        }
    return "$languageName ($caption_type)"
}

enum class TrackSupportReason {
    HANDLED,
    EXCEEDS_CAPABILITIES,
    UNSUPPORTED_DRM,
    UNSUPPORTED_SUBTYPE,
    UNSUPPORTED_TYPE,
    UNKNOWN,
    ;

    companion object {
        @OptIn(UnstableApi::class)
        fun fromInt(
            @C.FormatSupport value: Int,
        ): TrackSupportReason =
            when (value) {
                C.FORMAT_HANDLED -> HANDLED
                C.FORMAT_EXCEEDS_CAPABILITIES -> EXCEEDS_CAPABILITIES
                C.FORMAT_UNSUPPORTED_DRM -> UNSUPPORTED_DRM
                C.FORMAT_UNSUPPORTED_SUBTYPE -> UNSUPPORTED_SUBTYPE
                C.FORMAT_UNSUPPORTED_TYPE -> UNSUPPORTED_TYPE
                else -> UNKNOWN
            }
    }
}

enum class TrackType {
    UNKNOWN,
    DEFAULT,
    AUDIO,
    VIDEO,
    TEXT,
    IMAGE,
    METADATA,
    CAMERA_MOTION,
    NONE,
    ;

    companion object {
        @OptIn(UnstableApi::class)
        fun fromInt(value: Int): TrackType =
            when (value) {
                C.TRACK_TYPE_UNKNOWN -> UNKNOWN
                C.TRACK_TYPE_DEFAULT -> DEFAULT
                C.TRACK_TYPE_AUDIO -> AUDIO
                C.TRACK_TYPE_VIDEO -> VIDEO
                C.TRACK_TYPE_TEXT -> TEXT
                C.TRACK_TYPE_IMAGE -> IMAGE
                C.TRACK_TYPE_METADATA -> METADATA
                C.TRACK_TYPE_CAMERA_MOTION -> CAMERA_MOTION
                C.TRACK_TYPE_NONE -> NONE
                else -> UNKNOWN
            }
    }
}

data class TrackSupport(
    val id: String?,
    val type: TrackType,
    val supported: TrackSupportReason,
    val selected: Boolean,
    val labels: List<String>,
    val codecs: String?,
    val format: Format,
)

@OptIn(UnstableApi::class)
fun checkForSupport(tracks: Tracks): List<TrackSupport> =
    tracks.groups.flatMap {
        buildList {
            val type = TrackType.fromInt(it.type)
            for (i in 0..<it.length) {
                val format = it.getTrackFormat(i)
                val labels =
                    format.labels
                        .map {
                            if (it.language != null) {
                                "${it.value} (${it.language})"
                            } else {
                                it.value
                            }
                        }
                val reason = TrackSupportReason.fromInt(it.getTrackSupport(i))
                add(
                    TrackSupport(
                        format.id,
                        type,
                        reason,
                        it.isSelected,
                        labels,
                        format.codecs,
                        format,
                    ),
                )
            }
        }
    }

/**
 * If [R.string.pref_key_playback_start_muted] is true, disable gselection of audio tracks by default on the given [Player]
 */
fun maybeMuteAudio(
    context: Context,
    player: Player?,
) {
    if (getPreference(context, R.string.pref_key_playback_start_muted, false)) {
        player?.let {
            if (C.TRACK_TYPE_AUDIO !in it.trackSelectionParameters.disabledTrackTypes) {
                it.trackSelectionParameters =
                    it.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                        .build()
            }
        }
    }
}

fun findPossibleTranscodeLabels(
    context: Context,
    streams: List<FullSceneData.SceneStream>?,
): List<StreamLabel> {
    if (streams == null) {
        return listOf()
    }
    val format =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString("stream_choice", "HLS")!!
    return streams
        .filter { it.label.isNotNullOrBlank() && it.label.startsWith(format) }
        .mapNotNull {
            if (it.label.isNotNullOrBlank()) {
                StreamLabel(
                    if (it.label == format) "${it.label} (Original)" else it.label,
                    it.label,
                )
            } else {
                null
            }
        }
}
