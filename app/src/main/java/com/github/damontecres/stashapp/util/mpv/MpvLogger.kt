package com.github.damontecres.stashapp.util.mpv

import android.util.Log

class MpvLogger : MPVLib.LogObserver {
    override fun logMessage(
        prefix: String,
        level: Int,
        text: String,
    ) {
        // https://github.com/mpv-player/mpv/blob/122abdfec3124bfc92a2918a70ca8150eee68338/include/mpv/client.h#L1423
        when {
            level <= 10 -> Timber.wtf("%s, %s", prefix, text)
            level <= 20 -> Timber.e("%s, %s", prefix, text)
            level <= 30 -> Timber.w("%s, %s", prefix, text)
            level <= 40 -> Timber.i("%s, %s", prefix, text)
            level <= 50 -> Timber.d("%s, %s", prefix, text)
            else -> Timber.v("%s, %s", prefix, text)
        }
    }
}

object Timber {
    fun wtf(
        format: String,
        vararg args: Any?,
    ) = Log.wtf("MPV", String.format(format, args))

    fun e(
        format: String,
        vararg args: Any?,
    ) = Log.e("MPV", String.format(format, args))

    fun w(
        format: String,
        vararg args: Any?,
    ) = Log.w("MPV", String.format(format, args))

    fun i(
        format: String,
        vararg args: Any?,
    ) = Log.i("MPV", String.format(format, args))

    fun d(
        format: String,
        vararg args: Any?,
    ) = Log.d("MPV", String.format(format, args))

    fun v(
        format: String,
        vararg args: Any?,
    ) = Log.v("MPV", String.format(format, args))
}
