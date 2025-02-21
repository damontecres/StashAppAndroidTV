package com.github.damontecres.stashapp.util

import android.content.Context
import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.caverock.androidsvg.SVG
import com.github.damontecres.stashapp.util.svg.SvgDecoder
import com.github.damontecres.stashapp.util.svg.SvgDrawableTranscoder
import java.io.InputStream

@GlideModule
class StashGlideModule : AppGlideModule() {
    override fun isManifestParsingEnabled(): Boolean = false

    override fun registerComponents(
        context: Context,
        glide: Glide,
        registry: Registry,
    ) {
        val server = StashServer.getCurrentStashServer()
        if (server != null) {
            registry.replace(
                GlideUrl::class.java,
                InputStream::class.java,
                OkHttpUrlLoader.Factory(StashClient.getGlideHttpClient(server)),
            )
        }
        registry
            .register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder())
            .append(InputStream::class.java, SVG::class.java, SvgDecoder())
    }

    override fun applyOptions(
        context: Context,
        builder: GlideBuilder,
    ) {
        val diskCacheSizeBytes = 1024 * 1024 * 100 // 100 MB
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, diskCacheSizeBytes.toLong()))
        builder.setMemoryCache(LruResourceCache(1024 * 1024 * 25))
    }
}
