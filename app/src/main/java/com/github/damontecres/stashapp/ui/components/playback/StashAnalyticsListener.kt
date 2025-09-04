package com.github.damontecres.stashapp.ui.components.playback

import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener

@UnstableApi
class StashAnalyticsListener(
    val onUpdate: (audioDecoder: String?, videoDecoder: String?) -> Unit,
) : AnalyticsListener {
    private var audioDecoder: String? = null
        set(value) {
            field = value
            update()
        }
    private var videoDecoder: String? = null
        set(value) {
            field = value
            update()
        }

    private fun update() {
        onUpdate(audioDecoder, videoDecoder)
    }

    override fun onVideoDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        videoDecoder = decoderName
    }

    override fun onVideoDisabled(
        eventTime: AnalyticsListener.EventTime,
        decoderCounters: DecoderCounters,
    ) {
        videoDecoder = null
    }

    override fun onVideoInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        decoderReuseEvaluation?.let {
            if (it.result != DecoderReuseEvaluation.REUSE_RESULT_NO) {
                videoDecoder = it.decoderName
            }
        }
    }

    override fun onAudioDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        audioDecoder = decoderName
    }

    override fun onAudioInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        decoderReuseEvaluation?.let {
            if (it.result != DecoderReuseEvaluation.REUSE_RESULT_NO) {
                audioDecoder = it.decoderName
            }
        }
    }

    override fun onAudioDisabled(
        eventTime: AnalyticsListener.EventTime,
        decoderCounters: DecoderCounters,
    ) {
        audioDecoder = null
    }
}
