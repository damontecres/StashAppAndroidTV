package com.github.damontecres.stashapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.leanback.paging.PagingDataAdapter
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.OnChildLaidOutListener
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashComparator
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.animateToVisible
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.views.SortButtonManager
import com.github.damontecres.stashapp.views.StashItemViewClickListener
import com.github.damontecres.stashapp.views.TitleTransitionHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StashGridFragment2() : Fragment() {
    // Views
    private lateinit var sortButton: Button
    private lateinit var positionTextView: TextView
    private lateinit var totalCountTextView: TextView
    private lateinit var mGridPresenter: VerticalGridPresenter
    private lateinit var mGridViewHolder: VerticalGridPresenter.ViewHolder
    private lateinit var mAdapter: ObjectAdapter

    // Arguments
    private lateinit var filterArgs: FilterArgs
    private lateinit var _currentSortAndDirection: SortAndDirection

    // State
    private var mOnItemViewSelectedListener: OnItemViewSelectedListener? = null
    private var mSelectedPosition = -1
    private var titleTransitionHelper: TitleTransitionHelper? = null
    private var cardSize: Int? = null
    private var scrollToNextPage = false

    // Modifiable properties
    var requestFocus: Boolean = false
    var name: String? = null
    var sortButtonEnabled = false
    var presenterSelector: PresenterSelector = StashPresenter.SELECTOR
    var onItemViewClickedListener: OnItemViewClickedListener? = null
        set(value) {
            field = value
            mGridPresenter.onItemViewClickedListener = value
        }
    var currentSelectedPosition: Int
        get() = mSelectedPosition
        set(position) {
            mSelectedPosition = position
            if (mGridViewHolder.gridView.adapter != null) {
                mGridViewHolder.gridView.setSelectedPositionSmooth(position)
            }
        }

    // Unmodifiable properties, current state
    val dataType: DataType
        get() = filterArgs.dataType
    val currentSortAndDirection: SortAndDirection
        get() = _currentSortAndDirection

    private val mViewSelectedListener =
        OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            val position = mGridViewHolder.gridView.selectedPosition
            gridOnItemSelected(position)
            if (mOnItemViewSelectedListener != null) {
                mOnItemViewSelectedListener!!.onItemSelected(
                    itemViewHolder,
                    item,
                    rowViewHolder,
                    row,
                )
            }
        }

    private val mChildLaidOutListener =
        OnChildLaidOutListener { parent, view, position, id ->
            if (position == 0) {
                showOrHideTitle()
            }
        }

    constructor(
        filterArgs: FilterArgs,
        cardSize: Int? = null,
        scrollToNextPage: Boolean = false,
    ) : this() {
        this.filterArgs =
            filterArgs.ensureParsed(FilterParser(StashServer.getCurrentServerVersion()))
        this.cardSize = cardSize
        this._currentSortAndDirection = filterArgs.sortAndDirection
        this.scrollToNextPage = scrollToNextPage
    }

    constructor(
        dataType: DataType,
        findFilter: StashFindFilter? = null,
        objectFilter: Any? = null,
        cardSize: Int? = null,
        scrollToNextPage: Boolean = false,
    ) : this(FilterArgs(dataType, findFilter, objectFilter), cardSize, scrollToNextPage)

    @SuppressLint("SetTextI18n")
    private fun gridOnItemSelected(position: Int) {
        if (position != mSelectedPosition) {
            Log.v(TAG, "gridOnItemSelected=$position")
            mSelectedPosition = position
            showOrHideTitle()
            positionTextView.text = (position + 1).toString()
        }
    }

    private fun showOrHideTitle() {
        if (mGridViewHolder.gridView.findViewHolderForAdapterPosition(mSelectedPosition) == null) {
            return
        }
//        if (!mGridViewHolder.gridView.hasPreviousViewInSameRow(mSelectedPosition)) {
//            showTitle(true)
//        } else {
//            showTitle(false)
//        }
        val shouldShowTitle = mSelectedPosition < mGridPresenter.numberOfColumns
        Log.v(
            TAG,
            "showOrHideTitle: mSelectedPosition=$mSelectedPosition, mGridPresenter.numberOfColumns=${mGridPresenter.numberOfColumns}",
        )
        showTitle(shouldShowTitle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cardSize =
            if (savedInstanceState == null) {
                this.cardSize
            } else {
                val temp = savedInstanceState.getInt("cardSize")
                if (temp > 0) {
                    temp
                } else {
                    null
                }
            }

        val gridPresenter = StashGridPresenter()
        val columns =
            cardSize ?: PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("cardSize", requireContext().getString(R.string.card_size_default))

        gridPresenter.numberOfColumns = columns
        setGridPresenter(gridPresenter)

        if (savedInstanceState != null) {
            name = savedInstanceState.getString("name")
            sortButtonEnabled = savedInstanceState.getBoolean("sortButtonEnabled")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val root =
            inflater.inflate(
                R.layout.stash_grid_fragment2,
                container,
                false,
            ) as ViewGroup
        val gridFrame = root.findViewById<View>(androidx.leanback.R.id.grid_frame) as ViewGroup
        sortButton = root.findViewById(R.id.sort_button)
        val gridDock = root.findViewById<View>(androidx.leanback.R.id.browse_grid_dock) as ViewGroup
        mGridViewHolder = mGridPresenter.onCreateViewHolder(gridDock)
        gridDock.addView(mGridViewHolder.view)
        mGridViewHolder.gridView.setOnChildLaidOutListener(mChildLaidOutListener)
        if (name == null) {
            name = getString(dataType.pluralStringId)
        }
        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        positionTextView = view.findViewById(R.id.position_text)
        totalCountTextView = view.findViewById(R.id.total_count_text)

        onItemViewClickedListener = StashItemViewClickListener(requireContext())

        if (savedInstanceState == null) {
            refresh(filterArgs.sortAndDirection) {
                if (scrollToNextPage) {
                    currentSelectedPosition =
                        PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .getInt("maxSearchResults", 25)
                }
            }
        } else {
            filterArgs = savedInstanceState.getParcelable("filterArgs")!!
            Log.v(TAG, "sortAndDirection=${filterArgs.sortAndDirection}")
            val previousPosition = savedInstanceState.getInt("mSelectedPosition")
            Log.v(TAG, "previousPosition=$previousPosition")

            refresh(filterArgs.sortAndDirection) {
                if (previousPosition > 0) {
                    positionTextView.text = (previousPosition + 1).toString()
                    mGridViewHolder.gridView.requestFocus()
                    currentSelectedPosition = previousPosition
                }
            }
        }
        if (sortButtonEnabled) {
            sortButton.visibility = View.VISIBLE
            SortButtonManager {
                refresh(it)
            }.setUpSortButton(sortButton, dataType, filterArgs.sortAndDirection)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("mSelectedPosition", mSelectedPosition)
        cardSize?.let { outState.putInt("cardSize", it) }
        outState.putParcelable("filterArgs", filterArgs.with(currentSortAndDirection))
        outState.putString("name", name)
        outState.putBoolean("sortButtonEnabled", sortButtonEnabled)
    }

    private fun setGridPresenter(gridPresenter: VerticalGridPresenter) {
        mGridPresenter = gridPresenter
        mGridPresenter.onItemViewSelectedListener = mViewSelectedListener
        if (onItemViewClickedListener != null) {
            mGridPresenter.onItemViewClickedListener = onItemViewClickedListener
        }
    }

    private fun updateAdapter() {
        mGridPresenter.onBindViewHolder(mGridViewHolder, mAdapter)
        if (mSelectedPosition != -1) {
            mGridViewHolder.gridView.selectedPosition = mSelectedPosition
        }
    }

    /**
     * Update the filter's sort & direction
     *
     * Specify a callback for when the data is first loaded allowing for scrolling, etc
     */
    fun refresh(
        newSortAndDirection: SortAndDirection,
        firstPageListener: (() -> Unit)? = null,
    ) {
        Log.v(
            TAG,
            "refresh: dataType=${filterArgs.dataType}, newSortAndDirection=$newSortAndDirection",
        )
        val pagingAdapter = PagingDataAdapter(presenterSelector, StashComparator)
        mAdapter = pagingAdapter
        updateAdapter()
        val pageSize =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("maxSearchResults", 50)
        val factory = DataSupplierFactory(ServerPreferences(requireContext()).serverVersion)
        val dataSupplier =
            factory.create<Query.Data, Any, Query.Data>(filterArgs.with(newSortAndDirection))
        val pagingSource =
            StashPagingSource(
                requireContext(),
                pageSize,
                dataSupplier = dataSupplier,
                useRandom = false,
                sortByOverride = null,
            )
        if (firstPageListener != null) {
            pagingSource.addListener(
                object : StashPagingSource.Listener<Any> {
                    override fun onPageFetch(
                        pageNum: Int,
                        page: List<Any>,
                    ) {
                        firstPageListener()
                        pagingSource.removeListener(this)
                    }
                },
            )
        }

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

        _currentSortAndDirection = newSortAndDirection
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

    fun setTitleView(titleView: View?) {
        if (view is ViewGroup && titleView != null) {
            titleTransitionHelper = TitleTransitionHelper(requireView() as ViewGroup, titleView)
        } else {
            titleTransitionHelper = null
        }
    }

    fun showTitle(show: Boolean) {
        titleTransitionHelper?.showTitle(show)
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
