package com.github.damontecres.stashapp.di

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.room.Room
import co.touchlab.kermit.Logger
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.room.AppDatabase
import com.github.damontecres.stashapp.data.room.MIGRATION_4_TO_5
import com.github.damontecres.stashapp.di.server.ServerInterceptor
import com.github.damontecres.stashapp.di.server.ServerRepository
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.util.joinNotNullOrBlank
import com.github.damontecres.stashapp.util.joinValueNotNull
import com.github.damontecres.stashapp.util.preferences
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
            val server = serverRepository.currentServer.value.server
            ServerInterceptor(server).intercept(it)
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

@Single
fun provideDataStore(context: Context): DataStore<StashPreferences> = context.preferences

@Single
fun provideDatabase(context: Application): AppDatabase {
    val dbName = context.getString(R.string.app_name)
    return Room
        .databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(MIGRATION_4_TO_5)
        .fallbackToDestructiveMigration()
        .build()
}
