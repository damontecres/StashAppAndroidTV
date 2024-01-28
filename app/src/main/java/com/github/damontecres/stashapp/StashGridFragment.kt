package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.paging.PagingDataAdapter
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.FindDefaultFilterQuery
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.presenters.StashPagingSource
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.QueryEngine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StashGridFragment<T : Query.Data, D : Any>(
    presenter: PresenterSelector,
    comparator: DiffUtil.ItemCallback<D>,
    private val dataSupplier: StashPagingSource.DataSupplier<T, D>,
    val filter: SavedFilterData?,
) : VerticalGridSupportFragment() {
    constructor(
        comparator: DiffUtil.ItemCallback<D>,
        dataSupplier: StashPagingSource.DataSupplier<T, D>,
    ) : this(StashPresenter.SELECTOR, comparator, dataSupplier, null)

    constructor(
        comparator: DiffUtil.ItemCallback<D>,
        dataSupplier: StashPagingSource.DataSupplier<T, D>,
        filter: SavedFilterData?,
    ) : this(StashPresenter.SELECTOR, comparator, dataSupplier, filter)

    private val mAdapter = PagingDataAdapter(presenter, comparator)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paddingTop =
            resources.getDimension(R.dimen.title_bar_height) + resources.getDimension(R.dimen.grid_top_padding)

        val gridPresenter = StashGridPresenter(paddingTop.toInt())
        gridPresenter.numberOfColumns =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("numberOfColumns", 5)
        setGridPresenter(gridPresenter)

        adapter = mAdapter
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        onItemViewClickedListener = StashItemViewClickListener(requireActivity())

        val pageSize =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("maxSearchResults", 50)

        val flow =
            Pager(
                PagingConfig(pageSize = pageSize, prefetchDistance = pageSize * 2),
            ) {
                StashPagingSource(
                    requireContext(),
                    pageSize,
                    dataSupplier = dataSupplier,
                )
            }.flow
                .cachedIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launch {
            val queryEngine = QueryEngine(requireContext(), true)
            val query = FindDefaultFilterQuery(FilterMode.TAGS)

            viewLifecycleOwner.lifecycleScope.launch {
                flow.collectLatest {
                    mAdapter.submitData(it)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        val browseFrameLayout =
            requireView().findViewById<BrowseFrameLayout>(androidx.leanback.R.id.grid_frame)
        browseFrameLayout.onFocusSearchListener =
            BrowseFrameLayout.OnFocusSearchListener { focused: View?, direction: Int ->
                if (direction == View.FOCUS_UP) {
                    val filterButton = requireActivity().findViewById<View>(R.id.filter_button)
                    Log.v(TAG, "Found filterButton=$filterButton")
                    filterButton
                } else {
                    null
                }
            }
        browseFrameLayout.requestFocus()
    }

    companion object {
        const val TAG = "StashGridFragment"
    }

    private class StashGridPresenter(val paddingTop: Int) :
        VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false) {
        override fun initializeGridViewHolder(vh: ViewHolder?) {
            super.initializeGridViewHolder(vh)
            val gridView = vh!!.gridView
            val top = 10 // gridView.paddingTop
            val bottom = gridView.paddingBottom
            val right = 20
            val left = 20
            gridView.setPadding(left, top, right, bottom)
        }
    }
}
