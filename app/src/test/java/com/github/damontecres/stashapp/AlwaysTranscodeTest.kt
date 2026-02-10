package com.github.damontecres.stashapp

import com.github.damontecres.stashapp.playback.checkIfAlwaysTranscode
import com.github.damontecres.stashapp.proto.Resolution
import com.github.damontecres.stashapp.proto.StreamChoice
import org.junit.Assert
import org.junit.Test

class AlwaysTranscodeTest {
    val streams =
        listOf(
            "direct stream",
            "mp4",
            "hls",
            "hls 4k (2160p)",
            "hls full hd (1080p)",
            "hls hd (720p)",
            "hls standard (480p)",
            "hls low (240p)",
        ).associateBy { it }

    @Test
    fun `Don't transcode 1080p`() {
        val result =
            checkIfAlwaysTranscode(
                videoResolution = 1080,
                streams = streams,
                streamChoice = StreamChoice.HLS,
                alwaysTarget = Resolution.RES_1080P,
            )
        Assert.assertNull(result)
    }

    @Test
    fun `Don't transcode for disabled`() {
        val result =
            checkIfAlwaysTranscode(
                videoResolution = 1080,
                streams = streams,
                streamChoice = StreamChoice.HLS,
                alwaysTarget = Resolution.UNSPECIFIED,
            )
        Assert.assertNull(result)
    }

    @Test
    fun `Don't transcode unknown stream choice`() {
        val result =
            checkIfAlwaysTranscode(
                videoResolution = 1080,
                streams = streams,
                streamChoice = StreamChoice.DASH,
                alwaysTarget = Resolution.UNSPECIFIED,
            )
        Assert.assertNull(result)
    }

    @Test
    fun `Transcode 720p`() {
        val result =
            checkIfAlwaysTranscode(
                videoResolution = 1080,
                streams = streams,
                streamChoice = StreamChoice.HLS,
                alwaysTarget = Resolution.RES_720P,
            )
        Assert.assertEquals("hls hd (720p)", result)
    }

    @Test
    fun `Transcode 4k`() {
        val result =
            checkIfAlwaysTranscode(
                videoResolution = 2160,
                streams = streams,
                streamChoice = StreamChoice.HLS,
                alwaysTarget = Resolution.RES_720P,
            )
        Assert.assertEquals("hls hd (720p)", result)
    }

    @Test
    fun `Transcode 4k to 240p`() {
        val result =
            checkIfAlwaysTranscode(
                videoResolution = 2160,
                streams = streams,
                streamChoice = StreamChoice.HLS,
                alwaysTarget = Resolution.RES_240P,
            )
        Assert.assertEquals("hls low (240p)", result)
    }

    @Test
    fun `Don't transcode 4k`() {
        val result =
            checkIfAlwaysTranscode(
                videoResolution = 2160,
                streams = streams,
                streamChoice = StreamChoice.HLS,
                alwaysTarget = Resolution.RES_2160P,
            )
        Assert.assertNull(result)
    }

    @Test
    fun `Don't transcode 1440p`() {
        val result =
            checkIfAlwaysTranscode(
                videoResolution = 1440,
                streams = streams,
                streamChoice = StreamChoice.HLS,
                alwaysTarget = Resolution.RES_2160P,
            )
        Assert.assertNull(result)
    }

    @Test
    fun `Transcode 1440p`() {
        val result =
            checkIfAlwaysTranscode(
                videoResolution = 1440,
                streams = streams,
                streamChoice = StreamChoice.HLS,
                alwaysTarget = Resolution.RES_1080P,
            )
        Assert.assertEquals("hls full hd (1080p)", result)
    }
}
