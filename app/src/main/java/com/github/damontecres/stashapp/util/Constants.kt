package com.github.damontecres.stashapp.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Adapter
import android.widget.FrameLayout
import android.widget.ListAdapter
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.widget.NestedScrollView
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.Visibility
import androidx.preference.PreferenceManager
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.chrynan.parcelable.core.getParcelable
import com.chrynan.parcelable.core.getParcelableExtra
import com.chrynan.parcelable.core.putExtra
import com.github.damontecres.stashapp.ImageActivity
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.ServerInfoQuery
import com.github.damontecres.stashapp.api.fragment.FullSceneData
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.SlimTagData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.fragment.VideoFileData
import com.github.damontecres.stashapp.api.fragment.VideoSceneData
import com.github.damontecres.stashapp.data.DataType
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
import javax.net.ssl.SSLHandshakeException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Originally this file was for constant values (such as API Key header name).
 *
 * Now it's mostly a dumping ground for various extension functions
 */
object Constants {
    /**
     * The name of the header for authenticating to Stash
     */
    const val STASH_API_HEADER = "ApiKey"
    const val TAG = "Constants"
    private const val OK_HTTP_CACHE_DIR = "okhttpcache"

    const val ARG = "arg"
    const val SCENE_ARG = "$ARG.scene"
    const val SCENE_ID_ARG = "$ARG.scene.id"
    const val POSITION_ARG = "$ARG.position"

    fun getNetworkCache(context: Context): Cache {
        val cacheSize =
            PreferenceManager.getDefaultSharedPreferences(context)
                .getLong("networkCache", 100) * 1024 * 1024
        return Cache(File(context.cacheDir, OK_HTTP_CACHE_DIR), cacheSize)
    }
}

fun joinValueNotNull(
    prefix: String,
    value: String?,
): String? {
    return if (value.isNotNullOrBlank()) {
        "$prefix/$value"
    } else {
        null
    }
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
                .addHeader(STASH_API_HEADER, apiKey.trim())
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

enum class TestResultStatus {
    SUCCESS,
    AUTH_REQUIRED,
    ERROR,
    UNSUPPORTED_VERSION,
    SSL_REQUIRED,
    SELF_SIGNED_REQUIRED,
}

data class TestResult(val status: TestResultStatus, val serverInfo: ServerInfoQuery.Data?) {
    constructor(status: TestResultStatus) : this(status, null)
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
            Toast.makeText(
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
                    if (showToast) {
                        val version = info.data?.version?.version
                        Toast.makeText(
                            context,
                            "Connected to unsupported Stash version $version!",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    return TestResult(TestResultStatus.UNSUPPORTED_VERSION, info.data)
                } else {
                    if (showToast) {
                        val version = info.data?.version?.version
                        val sceneCount = info.data?.findScenes?.count
                        Toast.makeText(
                            context,
                            "Connected to Stash ($version) with $sceneCount scenes!",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    return TestResult(TestResultStatus.SUCCESS, info.data)
                }
            } else if (info.exception != null) {
                when (val ex = info.exception) {
                    is ApolloHttpException -> {
                        Log.e(Constants.TAG, "ApolloHttpException", ex)
                        if (ex.statusCode == 400) {
                            // Server returns 400 with body "Client sent an HTTP request to an HTTPS server.", but apollo doesn't record the body
                            if (showToast) {
                                Toast.makeText(
                                    context,
                                    "Connected to server, but server may require using HTTPS.",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                            return TestResult(TestResultStatus.SSL_REQUIRED)
                        } else if (ex.statusCode == 401 || ex.statusCode == 403) {
                            if (showToast) {
                                Toast.makeText(
                                    context,
                                    "Connected to server, but an API key is required.",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                            return TestResult(TestResultStatus.AUTH_REQUIRED)
                        } else {
                            if (showToast) {
                                Toast.makeText(
                                    context,
                                    "Connected to Stash, but got HTTP ${ex.statusCode}: '${ex.message}'",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                            return TestResult(TestResultStatus.ERROR)
                        }
                    }

                    is ApolloException -> {
                        Log.e(Constants.TAG, "ApolloException", ex)
                        if (showToast) {
                            val message =
                                when (val cause = ex.cause) {
                                    is UnknownHostException, is ConnectException -> cause.localizedMessage
                                    is SSLHandshakeException -> "server may be using a self-signed certificate"
                                    is IOException -> cause.localizedMessage
                                    else -> ex.localizedMessage
                                }
                            Toast.makeText(
                                context,
                                "Failed to connect to Stash: $message",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                        if (ex.cause is SSLHandshakeException) {
                            return TestResult(TestResultStatus.SELF_SIGNED_REQUIRED)
                        }
                    }

                    else -> {
                        Log.e(Constants.TAG, "Exception", ex)
                        if (showToast) {
                            Toast.makeText(
                                context,
                                "Failed to connect to Stash: ${ex?.message}",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                        return TestResult(TestResultStatus.ERROR)
                    }
                }
            } else {
                if (showToast) {
                    Toast.makeText(
                        context,
                        "Connected to Stash, but server returned an error: ${info.errors}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                Log.w(Constants.TAG, "Errors in ServerInfoQuery: ${info.errors}")
            }
        } catch (ex: Exception) {
            Log.e(Constants.TAG, "Exception", ex)
            if (showToast) {
                Toast.makeText(
                    context,
                    "Failed to connect to Stash: ${ex.message}",
                    Toast.LENGTH_LONG,
                ).show()
            }
            return TestResult(TestResultStatus.ERROR)
        }
    }
    return TestResult(TestResultStatus.ERROR)
}

/**
 * Gets the value for the key trying first the key as provided and next the key lower cased
 */
fun <V> Map<*, V>.getCaseInsensitive(k: String?): V? {
    if (k == null) {
        return null
    }
    return this[k] ?: this[k.lowercase()]
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
): String {
    return strings.filter { it.isNotNullOrBlank() }.joinToString(sep)
}

fun concatIfNotBlank(
    sep: CharSequence,
    strings: List<CharSequence?>,
): String {
    return strings.joinNotNullOrBlank(sep)
}

fun List<CharSequence?>.joinNotNullOrBlank(sep: CharSequence): String {
    return this.filter { it.isNotNullOrBlank() }.joinToString(sep)
}

fun cacheDurationPrefToDuration(value: Int): Duration? {
    return when (value) {
        0 -> null
        1 -> 1.toDuration(DurationUnit.HOURS)
        2 -> 4.toDuration(DurationUnit.HOURS)
        3 -> 12.toDuration(DurationUnit.HOURS)
        else -> (value - 3).toDuration(DurationUnit.DAYS)
    }
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
            val path = files.firstOrNull()?.videoFileData?.path
            path?.fileNameFromPath
        } else {
            title
        }

val SlimSceneData.titleOrFilename: String?
    get() =
        if (title.isNullOrBlank()) {
            val path = files.firstOrNull()?.videoFileData?.path
            path?.fileNameFromPath
        } else {
            title
        }

val VideoSceneData.titleOrFilename: String?
    get() =
        if (title.isNullOrBlank()) {
            val path = files.firstOrNull()?.videoFileData?.path
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
            rating100 = this.rating100,
            o_counter = this.o_counter,
            organized = this.organized,
            resume_time = this.resume_time,
            created_at = this.created_at,
            updated_at = this.updated_at,
            files = this.files.map { SlimSceneData.File(it.__typename, it.videoFileData) },
            paths =
                SlimSceneData.Paths(
                    screenshot = this.paths.screenshot,
                    preview = this.paths.preview,
                    stream = this.paths.stream,
                    sprite = this.paths.sprite,
                    caption = this.paths.caption,
                ),
            sceneStreams =
                this.sceneStreams.map {
                    SlimSceneData.SceneStream(
                        it.url,
                        it.mime_type,
                        it.label,
                    )
                },
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
            captions =
                this.captions?.map {
                    SlimSceneData.Caption(
                        it.language_code,
                        it.caption_type,
                    )
                },
        )

val FullSceneData.asVideoSceneData: VideoSceneData
    get() =
        VideoSceneData(
            id,
            title,
            urls,
            date,
            rating100,
            o_counter,
            created_at,
            updated_at,
            files.map { VideoSceneData.File("", it.videoFileData) },
            VideoSceneData.Paths(paths.screenshot, paths.preview, paths.stream, paths.sprite),
            sceneStreams.map { VideoSceneData.SceneStream(it.url, it.mime_type, it.label) },
        )

val TagData.asSlimTagData: SlimTagData
    get() = SlimTagData(id, name, description, favorite, image_path)

val PerformerData.ageInYears: Int?
    @RequiresApi(Build.VERSION_CODES.O)
    get() =
        if (birthdate != null) {
            Period.between(
                LocalDate.parse(birthdate, DateTimeFormatter.ISO_LOCAL_DATE),
                LocalDate.now(),
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

fun ScrollView.onlyScrollIfNeeded() {
    viewTreeObserver.addOnGlobalLayoutListener {
        val childHeight = getChildAt(0).height
        val isScrollable =
            height < childHeight + paddingTop + paddingBottom
        isFocusable = isScrollable
    }
}

fun NestedScrollView.onlyScrollIfNeeded() {
    viewTreeObserver.addOnGlobalLayoutListener {
        val childHeight = getChildAt(0).height
        val isScrollable =
            height < childHeight + paddingTop + paddingBottom
        isFocusable = isScrollable
    }
}

fun SharedPreferences.getStringNotNull(
    key: String,
    defValue: String,
): String {
    return getString(key, defValue)!!
}

fun SharedPreferences.getInt(
    key: String,
    defValue: String,
): Int {
    return getStringNotNull(key, defValue).toInt()
}

fun ArrayObjectAdapter.isEmpty(): Boolean = size() == 0

fun ArrayObjectAdapter.isNotEmpty(): Boolean = !isEmpty()

val ImageData.maxFileSize: Int
    get() =
        visual_files.maxOfOrNull {
            it.onBaseFile?.size?.toString()?.toInt() ?: -1
        } ?: -1

fun ImageData.addToIntent(intent: Intent): Intent {
    intent.putExtra(ImageActivity.INTENT_IMAGE_ID, id)
    intent.putExtra(ImageActivity.INTENT_IMAGE_URL, paths.image)
    intent.putExtra(ImageActivity.INTENT_IMAGE_SIZE, maxFileSize)
    return intent
}

fun showSetRatingToast(
    context: Context,
    rating100: Int,
    ratingsAsStars: Boolean? = null,
) {
    val asStars =
        ratingsAsStars ?: StashServer.requireCurrentServer().serverPreferences.ratingsAsStars
    val ratingStr = getRatingString(rating100, asStars)
    Toast.makeText(
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
val SlimSceneData.resume_position get() = resume_time?.times(1000L)?.toLong()

val Long.toMilliseconds get() = this / 1000.0

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

fun VideoFileData.resolutionName(): CharSequence {
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

/**
 * Gets a sort by string for a random sort
 */
fun getRandomSort(): String {
    return "random_" + Random.nextInt(1e8.toInt()).toString()
}

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
): Job {
    return if (exceptionHandler == null) {
        launch(Dispatchers.IO, block = block)
    } else {
        launch(Dispatchers.IO + exceptionHandler, block = block)
    }
}

fun Intent.putDataType(dataType: DataType): Intent {
    return this.putExtra("dataType", dataType.name)
}

fun Intent.getDataType(): DataType {
    return DataType.valueOf(getStringExtra("dataType")!!)
}

@OptIn(ExperimentalSerializationApi::class)
fun Intent.putFilterArgs(
    name: String,
    filterArgs: FilterArgs,
): Intent {
    return putExtra(name, filterArgs, StashParcelable)
}

@OptIn(ExperimentalSerializationApi::class)
fun Intent.getFilterArgs(name: String): FilterArgs? {
    return getParcelableExtra(name, FilterArgs::class, 0, StashParcelable)
}

@OptIn(ExperimentalSerializationApi::class)
fun Bundle.getFilterArgs(name: String): FilterArgs? {
    return getParcelable(name, FilterArgs::class, 0, StashParcelable)
}

fun experimentalFeaturesEnabled(): Boolean {
    val context = StashApplication.getApplication()
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean(context.getString(R.string.pref_key_experimental_features), false)
}

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
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getStringSet(context.getString(prefKey), defaultValues)!!
}
