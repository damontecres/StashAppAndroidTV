package com.github.damontecres.wholphin.mpv

import android.content.Context
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi

/**
 * Stub implementation to allow for compiling
 *
 * See DEVELOPMENT.md for instructions on including the actual implementation in a build
 */
@OptIn(UnstableApi::class)
class MpvPlayer(
    private val context: Context,
    private val enableHardwareDecoding: Boolean,
    private val useGpuNext: Boolean,
) : SimpleBasePlayer(Looper.getMainLooper()) {
    init {
        throw UnsupportedOperationException("mpv-stub has no runtime functionality!")
    }

    val isReleased: Boolean get() = throw UnsupportedOperationException("mpv-stub has no runtime functionality!")

    override fun getState(): State = throw UnsupportedOperationException("mpv-stub has no runtime functionality!")
}
