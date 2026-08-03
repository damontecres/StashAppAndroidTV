package com.github.damontecres.stashapp.di.server

import com.github.damontecres.stashapp.util.Constants
import okhttp3.Interceptor
import okhttp3.Response

class ServerInterceptor(
    private val server: StashServer,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request =
            if (server.apiKey != null) {
                val isStashUrl =
                    chain
                        .request()
                        .url
                        .toString()
                        .startsWith(server.serverRoot)
                if (isStashUrl) {
                    // Only set the API Key if the target URL is the stash server
                    chain
                        .request()
                        .newBuilder()
                        .addHeader(Constants.STASH_API_HEADER, server.cleanedApiKey!!)
                        .build()
                } else {
                    chain.request()
                }
            } else {
                chain.request()
            }
        return chain.proceed(request)
    }
}
