package com.github.damontecres.stashapp

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.util.StashClient
import okhttp3.CacheControl

class StashExoPlayer private constructor() {
    companion object {
        private val listeners: MutableList<Player.Listener> = mutableListOf()

        @Volatile
        private var instance: ExoPlayer? = null // Volatile modifier is necessary

        @OptIn(UnstableApi::class)
        fun getInstance(context: Context): ExoPlayer {
            val skipForward =
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("skip_forward_time", 30)
            val skipBack =
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("skip_back_time", 10)
            return getInstance(context, skipForward * 1000L, skipBack * 1000L)
        }

        @OptIn(UnstableApi::class)
        fun getInstance(
            context: Context,
            skipForward: Long,
            skipBack: Long,
        ): ExoPlayer {
            if (instance == null) {
                synchronized(this) { // synchronized to avoid concurrency problem
                    if (instance == null) {
                        val dataSourceFactory =
                            OkHttpDataSource.Factory(StashClient.getStreamHttpClient(context))
                                .setCacheControl(CacheControl.FORCE_NETWORK)

                        instance =
                            ExoPlayer.Builder(context)
                                .setMediaSourceFactory(
                                    DefaultMediaSourceFactory(context).setDataSourceFactory(
                                        dataSourceFactory,
                                    ),
                                )
                                .setSeekBackIncrementMs(skipBack)
                                .setSeekForwardIncrementMs(skipForward)
                                .build()
                    }
                }
            }
            return instance!!
        }

        fun releasePlayer() {
            if (instance != null) {
                synchronized(this) { // synchronized to avoid concurrency problem
                    if (instance != null) {
                        instance!!.release()
                        instance = null
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
