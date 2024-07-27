package com.github.damontecres.stashapp.playback

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.CountMarkersQuery
import com.github.damontecres.stashapp.api.FindMarkersQuery
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.AppFilter
import com.github.damontecres.stashapp.data.FilterType
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.suppliers.MarkerDataSupplier
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.toFindFilterType
import com.github.damontecres.stashapp.views.showSimpleListPopupWindow
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackMarkersFragment : PlaybackFragment() {
    private lateinit var sourceFilter: StashFilter
    private lateinit var queryEngine: QueryEngine
    private lateinit var pagingSource: StashPagingSource<FindMarkersQuery.Data, MarkerData, CountMarkersQuery.Data>
    override val previewsEnabled: Boolean
        get() = false

    @OptIn(UnstableApi::class)
    override fun initializePlayer(): ExoPlayer {
        val duration = requireActivity().intent.getLongExtra(INTENT_DURATION_ID, 15_000L)
        return StashExoPlayer.getInstance(requireContext(), duration / 5, duration / 5)
            .also { exoPlayer ->
                StashExoPlayer.addListener(
                    object : Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            exoPlayer.seekToNext()
                            exoPlayer.prepare()
                            exoPlayer.playWhenReady = true
                        }
                    },
                )
            }.also { exoPlayer ->
                exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    buildPlaylist()
                }

                if (videoView.controllerShowTimeoutMs > 0) {
                    videoView.hideController()
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceFilter = requireActivity().intent.getParcelableExtra(INTENT_FILTER_ID)!!
        queryEngine = QueryEngine(requireContext())
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        moreOptionsButton.setOnClickListener {
            val debugToggleText =
                if (debugView.isVisible) "Hide transcode info" else "Show transcode info"
            showSimpleListPopupWindow(moreOptionsButton, listOf(debugToggleText)) { position ->

                if (position == 0) {
                    if (debugView.isVisible) {
                        debugView.visibility = View.GONE
                    } else {
                        debugView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private suspend fun buildPlaylist() {
        val filter = sourceFilter
        val savedFilter =
            when (filter.filterType) {
                FilterType.APP_FILTER -> {
                    filter as AppFilter
                    filter.toSavedFilterData(requireContext())
                }

                FilterType.SAVED_FILTER -> {
                    filter as StashSavedFilter
                    queryEngine.getSavedFilter(filter.savedFilterId)
                }

                FilterType.CUSTOM_FILTER -> {
                    filter as StashCustomFilter
                    filter.toSavedFilterData()
                }
                FilterType.DEFAULT_FILTER -> throw IllegalStateException()
            }
        if (savedFilter != null) {
            val newSort = filter.sortBy ?: savedFilter.find_filter?.sort
            val newDirection =
                if (filter.direction != null) {
                    SortDirectionEnum.valueOf(filter.direction!!)
                } else {
                    savedFilter.find_filter?.direction
                }

            val duration = requireActivity().intent.getLongExtra(INTENT_DURATION_ID, 15_000L)
            val filterParser = FilterParser(ServerPreferences(requireContext()).serverVersion)
            val findFilter =
                savedFilter.find_filter?.toFindFilterType()
                    ?.copy(
                        sort = Optional.presentIfNotNull(newSort),
                        direction = Optional.presentIfNotNull(newDirection),
                    )
            val objectFilter = filterParser.convertMarkerObjectFilter(savedFilter.object_filter)
            val dataSupplier =
                MarkerDataSupplier(findFilter, objectFilter)
            pagingSource = StashPagingSource(requireContext(), 25, dataSupplier, useRandom = false)
            addPageToPlaylist(1, duration)
            player!!.seekBackIncrement
            player!!.addListener(PlaylistListener(duration))
            player!!.prepare()
            player!!.volume = 1f
            player!!.playWhenReady = true
        } else {
            Log.w(TAG, "savedFilter is null")
            Toast.makeText(requireContext(), "Could not determine filter", Toast.LENGTH_LONG).show()
        }
    }

    private data class MediaItemTag(val marker: MarkerData, val streamDecision: StreamDecision)

    private suspend fun addPageToPlaylist(
        page: Int,
        duration: Long,
    ): Boolean {
        Log.v(TAG, "Fetching page #$page")
        val newMarkers = pagingSource.fetchPage(page, 25)
        val mediaItems =
            newMarkers.map { marker ->
                val scene = Scene.fromVideoSceneData(marker.scene.videoSceneData)
                val startPos = (marker.seconds * 1000).toLong()
                val clipConfig =
                    ClippingConfiguration.Builder()
                        .setStartPositionMs(startPos)
                        .setEndPositionMs(startPos + duration)
                        .build()
                val streamDecision = getStreamDecision(requireContext(), scene)
                buildMediaItem(requireContext(), streamDecision, scene) {
                    it.setClippingConfiguration(clipConfig)
                    it.setTag(MediaItemTag(marker, streamDecision))
                }
            }
        Log.v(TAG, "Got ${mediaItems.size} media items")
        if (mediaItems.isNotEmpty()) {
            val scene = Scene.fromVideoSceneData(newMarkers[0].scene.videoSceneData)
            currentScene = scene
            updateDebugInfo(
                (mediaItems[0].localConfiguration!!.tag as MediaItemTag).streamDecision,
                scene,
            )
            player!!.addMediaItems(mediaItems)
            return true
        } else {
            return false
        }
    }

    private inner class PlaylistListener(val duration: Long) :
        Listener {
        private var hasMorePages = true

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int,
        ) {
            if (mediaItem != null) {
                val tag = mediaItem.localConfiguration!!.tag!! as MediaItemTag
                val scene = Scene.fromVideoSceneData(tag.marker.scene.videoSceneData)
                Log.v(TAG, "Starting playback of marker ${tag.marker.id} from scene ${scene.id}")
                currentScene = scene
                updateDebugInfo(tag.streamDecision, scene)
            }
            if (hasMorePages) {
                val count = player!!.mediaItemCount
                // TODO: https://medium.com/@nicholas.rose/exoplayer-playlist-diffing-f8fcd4b2ab7c
                if (count - player!!.currentMediaItemIndex <= 5) {
                    Log.v(TAG, "Too few items in playlist")
                    val nextPage = count / 25 + 2
                    // TODO race condition
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        if (!addPageToPlaylist(nextPage, duration)) {
                            Log.v(TAG, "No more markers")
                            hasMorePages = false
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "PlaybackMarkersFragment"
        const val INTENT_FILTER_ID = "$TAG.filter"
        const val INTENT_DURATION_ID = "$TAG.duration"
    }
}
