package com.github.damontecres.stashapp.di.services

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.TsExtractor
import com.github.damontecres.stashapp.StashExoPlayer.Companion.getInstance
import com.github.damontecres.stashapp.di.AuthHttpClient
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.di.server.StashServer
import com.github.damontecres.stashapp.proto.PlaybackBackend
import com.github.damontecres.stashapp.proto.PlaybackHttpClient
import com.github.damontecres.stashapp.proto.PlaybackPreferences
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.SkipParams
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.wholphin.mpv.MpvPlayer
import okhttp3.OkHttpClient
import org.koin.core.annotation.Single
import timber.log.Timber

@Single
class PlayerFactory(
    private val context: Application,
    @param:AuthHttpClient private val okHttpClient: OkHttpClient,
    private val serverRepository: ServerRepository,
) {
    private var currentPlayer: Player? = null
    private var currentCardPlayer: Player? = null

    /**
     * Create a new [ExoPlayer] instance. [getInstance] should be preferred where possible.
     */
    @OptIn(UnstableApi::class)
    fun createPlayer(playbackPreferences: PlaybackPreferences): Player {
        currentPlayer?.release()

        val server = serverRepository.currentServer.value.server

        Timber.i("backend=%s", playbackPreferences.playbackBackend)
        val skipParams =
            playbackPreferences.let {
                SkipParams.Values(
                    it.skipForwardMs,
                    it.skipBackwardMs,
                )
            }
        val httpClient = playbackPreferences.playbackHttpClient
        val debugLogging = playbackPreferences.debugLoggingEnabled
        val player =
            if (playbackPreferences.playbackBackend == PlaybackBackend.MPV) {
                MpvPlayer(
                    context,
                    playbackPreferences.mpvPreferences.hardwareDecoding,
                    playbackPreferences.mpvPreferences.gpuNext,
                )
            } else {
                val dataSourceFactory =
                    when (httpClient) {
                        PlaybackHttpClient.OKHTTP -> {
                            OkHttpDataSource
                                .Factory(okHttpClient)
                        }

                        else -> {
                            DefaultHttpDataSource
                                .Factory()
                                .setConnectTimeoutMs(5_000)
                                .setReadTimeoutMs(30_000)
                                .setUserAgent(StashClient.createUserAgent(context))
                                .apply {
                                    if (server.apiKey.isNotNullOrBlank()) {
                                        setDefaultRequestProperties(mapOf(Constants.STASH_API_HEADER to server.apiKey))
                                    }
                                }
                        }
                    }
                val skipForward = skipParams.skipForward
                val skipBack = skipParams.skipBack
                val trackSelector = DefaultTrackSelector(context)
                trackSelector.parameters =
                    trackSelector
                        .buildUponParameters()
                        .setAllowInvalidateSelectionsOnRendererCapabilitiesChange(true)
                        .setAudioOffloadPreferences(
                            TrackSelectionParameters.AudioOffloadPreferences
                                .Builder()
                                .setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                                .build(),
                        ).build()
                val extractorsFactory =
                    DefaultExtractorsFactory().apply {
                        setTsExtractorTimestampSearchBytes(TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES * 3)
                        setConstantBitrateSeekingEnabled(true)
                        setConstantBitrateSeekingAlwaysEnabled(true)
                    }
                ExoPlayer
                    .Builder(context)
//                .setLoadControl(
//                    DefaultLoadControl
//                        .Builder()
//                        .setBufferDurationsMs(
//                            5_000,
//                            30_000,
//                            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
//                            DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
//                        ).setTargetBufferBytes(64_000_000)
//                        .setPrioritizeTimeOverSizeThresholds(false)
//                        .build(),
//                )
                    .setMediaSourceFactory(
                        DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory),
                    ).setRenderersFactory(
                        DefaultRenderersFactory(context)
                            .setEnableDecoderFallback(true)
                            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON),
                    ).setSeekBackIncrementMs(skipBack)
                    .setSeekForwardIncrementMs(skipForward)
                    .setTrackSelector(trackSelector)
                    .build()
                    .also {
                        if (debugLogging) {
                            it.addAnalyticsListener(EventLogger())
                        }
                    }
            }

        this@PlayerFactory.currentPlayer = player
        return player
    }

    @OptIn(UnstableApi::class)
    fun createPlayerForCard(server: StashServer): ExoPlayer {
        this.currentCardPlayer?.release()
        val dataSourceFactory =
            DefaultHttpDataSource
                .Factory()
                .setConnectTimeoutMs(5_000)
                .setReadTimeoutMs(30_000)
                .setUserAgent(StashClient.createUserAgent(context))
                .apply {
                    if (server.apiKey.isNotNullOrBlank()) {
                        setDefaultRequestProperties(mapOf(Constants.STASH_API_HEADER to server.apiKey))
                    }
                }
        val trackSelector = DefaultTrackSelector(context)
        trackSelector.parameters =
            trackSelector
                .buildUponParameters()
                .setAllowInvalidateSelectionsOnRendererCapabilitiesChange(true)
                .setAudioOffloadPreferences(
                    TrackSelectionParameters.AudioOffloadPreferences
                        .Builder()
                        .setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                        .build(),
                ).build()
        val extractorsFactory =
            DefaultExtractorsFactory().apply {
                setTsExtractorTimestampSearchBytes(TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES * 3)
                setConstantBitrateSeekingEnabled(true)
                setConstantBitrateSeekingAlwaysEnabled(true)
            }
        val player =
            ExoPlayer
                .Builder(context)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory),
                ).setRenderersFactory(
                    DefaultRenderersFactory(context)
                        .setEnableDecoderFallback(true)
                        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON),
                ).setTrackSelector(trackSelector)
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = true
                }
        this.currentCardPlayer = player
        return player
    }
}
