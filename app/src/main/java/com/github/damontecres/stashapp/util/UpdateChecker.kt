package com.github.damontecres.stashapp.util

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
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
        private const val ASSET_NAME = "StashAppAndroidTV.apk"
        private const val DEBUG_ASSET_NAME = "StashAppAndroidTV-debug.apk"

        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

        private const val PERMISSION_REQUEST_CODE = 12345

        private const val TAG = "UpdateChecker"

        suspend fun checkForUpdate(
            activity: Activity,
            showNegativeToast: Boolean = false,
        ) {
            val installedVersion = getInstalledVersion(activity)
            val latestRelease = getLatestRelease(activity)
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

        suspend fun getLatestRelease(context: Context): Release? {
            return withContext(Dispatchers.IO) {
                val updateUrl =
                    PreferenceManager.getDefaultSharedPreferences(context).getStringNotNull(
                        "updateCheckUrl",
                        context.getString(
                            R.string.app_update_url,
                        ),
                    )

                val client = OkHttpClient.Builder().build()
                val request = Request.Builder().url(updateUrl).get().build()
                client.newCall(request).execute().use {
                    if (it.isSuccessful && it.body != null) {
                        val result = Json.parseToJsonElement(it.body!!.string())
                        val name = result.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                        val version = Version.tryFromString(name)
                        val publishedAt = result.jsonObject["published_at"]?.jsonPrimitive?.contentOrNull
                        val body = result.jsonObject["body"]?.jsonPrimitive?.contentOrNull
                        val downloadUrl =
                            result.jsonObject["assets"]?.jsonArray?.firstOrNull { asset ->
                                val assetName =
                                    asset.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                                assetName == ASSET_NAME || assetName == DEBUG_ASSET_NAME
                            }?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.contentOrNull
                        if (version != null) {
                            return@use Release(version, downloadUrl, publishedAt, body)
                        } else {
                            Log.w(TAG, "Update version parsing failed. name=$name")
                        }
                    } else {
                        Log.w(TAG, "Update check failed: ${it.message}")
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
                                Toast.makeText(
                                    activity,
                                    "There was an error downloading the release",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        } else {
                            if (ContextCompat.checkSelfPermission(
                                    activity,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                ) != PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(
                                    activity,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                ActivityCompat.requestPermissions(
                                    activity,
                                    arrayOf(
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_EXTERNAL_STORAGE,
                                    ),
                                    PERMISSION_REQUEST_CODE,
                                )
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
                        }
                    } else {
                        Log.v(TAG, "Request failed for ${release.downloadUrl}: ${it.code}")
                        Toast.makeText(
                            activity,
                            "Error downloading release: ${it.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        }
    }

    data class Release(val version: Version, val downloadUrl: String?, val publishedAt: String?, val body: String?)
}
