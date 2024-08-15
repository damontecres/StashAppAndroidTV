package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
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
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.animateToVisible
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.views.StashItemViewClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StashGridFragment2<T : Query.Data, D : Any, C : Query.Data>(
    private val presenter: PresenterSelector = StashPresenter.SELECTOR,
    private val comparator: DiffUtil.ItemCallback<D>,
    val dataType: DataType,
    private var sortAndDirection: SortAndDirection = dataType.defaultSort,
    private val cardSize: Int? = null,
    val name: String? = null,
    private val factory: DataSupplierFactory<T, D, C>,
) : VerticalGridSupportFragment() {
    fun interface DataSupplierFactory<T : Query.Data, D : Any, C : Query.Data> {
        fun createDataSupplier(sortAndDirection: SortAndDirection): StashPagingSource.DataSupplier<T, D, C>
    }

    private var job: Job? = null
    val currentSortAndDirection: SortAndDirection
        get() = sortAndDirection

    private lateinit var positionTextView: TextView
    private lateinit var totalCountTextView: TextView

    var requestFocus: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gridPresenter = StashGridPresenter()
        val columns =
            cardSize ?: PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("cardSize", requireContext().getString(R.string.card_size_default))

        gridPresenter.numberOfColumns = columns
        setGridPresenter(gridPresenter)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        positionTextView = view.findViewById(R.id.position_text)
        totalCountTextView = view.findViewById(R.id.total_count_text)

        if (onItemViewClickedListener == null) {
            onItemViewClickedListener = StashItemViewClickListener(requireActivity())
        }
        refresh(sortAndDirection)
    }

    fun refresh(newSortAndDirection: SortAndDirection) {
        Log.v(TAG, "refresh: dataType=$dataType, newSortAndDirection=$newSortAndDirection")
        val pagingAdapter = PagingDataAdapter(presenter, comparator)
        adapter = pagingAdapter
        val pageSize =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("maxSearchResults", 50)
        val dataSupplier = factory.createDataSupplier(newSortAndDirection)
        val pagingSource =
            StashPagingSource(
                requireContext(),
                pageSize,
                dataSupplier = dataSupplier,
                useRandom = false,
                sortByOverride = null,
            )

        val showFooter =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_show_grid_footer), true)
        val footerLayout = requireView().findViewById<View>(R.id.footer_layout)
        if (showFooter) {
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                val count = pagingSource.getCount()
                if (count > 0) {
                    totalCountTextView.text = count.toString()
                    footerLayout.animateToVisible()
                }
            }
            setOnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val position =
                        withContext(Dispatchers.IO) {
                            val snapshot = pagingAdapter.snapshot()
                            snapshot.indexOf(item) + 1
                        }
                    if (position > 0) {
                        positionTextView.text = position.toString()
                    }
                }
            }
        } else {
            footerLayout.visibility = View.GONE
        }

        val flow =
            Pager(
                PagingConfig(
                    pageSize = pageSize,
                    prefetchDistance = pageSize * 2,
                    initialLoadSize = pageSize * 2,
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

        sortAndDirection = newSortAndDirection
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
        if (requestFocus) {
            browseFrameLayout.requestFocus()
        }
    }

    override fun showTitle(show: Boolean) {
        job?.cancel()
        super.showTitle(show)
        if (!show) {
            // Hacky workaround
            job =
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    delay(100)
                    titleView.visibility = View.GONE
                }
        }
    }

    companion object {
        private const val TAG = "StashGridFragment2"
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
