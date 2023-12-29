package com.github.damontecres.stashapp

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.ServerInfoQuery
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType

object Constants {
    const val STASH_API_HEADER = "ApiKey"
}

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

fun createApolloClient(stashUrl: String?, apiKey: String?): ApolloClient? {
    return if (stashUrl!!.isNotBlank()) {
        var url = Uri.parse(stashUrl.trim())
        val scheme = if (url.scheme.isNullOrBlank()) "http" else url.scheme
        url = url.buildUpon()
            .scheme(scheme)
            .path("/graphql")
            .build()
        ApolloClient.Builder()
            .serverUrl(url.toString())
            .addHttpInterceptor(AuthorizationInterceptor(apiKey))
            .build()
    } else {
        null
    }
}

fun createApolloClient(context: Context): ApolloClient? {
    var stashUrl = PreferenceManager.getDefaultSharedPreferences(context).getString("stashUrl", "")
    val apiKey = PreferenceManager.getDefaultSharedPreferences(context).getString("stashApiKey", "")
    return createApolloClient(stashUrl, apiKey)
}

suspend fun getStashServerInfo(
    stashUrl: String?,
    apiKey: String?
): ApolloResponse<ServerInfoQuery.Data>? {
    try {
        return createApolloClient(stashUrl, apiKey)?.query(ServerInfoQuery())?.execute()
    } catch (exception: ApolloException) {
        return null
    }
}

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

suspend fun fetchScenesById(context: Context, sceneIds: List<Int>): List<SlimSceneData> {
    val apolloClient = createApolloClient(context)
    if (apolloClient != null) {
        val results = apolloClient.query(
            FindScenesQuery(scene_ids = Optional.present(sceneIds))
        ).execute()
        return results.data?.findScenes?.scenes?.map { it.slimSceneData }.orEmpty()
    }
    return listOf()
}

suspend fun fetchSceneById(context: Context, sceneId: Int): SlimSceneData? {
    val results = fetchScenesById(context, listOf(sceneId))
    return results.getOrNull(0)
}

suspend fun fetchScenesByTag(context: Context, tagId: Int): List<SlimSceneData> {
    val apolloClient = createApolloClient(context)
    if (apolloClient != null) {
        val results = apolloClient.query(
            FindScenesQuery(
                scene_filter = Optional.present(
                    SceneFilterType(
                        tags = Optional.present(
                            HierarchicalMultiCriterionInput(
                                value = Optional.present(listOf(tagId.toString())),
                                modifier = CriterionModifier.INCLUDES_ALL
                            )
                        )
                    )
                )
            )
        ).execute()
        return results.data?.findScenes?.scenes?.map { it.slimSceneData }.orEmpty()
    }
    return listOf()
}

suspend fun fetchScenesByStudio(context: Context, studioId: Int): List<SlimSceneData> {
    val apolloClient = createApolloClient(context)
    if (apolloClient != null) {
        val results = apolloClient.query(
            FindScenesQuery(
                scene_filter = Optional.present(
                    SceneFilterType(
                        studios = Optional.present(
                            HierarchicalMultiCriterionInput(
                                value = Optional.present(listOf(studioId.toString())),
                                modifier = CriterionModifier.INCLUDES_ALL
                            )
                        )
                    )
                )
            )
        ).execute()
        return results.data?.findScenes?.scenes?.map { it.slimSceneData }.orEmpty()
    }
    return listOf()
}