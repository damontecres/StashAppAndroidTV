package com.github.damontecres.stashapp.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.github.damontecres.stashapp.R
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Provides static functions to get clients to interact with the server
 */
class StashClient private constructor() {
    companion object {
        private const val TAG = "StashClient"
        private const val OK_HTTP_TAG = "$TAG.OkHttpClient"

        /**
         * Create the user agent used in HTTP calls
         *
         * Form of: StashAppAndroidTV/<version> (release/<android version>; sdk/<android sdk int>) (<manufacturer>; <device model>; <device name>)
         */
        fun createUserAgent(context: Context): String {
            val appName = context.getString(R.string.app_name)
            val versionStr = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            val comments =
                listOf(
                    joinValueNotNull("os", Build.VERSION.BASE_OS),
                    joinValueNotNull("release", Build.VERSION.RELEASE),
                    "sdk/${Build.VERSION.SDK_INT}",
                ).joinNotNullOrBlank("; ")
            val device =
                listOf(
                    Build.MANUFACTURER,
                    Build.MODEL,
                    if (Build.MODEL != Build.PRODUCT) Build.PRODUCT else null,
                    Build.DEVICE,
                ).joinNotNullOrBlank("; ")
            return "$appName/$versionStr ($comments) ($device)"
        }

        /**
         * Cleans up the URL that a user enters
         *
         * Tries to add protocol (http) and endpoint (/graphql)
         */
        fun cleanServerUrl(stashUrl: String): String {
            var cleanedStashUrl = stashUrl.trim()
            if (!cleanedStashUrl.startsWith("http://") && !cleanedStashUrl.startsWith("https://")) {
                // Assume http
                cleanedStashUrl = "http://$cleanedStashUrl"
            }
            var url = cleanedStashUrl.toUri()
            val pathSegments = url.pathSegments.toMutableList()
            if (url.host.isNotNullOrBlank() && (pathSegments.isEmpty() || pathSegments.last() != "graphql")) {
                pathSegments.add("graphql")
            }
            url =
                url
                    .buildUpon()
                    .path(pathSegments.joinToString("/")) // Ensure the URL is the graphql endpoint
                    .build()
            return url.toString()
        }

        fun createLoginUrl(stashUrl: String): String {
            val cleanedStashUrl = cleanServerUrl(stashUrl)
            var url = cleanedStashUrl.toUri()
            val pathSegments = url.pathSegments.toMutableList()
            if (url.host.isNotNullOrBlank() && pathSegments.isNotEmpty() && pathSegments.last() == "graphql") {
                pathSegments.removeLastOrNull()
            }
            if (url.host.isNotNullOrBlank()) {
                pathSegments.add("login")
            }
            url =
                url
                    .buildUpon()
                    .path(pathSegments.joinToString("/")) // Ensure the URL is the graphql endpoint
                    .build()
            return url.toString()
        }

        /**
         * Get the server URL excluding the (unlikely) `/graphql` last path segment
         *
         * Basically, if a user is using a reverse proxy routes the path ending with `/graphql`, this will remove that
         */
        fun getServerRoot(stashUrl: String): String {
            var cleanedStashUrl = stashUrl.trim()
            if (!cleanedStashUrl.startsWith("http://") && !cleanedStashUrl.startsWith("https://")) {
                // Assume http
                cleanedStashUrl = "http://$cleanedStashUrl"
            }
            var url = Uri.parse(cleanedStashUrl)
            val pathSegments = url.pathSegments.toMutableList()
            if (pathSegments.isNotEmpty() && pathSegments.last() == "graphql") {
                pathSegments.removeAt(pathSegments.size - 1)
            }
            url =
                url
                    .buildUpon()
                    .path(pathSegments.joinToString("/"))
                    .build()
            return url.toString()
        }
    }
}

val TRUST_ALL_CERTS: X509TrustManager =
    @SuppressLint("CustomX509TrustManager")
    object : X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkClientTrusted(
            chain: Array<X509Certificate>,
            authType: String,
        ) {
        }

        @SuppressLint("TrustAllX509TrustManager")
        override fun checkServerTrusted(
            chain: Array<X509Certificate>,
            authType: String,
        ) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
