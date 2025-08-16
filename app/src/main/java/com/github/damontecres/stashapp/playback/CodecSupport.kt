package com.github.damontecres.stashapp.playback

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.proto.PlaybackPreferences

/**
 * List the supported video and audio codecs
 */
data class CodecSupport(
    val videoCodecs: Set<String>,
    val audioCodecs: Set<String>,
    val containers: Set<String>,
) {
    fun isVideoSupported(videoCodec: String?): Boolean = videoCodecs.contains(videoCodec)

    fun isAudioSupported(audioCodec: String?): Boolean = audioCodec.isNullOrBlank() || audioCodecs.contains(audioCodec)

    fun isContainerFormatSupported(format: String?): Boolean = containers.contains(format)

    companion object {
        private const val TAG = "CodecSupport"

        private val videoCodecMapping =
            buildMap {
                put(MediaFormat.MIMETYPE_VIDEO_MPEG2, "mpeg2video")
                put(MediaFormat.MIMETYPE_VIDEO_MPEG4, "mpeg4")
                put(MediaFormat.MIMETYPE_VIDEO_H263, "h263")
                put(MediaFormat.MIMETYPE_VIDEO_AVC, "h264")
                put(MediaFormat.MIMETYPE_VIDEO_HEVC, "hevc")
                put(MediaFormat.MIMETYPE_VIDEO_VP8, "vp8")
                put(MediaFormat.MIMETYPE_VIDEO_VP9, "vp9")
                put("video/wvc1", "vc1")
                put("video/x-ms-wmv", "vc1")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaFormat.MIMETYPE_VIDEO_AV1, "av1")
                }
            }.toMap()

        private val audioCodecMapping =
            buildMap {
                put(MediaFormat.MIMETYPE_AUDIO_AAC, "aac")
                put(MediaFormat.MIMETYPE_AUDIO_AC3, "ac3")
                put(MediaFormat.MIMETYPE_AUDIO_EAC3, "eac3")
                put(MediaFormat.MIMETYPE_AUDIO_MPEG, "mp3")
                put(MediaFormat.MIMETYPE_AUDIO_FLAC, "flac")
                put(MediaFormat.MIMETYPE_AUDIO_OPUS, "opus")
                put(MediaFormat.MIMETYPE_AUDIO_VORBIS, "vorbis")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaFormat.MIMETYPE_AUDIO_AC4, "ac4")
                }
            }.toMap()

        fun getSupportedCodecs(prefs: PlaybackPreferences): CodecSupport {
            val videoCodecs = prefs.directPlayVideoList.toMutableSet()
            val audioCodecs = prefs.directPlayAudioList.toMutableSet()
            val containers = prefs.directPlayFormatList.toMutableSet()
            return getSupportedCodecs(videoCodecs, audioCodecs, containers)
        }

        fun getSupportedCodecs(context: Context): CodecSupport {
            val manager = PreferenceManager.getDefaultSharedPreferences(context)

            // Some codecs aren't listed in MediaCodecList, but should be direct played anyway
            // The user can override this in settings
            val videoCodecs = mutableSetOf<String>()
            videoCodecs.addAll(
                manager.getStringSet(
                    context.getString(R.string.pref_key_default_forced_direct_video),
                    setOf(*context.resources.getStringArray(R.array.default_force_video_codecs)),
                )!!,
            )
            val audioCodecs = mutableSetOf<String>()
            audioCodecs.addAll(
                manager.getStringSet(
                    context.getString(R.string.pref_key_default_forced_direct_audio),
                    setOf(*context.resources.getStringArray(R.array.default_force_audio_codecs)),
                )!!,
            )
            val containers =
                manager.getStringSet(
                    context.getString(R.string.pref_key_default_forced_direct_containers),
                    setOf(*context.resources.getStringArray(R.array.default_force_container_formats)),
                )!!
            return getSupportedCodecs(videoCodecs, audioCodecs, containers)
        }

        private fun getSupportedCodecs(
            videoCodecs: MutableSet<String>,
            audioCodecs: MutableSet<String>,
            containers: MutableSet<String>,
        ): CodecSupport {
            Log.v(
                TAG,
                "Default forced direct: video=$videoCodecs, audio=$audioCodecs, containers=$containers",
            )

            val androidCodecs = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            for (codecInfo in androidCodecs.codecInfos) {
                if (!codecInfo.isEncoder) {
//                    Log.v(TAG, "CodecInfo: name=${codecInfo.name}, supportedTypes=${codecInfo.supportedTypes.contentToString()}")
                    for (type in codecInfo.supportedTypes) {
                        val video = videoCodecMapping[type]
                        if (video != null) {
                            videoCodecs.add(video)
                        }
                        val audio = audioCodecMapping[type]
                        if (audio != null) {
                            audioCodecs.add(audio)
                        }
                    }
                }
            }
            Log.v(
                TAG,
                "Supported codecs: video=$videoCodecs, audio=$audioCodecs, containers=$containers",
            )
            return CodecSupport(videoCodecs, audioCodecs, containers)
        }
    }
}
