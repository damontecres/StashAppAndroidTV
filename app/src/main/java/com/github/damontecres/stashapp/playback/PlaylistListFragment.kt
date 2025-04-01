package com.github.damontecres.stashapp.playback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.leanback.widget.SinglePresenterSelector
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.data.PlaylistItem
import com.github.damontecres.stashapp.data.toPlayListItem
import com.github.damontecres.stashapp.presenters.PlaylistItemPresenter
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.PagingObjectAdapter
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.launch

/**
 * Shows a playlist as a scrollable list of [PlaylistItem]s
 */
class PlaylistListFragment<T : Query.Data, D : StashData, Count : Query.Data> : Fragment(R.layout.playlist_list) {
    private val serverViewModel: ServerViewModel by activityViewModels()
    private val viewModel: PlaylistViewModel by viewModels(ownerProducer = { requireParentFragment() })

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

        val parent = (parentFragment as PlaylistFragment<*, *, *>)
        val server = serverViewModel.requireServer()
        val filter = viewModel.filterArgs.value!!

        val playlistTitleView = view.findViewById<TextView>(R.id.playlist_title)
        playlistTitleView.text =
            if (filter.name.isNotNullOrBlank()) {
                filter.name
            } else {
                getString(filter.dataType.pluralStringId)
            }

        val dataSupplier =
            DataSupplierFactory(serverViewModel.requireServer().version).create<T, D, Count>(filter)
        val pageSize = 25
        val pagingSource =
            StashPagingSource(QueryEngine(server), dataSupplier) { page, index, item ->
                // Pages are 1 indexed
                val position = (page - 1) * pageSize + index
                when (item) {
                    is MarkerData -> item.toPlayListItem(position)
                    is SlimSceneData -> item.toPlayListItem(position)
                    else -> throw UnsupportedOperationException()
                }
            }
        val pagingAdapter =
            PagingObjectAdapter(
                pagingSource,
                pageSize,
                viewLifecycleOwner.lifecycleScope,
                SinglePresenterSelector(PlaylistItemPresenter()),
            )

        viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
            pagingAdapter.init()
            val count = pagingAdapter.size()
            playlistTitleView.text = playlistTitleView.text.toString() + " ($count)"
        }

        mGridPresenter.onBindViewHolder(mGridViewHolder, pagingAdapter)
        mGridPresenter.setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            item as PlaylistItem
            parent.playIndex(item.index)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val parent = (parentFragment as PlaylistFragment<*, *, *>)
            mGridViewHolder.gridView.selectedPosition = parent.player!!.currentMediaItemIndex
        }
    }

    companion object {
        private const val TAG = "PlaylistListFragment"
    }
}
