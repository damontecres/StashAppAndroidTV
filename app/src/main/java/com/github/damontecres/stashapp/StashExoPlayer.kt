package com.github.damontecres.stashapp

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.util.Constants

class StashExoPlayer private constructor() {
    companion object {
        private val listeners: MutableList<Player.Listener> = mutableListOf()

        @Volatile private var instance: ExoPlayer? = null // Volatile modifier is necessary

        @OptIn(UnstableApi::class)
        fun getInstance(context: Context): ExoPlayer {
            val skipForward =
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("skip_forward_time", 30)
            val skipBack =
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("skip_back_time", 10)
            // TODO invalidate
            if (instance == null) {
                synchronized(this) { // synchronized to avoid concurrency problem
                    if (instance == null) {
                        val dataSourceFactory =
                            DataSource.Factory {
                                val apiKey =
                                    PreferenceManager.getDefaultSharedPreferences(context)
                                        .getString("stashApiKey", null)
                                val dataSource = DefaultHttpDataSource.Factory().createDataSource()
                                if (!apiKey.isNullOrBlank()) {
                                    dataSource.setRequestProperty(
                                        Constants.STASH_API_HEADER,
                                        apiKey,
                                    )
                                    dataSource.setRequestProperty(
                                        Constants.STASH_API_HEADER.lowercase(),
                                        apiKey,
                                    )
                                }
                                dataSource
                            }

                        instance =
                            ExoPlayer.Builder(context)
                                .setMediaSourceFactory(
                                    DefaultMediaSourceFactory(context).setDataSourceFactory(
                                        dataSourceFactory,
                                    ),
                                )
                                .setSeekBackIncrementMs(skipBack * 1000L)
                                .setSeekForwardIncrementMs(skipForward * 1000L)
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
