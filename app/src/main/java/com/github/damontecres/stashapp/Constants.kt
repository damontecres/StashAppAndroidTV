package com.github.damontecres.stashapp

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.github.damontecres.stashapp.api.FindSavedFilterQuery
import com.github.damontecres.stashapp.api.ServerInfoQuery
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.Scene

object Constants {
    /**
     * The name of the header for authenticating to Stash
     */
    const val STASH_API_HEADER = "ApiKey"
    const val PREF_KEY_STASH_URL = "stashUrl"
    const val PREF_KEY_STASH_API_KEY = "stashApi"
}

/**
 * Create a [GlideUrl], adding the API key to the headers if needed
 */
fun createGlideUrl(url: String, apiKey: String?): GlideUrl {
    return if (apiKey.isNullOrBlank()) {
        GlideUrl(url)
    } else {
        GlideUrl(
            url,
            LazyHeaders.Builder()
                .addHeader(Constants.STASH_API_HEADER, apiKey.trim())
                .build()
        )
    }
}

/**
 * Create a [GlideUrl], adding the API key to the headers if needed
 */
fun createGlideUrl(url: String, context: Context): GlideUrl {
    val apiKey = PreferenceManager.getDefaultSharedPreferences(context)
        .getString("stashApiKey", "")
    return createGlideUrl(url, apiKey)
}

/**
 * Add API key to headers for Apollo GraphQL requests
 */
class AuthorizationInterceptor(val apiKey: String?) : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain
    ): HttpResponse {
        return if (apiKey.isNullOrBlank()) {
            chain.proceed(request)
        } else {
            chain.proceed(
                request.newBuilder().addHeader(Constants.STASH_API_HEADER, apiKey.trim()).build()
            )
        }

    }
}

/**
 * Create a client for accessing Stash's GraphQL API
 */
fun createApolloClient(stashUrl: String?, apiKey: String?): ApolloClient? {
    return if (stashUrl!!.isNotBlank()) {
        var cleanedStashUrl = stashUrl.trim()
        if (!cleanedStashUrl.startsWith("http://") && !cleanedStashUrl.startsWith("https://")) {
            // Assume http
            cleanedStashUrl = "http://$cleanedStashUrl"
        }
        var url = Uri.parse(cleanedStashUrl)
        url = url.buildUpon()
            .path("/graphql") // Ensure the URL is the graphql endpoint
            .build()
        Log.d("Constants", "StashUrl: $stashUrl => $url")
        ApolloClient.Builder()
            .serverUrl(url.toString())
            .addHttpInterceptor(AuthorizationInterceptor(apiKey))
            .build()
    } else {
        null
    }
}

/**
 * Create a client for accessing Stash's GraphQL API using the default shared preferences for the URL & API key
 */
fun createApolloClient(context: Context): ApolloClient? {
    val stashUrl = PreferenceManager.getDefaultSharedPreferences(context).getString("stashUrl", "")
    val apiKey = PreferenceManager.getDefaultSharedPreferences(context).getString("stashApiKey", "")
    return createApolloClient(stashUrl, apiKey)
}

/**
 * Test whether the app can connect to Stash
 *
 * @param context the context to pull preferences from
 * @param showToast whether a Toast message should be displayed with error/success information
 */
suspend fun testStashConnection(context: Context, showToast: Boolean): Boolean {
    val client = createApolloClient(context)
    if (client == null) {
        if (showToast) {
            Toast.makeText(
                context, "Stash server URL is not set.",
                Toast.LENGTH_LONG
            ).show()
        }
    } else {
        try {
            val info = client.query(ServerInfoQuery()).execute()
            if (info.hasErrors()) {
                if (showToast) {
                    Toast.makeText(
                        context, "Failed to connect to Stash. Check URL or API Key.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                if (showToast) {
                    val version = info.data?.version?.version
                    val sceneCount = info.data?.stats?.scene_count
                    Toast.makeText(
                        context, "Connected to Stash ($version) with $sceneCount scenes!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return true
            }
        } catch (ex: ApolloHttpException) {
            Log.e("Constants", "ApolloHttpException", ex)
            if (ex.statusCode == 401 || ex.statusCode == 403) {
                if (showToast) {
                    Toast.makeText(
                        context, "Failed to connect to Stash. API Key was not valid.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                if (showToast) {
                    Toast.makeText(
                        context, "Failed to connect to Stash. Error was '${ex.message}'",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (ex: ApolloException) {
            Log.e("Constants", "ApolloException", ex)
            if (showToast) {
                Toast.makeText(
                    context,
                    "Failed to connect to Stash. Error was '${ex.message}'",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    return false
}

fun selectStream(scene: Scene?): String? {
    if (scene == null) {
        return null
    }
    var stream = scene.streams["Direct stream"]
    if (stream == null) {
        stream = scene.streams["WEBM"]
    }
    if (stream == null) {
        stream = scene.streams["MP4"]
    }
    if (stream == null) {
        stream = scene.streamUrl
    }
    return stream
}

fun convertFilter(filter: FindSavedFilterQuery.Find_filter?): FindFilterType? {
    return if (filter != null) {
        FindFilterType(
            q = Optional.presentIfNotNull(filter.q),
            page = Optional.presentIfNotNull(filter.page),
            per_page = Optional.presentIfNotNull(filter.per_page),
            sort = Optional.presentIfNotNull(filter.sort),
            direction = Optional.presentIfNotNull(filter.direction)
        )
    } else {
        null
    }
}

val supportedFilterModes =
    setOf(FilterMode.SCENES, FilterMode.STUDIOS, FilterMode.PERFORMERS, FilterMode.TAGS)