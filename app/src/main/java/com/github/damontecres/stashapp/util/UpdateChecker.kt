package com.github.damontecres.stashapp.util

import android.app.Activity
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class UpdateChecker {
    companion object {
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/damontecres/StashAppAndroidTV/releases/latest"
        private const val ASSET_NAME = "StashAppAndroidTV.apk"

        suspend fun checkForUpdate(
            activity: Activity,
            showNegativeToast: Boolean = false,
        ) {
            val installedVersion = getInstalledVersion(activity)
            val latestRelease = getLatestRelease()
            if (latestRelease != null && latestRelease.version.isGreaterThan(installedVersion)) {
                Toast.makeText(activity, "Update available: $installedVersion => ${latestRelease.version}!", Toast.LENGTH_LONG).show()
            } else if (showNegativeToast) {
                Toast.makeText(activity, "No updates available, $installedVersion is the latest!", Toast.LENGTH_LONG).show()
            }
        }

        fun getInstalledVersion(activity: Activity): Version {
            val pkgInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            return Version.fromString(pkgInfo.versionName)
        }

        suspend fun getLatestRelease(): Release? {
            return withContext(Dispatchers.IO) {
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder().url(LATEST_RELEASE_URL).get().build()
                client.newCall(request).execute().use {
                    if (it.isSuccessful && it.body != null) {
                        val result = Json.parseToJsonElement(it.body!!.string())
                        val name = result.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                        val version = Version.tryFromString(name)
                        val publishedAt = result.jsonObject["published_at"]?.jsonPrimitive?.contentOrNull
                        val body = result.jsonObject["body"]?.jsonPrimitive?.contentOrNull
                        val downloadUrl =
                            result.jsonObject["assets"]?.jsonArray?.firstOrNull { asset ->
                                asset.jsonObject["name"]?.jsonPrimitive?.contentOrNull == ASSET_NAME
                            }?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.contentOrNull
                        if (version != null) {
                            return@use Release(version, downloadUrl, publishedAt, body)
                        }
                    }
                    return@use null
                }
            }
        }
    }

    data class Release(val version: Version, val downloadUrl: String?, val publishedAt: String?, val body: String?)
}
