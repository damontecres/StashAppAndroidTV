package com.github.damontecres.stashapp.util

import android.annotation.SuppressLint
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
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.github.damontecres.stashapp.ImageActivity
import com.github.damontecres.stashapp.api.ServerInfoQuery
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.Constants.OK_HTTP_TAG
import com.github.damontecres.stashapp.util.Constants.STASH_API_HEADER
import com.github.damontecres.stashapp.util.Constants.getNetworkCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object Constants {
    /**
     * The name of the header for authenticating to Stash
     */
    const val STASH_API_HEADER = "ApiKey"
    const val TAG = "Constants"
    const val OK_HTTP_TAG = "$TAG.OkHttpClient"
    const val OK_HTTP_CACHE_DIR = "okhttpcache"

    /**
     * Converts seconds into a Duration string where fractional seconds are removed
     */
    fun durationToString(duration: Double): String {
        return duration
            .times(100L).toLong()
            .div(100L).toDuration(DurationUnit.SECONDS)
            .toString()
    }

    fun getNetworkCache(context: Context): Cache {
        val cacheSize =
            PreferenceManager.getDefaultSharedPreferences(context)
                .getLong("networkCache", 100) * 1024 * 1024
        return Cache(File(context.cacheDir, OK_HTTP_CACHE_DIR), cacheSize)
    }

    fun getRatingAsDecimalString(
        context: Context,
        rating100: Int,
        ratingsAsStars: Boolean? = null,
    ): String {
        val asStars = ratingsAsStars ?: ServerPreferences(context).ratingsAsStars
        return if (asStars) {
            (rating100 / 20.0).toString()
        } else {
            (rating100 / 10.0).toString()
        }
    }

    fun parseTimeToString(ts: Any?): String? {
        return if (ts == null) {
            null
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val dateTimeFormatter = DateTimeFormatter.ofPattern("eee, MMMM d, yyyy h:mm a")
                val dateTime =
                    ZonedDateTime.parse(
                        ts.toString(),
                        DateTimeFormatter.ISO_DATE_TIME,
                    )
                dateTime.format(dateTimeFormatter)
            } catch (ex: DateTimeParseException) {
                ts.toString()
            }
        } else {
            ts.toString()
        }
    }
}

val TRUST_ALL_CERTS: X509TrustManager =
    @SuppressLint("CustomX509TrustManager")
    object : X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkClientTrusted(
            chain: Array<X509Certificate>,
            authType: String,
        ) {
        }

        @SuppressLint("TrustAllX509TrustManager")
        override fun checkServerTrusted(
            chain: Array<X509Certificate>,
            authType: String,
        ) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }

fun createOkHttpClient(context: Context): OkHttpClient {
    val manager = PreferenceManager.getDefaultSharedPreferences(context)
    val trustAll = manager.getBoolean("trustAllCerts", false)
    val apiKey = manager.getString("stashApiKey", null)
    val cacheDuration = cacheDurationPrefToDuration(manager.getInt("networkCacheDuration", 3))
    val cacheLogging = manager.getBoolean("networkCacheLogging", false)

    var builder = OkHttpClient.Builder()
    if (trustAll) {
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(TRUST_ALL_CERTS), SecureRandom())
        builder =
            builder.sslSocketFactory(
                sslContext.socketFactory,
                TRUST_ALL_CERTS,
            ).hostnameVerifier { _, _ ->
                true
            }
    }
    if (apiKey.isNotNullOrBlank()) {
        builder =
            builder.addInterceptor {
                val request =
                    it.request().newBuilder()
                        .addHeader(STASH_API_HEADER, apiKey.trim())
                        .build()
                it.proceed(request)
            }
    }
    if (cacheLogging) {
        Log.d(OK_HTTP_TAG, "cacheDuration in hours: ${cacheDuration?.toInt(DurationUnit.HOURS)}")
        builder =
            builder.eventListener(
                object : EventListener() {
                    override fun cacheHit(
                        call: Call,
                        response: Response,
                    ) {
                        Log.v(OK_HTTP_TAG, "cacheHit: ${call.request().url} => ${response.code}")
                    }

                    override fun cacheMiss(call: Call) {
                        Log.v(OK_HTTP_TAG, "cacheMiss: ${call.request().url}")
                    }

                    override fun cacheConditionalHit(
                        call: Call,
                        cachedResponse: Response,
                    ) {
                        Log.v(
                            OK_HTTP_TAG,
                            "cacheConditionalHit: ${call.request().url} => ${cachedResponse.code}",
                        )
                    }
                },
            )
    }
    if (cacheDuration != null) {
        builder =
            builder.addInterceptor {
                val request =
                    it.request().newBuilder()
                        .cacheControl(
                            CacheControl.Builder()
                                .maxAge(cacheDuration.toInt(DurationUnit.HOURS), TimeUnit.HOURS)
                                .build(),
                        )
                        .build()
                it.proceed(request)
            }
    }
    builder = builder.cache(getNetworkCache(context))
    return builder.build()
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
class AuthorizationInterceptor(private val apiKey: String?) : HttpInterceptor {
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
 * Create a client for accessing Stash's GraphQL API using the default shared preferences for the URL & API key
 */
fun createApolloClient(context: Context): ApolloClient? {
    val stashUrl = PreferenceManager.getDefaultSharedPreferences(context).getString("stashUrl", "")
    val apiKey = PreferenceManager.getDefaultSharedPreferences(context).getString("stashApiKey", "")
    return if (!stashUrl.isNullOrBlank()) {
        var cleanedStashUrl = stashUrl.trim()
        if (!cleanedStashUrl.startsWith("http://") && !cleanedStashUrl.startsWith("https://")) {
            // Assume http
            cleanedStashUrl = "http://$cleanedStashUrl"
        }
        var url = Uri.parse(cleanedStashUrl)
        val pathSegments = url.pathSegments.toMutableList()
        if (pathSegments.isEmpty() || pathSegments.last() != "graphql") {
            pathSegments.add("graphql")
        }
        url =
            url.buildUpon()
                .path(pathSegments.joinToString("/")) // Ensure the URL is the graphql endpoint
                .build()
        Log.d(Constants.TAG, "StashUrl: $stashUrl => $url")

        val httpEngine = DefaultHttpEngine(createOkHttpClient(context))
        ApolloClient.Builder()
            .serverUrl(url.toString())
            .httpEngine(httpEngine)
            .addHttpInterceptor(AuthorizationInterceptor(apiKey))
            .build()
    } else {
        Log.v(
            Constants.TAG,
            "Cannot create ApolloClient: stashUrl='$stashUrl', apiKey set: ${!apiKey.isNullOrBlank()}",
        )
        null
    }
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
): ServerInfoQuery.Data? {
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
            val info =
                withContext(Dispatchers.IO) {
                    client.query(ServerInfoQuery()).execute()
                }
            if (info.hasErrors()) {
                if (showToast) {
                    Toast.makeText(
                        context,
                        "Failed to connect to Stash. Check URL or API Key.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                Log.w(Constants.TAG, "Errors in ServerInfoQuery: ${info.errors}")
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
                return info.data
            }
        } catch (ex: ApolloHttpException) {
            Log.e(Constants.TAG, "ApolloHttpException", ex)
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
            Log.e(Constants.TAG, "ApolloException", ex)
            if (showToast) {
                Toast.makeText(
                    context,
                    "Failed to connect to Stash. Error was '${ex.message}'",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
    return null
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
    return strings.filter { !it.isNullOrBlank() }.joinToString(sep)
}

fun concatIfNotBlank(
    sep: CharSequence,
    strings: List<CharSequence?>,
): String {
    return strings.filter { !it.isNullOrBlank() }.joinToString(sep)
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
    return Math.min(widest, maxMeasuredWidth)
}

/**
 * Gets the [SlimSceneData.resume_time] in milliseconds
 */
val SlimSceneData.resume_position get() = resume_time?.times(1000L)?.toLong()
