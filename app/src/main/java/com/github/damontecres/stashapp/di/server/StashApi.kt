package com.github.damontecres.stashapp.di.server

import android.annotation.SuppressLint
import android.content.Context
import co.touchlab.kermit.Logger
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.websocket.GraphQLWsProtocol
import com.apollographql.apollo.network.websocket.WebSocketEngine
import com.apollographql.apollo.network.websocket.WebSocketNetworkTransport
import com.github.damontecres.stashapp.di.StandardHttpClient
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.util.Constants.OK_HTTP_CACHE_DIR
import com.github.damontecres.stashapp.util.cacheDurationPrefToDuration
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Response
import org.koin.core.annotation.Single
import timber.log.Timber
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.time.DurationUnit

@Single
class StashApi(
    private val context: Context,
    @param:StandardHttpClient private val httpClient: OkHttpClient,
) {
    @OptIn(ApolloExperimental::class)
    var server: StashServer = StashServer.UNSET
        private set

    lateinit var apolloClient: ApolloClient
        private set

    suspend fun changeServer(
        preferences: StashPreferences,
        server: StashServer,
    ) {
        Timber.i("Switching server to %s", server.url)
        this.apolloClient = createApolloClient(preferences, server)
        this.server = server
    }

    fun createFor(server: StashServer) =
        StashApi(context, httpClient).apply {
            this.server = server
            apolloClient = createApolloClient(server, httpClient)
        }

    @OptIn(ApolloExperimental::class)
    suspend fun createApolloClient(
        preferences: StashPreferences,
        server: StashServer,
    ): ApolloClient {
        val client = createOkHttpClient(preferences, server)
        return createApolloClient(server, client)
    }

    private suspend fun createOkHttpClient(
        preferences: StashPreferences,
        server: StashServer,
    ): OkHttpClient {
        val trustAll = preferences.advancedPreferences.trustSelfSignedCertificates
        val cacheDuration =
            cacheDurationPrefToDuration(preferences.cachePreferences.cacheExpirationTime)
        val cacheLogging = preferences.cachePreferences.logCacheHits
        val networkTimeout = preferences.advancedPreferences.networkTimeoutMs

        var builder =
            httpClient
                .newBuilder()
                .readTimeout(networkTimeout, TimeUnit.SECONDS)
                .writeTimeout(networkTimeout, TimeUnit.SECONDS)

        if (trustAll) {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, arrayOf(TRUST_ALL_CERTS), SecureRandom())
            builder =
                builder
                    .sslSocketFactory(
                        sslContext.socketFactory,
                        TRUST_ALL_CERTS,
                    ).hostnameVerifier { _, _ ->
                        true
                    }
        }
        if (cacheLogging) {
            Logger.d {
                "cacheDuration in hours: ${
                    cacheDuration?.toInt(
                        DurationUnit.HOURS,
                    )
                }"
            }
            builder =
                builder.eventListener(
                    object : EventListener() {
                        override fun cacheHit(
                            call: Call,
                            response: Response,
                        ) {
                            Logger.v {
                                "cacheHit: ${call.request().url} => ${response.code}"
                            }
                        }

                        override fun cacheMiss(call: Call) {
                            Logger.v { "cacheMiss: ${call.request().url}" }
                        }

                        override fun cacheConditionalHit(
                            call: Call,
                            cachedResponse: Response,
                        ) {
                            Logger.v {
                                "cacheConditionalHit: ${call.request().url} => ${cachedResponse.code}"
                            }
                        }
                    },
                )
        }
        val cacheControl =
            if (cacheDuration != null) {
                CacheControl
                    .Builder()
                    .maxAge(
                        cacheDuration.toInt(DurationUnit.HOURS),
                        TimeUnit.HOURS,
                    ).build()
            } else {
                CacheControl.Builder().noCache().build()
            }
        builder =
            builder
                .addInterceptor(ServerInterceptor(server))
                .addNetworkInterceptor {
                    val request =
                        it
                            .request()
                            .newBuilder()
                            .cacheControl(cacheControl)
                            .build()
                    it.proceed(request)
                }

        val cacheSize = preferences.cachePreferences.networkCacheSize * 1024 * 1024
        builder.cache(Cache(File(context.cacheDir, OK_HTTP_CACHE_DIR), cacheSize))

        return builder.build()
    }

    companion object {
        @OptIn(ApolloExperimental::class)
        fun createApolloClient(
            server: StashServer,
            httpClient: OkHttpClient,
        ): ApolloClient {
            val apolloClient =
                ApolloClient
                    .Builder()
                    .serverUrl(server.url)
                    .httpEngine(DefaultHttpEngine(httpClient))
                    .subscriptionNetworkTransport(
                        WebSocketNetworkTransport
                            .Builder()
                            .serverUrl(server.url)
                            .wsProtocol(GraphQLWsProtocol())
                            .webSocketEngine(WebSocketEngine(httpClient))
                            .build(),
                    ).build()
            return apolloClient
        }
    }
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

        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
