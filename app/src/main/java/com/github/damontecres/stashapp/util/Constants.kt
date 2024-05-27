package com.github.damontecres.stashapp.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Adapter
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.widget.NestedScrollView
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.Visibility
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.github.damontecres.stashapp.ImageActivity
import com.github.damontecres.stashapp.api.ServerInfoQuery
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.VideoFileData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.Constants.STASH_API_HEADER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object Constants {
    /**
     * The name of the header for authenticating to Stash
     */
    const val STASH_API_HEADER = "ApiKey"
    const val TAG = "Constants"
    const val OK_HTTP_CACHE_DIR = "okhttpcache"

    fun getNetworkCache(context: Context): Cache {
        val cacheSize =
            PreferenceManager.getDefaultSharedPreferences(context)
                .getLong("networkCache", 100) * 1024 * 1024
        return Cache(File(context.cacheDir, OK_HTTP_CACHE_DIR), cacheSize)
    }
}

fun join(
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
    SSL_REQUIRED,
    SELF_SIGNED_REQUIRED,
}

data class TestResult(val status: TestResultStatus, val serverInfo: ServerInfoQuery.Data?) {
    constructor(status: TestResultStatus) : this(status, null)
}

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
            if (info.hasErrors()) {
                if (showToast) {
                    Toast.makeText(
                        context,
                        "Connected to Stash, but server returned an error: ${info.errors}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                Log.w(Constants.TAG, "Errors in ServerInfoQuery: ${info.errors}")
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
        } catch (ex: ApolloHttpException) {
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
            } else if (ex.statusCode == 500) {
                // In server <0.26.0, the server may return a 500 for incorrect API keys
                if (showToast) {
                    Toast.makeText(
                        context,
                        "Connected to server, but the API key may be incorrect.",
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
        } catch (ex: ApolloException) {
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
    }
    return TestResult(TestResultStatus.ERROR)
}

fun SavedFilterData.Find_filter.toFindFilterType(): FindFilterType {
    return FindFilterType(
        q = Optional.presentIfNotNull(this.q),
        page = Optional.presentIfNotNull(this.page),
        per_page = Optional.presentIfNotNull(this.per_page),
        sort = Optional.presentIfNotNull(this.sort),
        direction = Optional.presentIfNotNull(this.direction),
    )
}

@Suppress("ktlint:standard:function-naming")
fun FindFilterType.toFind_filter(): SavedFilterData.Find_filter {
    return SavedFilterData.Find_filter(
        q = q.getOrNull(),
        page = page.getOrNull(),
        per_page = per_page.getOrNull(),
        sort = sort.getOrNull(),
        direction = direction.getOrNull(),
        __typename = "FindFilterType",
    )
}

val supportedFilterModes = DataType.entries.map { it.filterMode }.toSet()

/**
 * Gets the value for the key trying first the key as provided and next the key lower cased
 */
fun <V> Map<String, V>.getCaseInsensitive(k: String?): V? {
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

fun Context.toPx(dp: Int): Float =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics,
    )

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
val SlimSceneData.titleOrFilename: String?
    get() =
        if (title.isNullOrBlank()) {
            val path = files.firstOrNull()?.videoFileData?.path
            if (path != null) {
                File(path).name
            } else {
                null
            }
        } else {
            title
        }

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
        } else if (folder != null && folder.path.isNotNullOrBlank()) {
            Uri.parse(folder.path).pathSegments.last()
        } else if (files.firstOrNull()?.path.isNotNullOrBlank()) {
            Uri.parse(files.firstOrNull()?.path).pathSegments.last()
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
    val asStars = ratingsAsStars ?: ServerPreferences(context).ratingsAsStars
    val ratingStr =
        if (asStars) {
            (rating100 / 20.0).toString() + " stars"
        } else {
            (rating100 / 10.0).toString()
        }
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
    adapter: ArrayAdapter<String>,
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
    if (adapter.viewTypeCount != 1) {
        throw IllegalStateException("Adapter creates more than 1 type of view")
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
