package com.github.damontecres.stashapp.util

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl

class StashGlide private constructor() {
    companion object {
        fun with(
            context: Context,
            url: GlideUrl,
        ): RequestBuilder<Drawable> {
            return Glide.with(context).load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(false)
        }

        fun with(
            context: Context,
            url: String,
        ): RequestBuilder<Drawable> {
            return with(context, createGlideUrl(url, context))
        }
    }
}
