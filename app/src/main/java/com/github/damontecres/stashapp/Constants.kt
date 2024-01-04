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
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.StringCriterionInput
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


fun convertIntCriterionInput(it: Map<String, *>?): IntCriterionInput? {
    return if (it != null) {
        val values = it["value"]!! as Map<String, Int?>
        IntCriterionInput(
            values["value"]!!,
            Optional.presentIfNotNull(values["value2"]),
            CriterionModifier.valueOf(it["modifier"]!! as String)
        )
    } else {
        null
    }
}

fun convertFloatCriterionInput(it: Map<String, *>?): FloatCriterionInput? {
    return if (it != null) {
        val values = it["value"]!! as Map<String, Double?>
        FloatCriterionInput(
            values["value"]!!,
            Optional.presentIfNotNull(values["value2"]),
            CriterionModifier.valueOf(it["modifier"]!! as String)
        )
    } else {
        null
    }
}

fun convertStringCriterionInput(it: Map<String, *>?): StringCriterionInput? {
    return if (it != null) {
        val values = it["value"]!! as Map<String, String?>
        StringCriterionInput(
            values["value"]!!,
            CriterionModifier.valueOf(it["modifier"]!!.toString())
        )
    } else {
        null
    }
}

fun mapToIds(list: Any?): List<String>? {
    return (list as List<*>?)?.map { (it as Map<String, String>)["id"].orEmpty() }?.toList()
}

fun convertHierarchicalMultiCriterionInput(it: Map<String, *>?): HierarchicalMultiCriterionInput? {
    return if (it != null) {
        val values = it["value"]!! as Map<String, *>
        val items = mapToIds(values["items"])
        val excludes = mapToIds(values["excludes"])
        HierarchicalMultiCriterionInput(
            Optional.presentIfNotNull(items),
            CriterionModifier.valueOf(it["modifier"]!!.toString()),
            Optional.presentIfNotNull(it["depth"] as Int?),
            Optional.presentIfNotNull(excludes)
        )
    } else {
        null
    }
}

fun convertMultiCriterionInput(it: Map<String, *>?): MultiCriterionInput? {
    return if (it != null) {
        val values = it["value"]!! as Map<String, *>
        val items = mapToIds(values["items"])
        val excludes = mapToIds(values["excludes"])
        MultiCriterionInput(
            Optional.presentIfNotNull(items),
            CriterionModifier.valueOf(it["modifier"]!!.toString()),
            Optional.presentIfNotNull(excludes)
        )
    } else {
        null
    }
}

fun convertBoolean(it: Map<String, *>?): Boolean? {
    return if (it != null) {
        val value = (it["value"] as String?).toBoolean()
        when (CriterionModifier.valueOf(it["modifier"] as String)) {
            CriterionModifier.EQUALS -> value
            CriterionModifier.NOT_EQUALS -> !value
            else -> null
        }
    } else {
        null
    }
}

fun convertPerformerObjectFilter(filter: Map<String, Map<String, *>>?): PerformerFilterType? {
    // TODO AND, OR, & NOT
    return PerformerFilterType(
        name = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("name"))),
        disambiguation = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("disambiguation"))),
        details = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("details"))),
        filter_favorites = Optional.presentIfNotNull(convertBoolean(filter?.get("filter_favorites"))),
        birth_year = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("birth_year"))),
        age = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("age"))),
        ethnicity = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("ethnicity"))),
        country = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("country"))),
        eye_color = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("eye_color"))),
        height_cm = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("height_cm"))),
        measurements = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("measurements"))),
        fake_tits = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("fake_tits"))),
        penis_length = Optional.presentIfNotNull(convertFloatCriterionInput(filter?.get("penis_length"))),
//        circumcised = Optional.presentIfNotNull(convertCircumcisionCriterionInput(filter?.get("circumcised"))),
        career_length = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("career_length"))),
        tattoos = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("tattoos"))),
        piercings = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("piercings"))),
        aliases = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("aliases"))),
//        gender = Optional.presentIfNotNull(convertGenderCriterionInput(filter?.get("gender"))),
        // is_missing = Optional.presentIfNotNull(convertString(filter?.get("is_missing"))),
        tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter?.get("tags"))),
        tag_count = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("tag_count"))),
        scene_count = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("scene_count"))),
        image_count = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("image_count"))),
        gallery_count = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("gallery_count"))),
        o_counter = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("o_counter"))),
        //stash_id_endpoint = Optional.presentIfNotNull(convertStashIDCriterionInput(filter?.get("stash_id_endpoint"))),
        rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("rating100"))),
        url = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("url"))),
        hair_color = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("hair_color"))),
        weight = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("weight"))),
        death_year = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("death_year"))),
        studios = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter?.get("studios"))),
        performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter?.get("performers"))),
        ignore_auto_tag = Optional.presentIfNotNull(convertBoolean(filter?.get("ignore_auto_tag"))),
//        birthdate = Optional.presentIfNotNull(convertDateCriterionInput(filter?.get("birthdate"))),
//        death_date = Optional.presentIfNotNull(convertDateCriterionInput(filter?.get("death_date"))),
//        created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter?.get("created_at"))),
//        updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter?.get("updated_at")))
    )
}