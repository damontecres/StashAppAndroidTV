package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.paging.PagingDataAdapter
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StashGridFragment<T : Query.Data, D : Any>(
    presenter: PresenterSelector,
    comparator: DiffUtil.ItemCallback<D>,
    private val dataSupplier: StashPagingSource.DataSupplier<T, D>,
    val filter: SavedFilterData?,
    private val numberOfColumns: Int? = null,
) : VerticalGridSupportFragment() {
    constructor(
        comparator: DiffUtil.ItemCallback<D>,
        dataSupplier: StashPagingSource.DataSupplier<T, D>,
        numberOfColumns: Int? = null,
    ) : this(StashPresenter.SELECTOR, comparator, dataSupplier, null, numberOfColumns)

    val mAdapter = PagingDataAdapter(presenter, comparator)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gridPresenter = StashGridPresenter()
        var columns =
            numberOfColumns ?: PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("numberOfColumns", 5)

        gridPresenter.numberOfColumns = columns
        setGridPresenter(gridPresenter)

        adapter = mAdapter

        val pageSize =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("maxSearchResults", 50)
        val scrollToNextResult =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean("scrollToNextResult", true)
        val moveOnePage = requireActivity().intent.getBooleanExtra("moveOnePage", false)
        if (moveOnePage && scrollToNextResult) {
            adapter.registerObserver(
                object : ObjectAdapter.DataObserver() {
                    override fun onChanged() {
                        Log.v(TAG, "Skipping one page")
                        setSelectedPosition(pageSize)
                        adapter.unregisterObserver(this)
                    }
                },
            )
        }
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

        val useRandom = requireActivity().intent.getBooleanExtra("useRandom", true)
        val sortBy = requireActivity().intent.getStringExtra("sortBy")

        Log.v(TAG, "useRandom=$useRandom, sortBy=$sortBy")

        val flow =
            Pager(
                PagingConfig(
                    pageSize = pageSize,
                    prefetchDistance = pageSize * 2,
                    initialLoadSize = pageSize * 2,
                ),
            ) {
                StashPagingSource(
                    requireContext(),
                    pageSize,
                    dataSupplier = dataSupplier,
                    useRandom = useRandom,
                    sortByOverride = sortBy,
                )
            }.flow
                .cachedIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launch {
            flow.collectLatest {
                mAdapter.submitData(it)
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

    private class StashGridPresenter :
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
