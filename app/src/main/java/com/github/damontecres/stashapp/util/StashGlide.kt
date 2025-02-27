package com.github.damontecres.stashapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.signature.ObjectKey

/**
 * Replacement for [Glide] static functions to incorporate the caching schemes used in the app
 */
class StashGlide private constructor() {
    companion object {
        // Images larger than 5mb need disk cache
        // https://github.com/bumptech/glide/issues/4950
        // Technically, it's >= 5_242_880, but be a little conservative
        private const val GLIDE_IMAGE_MAX_SIZE = 5_000_000

        fun with(
            context: Context,
            url: GlideUrl,
            size: Int = -1,
        ): RequestBuilder<Drawable> =
            if (size >= GLIDE_IMAGE_MAX_SIZE) {
                Log.v(TAG, "Image >= GLIDE_IMAGE_MAX_SIZE ($GLIDE_IMAGE_MAX_SIZE)")
                Glide
                    .with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .skipMemoryCache(true)
                    .signature(ObjectKey(System.currentTimeMillis()))
            } else {
                Glide
                    .with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
            }

        fun with(
            context: Context,
            url: String,
            size: Int = -1,
        ): RequestBuilder<Drawable> = with(context, createGlideUrl(url, context), size)

        fun with(
            context: Context,
            url: GlideUrl,
        ): RequestBuilder<Drawable> =
            Glide
                .with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)

        fun with(
            context: Context,
            url: String,
        ): RequestBuilder<Drawable> = with(context, createGlideUrl(url, context))

        fun withBitmap(
            context: Context,
            url: String,
            size: Int = 0,
        ): RequestBuilder<Bitmap> =
            if (size >= GLIDE_IMAGE_MAX_SIZE) {
                Glide
                    .with(context)
                    .asBitmap()
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .skipMemoryCache(true)
                    .signature(ObjectKey(System.currentTimeMillis()))
            } else {
                Glide
                    .with(context)
                    .asBitmap()
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
            }

        fun withCaching(
            context: Context,
            url: String,
        ): RequestBuilder<Drawable> =
            Glide
                .with(context)
                .load(createGlideUrl(url, context))
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .skipMemoryCache(false)

        fun withMemoryCaching(
            context: Context,
            url: String,
        ): RequestBuilder<Drawable> =
            Glide
                .with(context)
                .load(createGlideUrl(url, context))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(false)

        const val TAG = "StashGlide"
    }
}
