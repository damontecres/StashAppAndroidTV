package com.github.damontecres.stashapp.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Adapter
import android.widget.FrameLayout
import android.widget.ListAdapter
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.Visibility
import androidx.preference.PreferenceManager
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.chrynan.parcelable.core.getParcelable
import com.chrynan.parcelable.core.putParcelable
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.ServerInfoQuery
import com.github.damontecres.stashapp.api.fragment.FullMarkerData
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MinimalSceneData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.SlimTagData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.fragment.VideoFile
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.presenters.ClassPresenterSelector
import com.github.damontecres.stashapp.presenters.ImagePresenter
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.proto.TabPreferences
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.Constants.STASH_API_HEADER
import com.github.damontecres.stashapp.views.fileNameFromPath
import com.github.damontecres.stashapp.views.getRatingString
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.Cache
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.net.ssl.SSLHandshakeException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Originally this file was for constant values (such as API Key header name).
 *
 * Now it's mostly a dumping ground for various extension functions
 */
object Constants {
    private const val REQUEST_KEY = "requestKey"

    const val POSITION_REQUEST_KEY = "$REQUEST_KEY.position"

    /**
     * The name of the header for authenticating to Stash
     */
    const val STASH_API_HEADER = "ApiKey"
    const val TAG = "Constants"
    private const val OK_HTTP_CACHE_DIR = "okhttpcache"

    fun getNetworkCache(context: Context): Cache {
        val cacheSize =
            PreferenceManager
                .getDefaultSharedPreferences(context)
                .getInt(
                    context.getString(R.string.pref_key_network_cache_size),
                    100,
                ) * 1024L * 1024L
        return Cache(File(context.cacheDir, OK_HTTP_CACHE_DIR), cacheSize)
    }
}

fun joinValueNotNull(
    prefix: String,
    value: String?,
): String? =
    if (value.isNotNullOrBlank()) {
        "$prefix/$value"
    } else {
        null
    }

/**
 * Create a [GlideUrl], adding the API key to the headers if needed
 */
fun createGlideUrl(
    url: String,
    apiKey: String?,
): GlideUrl =
    if (apiKey.isNullOrBlank()) {
        GlideUrl(url)
    } else {
        GlideUrl(
            url,
            LazyHeaders
                .Builder()
                .addHeader(STASH_API_HEADER, apiKey.trim())
                .build(),
        )
    }

/**
 * Create a [GlideUrl], adding the API key to the headers if needed
 */
fun createGlideUrl(
    url: String,
    context: Context,
): GlideUrl {
    val apiKey =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getString("stashApiKey", "")
    return createGlideUrl(url, apiKey)
}

sealed interface TestResult {
    val message: String

    data class Success(
        val serverInfo: ServerInfoQuery.Data,
    ) : TestResult {
        override val message: String = ""
    }

    data object AuthRequired : TestResult {
        override val message: String = "Credentials are required"
    }

    data class Error(
        val errorMessage: String?,
        val exception: Exception?,
    ) : TestResult {
        override val message: String
            get() = errorMessage ?: exception?.localizedMessage ?: "Unknown Error"
    }

    data class UnsupportedVersion(
        val serverVersion: String?,
    ) : TestResult {
        override val message: String = "Unsupported server version: $serverVersion"
    }

    data object SslRequired : TestResult {
        override val message: String = "HTTPS may be required"
    }

    data object SelfSignedCertRequired : TestResult {
        override val message: String = "Possible self signed cert detected"
    }
}

/**
 * Test the connection to the server using the specified [ApolloClient]
 *
 * The client should have been configured with the server URL and API key if needed
 */
suspend fun testStashConnection(
    context: Context,
    showToast: Boolean,
    client: ApolloClient?,
): TestResult {
    if (client == null) {
        if (showToast) {
            Toast
                .makeText(
                    context,
                    "Stash server URL is not set.",
                    Toast.LENGTH_LONG,
                ).show()
        }
    } else {
        try {
            val info =
                withContext(Dispatchers.IO) {
                    client.query(ServerInfoQuery()).execute()
                }

            if (info.data != null) {
                val serverVersion = Version.tryFromString(info.data?.version?.version)
                if (!Version.isStashVersionSupported(serverVersion)) {
                    val version = info.data?.version?.version
                    if (showToast) {
                        Toast
                            .makeText(
                                context,
                                "Connected to unsupported Stash version $version!",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                    return TestResult.UnsupportedVersion(version)
                } else {
                    if (showToast) {
                        val version = info.data?.version?.version
                        val sceneCount = info.data?.findScenes?.count
                        Toast
                            .makeText(
                                context,
                                "Connected to Stash ($version) with $sceneCount scenes!",
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                    return TestResult.Success(info.data!!)
                }
            } else if (info.exception != null) {
                when (val ex = info.exception) {
                    is ApolloHttpException -> {
                        Log.e(Constants.TAG, "ApolloHttpException", ex)
                        if (ex.statusCode == 400) {
                            // Server returns 400 with body "Client sent an HTTP request to an HTTPS server.", but apollo doesn't record the body
                            if (showToast) {
                                Toast
                                    .makeText(
                                        context,
                                        "Connected to server, but server may require using HTTPS.",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            }
                            return TestResult.SslRequired
                        } else if (ex.statusCode == 401 || ex.statusCode == 403) {
                            if (showToast) {
                                Toast
                                    .makeText(
                                        context,
                                        "Can connect to server, but API key is required or is incorrect.",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            }
                            return TestResult.AuthRequired
                        } else {
                            if (showToast) {
                                Toast
                                    .makeText(
                                        context,
                                        "Connected to Stash, but got HTTP ${ex.statusCode}: '${ex.message}'",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            }
                            return TestResult.Error("HTTP ${ex.statusCode}: '${ex.message}'", ex)
                        }
                    }

                    is ApolloException -> {
                        Log.e(Constants.TAG, "ApolloException", ex)
                        val message =
                            when (val cause = ex.cause) {
                                is UnknownHostException, is ConnectException -> cause.localizedMessage
                                is SSLHandshakeException -> "server may be using a self-signed certificate"
                                // TODO handle case where cert is for a different host
//                                is SSLPeerUnverifiedException->
                                is IOException -> cause.localizedMessage
                                else -> ex.localizedMessage
                            }
                        if (showToast) {
                            Toast
                                .makeText(
                                    context,
                                    "Failed to connect to Stash: $message",
                                    Toast.LENGTH_LONG,
                                ).show()
                        }
                        if (ex.cause is SSLHandshakeException) {
                            return TestResult.SelfSignedCertRequired
                        } else {
                            return TestResult.Error(message, ex)
                        }
                    }

                    else -> {
                        Log.e(Constants.TAG, "Exception", ex)
                        if (showToast) {
                            Toast
                                .makeText(
                                    context,
                                    "Failed to connect to Stash: ${ex?.message}",
                                    Toast.LENGTH_LONG,
                                ).show()
                        }
                        return TestResult.Error(null, ex)
                    }
                }
            } else {
                if (showToast) {
                    Toast
                        .makeText(
                            context,
                            "Connected to Stash, but server returned an error: ${info.errors}",
                            Toast.LENGTH_LONG,
                        ).show()
                }
                Log.w(Constants.TAG, "Errors in ServerInfoQuery: ${info.errors}")
                return TestResult.Error("Errors: ${info.errors?.joinToString(", ")}", null)
            }
        } catch (ex: Exception) {
            Log.e(Constants.TAG, "Exception", ex)
            if (showToast) {
                Toast
                    .makeText(
                        context,
                        "Failed to connect to Stash: ${ex.message}",
                        Toast.LENGTH_LONG,
                    ).show()
            }
            return TestResult.Error(null, ex)
        }
    }
    return TestResult.Error("Unknown error", null)
}

/**
 * Gets the value for the key trying first the key as provided and next the key lower cased
 */
fun <V> Map<*, V>.getCaseInsensitive(k: String?): V? {
    if (k == null) {
        return null
    }
    return this[k] ?: this[k.lowercase()] ?: this[k.uppercase()]
}

fun TextView.enableMarquee(selected: Boolean = false) {
    marqueeRepeatLimit = -1
    ellipsize = TextUtils.TruncateAt.MARQUEE
    isSingleLine = true
    isSelected = selected
}

fun concatIfNotBlank(
    sep: CharSequence,
    vararg strings: CharSequence?,
): String = strings.filter { it.isNotNullOrBlank() }.joinToString(sep)

fun concatIfNotBlank(
    sep: CharSequence,
    strings: List<CharSequence?>,
): String = strings.joinNotNullOrBlank(sep)

fun List<CharSequence?>.joinNotNullOrBlank(sep: CharSequence): String = this.filter { it.isNotNullOrBlank() }.joinToString(sep)

fun listOfNotNullOrBlank(vararg strings: CharSequence?): List<String> =
    strings.filter { it.isNotNullOrBlank() }.map { it.toString() }.toList()

fun cacheDurationPrefToDuration(value: Int): Duration? =
    when (value) {
        0 -> null
        1 -> 1.toDuration(DurationUnit.HOURS)
        2 -> 4.toDuration(DurationUnit.HOURS)
        3 -> 12.toDuration(DurationUnit.HOURS)
        else -> (value - 3).toDuration(DurationUnit.DAYS)
    }

fun convertDpToPixel(
    context: Context,
    dp: Int,
): Int {
    val density = context.applicationContext.resources.displayMetrics.density
    return (dp.toFloat() * density).roundToInt()
}

@OptIn(ExperimentalContracts::class)
fun CharSequence?.isNotNullOrBlank(): Boolean {
    contract {
        returns(true) implies (this@isNotNullOrBlank != null)
    }
    return !this.isNullOrBlank()
}

/**
 * Gets the scene's title if not null otherwise the first file's name
 */
val FullSceneData.titleOrFilename: String?
    get() =
        if (title.isNullOrBlank()) {
            val path = files.firstOrNull()?.videoFile?.path
            path?.fileNameFromPath
        } else {
            title
        }

val SlimSceneData.titleOrFilename: String?
    get() =
        if (title.isNullOrBlank()) {
            val path = files.firstOrNull()?.videoFile?.path
            path?.fileNameFromPath
        } else {
            title
        }

val VideoSceneData.titleOrFilename: String?
    get() =
        if (title.isNullOrBlank()) {
            val path = files.firstOrNull()?.videoFile?.path
            path?.fileNameFromPath
        } else {
            title
        }

val MinimalSceneData.titleOrFilename: String?
    get() =
        if (title.isNullOrBlank()) {
            val path = files.firstOrNull()?.videoFile?.path
            path?.fileNameFromPath
        } else {
            title
        }

val ImageData.titleOrFilename: String?
    get() =
        if (title.isNullOrBlank()) {
            val path = visual_files.firstOrNull()?.onBaseFile?.path
            path?.fileNameFromPath
        } else {
            title
        }

val FullSceneData.asSlimeSceneData: SlimSceneData
    get() =
        SlimSceneData(
            id = this.id,
            title = this.title,
            code = this.code,
            details = this.details,
            director = this.director,
            urls = this.urls,
            date = this.date,
            play_count = play_count,
            play_duration = play_duration,
            rating100 = this.rating100,
            o_counter = this.o_counter,
            organized = this.organized,
            resume_time = this.resume_time,
            created_at = this.created_at,
            updated_at = this.updated_at,
            files = this.files.map { SlimSceneData.File(it.__typename, it.videoFile) },
            paths =
                SlimSceneData.Paths(
                    screenshot = this.paths.screenshot,
                    preview = this.paths.preview,
                    stream = this.paths.stream,
                    sprite = this.paths.sprite,
                    caption = this.paths.caption,
                ),
            scene_markers = this.scene_markers.map { SlimSceneData.Scene_marker(it.id, it.title) },
            galleries = this.galleries.map { SlimSceneData.Gallery(it.id, it.title) },
            studio =
                if (this.studio != null) {
                    SlimSceneData.Studio(
                        this.studio.studioData.id,
                        this.studio.studioData.name,
                        this.studio.studioData.image_path,
                    )
                } else {
                    null
                },
            groups =
                this.groups.map {
                    SlimSceneData.Group(
                        SlimSceneData.Group1(
                            it.group.groupData.id,
                            it.group.groupData.name,
                        ),
                    )
                },
            tags = this.tags.map { SlimSceneData.Tag("", it.tagData.asSlimTagData) },
            performers = this.performers.map { SlimSceneData.Performer(it.id, it.name) },
        )

val FullSceneData.asVideoSceneData: VideoSceneData
    get() =
        VideoSceneData(
            id,
            title,
            urls,
            date,
            resume_time,
            rating100,
            o_counter,
            files.map { VideoSceneData.File("", it.videoFile) },
            VideoSceneData.Paths(
                paths.caption,
                paths.screenshot,
                paths.preview,
                paths.stream,
                paths.sprite,
            ),
            sceneStreams.map { VideoSceneData.SceneStream(it.url, it.mime_type, it.label) },
            captions?.map { VideoSceneData.Caption("", it.caption) },
        )

val FullSceneData.asMinimalSceneData: MinimalSceneData
    get() =
        MinimalSceneData(
            id,
            title,
            urls,
            date,
            rating100,
            o_counter,
            files.map { MinimalSceneData.File("", it.videoFile) },
        )

val TagData.asSlimTagData: SlimTagData
    get() = SlimTagData(id, name)

val PerformerData.ageInYears: Int?
    @RequiresApi(Build.VERSION_CODES.O)
    get() =
        if (birthdate != null) {
            Period
                .between(
                    LocalDate.parse(birthdate, DateTimeFormatter.ISO_LOCAL_DATE),
                    if (death_date.isNotNullOrBlank()) {
                        LocalDate.parse(
                            death_date,
                            DateTimeFormatter.ISO_LOCAL_DATE,
                        )
                    } else {
                        LocalDate.now()
                    },
                ).years
        } else {
            null
        }

val GalleryData.name: String?
    get() =
        if (title.isNotNullOrBlank()) {
            title
        } else if (files.isNotEmpty() && files.first().path.isNotNullOrBlank()) {
            files.first().path.fileNameFromPath
        } else if (folder != null && folder.path.isNotNullOrBlank()) {
            folder.path.fileNameFromPath
        } else {
            null
        }

fun FullSceneData.Scene_marker.asMarkerData(scene: FullSceneData): MarkerData =
    MarkerData(
        id = id,
        title = title,
        created_at = created_at,
        updated_at = updated_at,
        stream = stream,
        screenshot = screenshot,
        seconds = seconds,
        end_seconds = end_seconds,
        preview = preview,
        primary_tag = MarkerData.Primary_tag("", primary_tag.tagData.asSlimTagData),
        scene = MarkerData.Scene(scene.id, scene.asMinimalSceneData),
        tags = tags.map { MarkerData.Tag("", it.tagData.asSlimTagData) },
        __typename = "",
    )

fun FullMarkerData.asMarkerData(scene: FullSceneData): MarkerData =
    MarkerData(
        id = id,
        title = title,
        created_at = created_at,
        updated_at = updated_at,
        stream = stream,
        screenshot = screenshot,
        seconds = seconds,
        end_seconds = end_seconds,
        preview = preview,
        primary_tag = MarkerData.Primary_tag("", primary_tag.tagData.asSlimTagData),
        scene = MarkerData.Scene(scene.id, scene.asMinimalSceneData),
        tags = tags.map { MarkerData.Tag("", it.tagData.asSlimTagData) },
        __typename = "",
    )

/**
 * Create a fake [MarkerData] with minimal details
 */
fun fakeMarker(
    tagId: String,
    seconds: Double,
    scene: FullSceneData,
) = MarkerData(
    id = "",
    title = "",
    created_at = "",
    updated_at = "",
    stream = "",
    screenshot = "",
    seconds = seconds,
    end_seconds = null,
    preview = "",
    primary_tag =
        MarkerData.Primary_tag(
            __typename = "",
            slimTagData =
                SlimTagData(
                    id = tagId,
                    name = "",
                ),
        ),
    tags = listOf(),
    scene =
        MarkerData.Scene(
            __typename = "",
            minimalSceneData = scene.asMinimalSceneData,
        ),
    __typename = "",
)

fun ScrollView.onlyScrollIfNeeded() {
    val childHeight = getChildAt(0).height
    val isScrollable =
        height < childHeight + paddingTop + paddingBottom
    isFocusable = isScrollable
    isVerticalScrollBarEnabled = isScrollable
}

fun NestedScrollView.onlyScrollIfNeeded() {
    val childHeight = getChildAt(0).height
    val isScrollable =
        height < childHeight + paddingTop + paddingBottom
    isFocusable = isScrollable
    isVerticalScrollBarEnabled = isScrollable
}

fun SharedPreferences.getStringNotNull(
    key: String,
    defValue: String,
): String = getString(key, defValue)!!

fun SharedPreferences.getInt(
    key: String,
    defValue: String,
): Int = getStringNotNull(key, defValue).toInt()

fun ArrayObjectAdapter.isEmpty(): Boolean = size() == 0

fun ArrayObjectAdapter.isNotEmpty(): Boolean = !isEmpty()

val ImageData.maxFileSize: Int
    get() =
        visual_files.maxOfOrNull {
            it.onBaseFile
                ?.size
                ?.toString()
                ?.toInt() ?: -1
        } ?: -1

fun showSetRatingToast(
    context: Context,
    rating100: Int,
    ratingsAsStars: Boolean? = null,
) {
    val asStars =
        ratingsAsStars ?: StashServer.requireCurrentServer().serverPreferences.ratingsAsStars
    val ratingStr = getRatingString(rating100, asStars)
    Toast
        .makeText(
            context,
            "Set rating to $ratingStr!",
            Toast.LENGTH_SHORT,
        ).show()
}

val ImageData.Visual_file.width: Int?
    get() = onImageFile?.width ?: onVideoFile?.width

val ImageData.Visual_file.height: Int?
    get() = onImageFile?.height ?: onVideoFile?.height

fun View.animateToVisible(durationMs: Long? = null) {
    if (visibility == View.VISIBLE) return

    val duration =
        durationMs ?: resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    animate()
        .alpha(1f)
        .setDuration(duration)
        .setListener(null)
        .withStartAction {
            alpha = 0f
            visibility = View.VISIBLE
        }
}

fun View.animateToInvisible(
    @Visibility targetVisibility: Int = View.INVISIBLE,
    durationMs: Long? = null,
) {
    if (visibility == targetVisibility) return

    val duration =
        durationMs ?: resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    animate()
        .alpha(0f)
        .setDuration(duration)
        .setListener(null)
        .withEndAction {
            alpha = 1f
            visibility = targetVisibility
        }
}

/**
 * Gets the max measured width size for the views produced by an ArrayAdapter
 */
fun getMaxMeasuredWidth(
    context: Context,
    adapter: ListAdapter,
    maxWidth: Int? = null,
    maxWidthFraction: Double? = 0.4,
): Int {
    val widest =
        if (maxWidth != null) {
            maxWidth
        } else if (maxWidthFraction != null && context is Activity) {
            val displayMetrics = DisplayMetrics()
            context.windowManager.defaultDisplay.getMetrics(displayMetrics)
            (displayMetrics.widthPixels * maxWidthFraction).toInt()
        } else {
            Log.w(Constants.TAG, "maxWidthFraction is not null, but couldn't get window size")
            Int.MAX_VALUE
        }

    val tempParent = FrameLayout(context)
    var maxMeasuredWidth = 0
    var itemView: View? = null
    val measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

    for (i in 0 until adapter.count) {
        if (adapter.getItemViewType(i) != Adapter.IGNORE_ITEM_VIEW_TYPE) {
            itemView = adapter.getView(i, itemView, tempParent)
            itemView.measure(measureSpec, measureSpec)
            if (itemView.measuredWidth > maxMeasuredWidth) {
                maxMeasuredWidth = itemView.measuredWidth
            }
        }
    }
    return widest.coerceAtMost(maxMeasuredWidth)
}

/**
 * Gets the [SlimSceneData.resume_time] in milliseconds
 */
val SlimSceneData.resume_position get() = resume_time?.toLongMilliseconds

val FullSceneData.resume_position get() = resume_time?.toLongMilliseconds

val Duration.toSeconds get() = this.inWholeMilliseconds.toSeconds

val Long.toSeconds get() = this / 1000.0

val Double.toLongMilliseconds get() = (this * 1000).toLong()

/**
 * Show a [Toast] on [Dispatchers.Main]
 */
suspend fun showToastOnMain(
    context: Context,
    message: CharSequence,
    length: Int,
) = withContext(Dispatchers.Main) {
    Toast.makeText(context, message, length).show()
}

fun VideoFile.resolutionName() = resolutionName(width, height)

fun ImageData.OnVideoFile.resolutionName() = resolutionName(width, height)

fun resolutionName(
    width: Int,
    height: Int,
): CharSequence {
    val number = if (width > height) height else width
    return if (number >= 6144) {
        "HUGE"
    } else if (number >= 3840) {
        "8K"
    } else if (number >= 3584) {
        "7K"
    } else if (number >= 3000) {
        "6K"
    } else if (number >= 2560) {
        "5K"
    } else if (number >= 1920) {
        "4K"
    } else if (number >= 1440) {
        "1440p"
    } else if (number >= 1080) {
        "1080p"
    } else if (number >= 720) {
        "720p"
    } else if (number >= 540) {
        "540p"
    } else if (number >= 480) {
        "480p"
    } else if (number >= 360) {
        "360p"
    } else if (number >= 240) {
        "240p"
    } else if (number >= 144) {
        "144p"
    } else {
        "${number}p"
    }
}

fun VideoFile.bitRateString(): CharSequence {
    if (bit_rate > (1024 * 1024)) {
        return String.format(Locale.getDefault(), "%.1fMbps", bit_rate / (1024 * 1024.0))
    } else if (bit_rate > (1024)) {
        return String.format(Locale.getDefault(), "%.1fKbps", bit_rate / (1024.0))
    } else {
        return String.format(Locale.getDefault(), "%.1fbps", bit_rate.toDouble())
    }
}

/**
 * Gets a sort by string for a random sort
 */
fun getRandomSort(): Int = Random.nextInt(1e8.toInt())

val ImageData.isImageClip: Boolean
    get() =
        visual_files.firstOrNull()?.onVideoFile != null &&
            visual_files.firstOrNull()?.onVideoFile!!.format != "gif"

/**
 * Launch in the [Dispatchers.IO] context with an optional [CoroutineExceptionHandler] defaulting to [StashCoroutineExceptionHandler]
 */
fun CoroutineScope.launchIO(
    exceptionHandler: CoroutineExceptionHandler? = StashCoroutineExceptionHandler(),
    block: suspend CoroutineScope.() -> Unit,
): Job =
    if (exceptionHandler == null) {
        launch(Dispatchers.IO, block = block)
    } else {
        launch(Dispatchers.IO + exceptionHandler, block = block)
    }

fun Bundle.putDataType(dataType: DataType): Bundle {
    this.putString("dataType", dataType.name)
    return this
}

fun Bundle.getDataType(): DataType = DataType.valueOf(getString("dataType")!!)

@OptIn(ExperimentalSerializationApi::class)
fun Bundle.getFilterArgs(name: String): FilterArgs? = getParcelable(name, FilterArgs::class, 0, StashParcelable)

@OptIn(ExperimentalSerializationApi::class)
fun Bundle.putFilterArgs(
    name: String,
    filterArgs: FilterArgs,
): Bundle {
    putParcelable(name, filterArgs, StashParcelable)
    return this
}

@OptIn(ExperimentalSerializationApi::class)
fun Bundle.putDestination(destination: Destination): Bundle {
    putParcelable(NavigationManager.DESTINATION_ARG, destination, StashParcelable)
    return this
}

@OptIn(ExperimentalSerializationApi::class)
fun <T : Destination> Bundle.maybeGetDestination(): T? =
    getParcelable(NavigationManager.DESTINATION_ARG, Destination::class, 0, StashParcelable) as T?

fun <T : Destination> Bundle.getDestination(): T = maybeGetDestination()!!

@OptIn(ExperimentalSerializationApi::class)
fun <T : Destination> Bundle.getDestination(name: String): T = getParcelable(name, Destination::class, 0, StashParcelable) as T

fun experimentalFeaturesEnabled(): Boolean {
    val context = StashApplication.getApplication()
    return PreferenceManager
        .getDefaultSharedPreferences(context)
        .getBoolean(context.getString(R.string.pref_key_experimental_features), false)
}

fun readOnlyModeEnabled(): Boolean {
    val context = StashApplication.getApplication()
    return PreferenceManager
        .getDefaultSharedPreferences(context)
        .getBoolean(context.getString(R.string.pref_key_read_only_mode), false)
}

fun readOnlyModeDisabled(): Boolean = !readOnlyModeEnabled()

fun showDebugInfo(): Boolean {
    val context = StashApplication.getApplication()
    return PreferenceManager
        .getDefaultSharedPreferences(context)
        .getBoolean(context.getString(R.string.pref_key_show_playback_debug_info), false)
}

fun getUiTabs(
    tabPreferences: TabPreferences,
    dataType: DataType,
) = when (dataType) {
    DataType.PERFORMER -> tabPreferences.performerList
    DataType.GALLERY -> tabPreferences.galleryList
    DataType.GROUP -> tabPreferences.groupList
    DataType.STUDIO -> tabPreferences.studioList
    DataType.TAG -> tabPreferences.tagsList
    else -> throw UnsupportedOperationException("$dataType not supported")
}.toSet()

fun getUiTabs(
    context: Context,
    dataType: DataType,
): Set<String> {
    val prefKey: Int
    val defaultArrayKey: Int
    when (dataType) {
        DataType.PERFORMER -> {
            prefKey = R.string.pref_key_ui_performer_tabs
            defaultArrayKey = R.array.performer_tabs
        }
        DataType.GALLERY -> {
            prefKey = R.string.pref_key_ui_gallery_tabs
            defaultArrayKey = R.array.gallery_tabs
        }

        DataType.GROUP -> {
            prefKey = R.string.pref_key_ui_group_tabs
            defaultArrayKey = R.array.group_tabs
        }

        DataType.STUDIO -> {
            prefKey = R.string.pref_key_ui_studio_tabs
            defaultArrayKey = R.array.studio_tabs
        }

        DataType.TAG -> {
            prefKey = R.string.pref_key_ui_tag_tabs
            defaultArrayKey = R.array.tag_tabs
        }

        else -> throw UnsupportedOperationException("$dataType not supported")
    }
    val defaultValues = context.resources.getStringArray(defaultArrayKey).toSet()
    return PreferenceManager
        .getDefaultSharedPreferences(context)
        .getStringSet(context.getString(prefKey), defaultValues)!!
}

fun Optional.Companion.presentIfNotNullOrBlank(value: String?): Optional<String> = presentIfNotNull(value?.ifBlank { null })

fun maybeStartPlayback(
    context: Context,
    item: Any,
) {
    when (item) {
        is SlimSceneData -> {
            StashApplication.navigationManager.navigate(
                Destination.Playback(
                    item.id,
                    item.resume_position ?: 0L,
                    PlaybackMode.Choose,
                ),
            )
        }

        is MarkerData -> {
            StashApplication.navigationManager.navigate(
                Destination.Playback(
                    item.scene.minimalSceneData.id,
                    (item.seconds * 1000).toLong(),
                    PlaybackMode.Choose,
                ),
            )
        }
    }
}

fun addExtraGridLongClicks(
    ps: ClassPresenterSelector,
    dataType: DataType,
    getFilterPosition: () -> FilterAndPosition,
) {
    when (dataType) {
        DataType.SCENE -> {
            val current = ps.getPresenter(SlimSceneData::class.java) as ScenePresenter
            current.longClickCallBack.addAction(StashPresenter.PopUpItem.PLAY_FROM) { _, item ->
                val (filter, position) = getFilterPosition.invoke()
                if (position >= 0) {
                    StashApplication.navigationManager.navigate(
                        Destination.Playlist(filter, position),
                    )
                }
            }
        }

        DataType.MARKER -> {
            val current = ps.getPresenter(MarkerData::class.java) as MarkerPresenter
            current.longClickCallBack.addAction(StashPresenter.PopUpItem.PLAY_FROM) { _, item ->
                val (filter, position) = getFilterPosition.invoke()
                if (position >= 0) {
                    StashApplication.navigationManager.navigate(
                        Destination.Playlist(filter, position, 30_000L),
                    )
                }
            }
        }

        DataType.IMAGE -> {
            val current = ps.getPresenter(ImageData::class.java) as ImagePresenter
            current.longClickCallBack.addAction(StashPresenter.PopUpItem.PLAY_FROM) { _, item ->
                val (filter, position) = getFilterPosition.invoke()
                if (position >= 0) {
                    StashApplication.navigationManager.navigate(
                        Destination.Slideshow(filter, position, true),
                    )
                }
            }
        }

        else -> {
            // no-op
        }
    }
}

/**
 * Turns a [StashDataFilter] into a more readable string by excluding absent optional fields
 *
 * This is useful for debugging
 */
fun StashDataFilter.toReadableString(newlines: Boolean = false): String =
    buildString {
        append(this@toReadableString::class.simpleName)
        append("(")
        val params =
            this@toReadableString::class
                .declaredMemberProperties
                .mapNotNull { param ->
                    val obj =
                        (param as KProperty1<StashDataFilter, *>).get(this@toReadableString) as Optional<*>
                    val value = obj.getOrNull()
                    val str =
                        if (value != null) {
                            if (value is StashDataFilter) {
                                val str = value.toReadableString()
                                "${param.name}=$str"
                            } else if (value::class.simpleName?.endsWith("CriterionInput") == true) {
                                "${param.name}=${value.toReadableString()}"
                            } else {
                                "${param.name}=$value"
                            }
                        } else {
                            null
                        }
                    if (newlines && str != null) "\t$str" else str
                }
        if (newlines && params.isNotEmpty()) append("\n")
        append(params.joinNotNullOrBlank(if (newlines) ",\n" else ", "))
        if (newlines && params.isNotEmpty()) append("\n")
        append(")")
    }.replace(Optional.absent().toString(), "Absent")

fun Any.toReadableString(newlines: Boolean = false) =
    buildString {
        append(this@toReadableString::class.simpleName)
        append("(")
        val params =
            this@toReadableString::class
                .declaredMemberProperties
                .map { param ->
                    param as KProperty1<Any, *>
                    val value = param.get(this@toReadableString)
                    if (value is Optional<*>) {
                        "${param.name}=${value.getOrNull()}"
                    } else if (value is StashDataFilter) {
                        "${param.name}=${value.toReadableString(newlines)}"
                    } else {
                        "${param.name}=$value"
                    }
                }.joinNotNullOrBlank(", ")
        append(params)
        append(")")
    }

fun Fragment.keepScreenOn(keep: Boolean) {
    requireActivity().keepScreenOn(keep)
}

fun Activity.keepScreenOn(keep: Boolean) {
    Log.v("keepScreenOn", "Keep screen on: $keep")
    if (keep) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

fun getPreference(
    context: Context,
    @StringRes key: Int,
    default: Boolean,
) = PreferenceManager
    .getDefaultSharedPreferences(context)
    .getBoolean(context.getString(key), default)

fun getPreference(
    context: Context,
    @StringRes key: Int,
    default: String,
) = PreferenceManager
    .getDefaultSharedPreferences(context)
    .getString(context.getString(key), default)

fun composeEnabled(context: Context = StashApplication.getApplication()) = getPreference(context, R.string.pref_key_use_compose_ui, true)

fun View.updateLayoutParams(transform: ViewGroup.LayoutParams.() -> Unit) {
    val lp = layoutParams
    transform(layoutParams)
    layoutParams = lp
}

fun calculatePageSize(
    context: Context,
    dataType: DataType,
): Int {
    val cardSize =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getInt("cardSize", context.getString(R.string.card_size_default))
    val numberOfColumns =
        (cardSize * (ScenePresenter.CARD_WIDTH.toDouble() / dataType.defaultCardWidth)).toInt()
    val maxSearchResults =
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .getInt("maxSearchResults", 25)
    val number = (numberOfColumns * 5).coerceAtLeast(maxSearchResults + numberOfColumns * 2)
    val remainder = number % numberOfColumns
    return if (remainder == 0) {
        number
    } else {
        number + (numberOfColumns - remainder)
    }
}

fun Context.findActivity(): Activity? {
    if (this is Activity) {
        return this
    }
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
