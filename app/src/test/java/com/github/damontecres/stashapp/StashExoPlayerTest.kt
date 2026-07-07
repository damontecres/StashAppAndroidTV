package com.github.damontecres.stashapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.proto.PlaybackPreferences
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StashExoPlayerTest : BaseTest() {

    private lateinit var context: Context
    private val server = StashServer("http://localhost", null)

    @Before
    override fun setup() {
        super.setup()
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    override fun tearDown() {
        super.tearDown()
        StashExoPlayer.releasePlayer()
    }

    @Test
    fun testGetInstance() {
        val prefs = PlaybackPreferences.newBuilder().apply {
            skipForwardMs = 30_000
            skipBackwardMs = 30_000
        }.build()
        val player1 = StashExoPlayer.getInstance(context, server, prefs)
        assertNotNull(player1)
        
        val player2 = StashExoPlayer.getInstance(context, server, prefs)
        assertSame("Should return the same instance", player1, player2)
    }

    @Test
    fun testReleasePlayer() {
        val prefs = PlaybackPreferences.newBuilder().apply {
            skipForwardMs = 30_000
            skipBackwardMs = 30_000
        }.build()
        val player1 = StashExoPlayer.getInstance(context, server, prefs)
        StashExoPlayer.releasePlayer()
        
        val player2 = StashExoPlayer.getInstance(context, server, prefs)
        assertNotSame("Should return a new instance after release", player1, player2)
    }
}
