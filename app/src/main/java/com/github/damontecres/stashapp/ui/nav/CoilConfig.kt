package com.github.damontecres.stashapp.ui.nav

import androidx.compose.runtime.Composable
import androidx.preference.PreferenceManager
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.views.models.ServerViewModel
import okhttp3.Call
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoilApi::class)
@Composable
fun CoilConfig(serverViewModel: ServerViewModel) {
    setSingletonImageLoaderFactory { ctx ->
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        val cacheLogging = prefs.getBoolean("networkCacheLogging", false)
        val diskCacheSize =
            prefs
                .getInt(ctx.getString(R.string.pref_key_image_cache_size), 100)
                .coerceAtLeast(10)
        ImageLoader
            .Builder(ctx)
            .diskCache(
                DiskCache
                    .Builder()
                    .directory(ctx.cacheDir.resolve("coil3_image_cache"))
                    .maxSizeBytes(diskCacheSize * 1024 * 1024L)
                    .build(),
            ).crossfade(true)
            .logger(if (cacheLogging) DebugLogger() else null)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        cacheStrategy = { CacheControlCacheStrategy() },
                        callFactory = {
                            Call.Factory { request ->
                                // TODO this seems hacky?
                                serverViewModel.requireServer().okHttpClient.newCall(
                                    request,
                                )
                            }
                        },
                    ),
                )
            }.build()
    }
}
