package com.github.damontecres.stashapp.di

import android.content.Context
import android.os.Build
import co.touchlab.kermit.Logger
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import com.github.damontecres.stashapp.util.joinValueNotNull
import okhttp3.OkHttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.github.damontecres.stashapp")
class AppModule

@Named
annotation class StandardHttpClient

@Named
annotation class AuthHttpClient

@Single
@StandardHttpClient
fun provideStandardHttpClient(context: Context): OkHttpClient {
    val userAgent = createUserAgent(context)
    Logger.v { "User-Agent=$userAgent" }
    return OkHttpClient
        .Builder()
        .addInterceptor {
            it.proceed(
                it
                    .request()
                    .newBuilder()
                    .header("User-Agent", userAgent)
                    .build(),
            )
        }.build()
}

@Single
@AuthHttpClient
fun provideAuthHttpClient(
    @StandardHttpClient httpClient: OkHttpClient,
    serverRepository: ServerRepository,
): OkHttpClient =
    httpClient
        .newBuilder()
        .addInterceptor {
            val server = serverRepository.currentServer
            val request =
                if (server.apiKey != null) {
                    val isStashUrl =
                        it
                            .request()
                            .url
                            .toString()
                            .startsWith(server.serverRoot)
                    if (isStashUrl) {
                        // Only set the API Key if the target URL is the stash server
                        it
                            .request()
                            .newBuilder()
                            .addHeader(Constants.STASH_API_HEADER, server.cleanedApiKey!!)
                            .build()
                    } else {
                        it.request()
                    }
                } else {
                    it.request()
                }
            it.proceed(request)
        }.build()

fun createUserAgent(context: Context): String {
    val appName = context.getString(R.string.app_name)
    val versionStr = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    val comments =
        listOf(
            joinValueNotNull("os", Build.VERSION.BASE_OS),
            joinValueNotNull("release", Build.VERSION.RELEASE),
            "sdk/${Build.VERSION.SDK_INT}",
        ).joinNotNullOrBlank("; ")
    val device =
        listOf(
            Build.MANUFACTURER,
            Build.MODEL,
            if (Build.MODEL != Build.PRODUCT) Build.PRODUCT else null,
            Build.DEVICE,
        ).joinNotNullOrBlank("; ")
    return "$appName/$versionStr ($comments) ($device)"
}
