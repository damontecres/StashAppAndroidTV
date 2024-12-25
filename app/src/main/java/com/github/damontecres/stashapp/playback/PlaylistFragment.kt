package com.github.damontecres.stashapp.playback

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.annotation.OptIn
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
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
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A [PlaybackFragment] that manages and plays a playlist/queue of videos
 */
@OptIn(UnstableApi::class)
abstract class PlaylistFragment<T : Query.Data, D : StashData, C : Query.Data> : PlaybackFragment() {
    private val playlistViewModel: PlaylistViewModel by viewModels()

    protected lateinit var pagingSource: StashPagingSource<T, D, D, C>

    /**
     * Override the skip forward/back, if -1 then the user's settings will be used
     */
    protected var skipForwardOverride = -1L
    protected var skipBackOverride = -1L

    private val playlistListFragment = PlaylistListFragment<T, D, C>()

    // Pages are 1-indexed
    private var currentPage = 1
    private var totalCount = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dest = requireArguments().getDestination<Destination.Playlist>()
        playlistViewModel.setFilter(dest.filterArgs)
        // TODO position
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            // Add the playlist list fragment, but keep it hidden
            childFragmentManager.commit {
                add(R.id.video_overlay, playlistListFragment)
                hide(playlistListFragment)
            }
        }
    }

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

    override fun createPlayer(): ExoPlayer {
        val server = StashServer.requireCurrentServer()
        return if (skipForwardOverride == -1L || skipBackOverride == -1L) {
            StashExoPlayer.createInstance(requireContext(), server)
        } else {
            StashExoPlayer.createInstance(
                requireContext(),
                server,
                skipForwardOverride,
                skipBackOverride,
            )
        }
    }

    @OptIn(UnstableApi::class)
    override fun postCreatePlayer(player: Player) {
        player.addListener(
            object : Listener {
                override fun onPlayerError(error: PlaybackException) {
                    // If there is an error, just skip the video
                    player.seekToNext()
                    player.prepare()
                    player.playWhenReady = true
                }
            },
        )
        player.addListener(PlaylistListener())
        if (playlistViewModel.filterArgs.value?.dataType == DataType.SCENE) {
            // Only track activity for scene playback
            maybeAddActivityTracking(player)
        }
        player.repeatMode = Player.REPEAT_MODE_OFF
        if (videoView.controllerShowTimeoutMs > 0) {
            videoView.hideController()
        }
    }

    /**
     * Build the initial playlist
     */
    private suspend fun buildPlaylist() {
        val filter = playlistViewModel.filterArgs.value!!
        val dataSupplier =
            DataSupplierFactory(StashServer.getCurrentServerVersion()).create<T, D, C>(filter)
        pagingSource =
            StashPagingSource(
                QueryEngine(serverViewModel.requireServer()),
                dataSupplier,
            )
        addNextPageToPlaylist()
        maybeSetupVideoEffects(player!!)
        player!!.prepare()
        player!!.play()
        totalCount = pagingSource.getCount()
        withContext(Dispatchers.Main) {
            updatePlaylistDebug()
        }
    }

    /**
     * Add the next page of videos to the playlist
     *
     * @return false if no videos were added
     */
    suspend fun addNextPageToPlaylist(): Boolean {
        val page = currentPage++
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

    private fun updatePlaylistDebug() {
        debugPlaylistTextView.text =
            "${player?.currentMediaItemIndex?.plus(1)} of ${player?.mediaItemCount} ($totalCount)"
    }

    /**
     * A [Listener] for when a new [MediaItem] is playing in case the playlist needs to be extended
     */
    private inner class PlaylistListener : Listener {
        @Volatile
        private var hasMorePages = true

        private val lock = Mutex()

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int,
        ) {
            if (mediaItem != null) {
                // Update the UI
                val tag = mediaItem.localConfiguration!!.tag!! as MediaItemTag
                val scene = tag.item
                Log.v(
                    TAG,
                    "Starting playback of index=${player?.currentMediaItemIndex}, id=${scene.id}",
                )
                currentScene = scene
                updateDebugInfo(tag.streamDecision, scene)
                updatePlaylistDebug()

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
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        // If the user skips a lot of videos very quickly, the same page might be fetched multiple times
                        // Locking here will prevent that
                        lock.withLock {
                            if (hasMorePages && !addNextPageToPlaylist()) {
                                Log.v(TAG, "No more items")
                                hasMorePages = false
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Show the playlist list. This will disable the video controls.
     */
    fun showPlaylist() {
        requireActivity().onBackPressedDispatcher.addCallback {
            hidePlaylist()
            remove()
        }
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
    data class MediaItemTag(
        val item: Scene,
        val streamDecision: StreamDecision,
    )

    companion object {
        private const val TAG = "PlaylistFragment"
        private const val PAGE_SIZE = 25
    }
}
