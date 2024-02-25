package com.github.damontecres.stashapp.util

import android.content.Context
import android.graphics.Bitmap
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
                .skipMemoryCache(true)
        }

        fun with(
            context: Context,
            url: String,
        ): RequestBuilder<Drawable> {
            return with(context, createGlideUrl(url, context))
        }

        fun withBitmap(
            context: Context,
            url: String,
        ): RequestBuilder<Bitmap> {
            return Glide.with(context)
                .asBitmap()
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
        }

        const val TAG = "StashGlide"
    }
}
