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
import androidx.media3.exoplayer.ExoPlayer
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
import java.util.concurrent.ConcurrentHashMap

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
    private var totalCount = 0
    private var codecSupport: CodecSupport? = null
    private val streamDecisionCache = ConcurrentHashMap<String, StreamDecision>()
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
        if (android.os.Build.VERSION.SDK_INT > 23) {
            // The player isn't available until after super is called
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                buildPlaylist()
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onResume() {
        super.onResume()
        if ((android.os.Build.VERSION.SDK_INT <= 23 || player == null)) {
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

                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && !isAutoplayEnabled()) {
                        pause()
                        videoView.showController()
                    }
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
        currentPage = 1
        val filter = playlistViewModel.filterArgs.value!!
        val dataSupplier =
            DataSupplierFactory(StashServer.getCurrentServerVersion()).create<T, D, C>(filter)
        pagingSource =
            StashPagingSource(
                QueryEngine(serverViewModel.requireServer()),
                dataSupplier,
            )
        
        codecSupport = CodecSupport.getSupportedCodecs(requireContext())
        addNextPageToPlaylist()
        if (player is ExoPlayer) {
            maybeSetupVideoEffects(player!! as ExoPlayer)
        }
        maybeMuteAudio(requireContext(), false, player!!)
        player!!.prepare()
        seekToIndex(destination.position, destination.startPosition)
        player!!.play()
        totalCount = pagingSource.getCount()
        withContext(Dispatchers.Main) {
            updatePlaylistDebug()
        }
    }

    private fun createMediaItems(items: List<D>): List<MediaItem> {
        return items.map { item ->
            val scene = convertToScene(item)
            buildUnresolvedMediaItem(requireContext(), scene) {
                builderCallback(item)?.invoke(this)
                setTag(MediaItemTag(scene, null))
            }
        }
    }

    private suspend fun resolveMediaItemAt(index: Int) {
        if (player == null || index < 0 || index >= player!!.mediaItemCount) return
        val mediaItem = player!!.getMediaItemAt(index)
        val tag = mediaItem.localConfiguration?.tag as? MediaItemTag ?: return
        if (tag.streamDecision != null) return // Already resolved

        Log.v(TAG, "Resolving stream for item at index $index (sceneId=${tag.item.id})")
        val scene = tag.item
        
        // Check cache first
        var streamDecision = streamDecisionCache[scene.id]
        if (streamDecision != null) {
            Log.v(TAG, "Cache hit for sceneId=${scene.id}")
        }
        
        if (streamDecision == null) {
            Log.v(TAG, "Cache miss for sceneId=${scene.id}, calculating...")
            val streamChoice = getStreamChoiceFromPreferences(requireContext())
            val transcodeResolution = getTranscodeAboveFromPreferences(requireContext())

            streamDecision =
                getStreamDecision(
                    requireContext(),
                    scene,
                    PlaybackMode.Choose,
                    streamChoice,
                    transcodeResolution,
                    codecSupport ?: CodecSupport.getSupportedCodecs(requireContext()),
                )
            streamDecisionCache[scene.id] = streamDecision
        }

        val resolvedItem =
            buildMediaItem(requireContext(), streamDecision, scene) {
                // Keep the metadata and clipping config from the unresolved item
                setMediaMetadata(mediaItem.mediaMetadata)
                mediaItem.clippingConfiguration.let {
                    setClipStartPositionMs(it.startPositionMs)
                    setClipEndPositionMs(it.endPositionMs)
                    setClipRelativeToDefaultPosition(it.relativeToDefaultPosition)
                    setClipStartsAtKeyFrame(it.startsAtKeyFrame)
                }
                setTag(MediaItemTag(scene, streamDecision))
            }

        withContext(Dispatchers.Main) {
            if (player != null && index < player!!.mediaItemCount) {
                // Double check it's still the same item before replacing
                val currentItemAtPos = player!!.getMediaItemAt(index)
                if (currentItemAtPos.mediaId == resolvedItem.mediaId) {
                    player!!.replaceMediaItem(index, resolvedItem)
                }
            }
        }
    }

    private suspend fun ensurePageLoaded(page: Int) {
        if (page < 1 || player == null) return
        val startIndex = (page - 1) * PAGE_SIZE
        if (startIndex < player!!.mediaItemCount && player!!.getMediaItemAt(startIndex).mediaId.startsWith("dummy_")) {
            val items = pagingSource.fetchPage(page, PAGE_SIZE)
            if (items.isNotEmpty()) {
                val mediaItems = createMediaItems(items)
                val safeReplaceCount = minOf(mediaItems.size, player!!.mediaItemCount - startIndex)
                if (safeReplaceCount > 0) {
                    player!!.replaceMediaItems(startIndex, startIndex + safeReplaceCount, mediaItems.take(safeReplaceCount))
                }
            }
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
        val mediaItems = createMediaItems(newItems)
        Log.v(TAG, "Got ${mediaItems.size} media items")
        if (mediaItems.isNotEmpty()) {
            player!!.addMediaItems(mediaItems)
            return true
        } else {
            return false
        }
    }

    private suspend fun seekToIndex(
        index: Int,
        startPosition: Long = 0L,
    ) {
        val player = player!!
        Log.v(
            TAG,
            "index=$index, startPosition=$startPosition, player.mediaItemCount=${player.mediaItemCount}",
        )
        
        if (index >= player.mediaItemCount) {
            val targetPage = (index / PAGE_SIZE) + 1
            val missingItemsCount = (targetPage - 1) * PAGE_SIZE - player.mediaItemCount
            
            if (missingItemsCount > 0) {
                val dummyItems = (0 until missingItemsCount).map {
                    MediaItem.Builder()
                        .setMediaId("dummy_${player.mediaItemCount + it}")
                        .setUri("http://dummy")
                        .build()
                }
                player.addMediaItems(dummyItems)
                currentPage = targetPage
            }

            // Check if the index is out of bounds and add pages until the item is available
            while (index >= player.mediaItemCount) {
                if (!addNextPageToPlaylist()) {
                    Log.w(
                        TAG,
                        "Requested $index with ${player.mediaItemCount} media items in player, " +
                            "but addNextPageToPlaylist returned no additional items",
                    )
                    withContext(Dispatchers.Main) {
                        Toast
                            .makeText(
                                requireContext(),
                                "Unable to find item to play. This might be a bug!",
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                    return
                }
                Log.v(TAG, "after fetch: player.mediaItemCount=${player.mediaItemCount}")
            }
        } else {
            val targetPage = (index / PAGE_SIZE) + 1
            ensurePageLoaded(targetPage)
        }
        
        val targetPage = (index / PAGE_SIZE) + 1
        ensurePageLoaded(targetPage - 1)

        resolveMediaItemAt(index)
        hidePlaylist()
        player.seekTo(index, if (startPosition > 0L) startPosition else androidx.media3.common.C.TIME_UNSET)
    }

    fun playIndex(
        index: Int,
        startPosition: Long = 0L,
    ) {
        viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
            seekToIndex(index, startPosition)
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
                val tag = mediaItem.localConfiguration?.tag as? MediaItemTag
                if (tag != null) {
                    val scene = tag.item
                    Log.v(
                        TAG,
                        "Starting playback of index=${player?.currentMediaItemIndex}, id=${scene.id}",
                    )
                    currentScene = scene
                    updateDebugInfo(tag.streamDecision, scene)
                    if (activityTrackingEnabled) {
                        maybeAddActivityTracking(scene)
                    }
                }
                updatePlaylistDebug()

                val currentIndex = player!!.currentMediaItemIndex
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    resolveMediaItemAt(currentIndex)
                    resolveMediaItemAt(currentIndex + 1)
                    if (currentIndex > 0) {
                        resolveMediaItemAt(currentIndex - 1)
                    }
                }
            }
            
            if (hasMorePages) {
                val count = player!!.mediaItemCount
                // If there are only a few items left in the playlist but there are more server-side, fetch the next page
                if (count - player!!.currentMediaItemIndex <= PAGE_SIZE / 2) {
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

            val currentIndex = player?.currentMediaItemIndex ?: 0
            
            // Check if we need to load previous items (when getting close to dummies backwards)
            if (currentIndex > 0) {
                val checkIndex = maxOf(0, currentIndex - PAGE_SIZE / 2)
                if (checkIndex < player!!.mediaItemCount) {
                    val checkItem = player!!.getMediaItemAt(checkIndex)
                    if (checkItem.mediaId.startsWith("dummy_")) {
                        viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                            lock.withLock {
                                if (player!!.getMediaItemAt(checkIndex).mediaId.startsWith("dummy_")) {
                                    val pageToFetch = (checkIndex / PAGE_SIZE) + 1
                                    Log.v(TAG, "Approaching dummy items backwards, fetching page $pageToFetch")
                                    ensurePageLoaded(pageToFetch)
                                }
                            }
                        }
                    }
                }
            }

            // Check if we need to load next items (when getting close to dummies forwards)
            if (currentIndex < player!!.mediaItemCount - 1) {
                val checkIndex = minOf(player!!.mediaItemCount - 1, currentIndex + PAGE_SIZE / 2)
                val checkItem = player!!.getMediaItemAt(checkIndex)
                if (checkItem.mediaId.startsWith("dummy_")) {
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        lock.withLock {
                            if (player!!.getMediaItemAt(checkIndex).mediaId.startsWith("dummy_")) {
                                val pageToFetch = (checkIndex / PAGE_SIZE) + 1
                                Log.v(TAG, "Approaching dummy items forwards, fetching page $pageToFetch")
                                ensurePageLoaded(pageToFetch)
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
        val streamDecision: StreamDecision?,
    )

    companion object {
        private const val TAG = "PlaylistFragment"
        private const val PAGE_SIZE = 25
    }
}
