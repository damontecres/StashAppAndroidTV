package com.github.damontecres.stashapp.util

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object HandyManager {
    private const val TAG = "HandyManager"
    internal var BASE_URL = "https://www.handyfeeling.com/api/handy/v2"
    internal var HOSTING_URL = "https://www.handyfeeling.com/api/hosting/v2"

    private const val TIMEOUT_SECONDS = 30L
    private const val HSSP_MODE = 1
    private const val HDSP_MODE = 2
    private const val TEST_POSITION_MIN = 0
    private const val TEST_POSITION_MAX = 100
    private const val TEST_DURATION_ZERO = 0L
    private const val TEST_DURATION_THREE_SECONDS = 3000L
    private const val TEST_DELAY_ONE_SECOND = 1000L
    private const val ERROR_CODE_UNSUPPORTED_PROTOCOL = 4003

    internal var client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private var serverTimeOffset: Long = 0
    private var prefs: android.content.SharedPreferences? = null
    private var connectionKeyPrefName: String = ""
    private var delayCompensationPrefName: String = ""
    private var cloudBridgePrefName: String = ""
    private var handyEnabledPrefName: String = ""

    var isHandyEnabled: Boolean
        get() = prefs?.getBoolean(handyEnabledPrefName, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(handyEnabledPrefName, value)?.apply()
        }

    private val connectionKey: String
        get() = prefs?.getString(connectionKeyPrefName, "")?.trim() ?: ""

    private val delayCompensation: Long
        get() = prefs?.getString(delayCompensationPrefName, "0")?.toLongOrNull() ?: 0L

    private val isCloudBridgeEnabled: Boolean
        get() = prefs?.getBoolean(cloudBridgePrefName, true) ?: true

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        connectionKeyPrefName = appContext.getString(R.string.pref_key_handy_connection_key)
        delayCompensationPrefName = appContext.getString(R.string.pref_key_handy_delay_compensation)
        cloudBridgePrefName = appContext.getString(R.string.pref_key_handy_cloud_bridge)
        handyEnabledPrefName = appContext.getString(R.string.pref_key_handy_enabled)
        if (connectionKey.isNotBlank()) {
            syncServerTime()
        }
    }

    private fun syncServerTime() {
        scope.launch {
            try {
                val sendTime = System.currentTimeMillis()
                val request = Request.Builder()
                    .url("$BASE_URL/servertime")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        val serverTime = JSONObject(body).getLong("serverTime")
                        val receiveTime = System.currentTimeMillis()
                        val rtt = receiveTime - sendTime
                        serverTimeOffset = serverTime - sendTime - (rtt / 2)
                        Log.i(TAG, "Handy server time offset calculated: $serverTimeOffset ms")
                    }
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e(TAG, "Failed to sync server time", e)
            }
        }
    }

    private fun getEstimatedServerTime(): Long {
        return System.currentTimeMillis() + serverTimeOffset + delayCompensation
    }

    sealed class HandyResult {
        object Success : HandyResult()
        data class ApiError(val code: Int, val message: String) : HandyResult()
        data class NetworkError(val message: String) : HandyResult()
        data class GenericError(val message: String) : HandyResult()

        override fun toString(): String {
            return when (this) {
                is Success -> "Success"
                is ApiError -> "Error $code: $message"
                is NetworkError -> "Network: $message"
                is GenericError -> message
            }
        }
    }

    suspend fun testConnection(): Pair<Boolean, String> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (connectionKey.isBlank()) return@withContext Pair(false, "Connection key is empty")
        try {
            val request = Request.Builder()
                .url("$BASE_URL/connected")
                .addHeader("X-Connection-Key", connectionKey)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Pair(true, "Connected successfully")
                } else {
                    val body = response.body.string()
                    var errorMsg = "HTTP ${response.code}"
                    try {
                        if (body.isNotEmpty()) {
                            val json = JSONObject(body)
                            if (json.has("error")) {
                                val errorObj = json.getJSONObject("error")
                                if (errorObj.has("message")) {
                                    errorMsg = "[${response.code}] $body"
                                }
                            }
                        }
                    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                        Log.e(TAG, "Failed to parse error response", e)
                    }
                    Pair(false, errorMsg)
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Pair(false, e.message ?: "Network error")
        }
    }

    suspend fun syncServerTimeV2(): Pair<Long, Long> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (connectionKey.isBlank()) return@withContext Pair(0L, 0L)
        try {
            val sendTime = System.currentTimeMillis()
            val request = Request.Builder()
                .url("$BASE_URL/servertime")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body.string()
                    val serverTime = JSONObject(body).getLong("serverTime")
                    val receiveTime = System.currentTimeMillis()
                    val rtt = receiveTime - sendTime
                    serverTimeOffset = serverTime - sendTime - (rtt / 2)
                    Log.i(TAG, "Handy server time offset calculated: $serverTimeOffset ms, RTT: $rtt ms")
                    Pair(rtt, serverTimeOffset)
                } else {
                    Pair(0L, 0L)
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Pair(0L, 0L)
        }
    }

    private suspend fun parseHandyResponse(response: okhttp3.Response): HandyResult {
        val body = response.body.string()
        if (response.isSuccessful) {
            val jsonResponse = try { JSONObject(body) } catch (@Suppress("TooGenericExceptionCaught") e: Exception) { null }
            if (jsonResponse != null && jsonResponse.has("error")) {
                val error = jsonResponse.getJSONObject("error")
                return HandyResult.ApiError(error.optInt("code", response.code), error.optString("message", "Unknown error"))
            }
            return HandyResult.Success
        } else {
            val jsonResponse = try { JSONObject(body) } catch (@Suppress("TooGenericExceptionCaught") e: Exception) { null }
            if (jsonResponse != null && jsonResponse.has("error")) {
                val error = jsonResponse.getJSONObject("error")
                return HandyResult.ApiError(error.optInt("code", response.code), error.optString("message", "Unknown API error ($body)"))
            }
            return HandyResult.ApiError(response.code, "HTTP ${response.code} ($body)")
        }
    }

    suspend fun setMode(mode: Int): HandyResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (connectionKey.isBlank()) return@withContext HandyResult.GenericError("Connection key is blank")
        try {
            val json = JSONObject().apply { put("mode", mode) }
            val request = Request.Builder()
                .url("$BASE_URL/mode")
                .addHeader("X-Connection-Key", connectionKey)
                .put(json.toString().toRequestBody(JSON))
                .build()
            client.newCall(request).execute().use { response ->
                Log.i(TAG, "Handy setMode($mode) response: ${response.code}")
                parseHandyResponse(response)
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.e(TAG, "Handy setMode failed", e)
            HandyResult.NetworkError(e.message ?: "Network error")
        }
    }

    suspend fun setup(url: String): HandyResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!isHandyEnabled) {
            Log.w(TAG, "Handy setup skipped: Integration is globally disabled")
            return@withContext HandyResult.GenericError("Integration is disabled")
        }

        if (connectionKey.isBlank()) {
            Log.e(TAG, "Handy setup failed: Connection key is blank")
            return@withContext HandyResult.GenericError("Connection key is blank. Please set it in Settings.")
        }
        
        var normalizedUrl = url.trim()
        if (!normalizedUrl.startsWith("http://", ignoreCase = true) && !normalizedUrl.startsWith("https://", ignoreCase = true)) {
            Log.e(TAG, "Handy setup failed: Unsupported URL protocol - $normalizedUrl")
            return@withContext HandyResult.ApiError(ERROR_CODE_UNSUPPORTED_PROTOCOL, "Unsupported URL protocol. Only http/https supported.")
        }

        // Check if we need to bridge local IP
        val isLocalIp = normalizedUrl.contains("//192.168.") || 
                        normalizedUrl.contains("//10.") || 
                        normalizedUrl.contains("//172.") || 
                        normalizedUrl.contains("//localhost") || 
                        normalizedUrl.contains("//127.0.0.1")

        if (isLocalIp && isCloudBridgeEnabled) {
            Log.i(TAG, "Local IP detected, attempting to use Cloud Bridge for $normalizedUrl")
            val bridgedUrl = uploadToHosting(normalizedUrl)
            if (bridgedUrl != null) {
                Log.i(TAG, "Cloud Bridge successful: $bridgedUrl")
                normalizedUrl = bridgedUrl
            } else {
                Log.w(TAG, "Cloud Bridge failed, proceeding with original URL (likely to fail)")
            }
        }

        try {
            Log.i(TAG, "Handy setup started for URL: $normalizedUrl")
            // Ensure we are in HSSP mode (HSSP_MODE) before setup
            val modeResult = setMode(HSSP_MODE)
            if (modeResult !is HandyResult.Success) {
                Log.e(TAG, "Handy setup failed: Could not set HSSP mode - $modeResult")
                return@withContext modeResult
            }

            val json = JSONObject().apply { put("url", normalizedUrl) }
            val request = Request.Builder()
                .url("$BASE_URL/hssp/setup")
                .addHeader("X-Connection-Key", connectionKey)
                .put(json.toString().toRequestBody(JSON))
                .build()
            
            Log.i(TAG, "Handy setup request sent")
            client.newCall(request).execute().use { response ->
                val result = parseHandyResponse(response)
                Log.i(TAG, "Handy setup result: $result")
                if (result !is HandyResult.Success) {
                    Log.w(TAG, "Handy setup failed by API: $result")
                }
                result
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.e(TAG, "Handy setup exception", e)
            HandyResult.NetworkError(e.message ?: "Network error")
        }
    }

    /**
     * Downloads the script from a local URL and uploads it to Handy's hosting service.
     * Returns a public temporary URL if successful, null otherwise.
     */
    private suspend fun uploadToHosting(localUrl: String): String? = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            // 1. Download local content
            Log.d(TAG, "Downloading local script from $localUrl")
            val downloadRequest = Request.Builder().url(localUrl).get().build()
            val content = client.newCall(downloadRequest).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body.bytes()
            } ?: return@withContext null

            // 2. Upload to Handy Hosting API
            Log.d(TAG, "Uploading script content to Handy Hosting API (${content.size} bytes)")
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "script.funscript", content.toRequestBody("application/octet-stream".toMediaType()))
                .build()

            val uploadRequest = Request.Builder()
                .url("$HOSTING_URL/upload")
                .post(multipartBody)
                .build()

            client.newCall(uploadRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body.string()
                    val json = JSONObject(body)
                    // The API returns { "url": "...", "id": "..." }
                    val publicUrl = json.optString("url")
                    if (publicUrl.isNotEmpty()) {
                        return@withContext publicUrl
                    }
                }
                Log.e(TAG, "Hosting upload failed: ${response.code}")
                null
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.e(TAG, "Error in Cloud Bridge upload", e)
            null
        }
    }

    suspend fun testHardware(): HandyResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (connectionKey.isBlank()) return@withContext HandyResult.GenericError("Connection key is empty")
        try {
            Log.i(TAG, "Handy hardware test started")
            // 1. Set mode to HDSP (HDSP_MODE)
            val modeResult = setMode(HDSP_MODE)
            if (modeResult !is HandyResult.Success) return@withContext modeResult
            
            // 2. Move to 0 immediately
            Log.i(TAG, "Moving to 0")
            val json0 = JSONObject().apply { 
                put("position", TEST_POSITION_MIN)
                put("duration", TEST_DURATION_ZERO)
            }
            val request0 = Request.Builder()
                .url("$BASE_URL/hdsp/xpt")
                .addHeader("X-Connection-Key", connectionKey)
                .put(json0.toString().toRequestBody(JSON))
                .build()
            client.newCall(request0).execute().use { response -> 
                val r0 = parseHandyResponse(response)
                if (r0 !is HandyResult.Success) return@withContext r0
            }
            
            // 3. Wait 1 second
            kotlinx.coroutines.delay(TEST_DELAY_ONE_SECOND)
            
            // 4. Move to 100 in 3 seconds
            Log.i(TAG, "Moving to 100 in ${TEST_DURATION_THREE_SECONDS}ms")
            val json100 = JSONObject().apply { 
                put("position", TEST_POSITION_MAX)
                put("duration", TEST_DURATION_THREE_SECONDS)
            }
            val request100 = Request.Builder()
                .url("$BASE_URL/hdsp/xpt")
                .addHeader("X-Connection-Key", connectionKey)
                .put(json100.toString().toRequestBody(JSON))
                .build()
            client.newCall(request100).execute().use { response -> 
                val r100 = parseHandyResponse(response)
                if (r100 !is HandyResult.Success) return@withContext r100
            }
            
            Log.i(TAG, "Handy hardware test finished")
            HandyResult.Success
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.e(TAG, "Handy hardware test exception", e)
            HandyResult.NetworkError(e.message ?: "Network error")
        }
    }

    fun play(videoPositionMs: Long) {
        if (!isHandyEnabled || connectionKey.isBlank()) return
        scope.launch {
            try {
                val json = JSONObject().apply { 
                    put("estimatedServerTime", getEstimatedServerTime())
                    put("startTime", videoPositionMs)
                }
                val request = Request.Builder()
                    .url("$BASE_URL/hssp/play")
                    .addHeader("X-Connection-Key", connectionKey)
                    .put(json.toString().toRequestBody(JSON))
                    .build()
                client.newCall(request).execute().use { response ->
                    Log.i(TAG, "Handy play response: ${response.code}")
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e(TAG, "Handy play failed", e)
            }
        }
    }

    fun stop() {
        if (!isHandyEnabled || connectionKey.isBlank()) return
        scope.launch {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/hssp/stop")
                    .addHeader("X-Connection-Key", connectionKey)
                    .put("".toRequestBody(JSON))
                    .build()
                client.newCall(request).execute().use { response ->
                    Log.i(TAG, "Handy stop response: ${response.code}")
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e(TAG, "Handy stop failed", e)
            }
        }
    }
}
