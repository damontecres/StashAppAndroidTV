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
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.animateToVisible
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.views.StashItemViewClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StashGridFragment<T : Query.Data, D : Any>(
    presenter: PresenterSelector,
    comparator: DiffUtil.ItemCallback<D>,
    private val dataSupplier: StashPagingSource.DataSupplier<T, D>,
    private val cardSize: Int? = null,
    val name: String? = null,
) : VerticalGridSupportFragment() {
    constructor(
        comparator: DiffUtil.ItemCallback<D>,
        dataSupplier: StashPagingSource.DataSupplier<T, D>,
        cardSize: Int? = null,
        name: String? = null,
    ) : this(StashPresenter.SELECTOR, comparator, dataSupplier, cardSize, name)

    private lateinit var positionTextView: TextView
    private lateinit var totalCountTextView: TextView

    val pagingAdapter = PagingDataAdapter(presenter, comparator)

    var requestFocus: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gridPresenter = StashGridPresenter()
        val columns =
            cardSize ?: PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("cardSize", requireContext().getString(R.string.card_size_default))

        gridPresenter.numberOfColumns = columns
        setGridPresenter(gridPresenter)
        adapter = pagingAdapter
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

        val pageSize =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("maxSearchResults", 50)

        val useRandom = requireActivity().intent.getBooleanExtra("useRandom", true)
        val sortBy = requireActivity().intent.getStringExtra("sortBy")

        Log.v(TAG, "useRandom=$useRandom, sortBy=$sortBy")

        val pagingSource =
            StashPagingSource(
                requireContext(),
                pageSize,
                dataSupplier = dataSupplier,
                useRandom = useRandom,
                sortByOverride = sortBy,
            )

        val showFooter =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_show_grid_footer), true)
        val footerLayout = view.findViewById<View>(R.id.footer_layout)
        if (showFooter) {
            val listener =
                object :
                    StashPagingSource.Listener<D> {
                    override fun onPageLoad(
                        pageNum: Int,
                        page: CountAndList<D>,
                    ) {
                        pagingSource.removeListener(this)
                        totalCountTextView.text = page.count.toString()
                        footerLayout.animateToVisible()
                    }
                }
            pagingSource.addListener(listener)

            setOnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val position =
                        withContext(Dispatchers.IO) {
                            val snapshot = pagingAdapter.snapshot()
                            snapshot.indexOf(item) + 1
                        }
                    positionTextView.text = position.toString()
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
