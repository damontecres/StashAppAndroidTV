package com.github.damontecres.stashapp.playback

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
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
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.Scene
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

    private val playlistListFragment = PlaylistListFragment<T, D, C>()

    // Pages are 1-indexed
    private var currentPage = 1
    private var totalCount = -1
    private lateinit var destination: Destination.Playlist

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        destination = requireArguments().getDestination<Destination.Playlist>()
        playlistViewModel.setFilter(destination.filterArgs)
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

    override fun Player.setupPlayer() {
        // no-op
    }

    @OptIn(UnstableApi::class)
    override fun Player.postSetupPlayer() {
        StashExoPlayer.addListener(
            object : Listener {
                override fun onPlayerError(error: PlaybackException) {
                    // If there is an error, just skip the video
                    seekToNext()
                    prepare()
                    playWhenReady = true
                }
            },
        )
        StashExoPlayer.addListener(PlaylistListener())
        repeatMode = Player.REPEAT_MODE_OFF
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
        maybeMuteAudio(requireContext(), false, player!!)
        player!!.prepare()
        if (destination.position > 0) {
            playIndex(destination.position)
        }
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
                val streamDecision = getStreamDecision(requireContext(), scene, PlaybackMode.Choose)
                Log.d(TAG, "streamDecision=$streamDecision")
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

    fun playIndex(index: Int) {
        viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
            val player = player!!
            Log.v(
                TAG,
                "index=$index, player.mediaItemCount=${player.mediaItemCount}",
            )
            // The play will ignore requests to play something not in the playlist
            // So check if the index is out of bounds and add pages until either the item is available or there are not more pages
            // The latter shouldn't happen until there's a bug
            while (index >= player.mediaItemCount) {
                if (!addNextPageToPlaylist()) {
                    // This condition is most likely a bug
                    Log.w(
                        TAG,
                        "Requested $index with ${player.mediaItemCount} media items in player, " +
                            "but addNextPageToPlaylist returned no additional items",
                    )
                    Toast
                        .makeText(
                            requireContext(),
                            "Unable to find item to play. This might be a bug!",
                            Toast.LENGTH_LONG,
                        ).show()
                    return@launch
                }
                Log.v(TAG, "after fetch: player.mediaItemCount=${player.mediaItemCount}")
            }
            hidePlaylist()
            player.seekTo(index, androidx.media3.common.C.TIME_UNSET)
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
     * Whether activity tracking should be enabled while playing the playlist
     */
    abstract val activityTrackingEnabled: Boolean

    @SuppressLint("SetTextI18n")
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
                if (activityTrackingEnabled) {
                    maybeAddActivityTracking(scene)
                }
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
        if (!playlistListFragment.isHidden) {
            childFragmentManager.commit {
                setCustomAnimations(android.R.anim.slide_in_left, R.anim.slide_out_left)
                hide(playlistListFragment)
            }
            videoView.useController = true
        }
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
