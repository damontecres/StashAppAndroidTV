package com.github.damontecres.stashapp

import android.content.Context
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
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

class AuthorizationInterceptor(val apiKey: String?) : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain
    ): HttpResponse {
        return if(apiKey.isNullOrBlank()){
            chain.proceed(request)
        }else{
            chain.proceed(request.newBuilder().addHeader(Constants.STASH_API_HEADER, apiKey).build())
        }

    }
}

fun createApolloClient(context: Context): ApolloClient?{
    var stashUrl = PreferenceManager.getDefaultSharedPreferences(context).getString("stashUrl", "")
    val apiKey = PreferenceManager.getDefaultSharedPreferences(context).getString("stashApiKey", "")

    return if(stashUrl!!.isNotBlank()) {
        if (!stashUrl.endsWith("/graphql")) {
            stashUrl += "/graphql"
        }
        ApolloClient.Builder()
            .serverUrl(stashUrl)
            .addHttpInterceptor(AuthorizationInterceptor(apiKey))
            .build()
    }else{
        null
    }
}