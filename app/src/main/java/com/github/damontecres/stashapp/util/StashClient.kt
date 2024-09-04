package com.github.damontecres.stashapp.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.github.damontecres.stashapp.R
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Response
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.time.DurationUnit

class StashClient private constructor() {
    companion object {
        private const val TAG = "StashClient"
        private const val OK_HTTP_TAG = "$TAG.OkHttpClient"

        @Volatile
        private var httpClient: OkHttpClient? = null

        @Volatile
        private var apolloClient: ApolloClient? = null

        /**
         * Invalidate the cached clients typically when switching servers
         */
        fun invalidate() {
            synchronized(this) {
                httpClient = null
                apolloClient = null
            }
        }

        /**
         * Get an [OkHttpClient] cached from a previous call or else created from the provided [Context]
         */
        fun getHttpClient(context: Context): OkHttpClient {
            if (httpClient == null) {
                synchronized(this) {
                    if (httpClient == null) {
                        Log.v(TAG, "Creating new OkHttpClient")
                        val newClient = createOkHttpClient(context, true, true)
                        httpClient = newClient
                    }
                }
            }
            return httpClient!!
        }

        /**
         * Get an [OkHttpClient] for use in [StashGlideModule].
         *
         * This client is not cached and does not include the API key in requests
         */
        fun getGlideHttpClient(context: Context): OkHttpClient {
            Log.v(TAG, "Creating new OkHttpClient for Glide")
            return createOkHttpClient(context, false, true)
        }

        fun getStreamHttpClient(context: Context): OkHttpClient {
            return createOkHttpClient(context, true, false)
        }

        private fun createOkHttpClient(
            context: Context,
            useApiKey: Boolean,
            useCache: Boolean,
        ): OkHttpClient {
            val manager = PreferenceManager.getDefaultSharedPreferences(context)
            val server =
                getServerRoot(
                    manager.getString(
                        context.getString(R.string.pref_key_current_server),
                        null,
                    ),
                )
            val apiKey =
                manager.getString(context.getString(R.string.pref_key_current_api_key), null)
                    ?.trim()
            val trustAll = manager.getBoolean("trustAllCerts", false)
            val cacheDuration = cacheDurationPrefToDuration(manager.getInt("networkCacheDuration", 3))
            val cacheLogging = manager.getBoolean("networkCacheLogging", false)
            val networkTimeout = manager.getInt("networkTimeout", 15).toLong()

            val userAgent = createUserAgent(context)

            Log.v(TAG, "User-Agent=$userAgent")
            var builder =
                OkHttpClient.Builder()
                    .readTimeout(networkTimeout, TimeUnit.SECONDS)
                    .writeTimeout(networkTimeout, TimeUnit.SECONDS)
                    .addNetworkInterceptor {
                        it.proceed(
                            it.request().newBuilder().header("User-Agent", userAgent)
                                .build(),
                        )
                    }

            if (trustAll) {
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, arrayOf(TRUST_ALL_CERTS), SecureRandom())
                builder =
                    builder.sslSocketFactory(
                        sslContext.socketFactory,
                        TRUST_ALL_CERTS,
                    ).hostnameVerifier { _, _ ->
                        true
                    }
            }
            if (useApiKey && apiKey.isNotNullOrBlank()) {
                builder =
                    builder.addInterceptor {
                        val request =
                            if (server != null && it.request().url.toString().startsWith(server)) {
                                // Only set the API Key if the target URL is the stash server
                                it.request().newBuilder()
                                    .addHeader(Constants.STASH_API_HEADER, apiKey)
                                    .build()
                            } else {
                                it.request()
                            }
                        it.proceed(request)
                    }
            }
            if (useCache && cacheLogging) {
                Log.d(
                    OK_HTTP_TAG,
                    "cacheDuration in hours: ${cacheDuration?.toInt(
                        DurationUnit.HOURS,
                    )}",
                )
                builder =
                    builder.eventListener(
                        object : EventListener() {
                            override fun cacheHit(
                                call: Call,
                                response: Response,
                            ) {
                                Log.v(OK_HTTP_TAG, "cacheHit: ${call.request().url} => ${response.code}")
                            }

                            override fun cacheMiss(call: Call) {
                                Log.v(OK_HTTP_TAG, "cacheMiss: ${call.request().url}")
                            }

                            override fun cacheConditionalHit(
                                call: Call,
                                cachedResponse: Response,
                            ) {
                                Log.v(
                                    OK_HTTP_TAG,
                                    "cacheConditionalHit: ${call.request().url} => ${cachedResponse.code}",
                                )
                            }
                        },
                    )
            }
            if (useCache && cacheDuration != null) {
                builder =
                    builder.addInterceptor {
                        val request =
                            it.request().newBuilder()
                                .cacheControl(
                                    CacheControl.Builder()
                                        .maxAge(cacheDuration.toInt(DurationUnit.HOURS), TimeUnit.HOURS)
                                        .build(),
                                )
                                .build()
                        it.proceed(request)
                    }
            }
            builder =
                if (useCache) {
                    builder.cache(Constants.getNetworkCache(context))
                } else {
                    builder.cache(null)
                }
            return builder.build()
        }

        fun createUserAgent(context: Context): String {
            val appName = context.getString(R.string.app_name)
            val versionStr = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            val comments =
                listOf(
                    join("os", Build.VERSION.BASE_OS),
                    join("release", Build.VERSION.RELEASE),
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
         * Get an [ApolloClient] cached from a previous call or else created from the provided [Context]
         */
        @Throws(QueryEngine.StashNotConfiguredException::class)
        fun getApolloClient(context: Context): ApolloClient {
            if (apolloClient == null) {
                synchronized(this) {
                    if (apolloClient == null) {
                        Log.v(TAG, "Creating new ApolloClient")
                        val newClient = createApolloClient(context)
                        apolloClient = newClient
                    }
                }
            }
            return apolloClient!!
        }

        /**
         * Create a new [ApolloClient]. Using [getApolloClient] is preferred.
         */
        private fun createApolloClient(context: Context): ApolloClient {
            val stashUrl =
                PreferenceManager.getDefaultSharedPreferences(context).getString(
                    context.getString(
                        R.string.pref_key_current_server,
                    ),
                    null,
                ) ?: throw QueryEngine.StashNotConfiguredException()

            val url = cleanServerUrl(stashUrl)
            val httpEngine = DefaultHttpEngine(getHttpClient(context))
            return ApolloClient.Builder()
                .serverUrl(url)
                .httpEngine(httpEngine)
                .build()
        }

        fun cleanServerUrl(stashUrl: String): String {
            var cleanedStashUrl = stashUrl.trim()
            if (!cleanedStashUrl.startsWith("http://") && !cleanedStashUrl.startsWith("https://")) {
                // Assume http
                cleanedStashUrl = "http://$cleanedStashUrl"
            }
            var url = Uri.parse(cleanedStashUrl)
            val pathSegments = url.pathSegments.toMutableList()
            if (pathSegments.isEmpty() || pathSegments.last() != "graphql") {
                pathSegments.add("graphql")
            }
            url =
                url.buildUpon()
                    .path(pathSegments.joinToString("/")) // Ensure the URL is the graphql endpoint
                    .build()
            return url.toString()
        }

        /**
         * Get the server URL excluding the (unlikely) `/graphql` last path segment
         */
        fun getServerRoot(stashUrl: String?): String? {
            if (stashUrl == null) {
                return null
            }
            var cleanedStashUrl = stashUrl.trim()
            if (!cleanedStashUrl.startsWith("http://") && !cleanedStashUrl.startsWith("https://")) {
                // Assume http
                cleanedStashUrl = "http://$cleanedStashUrl"
            }
            var url = Uri.parse(cleanedStashUrl)
            val pathSegments = url.pathSegments.toMutableList()
            if (pathSegments.isNotEmpty() && pathSegments.last() == "graphql") {
                pathSegments.removeLast()
            }
            url =
                url.buildUpon()
                    .path(pathSegments.joinToString("/"))
                    .build()
            return url.toString()
        }

        /**
         * Build an [ApolloClient] suitable for testing connectivity for the specified server.
         */
        fun createTestApolloClient(
            context: Context,
            server: StashServer,
            trustCerts: Boolean,
        ): ApolloClient {
            val url = cleanServerUrl(server.url)
            val userAgent = createUserAgent(context)
            var builder =
                OkHttpClient.Builder()
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addNetworkInterceptor {
                        it.proceed(
                            it.request().newBuilder().header("User-Agent", userAgent)
                                .build(),
                        )
                    }

            if (trustCerts) {
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, arrayOf(TRUST_ALL_CERTS), SecureRandom())
                builder =
                    builder.sslSocketFactory(
                        sslContext.socketFactory,
                        TRUST_ALL_CERTS,
                    ).hostnameVerifier { _, _ ->
                        true
                    }
            }
            if (server.apiKey.isNotNullOrBlank()) {
                builder =
                    builder.addInterceptor {
                        val request =
                            it.request().newBuilder()
                                .addHeader(Constants.STASH_API_HEADER, server.apiKey.trim())
                                .build()
                        it.proceed(request)
                    }
            }
            val httpClient = builder.build()
            return ApolloClient.Builder()
                .serverUrl(url)
                .httpEngine(DefaultHttpEngine(httpClient))
                .build()
        }

        private val TRUST_ALL_CERTS: X509TrustManager =
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

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
    }
}
