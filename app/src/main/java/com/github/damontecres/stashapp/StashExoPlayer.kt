package com.github.damontecres.stashapp

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.util.EventLogger
import androidx.preference.PreferenceManager
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
        ): ExoPlayer {
            val skipForward =
                when (skipParams) {
                    is SkipParams.Default ->
                        PreferenceManager
                            .getDefaultSharedPreferences(context)
                            .getInt("skip_forward_time", 30) * 1000L

                    is SkipParams.Values -> skipParams.skipForward
                }
            val skipBack =
                when (skipParams) {
                    is SkipParams.Default ->
                        PreferenceManager
                            .getDefaultSharedPreferences(context)
                            .getInt("skip_back_time", 10) * 1000L

                    is SkipParams.Values -> skipParams.skipBack
                }
            if (instance == null || skipParams != this.skipParams) {
                synchronized(this) {
                    // synchronized to avoid concurrency problem
                    if (instance == null || skipParams != this.skipParams) {
                        this.skipParams = skipParams
                        instance = createInstance(context, server, skipForward, skipBack)
                        if (getPreference(
                                context,
                                R.string.pref_key_playback_debug_logging,
                                false,
                            )
                        ) {
                            instance?.addAnalyticsListener(EventLogger())
                        }
                    }
                }
            }
            return instance!!
        }

        /**
         * Create a new [ExoPlayer] instance. [getInstance] should be preferred where possible.
         */
        @OptIn(UnstableApi::class)
        private fun createInstance(
            context: Context,
            server: StashServer,
            skipForward: Long,
            skipBack: Long,
        ): ExoPlayer {
            releasePlayer()
            val dataSourceFactory =
                when (
                    val httpClientChoice =
                        getPreference(
                            context,
                            R.string.pref_key_playback_http_client,
                            context.getString(R.string.playback_http_client_okhttp),
                        )
                ) {
                    context.getString(R.string.playback_http_client_okhttp) -> {
                        OkHttpDataSource
                            .Factory(server.okHttpClient)
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
//                        ).setPrioritizeTimeOverSizeThresholds(false)
//                        .build(),
//                )
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(context).setDataSourceFactory(
                        dataSourceFactory,
                    ),
                ).setRenderersFactory(
                    DefaultRenderersFactory(context)
                        .setEnableDecoderFallback(true)
                        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON),
                ).setSeekBackIncrementMs(skipBack)
                .setSeekForwardIncrementMs(skipForward)
                .build()
        }

        @OptIn(UnstableApi::class)
        fun releasePlayer() {
            if (instance != null) {
                synchronized(this) {
                    // synchronized to avoid concurrency problem
                    if (instance != null) {
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
            } else {
                Log.w(TAG, "Listener was not added previously: $listener")
            }
        }
    }
}
