package com.github.damontecres.stashapp.playback

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.toSeconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Listens to playback and periodically saves playback activity to the server
 */
@OptIn(UnstableApi::class)
class TrackActivityPlaybackListener(
    context: Context,
    private val server: StashServer,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val scene: Scene,
    private val getCurrentPosition: () -> Long,
) : Player.Listener {
    private val mutationEngine = MutationEngine(server)
    private val coroutineScope = CoroutineScope(dispatcher)
    private val task: TimerTask
    private val minimumPlayPercent = server.serverPreferences.minimumPlayPercent
    private val maxPlayPercent: Int

    private var totalPlayDurationMilliseconds = AtomicLong(0)
    private var currentDurationMilliseconds = AtomicLong(0)
    private var isPlaying = AtomicBoolean(false)
    private var incrementedPlayCount = AtomicBoolean(false)

    init {
        val manager = PreferenceManager.getDefaultSharedPreferences(context)
        val playbackDurationInterval = manager.getInt("playbackDurationInterval", 1)
        val saveActivityInterval =
            manager
                .getInt("saveActivityInterval", 10)
                .toDuration(DurationUnit.SECONDS)
                .inWholeMilliseconds
        maxPlayPercent = manager.getInt("maxPlayPercent", 98)

        val delay =
            playbackDurationInterval.toDuration(DurationUnit.SECONDS).inWholeMilliseconds
        // Every x seconds, check if the video is playing
        task =
            object : TimerTask() {
                private var timestamp = System.currentTimeMillis()

                override fun run() {
                    Log.v(TAG, "Firing")
                    try {
                        if (isPlaying.get()) {
                            val now = System.currentTimeMillis()
                            val diffTime = now - timestamp
                            timestamp = now
                            // If it is playing, add the interval to currently tracked duration
                            val current = currentDurationMilliseconds.addAndGet(diffTime)
                            // TODO currentDuration.getAndUpdate would be better, but requires API 24+
                            if (current >= saveActivityInterval) {
                                // If the accumulated currently tracked duration > threshold, reset it and save activity
                                currentDurationMilliseconds.set(0)
                                totalPlayDurationMilliseconds.addAndGet(current)
                                saveSceneActivity(-1L, current)
                            }
                        }
                    } catch (ex: Exception) {
                        Log.w(TAG, "Exception during track activity timer", ex)
                    }
                }
            }
        Log.d(TAG, "Scheduling for ${delay}ms")
        TIMER.schedule(task, delay, delay)
    }

    fun release() {
        task.cancel()
        TIMER.purge()
    }

    fun release(position: Long) {
        Log.v(TAG, "release: position=$position")
        release()
        if (position >= 0) {
            saveSceneActivity(position, currentDurationMilliseconds.getAndSet(0))
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        this.isPlaying.set(isPlaying)
        if (!isPlaying) {
            val diff = currentDurationMilliseconds.getAndSet(0)
            if (diff > 0) {
                totalPlayDurationMilliseconds.addAndGet(diff)
                saveSceneActivity(-1, diff)
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_ENDED) {
            Log.v(TAG, "onPlaybackStateChanged STATE_ENDED")
            val sceneDuration = scene.durationPosition
            val diff = currentDurationMilliseconds.getAndSet(0)
            totalPlayDurationMilliseconds.addAndGet(diff)
            if (sceneDuration != null) {
                saveSceneActivity(sceneDuration, diff)
            }
        }
    }

    private fun saveSceneActivity(
        position: Long,
        duration: Long,
    ) {
        coroutineScope.launch(StashCoroutineExceptionHandler()) {
            val sceneDuration = scene.duration
            val totalDuration = totalPlayDurationMilliseconds.get()
            val calcPosition = if (position >= 0) position else getCurrentPosition()
            Log.v(
                TAG,
                "saveSceneActivity: scene=${scene.id}, position=$position, duration=$duration, " +
                    "calcPosition=$calcPosition, totalDuration=$totalDuration",
            )
            if (sceneDuration != null) {
                val playedPercent = (calcPosition.toSeconds / sceneDuration) * 100
                val positionToSave =
                    if (playedPercent >= maxPlayPercent) {
                        Log.v(
                            TAG,
                            "Setting position to 0 since $playedPercent >= $maxPlayPercent",
                        )
                        0L
                    } else {
                        calcPosition
                    }
                mutationEngine.saveSceneActivity(scene.id, positionToSave, duration)
                val totalPlayPercent = (totalDuration.toSeconds / sceneDuration) * 100
                Log.v(
                    TAG,
                    "totalPlayPercent=$totalPlayPercent, minimumPlayPercent=$minimumPlayPercent",
                )
                if (totalPlayPercent >= minimumPlayPercent) {
                    // If the current session hasn't incremented the play count yet, do it
                    val shouldIncrement = !incrementedPlayCount.getAndSet(true)
                    if (shouldIncrement) {
                        Log.v(TAG, "Incrementing play count for ${scene.id}")
                        mutationEngine.incrementPlayCount(scene.id)
                    }
                }
            } else {
                // No scene duration
                mutationEngine.saveSceneActivity(scene.id, calcPosition, duration)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrackActivityPlaybackListener

        if (server != other.server) return false
        if (scene != other.scene) return false

        return true
    }

    override fun hashCode(): Int {
        var result = server.hashCode()
        result = 31 * result + scene.hashCode()
        return result
    }

    companion object {
        private const val TAG = "TrackActivityPlaybackListener"

        private val TIMER by lazy { Timer("$TAG-timer", true) }
    }
}
