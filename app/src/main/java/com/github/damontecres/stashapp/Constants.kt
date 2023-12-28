package com.github.damontecres.stashapp

import android.content.Context
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType

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
        return if(apiKey.isNullOrBlank()){
            chain.proceed(request)
        }else{
            chain.proceed(request.newBuilder().addHeader(Constants.STASH_API_HEADER, apiKey.trim()).build())
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

suspend fun fetchScenesById(context: Context, sceneIds:List<Int>): List<SlimSceneData>{
    val apolloClient = createApolloClient(context)
    if (apolloClient != null) {
        val results = apolloClient.query(
            FindScenesQuery(scene_ids = Optional.present(sceneIds))
        ).execute()
        return results.data?.findScenes?.scenes?.map { it.slimSceneData }.orEmpty()
    }
    return listOf()
}

suspend fun fetchSceneById(context: Context, sceneId:Int): SlimSceneData?{
    val results = fetchScenesById(context, listOf(sceneId))
    return results.getOrNull(0)
}

suspend fun fetchScenesByTag(context: Context, tagId:Int): List<SlimSceneData>{
    val apolloClient = createApolloClient(context)
    if (apolloClient != null) {
        val results = apolloClient.query(
            FindScenesQuery(scene_filter = Optional.present(SceneFilterType(tags= Optional.present(
                HierarchicalMultiCriterionInput(value= Optional.present(listOf(tagId.toString())), modifier = CriterionModifier.INCLUDES_ALL)
            ))))
        ).execute()
        return results.data?.findScenes?.scenes?.map { it.slimSceneData }.orEmpty()
    }
    return listOf()
}

suspend fun fetchScenesByStudio(context: Context, studioId:Int):List<SlimSceneData>{
    val apolloClient = createApolloClient(context)
    if (apolloClient != null) {
        val results = apolloClient.query(
            FindScenesQuery(scene_filter = Optional.present(SceneFilterType(studios=Optional.present(
                HierarchicalMultiCriterionInput(value= Optional.present(listOf(studioId.toString())), modifier = CriterionModifier.INCLUDES_ALL)
            ))))
        ).execute()
        return results.data?.findScenes?.scenes?.map { it.slimSceneData }.orEmpty()
    }
    return listOf()
}