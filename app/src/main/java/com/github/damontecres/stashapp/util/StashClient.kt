package com.github.damontecres.stashapp.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.websocket.GraphQLWsProtocol
import com.apollographql.apollo.network.websocket.WebSocketEngine
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
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

/**
 * Provides static functions to get clients to interact with the server
 */
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
        fun getHttpClient(server: StashServer): OkHttpClient {
            if (httpClient == null) {
                synchronized(this) {
                    if (httpClient == null) {
                        Log.v(TAG, "Creating new OkHttpClient")
                        val newClient = createOkHttpClient(server, true, true)
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
        fun getGlideHttpClient(server: StashServer): OkHttpClient {
            Log.v(TAG, "Creating new OkHttpClient for Glide")
            return createOkHttpClient(server, false, true)
        }

        fun getStreamHttpClient(server: StashServer): OkHttpClient {
            return createOkHttpClient(server, true, false)
        }

        private fun createOkHttpClient(
            server: StashServer,
            useApiKey: Boolean,
            useCache: Boolean,
        ): OkHttpClient {
            val context = StashApplication.getApplication()
            val manager = PreferenceManager.getDefaultSharedPreferences(context)
            val serverUrlRoot = getServerRoot(server.url)
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
            if (useApiKey && server.apiKey.isNotNullOrBlank()) {
                builder =
                    builder.addInterceptor {
                        val request =
                            if (it.request().url.toString().startsWith(serverUrlRoot)) {
                                // Only set the API Key if the target URL is the stash server
                                it.request().newBuilder()
                                    .addHeader(Constants.STASH_API_HEADER, server.apiKey)
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
         * Get an [ApolloClient] cached from a previous call or else created for the specified server
         */
        @Throws(QueryEngine.StashNotConfiguredException::class)
        fun getApolloClient(server: StashServer): ApolloClient {
            if (apolloClient == null) {
                synchronized(this) {
                    if (apolloClient == null) {
                        Log.v(TAG, "Creating new ApolloClient")
                        val newClient = createApolloClient(server)
                        apolloClient = newClient
                    }
                }
            }
            return apolloClient!!
        }

        /**
         * Create a new [ApolloClient]. Using [getApolloClient] is preferred.
         */
        @OptIn(ApolloExperimental::class)
        private fun createApolloClient(server: StashServer): ApolloClient {
            val url = cleanServerUrl(server.url)
            val httpClient = getHttpClient(server)
            return ApolloClient.Builder()
                .serverUrl(url)
                .httpEngine(DefaultHttpEngine(httpClient))
                .subscriptionNetworkTransport(
                    WebSocketNetworkTransport.Builder()
                        .serverUrl(url)
                        .wsProtocol(GraphQLWsProtocol())
                        .webSocketEngine(WebSocketEngine(httpClient))
                        .build(),
                )
                .build()
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
         *
         * @see [com.github.damontecres.stashapp.util.testStashConnection]
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
