package com.github.damontecres.stashapp

import android.content.Context
import android.util.Log
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
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.StashExoPlayer.Companion.getInstance
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.SkipParams
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getPreference
import com.github.damontecres.stashapp.util.isNotNullOrBlank

/**
 * Manages a static [ExoPlayer] which might be reused between views
 */
class StashExoPlayer private constructor() {
    companion object {
        private const val TAG = "StashExoPlayer"

        private val listeners: MutableList<Player.Listener> = mutableListOf()

        @Volatile
        private var instance: ExoPlayer? = null // Volatile modifier is necessary

        @Volatile
        private var skipParams: SkipParams? = null

        @OptIn(UnstableApi::class)
        fun getInstance(
            context: Context,
            server: StashServer,
        ): ExoPlayer = getInstance(context, server, SkipParams.Default)

        @OptIn(UnstableApi::class)
        fun getInstance(
            context: Context,
            server: StashServer,
            skipParams: SkipParams,
            httpClientChoice: String =
                getPreference(
                    context,
                    R.string.pref_key_playback_http_client,
                    context.getString(R.string.playback_http_client_okhttp),
                )!!,
            debugLogging: Boolean =
                getPreference(
                    context,
                    R.string.pref_key_playback_debug_logging,
                    false,
                ),
        ): ExoPlayer {
            if (instance == null || skipParams != this.skipParams) {
                synchronized(this) {
                    // synchronized to avoid concurrency problem
                    if (instance == null || skipParams != this.skipParams) {
                        this.skipParams = skipParams
                        instance =
                            createInstance(
                                context,
                                server,
                                skipParams,
                                httpClientChoice,
                                debugLogging,
                            )
                    }
                }
            }
            return instance!!
        }

        /**
         * Create a new [ExoPlayer] instance. [getInstance] should be preferred where possible.
         */
        @OptIn(UnstableApi::class)
        fun createInstance(
            context: Context,
            server: StashServer,
            skipParams: SkipParams,
            httpClientChoice: String,
            debugLogging: Boolean,
        ): ExoPlayer {
            releasePlayer()
            val dataSourceFactory =
                when (httpClientChoice.lowercase()) {
                    context.getString(R.string.playback_http_client_okhttp) -> {
                        OkHttpDataSource
                            .Factory(server.streamingOkHttpClient)
                    }

                    context.getString(R.string.playback_http_client_default) -> {
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

                    else -> throw IllegalArgumentException("Unknown HTTP client: $httpClientChoice")
                }
            Log.d(TAG, "createInstance")
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val skipForward =
                when (skipParams) {
                    is SkipParams.Default ->
                        preferences.getInt(
                            context.getString(R.string.pref_key_skip_forward_time),
                            30,
                        ) * 1000L
                    is SkipParams.Values -> skipParams.skipForward
                }
            val skipBack =
                when (skipParams) {
                    is SkipParams.Default ->
                        preferences.getInt(
                            context.getString(R.string.pref_key_skip_back_time),
                            10,
                        ) * 1000L
                    is SkipParams.Values -> skipParams.skipBack
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
            return ExoPlayer
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

        @OptIn(UnstableApi::class)
        fun releasePlayer() {
            if (instance != null) {
                synchronized(this) {
                    // synchronized to avoid concurrency problem
                    if (instance != null) {
                        Log.d(TAG, "releasePlayer")
                        removeListeners()
                        instance!!.stop()
                        if (!instance!!.isReleased) {
                            instance!!.release()
                        }
                        instance = null
                        skipParams = null
                    }
                }
            }
        }

        fun addListener(listener: Player.Listener) {
            if (instance == null) {
                Log.w(TAG, "Cannot add listener to null instance: $listener")
            } else if (listeners.contains(listener)) {
                Log.w(TAG, "Listener already added: $listener")
            } else {
                Log.v(TAG, "Added listener: $listener")
                listeners.add(listener)
                instance?.addListener(listener)
            }
        }

        fun removeListeners() {
            Log.v(TAG, "Removing ${listeners.size} listeners")
            listeners.forEach {
                instance?.removeListener(it)
            }
            listeners.clear()
        }

        fun removeListener(listener: Player.Listener) {
            if (listeners.remove(listener)) {
                instance?.removeListener(listener)
                Log.v(TAG, "Removed listener: $listener")
            } else {
                Log.w(TAG, "Listener was not added previously: $listener")
            }
        }
    }
}
