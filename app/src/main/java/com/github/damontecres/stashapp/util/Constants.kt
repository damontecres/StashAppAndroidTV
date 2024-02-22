package com.github.damontecres.stashapp.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.widget.NestedScrollView
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
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.ServerInfoQuery
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.Constants.STASH_API_HEADER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object Constants {
    /**
     * The name of the header for authenticating to Stash
     */
    const val STASH_API_HEADER = "ApiKey"
    const val TAG = "Constants"

    /**
     * Converts seconds into a Duration string where fractional seconds are removed
     */
    fun durationToString(duration: Double): String {
        return duration
            .times(100L).toLong()
            .div(100L).toDuration(DurationUnit.SECONDS)
            .toString()
    }
}

val TRUST_ALL_CERTS: TrustManager =
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
    val trustAll =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("trustAllCerts", false)
    val apiKey =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString("stashApiKey", null)
    val builder = OkHttpClient.Builder()
    if (trustAll) {
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(TRUST_ALL_CERTS), SecureRandom())
        builder.sslSocketFactory(
            sslContext.socketFactory,
            TRUST_ALL_CERTS as X509TrustManager,
        )
            .hostnameVerifier { _, _ ->
                true
            }
    }
    if (apiKey.isNotNullOrBlank()) {
        builder.addInterceptor {
            val request =
                it.request().newBuilder().addHeader(STASH_API_HEADER, apiKey.trim()).build()
            it.proceed(request)
        }
    }
    return builder.build()
}

fun configureHttpsTrust(
    app: StashApplication,
    trustAll: Boolean? = null,
) {
    val trust =
        trustAll ?: PreferenceManager.getDefaultSharedPreferences(app.baseContext)
            .getBoolean("trustAllCerts", false)
    if (trust) {
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf(TRUST_ALL_CERTS), SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
    } else {
        HttpsURLConnection.setDefaultSSLSocketFactory(app.defaultSSLSocketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier(app.defaultHostnameVerifier)
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

        val trustAll =
            PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("trustAllCerts", false)
        val httpEngine =
            if (trustAll) {
                DefaultHttpEngine(createOkHttpClient(context))
            } else {
                DefaultHttpEngine()
            }
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
    return withContext(Dispatchers.IO) {
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
                    return@withContext info.data
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
        return@withContext null
    }
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
