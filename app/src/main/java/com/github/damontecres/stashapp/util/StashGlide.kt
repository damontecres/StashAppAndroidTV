package com.github.damontecres.stashapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.signature.ObjectKey
import java.util.concurrent.TimeUnit

class StashGlide private constructor() {
    companion object {
        fun with(
            context: Context,
            url: GlideUrl,
        ): RequestBuilder<Drawable> {
            val cacheTimeout =
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("memoryCacheTimeout", 15).toLong()
            val cacheTime = TimeUnit.MINUTES.toMillis(cacheTimeout).toDouble()
            val cacheKey = (System.currentTimeMillis() / cacheTime).toInt()
            return Glide.with(context).load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .signature(ObjectKey(cacheKey))
                .skipMemoryCache(cacheTimeout == 0L)
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
            val cacheTimeout =
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt("memoryCacheTimeout", 15).toLong()
            val cacheTime = TimeUnit.MINUTES.toMillis(cacheTimeout).toDouble()
            val cacheKey = (System.currentTimeMillis() / cacheTime).toInt()
            return Glide.with(context)
                .asBitmap()
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .signature(ObjectKey(cacheKey))
                .skipMemoryCache(cacheTimeout == 0L)
        }

        const val TAG = "StashGlide"
    }
}
