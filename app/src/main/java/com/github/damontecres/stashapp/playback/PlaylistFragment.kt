package com.github.damontecres.stashapp.playback

import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

/**
 * A [PlaybackFragment] that manages and plays a playlist/queue of videos
 */
@OptIn(UnstableApi::class)
abstract class PlaylistFragment<T : Query.Data, D : StashData, C : Query.Data> :
    PlaybackFragment() {
    private val playlistViewModel: PlaylistViewModel by activityViewModels()

    protected lateinit var pagingSource: StashPagingSource<T, D, D, C>

    /**
     * Override the skip forward/back, if -1 then the user's settings will be used
     */
    protected var skipForwardOverride = -1L
    protected var skipBackOverride = -1L

    private val playlistListFragment = PlaylistListFragment<T, D, C>()

    @OptIn(UnstableApi::class)
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            // The player isn't available until after super is called
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                buildPlaylist()
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onResume() {
        super.onResume()
        if ((Util.SDK_INT <= 23 || player == null)) {
            // The player isn't available until after super is called
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                buildPlaylist()
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun initializePlayer(): ExoPlayer {
        return if (skipForwardOverride == -1L || skipBackOverride == -1L) {
            StashExoPlayer.getInstance(requireContext())
        } else {
            StashExoPlayer.getInstance(requireContext(), skipForwardOverride, skipBackOverride)
        }.also { exoPlayer ->
            exoPlayer.addListener(
                object : Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        // If there is an error, just skip the video
                        exoPlayer.seekToNext()
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                },
            )
            exoPlayer.addListener(PlaylistListener())
            if (playlistViewModel.filterArgs.value?.dataType == DataType.SCENE) {
                // Only track activity for scene playback
                maybeAddActivityTracking(exoPlayer)
            }
        }.also { exoPlayer ->
            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            if (videoView.controllerShowTimeoutMs > 0) {
                videoView.hideController()
            }
        }
    }

    /**
     * Build the initial playlist
     */
    private suspend fun buildPlaylist() {
        val filter = playlistViewModel.filterArgs.value!!
        val dataSupplier = DataSupplierFactory(StashServer.getCurrentServerVersion()).create<T, D, C>(filter)
        pagingSource =
            StashPagingSource(requireContext(), PAGE_SIZE, dataSupplier, useRandom = false)
        addNextPageToPlaylist()
        maybeSetupVideoEffects(player!!)
        player!!.prepare()
        player!!.play()

        // Add the playlist list fragment, but keep it hidden
        childFragmentManager.commit {
            add(R.id.video_overlay, playlistListFragment)
            hide(playlistListFragment)
        }
    }

    /**
     * Add the next page of videos to the playlist
     *
     * @return false if no videos were added
     */
    suspend fun addNextPageToPlaylist(): Boolean {
        // Pages are 1-indexed
        val nextPage = player!!.mediaItemCount / PAGE_SIZE + 1
        return addPageToPlaylist(nextPage)
    }

    /**
     * Add a specific page of videos to the playlist. Prefer using [addNextPageToPlaylist] to preserve filter order
     *
     * @return false if no videos were added
     */
    private suspend fun addPageToPlaylist(page: Int): Boolean {
        Log.v(TAG, "Fetching page #$page")
        val newItems = pagingSource.fetchPage(page, PAGE_SIZE)
        val mediaItems =
            newItems.map { item ->
                val scene = convertToScene(item)
                val streamDecision = getStreamDecision(requireContext(), scene)
                buildMediaItem(requireContext(), streamDecision, scene) {
                    builderCallback(item)?.invoke(this)
                    setTag(MediaItemTag(scene, streamDecision))
                }
            }
        Log.v(TAG, "Got ${mediaItems.size} media items")
        if (mediaItems.isNotEmpty()) {
            player!!.addMediaItems(mediaItems)
            return true
        } else {
            return false
        }
    }

    /**
     * Convert items provided by the data supplier into a [Scene]
     */
    abstract fun convertToScene(item: D): Scene

    /**
     * A callback when building the [MediaItem] for a given item.
     *
     * This allows the subclass need to manipulate it such as adding clipping information or adjusting the start position
     *
     * @return a callback for the given item or null if none is needed
     */
    abstract fun builderCallback(item: D): (MediaItem.Builder.() -> Unit)?

    /**
     * A [Listener] for when a new [MediaItem] is playing in case the playlist needs to be extended
     */
    private inner class PlaylistListener : Listener {
        private var hasMorePages = true

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int,
        ) {
            if (mediaItem != null) {
                // Update the UI
                val tag = mediaItem.localConfiguration!!.tag!! as MediaItemTag
                val scene = tag.item
                Log.v(TAG, "Starting playback of ${scene.id}")
                currentScene = scene
                updateDebugInfo(tag.streamDecision, scene)

                // Replace activity tracker
                if (trackActivityListener != null) {
                    trackActivityListener?.release()
                    player!!.removeListener(trackActivityListener!!)
                }
                maybeAddActivityTracking(player!!)
            }
            if (hasMorePages) {
                val count = player!!.mediaItemCount
                // TODO: https://medium.com/@nicholas.rose/exoplayer-playlist-diffing-f8fcd4b2ab7c
                // If there are only a few items left in the playlist but there are more server-side, fetch the next page
                if (count - player!!.currentMediaItemIndex <= 5) {
                    Log.v(TAG, "Too few items in playlist")
                    // TODO: if the user skips a lot of videos very quickly, there is a race condition where the same page might be fetched multiple times
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        if (!addNextPageToPlaylist()) {
                            Log.v(TAG, "No more items")
                            hasMorePages = false
                        }
                    }
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            event.keyCode == KeyEvent.KEYCODE_BACK &&
            !playlistListFragment.isHidden
        ) {
            // If showing the playlist and user hits back, hide it
            hidePlaylist()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Show the playlist list. This will disable the video controls.
     */
    fun showPlaylist() {
        hideControlsIfVisible()
        videoView.useController = false
        childFragmentManager.commit {
            setCustomAnimations(android.R.anim.slide_in_left, R.anim.slide_out_left)
            show(playlistListFragment)
        }
    }

    /**
     * Hide the playlist list. This will enable the video controls.
     */
    fun hidePlaylist() {
        childFragmentManager.commit {
            setCustomAnimations(android.R.anim.slide_in_left, R.anim.slide_out_left)
            hide(playlistListFragment)
        }
        videoView.useController = true
    }

    /**
     * Holds an item to play and its [StreamDecision]
     *
     * This will added as a tag to the [MediaItem]s
     */
    data class MediaItemTag(val item: Scene, val streamDecision: StreamDecision)

    companion object {
        private const val TAG = "PlaylistFragment"
        private const val PAGE_SIZE = 25
    }
}
