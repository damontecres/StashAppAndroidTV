package com.github.damontecres.stashapp.playback

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.leanback.paging.PagingDataAdapter
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.PlaylistItem
import com.github.damontecres.stashapp.data.toPlayListItem
import com.github.damontecres.stashapp.presenters.PlaylistItemPresenter
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.PlaylistItemComparator
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Shows a playlist as a scrollable list of [PlaylistItem]s
 */
class PlaylistListFragment<T : Query.Data, D : StashData, Count : Query.Data> :
    Fragment(R.layout.playlist_list) {
    private val viewModel: PlaylistViewModel by activityViewModels()

    private val mGridPresenter: VerticalGridPresenter = VerticalGridPresenter()
    private lateinit var mGridViewHolder: VerticalGridPresenter.ViewHolder

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root =
            inflater.inflate(
                R.layout.playlist_list,
                container,
                false,
            ) as ViewGroup
        mGridPresenter.numberOfColumns = 1
        val view = root.findViewById<ViewGroup>(R.id.list_view)
        mGridViewHolder = mGridPresenter.onCreateViewHolder(view)
        view.addView(mGridViewHolder.gridView)
        return root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Kind of hacky, but this will always return the same instance
        val player = StashExoPlayer.getInstance(requireContext())
        val filter = viewModel.filterArgs.value!!

        val playlistTitleView = view.findViewById<TextView>(R.id.playlist_title)
        playlistTitleView.text =
            if (filter.name.isNotNullOrBlank()) {
                filter.name
            } else {
                getString(filter.dataType.pluralStringId)
            }

        val dataSupplier = DataSupplierFactory(StashServer.getCurrentServerVersion()).create<T, D, Count>(filter)
        val pagingAdapter = PagingDataAdapter(PlaylistItemPresenter(), PlaylistItemComparator)
        val pageSize = 10
        val pagingSource =
            StashPagingSource(
                requireContext(),
                pageSize,
                dataSupplier = dataSupplier,
                useRandom = false,
                sortByOverride = null,
            ) { page, index, item ->
                // Pages are 1 indexed
                val position = (page - 1) * pageSize + index
                when (item) {
                    is MarkerData -> item.toPlayListItem(position)
                    is SlimSceneData -> item.toPlayListItem(position)
                    else -> throw UnsupportedOperationException()
                }
            }
        pagingAdapter.registerObserver(
            object : ObjectAdapter.DataObserver() {
                override fun onChanged() {
                    mGridViewHolder.gridView.selectedPosition = player.currentMediaItemIndex
                    pagingAdapter.unregisterObserver(this)
                }
            },
        )

        val flow =
            Pager(
                PagingConfig(
                    pageSize = pageSize,
                    prefetchDistance = pageSize * 2,
                    initialLoadSize = pageSize * 2,
                    maxSize = pageSize * 6,
                ),
            ) {
                pagingSource
            }.flow
                .cachedIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launch(
            StashCoroutineExceptionHandler {
                Toast.makeText(
                    requireContext(),
                    "Error loading results: ${it.message}",
                    Toast.LENGTH_LONG,
                )
            },
        ) {
            flow.collectLatest {
                pagingAdapter.submitData(it)
            }
        }

        mGridPresenter.onBindViewHolder(mGridViewHolder, pagingAdapter)
        mGridPresenter.setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            item as PlaylistItem
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val parent = (parentFragment as PlaylistFragment<*, *, *>)
                Log.v(
                    TAG,
                    "item.index=${item.index}, player.mediaItemCount=${player.mediaItemCount}",
                )
                // The play will ignore requests to play something not in the playlist
                // So check if the index is out of bounds and add pages until either the item is available or there are not more pages
                // The latter shouldn't happen until there's a bug
                while (item.index >= player.mediaItemCount) {
                    if (!parent.addNextPageToPlaylist()) {
                        // This condition is most likely a bug
                        Log.w(
                            TAG,
                            "Requested ${item.index} with ${player.mediaItemCount} media items in player, " +
                                "but addNextPageToPlaylist returned no additional items",
                        )
                        Toast.makeText(
                            requireContext(),
                            "Unable to find item to play. This might be a bug!",
                            Toast.LENGTH_LONG,
                        ).show()
                        return@launch
                    }
                    Log.v(TAG, "after fetch: player.mediaItemCount=${player.mediaItemCount}")
                }
                parent.hidePlaylist()
                player.seekTo(item.index, C.TIME_UNSET)
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            mGridViewHolder.gridView.scrollToPosition(StashExoPlayer.getInstance(requireContext()).currentMediaItemIndex)
        }
    }

    companion object {
        private const val TAG = "PlaylistListFragment"
    }
}
