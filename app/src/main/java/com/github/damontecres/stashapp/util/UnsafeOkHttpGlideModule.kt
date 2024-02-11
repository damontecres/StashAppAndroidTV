package com.github.damontecres.stashapp.util

import android.content.Context
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import java.io.InputStream

@GlideModule
class UnsafeOkHttpGlideModule : AppGlideModule() {
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    override fun registerComponents(
        context: Context,
        glide: Glide,
        registry: Registry,
    ) {
        val trustAll =
            PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("trustAllCerts", false)
        if (trustAll) {
            registry.replace<GlideUrl, InputStream>(
                GlideUrl::class.java,
                InputStream::class.java,
                OkHttpUrlLoader.Factory(createUnsafeOkHttpClient()),
            )
        }
    }
}
