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
import com.github.damontecres.stashapp.proto.PlaybackPreferences
import com.github.damontecres.stashapp.proto.Resolution
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.proto.StreamChoice
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
    val streamChoice: StreamChoice,
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
                streamDecision.streamChoice.label
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
                    .setLanguage(it.language_code)
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
    streamChoice: StreamChoice,
    alwaysTranscodeAbove: Resolution,
    supportedCodecs: CodecSupport = CodecSupport.getSupportedCodecs(context),
): StreamDecision {
    Log.d(
        TAG,
        "getStreamDecision: mode=$mode, " +
            "sceneId=${scene.id}, " +
            "videoCodec=${scene.videoCodec}, " +
            "videoHeight=${scene.videoHeight}, " +
            "audioCodec=${scene.audioCodec}, " +
            "format=${scene.format}, " +
            "alwaysTranscodeAbove=$alwaysTranscodeAbove",
    )
    val videoSupported = supportedCodecs.isVideoSupported(scene.videoCodec)
    val audioSupported = supportedCodecs.isAudioSupported(scene.audioCodec)
    val containerSupported = supportedCodecs.isContainerFormatSupported(scene.format)

    val alwaysTranscode = checkIfAlwaysTranscode(scene, streamChoice, alwaysTranscodeAbove)
    Log.d(TAG, "alwaysTranscode=$alwaysTranscode")
    if (mode != PlaybackMode.ForcedDirectPlay &&
        mode !is PlaybackMode.ForcedTranscode &&
        alwaysTranscode != null
    ) {
        return StreamDecision(
            scene.id,
            TranscodeDecision.ForcedTranscode(alwaysTranscode),
            streamChoice,
            videoSupported,
            audioSupported,
            containerSupported,
        )
    }

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
            streamChoice,
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
            streamChoice,
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
            streamChoice,
            videoSupported,
            audioSupported,
            containerSupported,
        )
    }
}

fun checkIfAlwaysTranscode(
    scene: Scene,
    streamChoice: StreamChoice,
    alwaysTarget: Resolution,
): String? {
    val format = streamChoice.label
    return if (alwaysTarget != Resolution.RESOLUTION_UNSPECIFIED && alwaysTarget != Resolution.UNRECOGNIZED) {
        scene.streams.keys.firstOrNull { it.startsWith(format) && it.contains(alwaysTarget.label) }
    } else {
        null
    }
}

fun Caption.displayString(context: Context): String {
    val languageName = languageName(context, language_code)
    return "$languageName ($caption_type)"
}

fun languageName(
    context: Context,
    code: String?,
) = if (code != null && code != "00") {
    try {
        Locale(code).displayLanguage
    } catch (ex: Exception) {
        Log.w(TAG, "Error in locale for '$code'", ex)
        code.uppercase()
    }
} else {
    context.getString(R.string.stashapp_display_mode_unknown)
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
) {
    @OptIn(UnstableApi::class)
    fun displayString(context: Context): String =
        if (labels.isNotEmpty()) {
            labels.joinToString(", ")
        } else {
            val type =
                when (codecs) {
                    MimeTypes.TEXT_VTT -> "vtt"
                    MimeTypes.APPLICATION_VOBSUB -> "vobsub"
                    MimeTypes.APPLICATION_SUBRIP -> "srt"
                    MimeTypes.TEXT_SSA -> "ssa"
                    MimeTypes.APPLICATION_PGS -> "pgs"
                    MimeTypes.APPLICATION_DVBSUBS -> "dvd"
                    MimeTypes.APPLICATION_TTML -> "ttml"
                    MimeTypes.TEXT_UNKNOWN -> "unknown"
                    null -> "unknown"
                    else -> {
                        val split = codecs.split("/")
                        if (split.size > 1) split[1] else codecs
                    }
                }
            val language = languageName(context, format.language)
            "$language ($type)"
        }
}

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

fun maybeMuteAudio(
    preferences: StashPreferences,
    checkPreviewAudioPref: Boolean,
    player: Player?,
) {
    maybeMuteAudio(
        preferences.interfacePreferences.videoPreviewAudio,
        preferences.playbackPreferences.startWithNoAudio,
        checkPreviewAudioPref,
        player,
    )
}

/**
 * If [R.string.pref_key_playback_start_muted] is true, disable selection of audio tracks by default on the given [Player]
 */
fun maybeMuteAudio(
    context: Context,
    checkPreviewAudioPref: Boolean,
    player: Player?,
) {
    val playAudioPreview =
        getPreference(
            context,
            R.string.pref_key_video_preview_audio,
            false,
        )
    val startMuted = getPreference(context, R.string.pref_key_playback_start_muted, false)
    maybeMuteAudio(playAudioPreview, startMuted, checkPreviewAudioPref, player)
}

fun maybeMuteAudio(
    playAudioPreview: Boolean,
    startMuted: Boolean,
    checkPreviewAudioPref: Boolean,
    player: Player?,
) {
    player?.let {
        Log.v(
            TAG,
            "maybeMuteAudio: playAudioPreview=$playAudioPreview, startMuted=$startMuted, checkPreviewAudioPref=$checkPreviewAudioPref",
        )
        if (!playAudioPreview && checkPreviewAudioPref || startMuted) {
            if (C.TRACK_TYPE_AUDIO !in it.trackSelectionParameters.disabledTrackTypes) {
                Log.v(TAG, "Disabling audio")
                it.trackSelectionParameters =
                    it.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                        .build()
            }
        } else if (C.TRACK_TYPE_AUDIO in it.trackSelectionParameters.disabledTrackTypes) {
            Log.v(TAG, "Enabling audio")
            it.trackSelectionParameters =
                it.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .build()
        }
    }
}

fun findPossibleTranscodeLabels(
    context: Context,
    streamChoice: StreamChoice,
    streams: List<FullSceneData.SceneStream>?,
): List<StreamLabel> {
    if (streams == null) {
        return listOf()
    }
    return streams
        .filter { it.label.isNotNullOrBlank() && it.label.startsWith(streamChoice.label) }
        .mapNotNull {
            if (it.label.isNotNullOrBlank()) {
                StreamLabel(
                    if (it.label == streamChoice.label) "${it.label} (Original)" else it.label,
                    it.label,
                )
            } else {
                null
            }
        }
}

fun switchToTranscode(
    context: Context,
    current: MediaItem,
    prefs: PlaybackPreferences,
): MediaItem {
    val currScene = (current.localConfiguration!!.tag as PlaylistFragment.MediaItemTag).item
    val transcodeDecision =
        getStreamDecision(
            context,
            currScene,
            PlaybackMode.ForcedTranscode(prefs.streamChoice.label),
            prefs.streamChoice,
            Resolution.RESOLUTION_UNSPECIFIED,
            CodecSupport.getSupportedCodecs(prefs),
        )
    return buildMediaItem(context, transcodeDecision, currScene) {
        setTag(PlaylistFragment.MediaItemTag(currScene, transcodeDecision))
    }
}

val StreamChoice.label: String
    get() =
        when (this) {
            StreamChoice.STREAM_CHOICE_HLS -> "HLS"
            StreamChoice.STREAM_CHOICE_DASH -> "DASH"
            StreamChoice.STREAM_CHOICE_MP4 -> "MP4"
            StreamChoice.STREAM_CHOICE_WEBM -> "WEBM"
            StreamChoice.UNRECOGNIZED -> "Disabled"
        }

fun streamChoiceFromLabel(label: String): StreamChoice =
    when (label.uppercase(Locale.getDefault())) {
        "HLS" -> StreamChoice.STREAM_CHOICE_HLS
        "DASH" -> StreamChoice.STREAM_CHOICE_DASH
        "MP4" -> StreamChoice.STREAM_CHOICE_MP4
        "WEBM" -> StreamChoice.STREAM_CHOICE_WEBM
        else -> StreamChoice.UNRECOGNIZED
    }

val Resolution.label: String
    get() =
        when (this) {
            Resolution.RESOLUTION_2160P -> "2160p"
            Resolution.RESOLUTION_1080P -> "1080p"
            Resolution.RESOLUTION_720P -> "720p"
            Resolution.RESOLUTION_480P -> "480p"
            Resolution.RESOLUTION_240P -> "240p"
            Resolution.UNRECOGNIZED -> "Unrecognized"
            Resolution.RESOLUTION_UNSPECIFIED -> "Unspecified"
        }

fun resolutionFromLabel(label: String): Resolution =
    when (label.uppercase(Locale.getDefault())) {
        "2160P" -> Resolution.RESOLUTION_2160P
        "1080P" -> Resolution.RESOLUTION_1080P
        "720P" -> Resolution.RESOLUTION_720P
        "480P" -> Resolution.RESOLUTION_480P
        "240P" -> Resolution.RESOLUTION_240P
        else -> Resolution.RESOLUTION_UNSPECIFIED
    }

fun getStreamChoiceFromPreferences(context: Context): StreamChoice {
    val streamChoice =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString("stream_choice", "HLS") ?: "HLS"
    return streamChoiceFromLabel(streamChoice)
}

fun getTranscodeAboveFromPreferences(context: Context): Resolution {
    val resolution =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString(
                context.getString(R.string.pref_key_playback_always_transcode),
                Resolution.RESOLUTION_UNSPECIFIED.label,
            )
    return resolutionFromLabel(resolution ?: Resolution.RESOLUTION_UNSPECIFIED.label)
}
