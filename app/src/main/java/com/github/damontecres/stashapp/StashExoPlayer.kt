package com.github.damontecres.stashapp

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.util.StashServer
import okhttp3.CacheControl

/**
 * Manages a static [ExoPlayer] which might be reused between views
 */
class StashExoPlayer private constructor() {
    companion object {
        private data class SkipParams(
            val skipForward: Long,
            val skipBack: Long,
        )

        private val listeners: MutableList<Player.Listener> = mutableListOf()

        @Volatile
        private var instance: ExoPlayer? = null // Volatile modifier is necessary

        @Volatile
        private var skipParams: SkipParams? = null

        @OptIn(UnstableApi::class)
        fun getInstance(
            context: Context,
            server: StashServer,
        ): ExoPlayer {
            val skipForward =
                PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getInt("skip_forward_time", 30)
            val skipBack =
                PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .getInt("skip_back_time", 10)
            return getInstance(context, server, skipForward * 1000L, skipBack * 1000L)
        }

        @OptIn(UnstableApi::class)
        fun getInstance(
            context: Context,
            server: StashServer,
            skipForward: Long,
            skipBack: Long,
        ): ExoPlayer {
            val newSkipParams = SkipParams(skipForward, skipBack)
            if (instance == null || skipParams != newSkipParams) {
                synchronized(this) {
                    // synchronized to avoid concurrency problem
                    if (instance == null || skipParams != newSkipParams) {
                        skipParams = newSkipParams
                        instance = createInstance(context, server, newSkipParams)
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
            skipParams: SkipParams,
        ): ExoPlayer {
            releasePlayer()
            val dataSourceFactory =
                OkHttpDataSource
                    .Factory(server.streamingOkHttpClient)
                    .setCacheControl(CacheControl.FORCE_NETWORK)
            return ExoPlayer
                .Builder(context)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(context).setDataSourceFactory(
                        dataSourceFactory,
                    ),
                ).setSeekBackIncrementMs(skipParams.skipBack)
                .setSeekForwardIncrementMs(skipParams.skipForward)
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
            listeners.add(listener)
            instance?.addListener(listener)
        }

        fun removeListeners() {
            listeners.forEach {
                instance?.removeListener(it)
            }
            listeners.clear()
        }
    }
}
