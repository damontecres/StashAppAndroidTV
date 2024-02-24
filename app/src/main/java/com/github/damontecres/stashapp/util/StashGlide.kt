package com.github.damontecres.stashapp.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
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
            // TODO
            val cacheTime = TimeUnit.MINUTES.toMillis(15).toDouble()
            val cacheKey = (System.currentTimeMillis() / cacheTime).toInt()
            Log.v(TAG, "${url.toURL()}, cacheTime=$cacheTime, cacheKey=$cacheKey")
            return Glide.with(context).load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .signature(ObjectKey(cacheKey))
                .skipMemoryCache(false)
        }

        fun with(
            context: Context,
            url: String,
        ): RequestBuilder<Drawable> {
            return with(context, createGlideUrl(url, context))
        }

        const val TAG = "StashGlide"
    }
}
