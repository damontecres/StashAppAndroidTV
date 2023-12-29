package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import androidx.leanback.R
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.paging.PagingDataAdapter
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DiffUtil
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashPagingSource
import com.github.damontecres.stashapp.presenters.stashPresenterSelector
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StashGridFragment<T : Query.Data, D : Any>(
    presenter: PresenterSelector,
    comparator: DiffUtil.ItemCallback<D>,
    private val dataSupplier: StashPagingSource.DataSupplier<T, D>,
) : VerticalGridSupportFragment() {

    constructor(
        comparator: DiffUtil.ItemCallback<D>,
        dataSupplier: StashPagingSource.DataSupplier<T, D>
    ) : this(stashPresenterSelector, comparator, dataSupplier)

    private val mAdapter = PagingDataAdapter(presenter, comparator)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("numberOfColumns", 5)
        setGridPresenter(gridPresenter)

        adapter = mAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onItemViewClickedListener = StashItemViewClickListener(requireActivity())

        val pageSize = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getInt("maxSearchResults", 50)

        val flow = Pager(
            PagingConfig(pageSize = pageSize, prefetchDistance = pageSize * 2)
        ) {
            StashPagingSource(
                requireContext(),
                pageSize,
                dataSupplier = dataSupplier
            )
        }.flow
            .cachedIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launch {
            flow.collectLatest {
                mAdapter.submitData(it)
            }
        }
    }
}