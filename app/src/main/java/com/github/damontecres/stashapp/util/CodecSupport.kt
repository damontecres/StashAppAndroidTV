package com.github.damontecres.stashapp.util

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R

data class CodecSupport(val videoCodecs: Set<String>, val audioCodecs: Set<String>) {
    fun isVideoSupported(videoCodec: String?): Boolean {
        return videoCodecs.contains(videoCodec)
    }

    fun isAudioSupported(audioCodec: String?): Boolean {
        return audioCodecs.contains(audioCodec)
    }

    companion object {
        private const val TAG = "CodecSupport"

        private var codecSupport: CodecSupport? = null

        private val videoCodecMapping =
            buildMap<String, String> {
                put(MediaFormat.MIMETYPE_VIDEO_MPEG2, "mpeg2video")
                put(MediaFormat.MIMETYPE_VIDEO_MPEG4, "mpeg4")
                put(MediaFormat.MIMETYPE_VIDEO_H263, "h263")
                put(MediaFormat.MIMETYPE_VIDEO_AVC, "h264")
                put(MediaFormat.MIMETYPE_VIDEO_HEVC, "hevc")
                put(MediaFormat.MIMETYPE_VIDEO_VP8, "vp8")
                put(MediaFormat.MIMETYPE_VIDEO_VP9, "vp9")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaFormat.MIMETYPE_VIDEO_AV1, "av1")
                }
            }.toMap()

        private val audioCodecMapping =
            buildMap<String, String> {
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

        private val defaultForcedDirectVideo = setOf("h264")
        private val defaultForcedDirectAudio = setOf("aac", "ac3", "dts", "truehd")

        fun getSupportedCodecs(context: Context): CodecSupport {
            if (codecSupport != null) {
                return codecSupport!!
            }
            val manager = PreferenceManager.getDefaultSharedPreferences(context)

            val videoCodecs =
                manager.getStringSet(
                    context.getString(R.string.pref_key_default_forced_direct_video),
                    defaultForcedDirectVideo,
                )!!.toMutableSet()
            val audioCodecs =
                manager.getStringSet(
                    context.getString(R.string.pref_key_default_forced_direct_audio),
                    defaultForcedDirectAudio,
                )!!.toMutableSet()

            Log.v(TAG, "Default forced direct codecs: video=$videoCodecs, audio=$audioCodecs")

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
            Log.v(TAG, "Supported codecs: video=$videoCodecs, audio=$audioCodecs")
            codecSupport = CodecSupport(videoCodecs, audioCodecs)
            return codecSupport!!
        }
    }
}
