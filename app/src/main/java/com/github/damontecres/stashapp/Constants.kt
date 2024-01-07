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
import com.github.damontecres.stashapp.api.ServerInfoQuery
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.CircumcisionCriterionInput
import com.github.damontecres.stashapp.api.type.CircumisedEnum
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.DateCriterionInput
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.GenderCriterionInput
import com.github.damontecres.stashapp.api.type.GenderEnum
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.PHashDuplicationCriterionInput
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.PhashDistanceCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionEnum
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StashIDCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.StudioFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.api.type.TimestampCriterionInput
import com.github.damontecres.stashapp.data.DataType
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
fun createGlideUrl(
    url: String,
    apiKey: String?,
): GlideUrl {
    return if (apiKey.isNullOrBlank()) {
        GlideUrl(url)
    } else {
        GlideUrl(
            url,
            LazyHeaders.Builder()
                .addHeader(Constants.STASH_API_HEADER, apiKey.trim())
                .build(),
        )
    }
}

/**
 * Create a [GlideUrl], adding the API key to the headers if needed
 */
fun createGlideUrl(
    url: String,
    context: Context,
): GlideUrl {
    val apiKey =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString("stashApiKey", "")
    return createGlideUrl(url, apiKey)
}

/**
 * Add API key to headers for Apollo GraphQL requests
 */
class AuthorizationInterceptor(val apiKey: String?) : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain,
    ): HttpResponse {
        return if (apiKey.isNullOrBlank()) {
            chain.proceed(request)
        } else {
            chain.proceed(
                request.newBuilder().addHeader(Constants.STASH_API_HEADER, apiKey.trim()).build(),
            )
        }
    }
}

/**
 * Create a client for accessing Stash's GraphQL API
 */
fun createApolloClient(
    stashUrl: String?,
    apiKey: String?,
): ApolloClient? {
    return if (stashUrl!!.isNotBlank()) {
        var cleanedStashUrl = stashUrl.trim()
        if (!cleanedStashUrl.startsWith("http://") && !cleanedStashUrl.startsWith("https://")) {
            // Assume http
            cleanedStashUrl = "http://$cleanedStashUrl"
        }
        var url = Uri.parse(cleanedStashUrl)
        url =
            url.buildUpon()
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
suspend fun testStashConnection(
    context: Context,
    showToast: Boolean,
): Boolean {
    val client = createApolloClient(context)
    if (client == null) {
        if (showToast) {
            Toast.makeText(
                context,
                "Stash server URL is not set.",
                Toast.LENGTH_LONG,
            ).show()
        }
    } else {
        try {
            val info = client.query(ServerInfoQuery()).execute()
            if (info.hasErrors()) {
                if (showToast) {
                    Toast.makeText(
                        context,
                        "Failed to connect to Stash. Check URL or API Key.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            } else {
                if (showToast) {
                    val version = info.data?.version?.version
                    val sceneCount = info.data?.stats?.scene_count
                    Toast.makeText(
                        context,
                        "Connected to Stash ($version) with $sceneCount scenes!",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                return true
            }
        } catch (ex: ApolloHttpException) {
            Log.e("Constants", "ApolloHttpException", ex)
            if (ex.statusCode == 401 || ex.statusCode == 403) {
                if (showToast) {
                    Toast.makeText(
                        context,
                        "Failed to connect to Stash. API Key was not valid.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            } else {
                if (showToast) {
                    Toast.makeText(
                        context,
                        "Failed to connect to Stash. Error was '${ex.message}'",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        } catch (ex: ApolloException) {
            Log.e("Constants", "ApolloException", ex)
            if (showToast) {
                Toast.makeText(
                    context,
                    "Failed to connect to Stash. Error was '${ex.message}'",
                    Toast.LENGTH_LONG,
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

fun convertFilter(filter: SavedFilterData.Find_filter?): FindFilterType? {
    return if (filter != null) {
        FindFilterType(
            q = Optional.presentIfNotNull(filter.q),
            page = Optional.presentIfNotNull(filter.page),
            per_page = Optional.presentIfNotNull(filter.per_page),
            sort = Optional.presentIfNotNull(filter.sort),
            direction = Optional.presentIfNotNull(filter.direction),
        )
    } else {
        null
    }
}

val supportedFilterModes = DataType.entries.map { it.filterMode }.toSet()

fun convertIntCriterionInput(it: Map<String, *>?): IntCriterionInput? {
    return if (it != null) {
        val values = it["value"]!! as Map<String, Int?>
        IntCriterionInput(
            values["value"] ?: 0,
            Optional.presentIfNotNull(values["value2"]),
            CriterionModifier.valueOf(it["modifier"]!! as String),
        )
    } else {
        null
    }
}

fun convertFloatCriterionInput(it: Map<String, *>?): FloatCriterionInput? {
    return if (it != null) {
        val values = it["value"]!! as Map<String, Number?> // Might be an int or double
        FloatCriterionInput(
            values["value"]?.toDouble() ?: 0.0,
            Optional.presentIfNotNull(values["value2"]?.toDouble()),
            CriterionModifier.valueOf(it["modifier"]!! as String),
        )
    } else {
        null
    }
}

fun convertStringCriterionInput(it: Map<String, *>?): StringCriterionInput? {
    return if (it != null) {
        StringCriterionInput(
            it["value"]?.toString() ?: "",
            CriterionModifier.valueOf(it["modifier"]!!.toString()),
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
        val excludes = mapToIds(values["excluded"])
        HierarchicalMultiCriterionInput(
            Optional.presentIfNotNull(items),
            CriterionModifier.valueOf(it["modifier"]!!.toString()),
            Optional.presentIfNotNull(it["depth"] as Int?),
            Optional.presentIfNotNull(excludes),
        )
    } else {
        null
    }
}

fun convertMultiCriterionInput(it: Map<String, *>?): MultiCriterionInput? {
    return if (it != null) {
        val items = mapToIds(it["items"])
        val excludes = mapToIds(it["excluded"])
        MultiCriterionInput(
            Optional.presentIfNotNull(items),
            CriterionModifier.valueOf(it["modifier"]!!.toString()),
            Optional.presentIfNotNull(excludes),
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

fun convertDateCriterionInput(it: Map<String, *>?): DateCriterionInput? {
    return if (it != null) {
        val values = it["value"]!! as Map<String, String?>
        DateCriterionInput(
            values["value"]!!,
            Optional.presentIfNotNull(values["value2"]),
            CriterionModifier.valueOf(it["modifier"]!! as String),
        )
    } else {
        null
    }
}

fun convertTimestampCriterionInput(it: Map<String, *>?): TimestampCriterionInput? {
    return if (it != null) {
        val values = it["value"]!! as Map<String, String?>
        TimestampCriterionInput(
            values["value"]!!,
            Optional.presentIfNotNull(values["value2"]),
            CriterionModifier.valueOf(it["modifier"]!! as String),
        )
    } else {
        null
    }
}

fun convertCircumcisionCriterionInput(it: Map<String, *>?): CircumcisionCriterionInput? {
    return if (it != null) {
        val valueList = (it["value"] as List<String>?)
        CircumcisionCriterionInput(
            Optional.presentIfNotNull(
                valueList?.map { CircumisedEnum.valueOf(it.uppercase()) }
                    ?.toList(),
            ),
            CriterionModifier.valueOf(it["modifier"]!! as String),
        )
    } else {
        null
    }
}

fun convertGenderCriterionInput(it: Map<String, *>?): GenderCriterionInput? {
    return if (it != null) {
        val value = it["value"].toString().uppercase().replace(" ", "_")
        GenderCriterionInput(
            Optional.presentIfNotNull(GenderEnum.valueOf(value)),
            CriterionModifier.valueOf(it["modifier"]!! as String),
        )
    } else {
        null
    }
}

fun convertString(it: Map<String, *>?): String? {
    return if (it != null) {
        return it["value"]?.toString()
    } else {
        null
    }
}

fun convertStashIDCriterionInput(it: Map<String, *>?): StashIDCriterionInput? {
    return if (it != null) {
        val values = it["value"]!! as Map<String, String?>
        StashIDCriterionInput(
            Optional.presentIfNotNull(values["endpoint"]),
            Optional.presentIfNotNull(values["stash_id"]),
            CriterionModifier.valueOf(it["modifier"]!! as String),
        )
    } else {
        null
    }
}

fun convertPhashDistanceCriterionInput(it: Map<String, *>?): PhashDistanceCriterionInput? {
    return if (it != null) {
        val values = it["value"]!! as Map<String, *>
        PhashDistanceCriterionInput(
            values["value"]!! as String,
            CriterionModifier.valueOf(it["modifier"]!!.toString()),
            Optional.presentIfNotNull(values["distance"].toString().toInt()),
        )
    } else {
        null
    }
}

fun convertPHashDuplicationCriterionInput(it: Map<String, *>?): PHashDuplicationCriterionInput? {
    return if (it != null) {
        PHashDuplicationCriterionInput(
            Optional.presentIfNotNull(it["duplicated"]?.toString()?.toBoolean()),
            Optional.presentIfNotNull(it["distance"]?.toString()?.toInt()),
        )
    } else {
        null
    }
}

fun convertToResolutionEnum(str: String): ResolutionEnum {
    return when (str) {
        "114p" -> ResolutionEnum.VERY_LOW
        "240p" -> ResolutionEnum.LOW
        "360p" -> ResolutionEnum.R360P
        "480p" -> ResolutionEnum.STANDARD
        "540p" -> ResolutionEnum.WEB_HD
        "720p" -> ResolutionEnum.STANDARD_HD
        "1080p" -> ResolutionEnum.FULL_HD
        "1440p" -> ResolutionEnum.QUAD_HD
        "4k" -> ResolutionEnum.FOUR_K
        "5k" -> ResolutionEnum.FIVE_K
        "6k" -> ResolutionEnum.SIX_K
        "7k" -> ResolutionEnum.SEVEN_K
        "8k" -> ResolutionEnum.EIGHT_K
        "Huge" -> ResolutionEnum.HUGE
        else -> ResolutionEnum.UNKNOWN__
    }
}

fun convertResolutionCriterionInput(it: Map<String, *>?): ResolutionCriterionInput? {
    return if (it != null) {
        ResolutionCriterionInput(
            convertToResolutionEnum(it["value"].toString()),
            CriterionModifier.valueOf(it["modifier"]!! as String),
        )
    } else {
        null
    }
}

fun convertPerformerObjectFilter(f: Any?): PerformerFilterType? {
    return if (f != null) {
        val filter = f as Map<String, Map<String, *>>
        PerformerFilterType(
            AND = Optional.presentIfNotNull(convertPerformerObjectFilter(filter.get("AND") as Map<String, Map<String, *>>?)),
            OR = Optional.presentIfNotNull(convertPerformerObjectFilter(filter.get("OR") as Map<String, Map<String, *>>?)),
            NOT = Optional.presentIfNotNull(convertPerformerObjectFilter(filter.get("NOT") as Map<String, Map<String, *>>?)),
            name = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("name"))),
            disambiguation = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("disambiguation"))),
            details = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("details"))),
            filter_favorites = Optional.presentIfNotNull(convertBoolean(filter.get("filter_favorites"))),
            birth_year = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("birth_year"))),
            age = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("age"))),
            ethnicity = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("ethnicity"))),
            country = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("country"))),
            eye_color = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("eye_color"))),
            height_cm = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("height_cm"))),
            measurements = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("measurements"))),
            fake_tits = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("fake_tits"))),
            penis_length = Optional.presentIfNotNull(convertFloatCriterionInput(filter.get("penis_length"))),
            circumcised = Optional.presentIfNotNull(convertCircumcisionCriterionInput(filter.get("circumcised"))),
            career_length = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("career_length"))),
            tattoos = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("tattoos"))),
            piercings = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("piercings"))),
            aliases = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("aliases"))),
            gender = Optional.presentIfNotNull(convertGenderCriterionInput(filter.get("gender"))),
            is_missing = Optional.presentIfNotNull(convertString(filter.get("is_missing"))),
            tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter.get("tags"))),
            tag_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("tag_count"))),
            scene_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("scene_count"))),
            image_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("image_count"))),
            gallery_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("gallery_count"))),
            o_counter = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("o_counter"))),
            stash_id_endpoint = Optional.presentIfNotNull(convertStashIDCriterionInput(filter.get("stash_id_endpoint"))),
            rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("rating100"))),
            url = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("url"))),
            hair_color = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("hair_color"))),
            weight = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("weight"))),
            death_year = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("death_year"))),
            studios = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter.get("studios"))),
            performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter.get("performers"))),
            ignore_auto_tag = Optional.presentIfNotNull(convertBoolean(filter.get("ignore_auto_tag"))),
            birthdate = Optional.presentIfNotNull(convertDateCriterionInput(filter.get("birthdate"))),
            death_date = Optional.presentIfNotNull(convertDateCriterionInput(filter.get("death_date"))),
            created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("created_at"))),
            updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("updated_at"))),
        )
    } else {
        null
    }
}

fun convertSceneObjectFilter(f: Any?): SceneFilterType? {
    return if (f != null) {
        val filter = f as Map<String, Map<String, *>>
        SceneFilterType(
            AND = Optional.presentIfNotNull(convertSceneObjectFilter(filter.get("AND") as Map<String, Map<String, *>>?)),
            OR = Optional.presentIfNotNull(convertSceneObjectFilter(filter.get("OR") as Map<String, Map<String, *>>?)),
            NOT = Optional.presentIfNotNull(convertSceneObjectFilter(filter.get("NOT") as Map<String, Map<String, *>>?)),
            id = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("id"))),
            title = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("title"))),
            code = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("code"))),
            details = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("details"))),
            director = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("director"))),
            oshash = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("oshash"))),
            checksum = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("checksum"))),
            phash = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("phash"))),
            phash_distance =
                Optional.presentIfNotNull(
                    convertPhashDistanceCriterionInput(
                        filter.get(
                            "phash_distance",
                        ),
                    ),
                ),
            path = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("path"))),
            file_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("file_count"))),
            rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("rating100"))),
            organized = Optional.presentIfNotNull(convertBoolean(filter.get("organized"))),
            o_counter = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("o_counter"))),
            duplicated =
                Optional.presentIfNotNull(
                    convertPHashDuplicationCriterionInput(
                        filter.get(
                            "duplicated",
                        ),
                    ),
                ),
            resolution = Optional.presentIfNotNull(convertResolutionCriterionInput(filter.get("resolution"))),
            framerate = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("framerate"))),
            video_codec = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("video_codec"))),
            audio_codec = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("audio_codec"))),
            duration = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("duration"))),
            has_markers = Optional.presentIfNotNull(convertString(filter.get("has_markers"))),
            is_missing = Optional.presentIfNotNull(convertString(filter.get("is_missing"))),
            studios = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter.get("studios"))),
            movies = Optional.presentIfNotNull(convertMultiCriterionInput(filter.get("movies"))),
            tags = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter.get("tags"))),
            tag_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("tag_count"))),
            performer_tags =
                Optional.presentIfNotNull(
                    convertHierarchicalMultiCriterionInput(
                        filter.get(
                            "performer_tags",
                        ),
                    ),
                ),
            performer_favorite = Optional.presentIfNotNull(convertBoolean(filter.get("performer_favorite"))),
            performer_age = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("performer_age"))),
            performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter.get("performers"))),
            performer_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("performer_count"))),
            stash_id_endpoint = Optional.presentIfNotNull(convertStashIDCriterionInput(filter.get("stash_id_endpoint"))),
            url = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("url"))),
            interactive = Optional.presentIfNotNull(convertBoolean(filter.get("interactive"))),
            interactive_speed = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("interactive_speed"))),
            captions = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("captions"))),
            resume_time = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("resume_time"))),
            play_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("play_count"))),
            play_duration = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("play_duration"))),
            date = Optional.presentIfNotNull(convertDateCriterionInput(filter.get("date"))),
            created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("created_at"))),
            updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("updated_at"))),
        )
    } else {
        null
    }
}

fun convertStudioObjectFilter(f: Any?): StudioFilterType? {
    return if (f != null) {
        val filter = f as Map<String, Map<String, *>>
        StudioFilterType(
            AND = Optional.presentIfNotNull(convertStudioObjectFilter(filter.get("AND") as Map<String, Map<String, *>>?)),
            OR = Optional.presentIfNotNull(convertStudioObjectFilter(filter.get("OR") as Map<String, Map<String, *>>?)),
            NOT = Optional.presentIfNotNull(convertStudioObjectFilter(filter.get("NOT") as Map<String, Map<String, *>>?)),
            name = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("name"))),
            details = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("details"))),
            parents = Optional.presentIfNotNull(convertMultiCriterionInput(filter.get("parents"))),
            stash_id_endpoint = Optional.presentIfNotNull(convertStashIDCriterionInput(filter.get("stash_id_endpoint"))),
            is_missing = Optional.presentIfNotNull(convertString(filter.get("is_missing"))),
            rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("rating100"))),
            scene_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("scene_count"))),
            image_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("image_count"))),
            gallery_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("gallery_count"))),
            url = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("url"))),
            aliases = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("aliases"))),
            ignore_auto_tag = Optional.presentIfNotNull(convertBoolean(filter.get("ignore_auto_tag"))),
            created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("created_at"))),
            updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("updated_at"))),
        )
    } else {
        null
    }
}

fun convertTagObjectFilter(f: Any?): TagFilterType? {
    return if (f != null) {
        val filter = f as Map<String, Map<String, *>>
        TagFilterType(
            AND = Optional.presentIfNotNull(convertTagObjectFilter(filter.get("AND") as Map<String, Map<String, *>>?)),
            OR = Optional.presentIfNotNull(convertTagObjectFilter(filter.get("OR") as Map<String, Map<String, *>>?)),
            NOT = Optional.presentIfNotNull(convertTagObjectFilter(filter.get("NOT") as Map<String, Map<String, *>>?)),
            name = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("name"))),
            aliases = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("aliases"))),
            description = Optional.presentIfNotNull(convertStringCriterionInput(filter.get("description"))),
            is_missing = Optional.presentIfNotNull(convertString(filter.get("is_missing"))),
            scene_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("scene_count"))),
            image_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("image_count"))),
            gallery_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("gallery_count"))),
            performer_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("performer_count"))),
            marker_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("marker_count"))),
            parents = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter.get("parents"))),
            children =
                Optional.presentIfNotNull(
                    convertHierarchicalMultiCriterionInput(
                        filter.get(
                            "children",
                        ),
                    ),
                ),
            parent_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("parent_count"))),
            child_count = Optional.presentIfNotNull(convertIntCriterionInput(filter.get("child_count"))),
            ignore_auto_tag = Optional.presentIfNotNull(convertBoolean(filter.get("ignore_auto_tag"))),
            created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("created_at"))),
            updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter.get("updated_at"))),
        )
    } else {
        null
    }
}

fun convertMovieObjectFilter(f: Any?): MovieFilterType? {
    return if (f != null) {
        val filter = f as Map<String, Map<String, *>>
        MovieFilterType(
            name = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("name"))),
            director = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("director"))),
            synopsis = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("synopsis"))),
            duration = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("duration"))),
            rating100 = Optional.presentIfNotNull(convertIntCriterionInput(filter?.get("rating100"))),
            studios = Optional.presentIfNotNull(convertHierarchicalMultiCriterionInput(filter?.get("studios"))),
            is_missing = Optional.presentIfNotNull(convertString(filter?.get("is_missing"))),
            url = Optional.presentIfNotNull(convertStringCriterionInput(filter?.get("url"))),
            performers = Optional.presentIfNotNull(convertMultiCriterionInput(filter?.get("performers"))),
            date = Optional.presentIfNotNull(convertDateCriterionInput(filter?.get("date"))),
            created_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter?.get("created_at"))),
            updated_at = Optional.presentIfNotNull(convertTimestampCriterionInput(filter?.get("updated_at"))),
        )
    } else {
        null
    }
}
