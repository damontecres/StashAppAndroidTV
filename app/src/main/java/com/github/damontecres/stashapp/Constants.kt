package com.github.damontecres.stashapp

import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders

object Constants{
    const val STASH_API_HEADER="ApiKey"
}

fun createGlideUrl(url:String, apiKey: String?): GlideUrl{
    return if(apiKey.isNullOrBlank()){
        GlideUrl(url)
    }else {
        GlideUrl(
            url,
            LazyHeaders.Builder()
                .addHeader(Constants.STASH_API_HEADER, apiKey)
                .build()
        )
    }
}