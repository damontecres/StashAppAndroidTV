package com.github.damontecres.stashapp.ui.components.playback

import androidx.media3.common.Player
import androidx.media3.common.util.Util

interface PlayerControls {
    val duration: Long
    val currentPosition: Long
    val bufferedPosition: Long

    fun seekTo(position: Long)

    fun seekBack()

    fun seekForward()

    fun seekToPrevious()

    fun seekToNext()

    fun hasNextMediaItem(): Boolean

    fun playOrPause()
}

class PlayerControlsImpl(
    private val player: Player,
) : PlayerControls {
    override val duration: Long
        get() = player.duration
    override val currentPosition: Long
        get() = player.currentPosition
    override val bufferedPosition: Long
        get() = player.bufferedPosition

    override fun seekTo(position: Long) {
        player.seekTo(position)
    }

    override fun seekBack() {
        player.seekBack()
    }

    override fun seekForward() {
        player.seekForward()
    }

    override fun seekToPrevious() {
        player.seekToPrevious()
    }

    override fun seekToNext() {
        player.seekToNext()
    }

    override fun hasNextMediaItem(): Boolean = player.hasNextMediaItem()

    override fun playOrPause() {
        Util.handlePlayPauseButtonAction(player)
    }
}

val FakePlayerControls =
    object : PlayerControls {
        override val duration: Long
            get() = 60000
        override val currentPosition: Long
            get() = 25000
        override val bufferedPosition: Long
            get() = 35000

        override fun seekTo(position: Long) {
        }

        override fun seekBack() {
        }

        override fun seekForward() {
        }

        override fun seekToPrevious() {
        }

        override fun seekToNext() {
        }

        override fun hasNextMediaItem(): Boolean = true

        override fun playOrPause() {
        }
    }
