package com.github.damontecres.stashapp.util

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class UpdateChecker {
    companion object {
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/damontecres/StashAppAndroidTV/releases/latest"
        private const val ASSET_NAME = "StashAppAndroidTV.apk"

        const val PACKAGE_INSTALLED_ACTION = "package.install.StashAppAndroidTV"
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"

        const val TAG = "UpdateChecker"

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

        suspend fun installRelease(
            activity: Activity,
            release: Release,
        ) {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder().url(release.downloadUrl!!).get().build()
                client.newCall(request).execute().use {
                    if (it.isSuccessful && it.body != null) {
                        Log.v(TAG, "Request successful for ${release.downloadUrl}")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val contentValues =
                                ContentValues().apply {
                                    put(MediaStore.MediaColumns.DISPLAY_NAME, ASSET_NAME)
                                    put(MediaStore.MediaColumns.MIME_TYPE, APK_MIME_TYPE)
                                    put(
                                        MediaStore.MediaColumns.RELATIVE_PATH,
                                        Environment.DIRECTORY_DOWNLOADS,
                                    )
                                }
                            val resolver = activity.contentResolver
                            val uri =
                                resolver.insert(
                                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                    contentValues,
                                )
                            if (uri != null) {
                                it.body!!.byteStream().use { input ->
                                    resolver.openOutputStream(uri).use { output ->
                                        input.copyTo(output!!)
                                    }
                                }

                                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.data = uri
                                activity.startActivity(intent)
                            } else {
                                Log.e(TAG, "Resolver URI is null")
                            }
                        } else {
                            val downloadDir =
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            downloadDir.mkdirs()
                            val targetFile = File(downloadDir, ASSET_NAME)
                            targetFile.outputStream().use { output ->
                                it.body!!.byteStream().copyTo(output)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.data =
                                    FileProvider.getUriForFile(
                                        activity,
                                        activity.packageName + ".provider",
                                        targetFile,
                                    )
                                activity.startActivity(intent)
                            } else {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.setDataAndType(Uri.fromFile(targetFile), APK_MIME_TYPE)
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                activity.startActivity(intent)
                            }
                        }
                    } else {
                        Log.v(TAG, "Request failed for ${release.downloadUrl}: ${it.code}")
                    }
                }
            }

//            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
//            intent.setDataAndType(
//                Uri.parse(release.downloadUrl),
//                "application/vnd.android.package-archive",
//            )
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            activity.startActivity(intent)
        }
    }

    data class Release(val version: Version, val downloadUrl: String?, val publishedAt: String?, val body: String?)
}
