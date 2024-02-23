package com.github.damontecres.stashapp.util

import android.content.Context
import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.caverock.androidsvg.SVG
import com.github.damontecres.stashapp.util.svg.SvgDecoder
import com.github.damontecres.stashapp.util.svg.SvgDrawableTranscoder
import java.io.InputStream

@GlideModule
class StashGlideModule : AppGlideModule() {
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    override fun registerComponents(
        context: Context,
        glide: Glide,
        registry: Registry,
    ) {
        registry.replace<GlideUrl, InputStream>(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(createOkHttpClient(context)),
        )
        registry
            .register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder())
            .append(InputStream::class.java, SVG::class.java, SvgDecoder())
    }
}
