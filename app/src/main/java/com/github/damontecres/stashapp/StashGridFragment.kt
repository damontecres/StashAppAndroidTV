package com.github.damontecres.stashapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
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
import com.apollographql.apollo.api.Query
import com.chrynan.parcelable.core.putParcelable
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.filter.CreateFilterActivity
import com.github.damontecres.stashapp.presenters.NullPresenterSelector
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashComparator
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashParcelable
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.animateToInvisible
import com.github.damontecres.stashapp.util.animateToVisible
import com.github.damontecres.stashapp.util.getFilterArgs
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.putDataType
import com.github.damontecres.stashapp.util.putFilterArgs
import com.github.damontecres.stashapp.views.ImageGridClickedListener
import com.github.damontecres.stashapp.views.PlayAllOnClickListener
import com.github.damontecres.stashapp.views.SortButtonManager
import com.github.damontecres.stashapp.views.StashItemViewClickListener
import com.github.damontecres.stashapp.views.TitleTransitionHelper
import com.github.damontecres.stashapp.views.formatNumber
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * A [Fragment] that shows a grid of items of the same [DataType].
 *
 * The items are derived from a [FilterArgs] and queried via [DataSupplierFactory].
 */
class StashGridFragment() : Fragment() {
    // Views
    private lateinit var sortButton: Button
    private lateinit var playAllButton: Button
    private lateinit var filterButton: Button
    private lateinit var subContentSwitch: SwitchMaterial
    private lateinit var positionTextView: TextView
    private lateinit var totalCountTextView: TextView
    private lateinit var noResultsTextView: TextView
    private lateinit var mGridPresenter: VerticalGridPresenter
    private lateinit var mGridViewHolder: VerticalGridPresenter.ViewHolder
    private lateinit var mAdapter: ObjectAdapter

    var titleView: View? = null

    // Arguments
    private lateinit var _filterArgs: FilterArgs
    private lateinit var _currentSortAndDirection: SortAndDirection

    // State
    private var mOnItemViewSelectedListener: OnItemViewSelectedListener? = null
    private var mSelectedPosition = -1
    private var gridHeaderTransitionHelper: TitleTransitionHelper? = null
    private var columns: Int? = null
    private var scrollToNextPage = false
    private var onBackPressedCallback: OnBackPressedCallback? = null

    // Modifiable properties

    /**
     * Request that the grid receive focus in [onStart]
     */
    var requestFocus: Boolean = false

    /**
     * An optional name for this fragment, not used in this View
     */
    var name: String? = null

    /**
     * Whether to enable the built-in sort button, defaults to true
     */
    var sortButtonEnabled = true

    /**
     * Whether to enable the built-in play all button, defaults to true
     */
    var playAllButtonEnabled = true

    /**
     * Whether to enable the built-in filter button, defaults to true
     */
    var filterButtonEnabled = true

    /**
     * Whether to enable scrolling to the top on a back press, defaults to true
     */
    var backPressScrollEnabled = true

    /**
     * The presenter for the items, defaults to [StashPresenter.SELECTOR]
     */
    var presenterSelector: PresenterSelector = StashPresenter.SELECTOR

    /**
     * The item clicked listener, will default to [StashItemViewClickListener] in [onViewCreated] if not specified before
     */
    var onItemViewClickedListener: OnItemViewClickedListener? = null

    /**
     * Callback for when the sub content switch state is updated. Also ensures the switch will be displayed if not null
     */
    var subContentSwitchCheckedListener: ((isChecked: Boolean) -> Unit)? = null

    /**
     * Sets whether the sub content switch should start checked or not
     */
    var subContentSwitchInitialIsChecked: Boolean = false

    /**
     * The initial text on the sub content switch
     */
    var subContentText: CharSequence? = null

    /**
     * Get or set the currently selected item
     */
    var currentSelectedPosition: Int
        get() = mSelectedPosition

        @SuppressLint("SetTextI18n")
        set(position) {
            if (mGridViewHolder.gridView.adapter != null) {
                if (Math.abs(mSelectedPosition - position) < mGridPresenter.numberOfColumns * 10) {
                    // If new position is close to the current, smooth scroll
                    mGridViewHolder.gridView.setSelectedPositionSmooth(position)
                } else {
                    // If not, just jump without smooth scrolling
                    mGridViewHolder.gridView.selectedPosition = position
                }

                positionTextView.text = formatNumber(position + 1, false)
            }
            mSelectedPosition = position
        }

    // Unmodifiable properties, current state

    /**
     * Type of items being displayed
     */
    val dataType: DataType
        get() = _filterArgs.dataType

    /**
     * The current [SortAndDirection] of the items
     */
    val currentSortAndDirection: SortAndDirection
        get() = _currentSortAndDirection

    /**
     * The current [FilterArgs] which may not be the original provided in the constructor!
     */
    val filterArgs: FilterArgs
        get() = _filterArgs.with(currentSortAndDirection)

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
        columns: Int? = null,
        scrollToNextPage: Boolean = false,
    ) : this() {
        this._filterArgs = filterArgs
        this.columns = columns
        this._currentSortAndDirection = _filterArgs.sortAndDirection
        this.scrollToNextPage = scrollToNextPage
    }

    constructor(
        dataType: DataType,
        findFilter: StashFindFilter? = null,
        objectFilter: StashDataFilter? = null,
        columns: Int? = null,
        scrollToNextPage: Boolean = false,
    ) : this(FilterArgs(dataType, null, findFilter, objectFilter), columns, scrollToNextPage)

    @SuppressLint("SetTextI18n")
    private fun gridOnItemSelected(position: Int) {
        if (position != mSelectedPosition) {
            Log.v(TAG, "gridOnItemSelected=$position")
            mSelectedPosition = position
            showOrHideTitle()
            positionTextView.text = formatNumber(position + 1, false)

            // If on the second row & the back callback exists, enable it
            onBackPressedCallback?.isEnabled = mSelectedPosition >= mGridPresenter.numberOfColumns
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

        if (savedInstanceState != null) {
            name = savedInstanceState.getString("name")
            sortButtonEnabled = savedInstanceState.getBoolean("sortButtonEnabled")
            filterButtonEnabled = savedInstanceState.getBoolean("filterButtonEnabled")
            _filterArgs =
                savedInstanceState.getFilterArgs("_filterArgs")!!
            Log.v(TAG, "sortAndDirection=${_filterArgs.sortAndDirection}")
        }

        val columns =
            if (savedInstanceState == null) {
                this.columns
            } else {
                val temp = savedInstanceState.getInt("columns")
                if (temp > 0) {
                    temp
                } else {
                    null
                }
            }

        val calculatedColumns =
            if (columns != null) {
                columns
            } else {
                val cardSize =
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getInt("cardSize", requireContext().getString(R.string.card_size_default))
                (cardSize * (ScenePresenter.CARD_WIDTH.toDouble() / dataType.defaultCardWidth)).toInt()
            }

        val gridPresenter = StashGridPresenter()
        gridPresenter.numberOfColumns = calculatedColumns
        setGridPresenter(gridPresenter)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val root =
            inflater.inflate(
                R.layout.stash_grid_fragment,
                container,
                false,
            ) as ViewGroup
        val gridFrame = root.findViewById<View>(androidx.leanback.R.id.grid_frame) as ViewGroup
        sortButton = root.findViewById(R.id.sort_button)
        playAllButton = root.findViewById(R.id.play_all_button)
        filterButton = root.findViewById(R.id.filter_button)
        subContentSwitch = root.findViewById(R.id.sub_content_switch)
        val gridDock = root.findViewById<View>(androidx.leanback.R.id.browse_grid_dock) as ViewGroup
        mGridViewHolder = mGridPresenter.onCreateViewHolder(gridDock)
        mGridViewHolder.view.isFocusableInTouchMode = false
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
        noResultsTextView = view.findViewById(R.id.no_results_text)

        mGridPresenter.onItemViewClickedListener =
            onItemViewClickedListener ?: StashItemViewClickListener(requireContext())

        if (savedInstanceState == null) {
            Log.v(TAG, "onViewCreated first time")
            refresh(_filterArgs) {
                if (scrollToNextPage) {
                    Log.v(TAG, "scrolling to next page")
                    currentSelectedPosition =
                        PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .getInt("maxSearchResults", 25)
                }
            }
        } else {
            Log.v(TAG, "onViewCreated restoring")
            val previousPosition = savedInstanceState.getInt("mSelectedPosition")
            Log.v(TAG, "previousPosition=$previousPosition")

            refresh(_filterArgs) {
                if (previousPosition > 0) {
                    positionTextView.text = formatNumber(previousPosition + 1, false)
                    mGridViewHolder.gridView.requestFocus()
                    currentSelectedPosition = previousPosition
                }
            }
        }

        val gridHeader = view.findViewById<View>(R.id.grid_header)
        gridHeaderTransitionHelper = TitleTransitionHelper(view as ViewGroup, gridHeader)

        if (sortButtonEnabled) {
            sortButton.visibility = View.VISIBLE
            sortButton.nextFocusUpId = R.id.tab_layout
            SortButtonManager(StashServer.getCurrentServerVersion()) {
                refresh(_filterArgs.with(it))
            }.setUpSortButton(sortButton, dataType, _filterArgs.sortAndDirection)
        }
        if (playAllButtonEnabled && dataType.supportsPlaylists) {
            playAllButton.visibility = View.VISIBLE
            playAllButton.nextFocusUpId = R.id.tab_layout
            playAllButton.setOnClickListener(
                PlayAllOnClickListener(requireContext(), dataType) {
                    filterArgs
                },
            )
        }
        if (filterButtonEnabled) {
            filterButton.visibility = View.VISIBLE
            filterButton.nextFocusUpId = R.id.tab_layout
            filterButton.setOnClickListener {
                val intent =
                    Intent(requireContext(), CreateFilterActivity::class.java)
                        .putDataType(dataType)
                        .putFilterArgs(CreateFilterActivity.INTENT_STARTING_FILTER, filterArgs)
                requireContext().startActivity(intent)
            }
        }

        subContentSwitch.nextFocusUpId = R.id.tab_layout
        if (subContentSwitchCheckedListener != null) {
            subContentSwitch.isChecked = subContentSwitchInitialIsChecked
            subContentSwitch.text = subContentText
            subContentSwitch.visibility = View.VISIBLE
            subContentSwitch.setOnCheckedChangeListener { _, isChecked ->
                subContentSwitchCheckedListener?.invoke(isChecked)
            }
        }

        val prefBackPressScrollEnabled =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_back_button_scroll), true)

        if (prefBackPressScrollEnabled && backPressScrollEnabled) {
            onBackPressedCallback =
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, false) {
                    currentSelectedPosition = 0
                    isEnabled = false
                }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("mSelectedPosition", mSelectedPosition)
        columns?.let { outState.putInt("columns", it) }
        outState.putParcelable(
            "_filterArgs",
            _filterArgs.with(currentSortAndDirection),
            StashParcelable,
        )
        outState.putString("name", name)
        outState.putBoolean("sortButtonEnabled", sortButtonEnabled)
        outState.putBoolean("filterButtonEnabled", filterButtonEnabled)
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
     */
    fun refresh(newSortAndDirection: SortAndDirection) {
        refresh(_filterArgs.with(newSortAndDirection))
    }

    /**
     * Update the filter
     *
     * Specify a callback for when the data is first loaded allowing for scrolling, etc
     */
    fun refresh(
        newFilterArgs: FilterArgs,
        firstPageListener: (() -> Unit)? = null,
    ) {
        Log.v(
            TAG,
            "refresh: dataType=${newFilterArgs.dataType}, newSortAndDirection=$newFilterArgs",
        )
        val pagingAdapter =
            PagingDataAdapter(NullPresenterSelector(presenterSelector), StashComparator)
        mAdapter = pagingAdapter
        updateAdapter()
        val pageSize = mGridPresenter.numberOfColumns * 10
        val server = StashServer.requireCurrentServer()
        val factory = DataSupplierFactory(server.serverPreferences.serverVersion)
        val dataSupplier =
            factory.create<Query.Data, StashData, Query.Data>(newFilterArgs)
        val pagingSource =
            StashPagingSource<Query.Data, StashData, StashData, Query.Data>(
                QueryEngine(server),
                pageSize,
                dataSupplier = dataSupplier,
                useRandom = false,
                sortByOverride = null,
            )
        if (firstPageListener != null) {
            pagingAdapter.registerObserver(
                object : ObjectAdapter.DataObserver() {
                    override fun onChanged() {
                        Log.v(TAG, "calling firstPageListener")
                        firstPageListener()
                        pagingAdapter.unregisterObserver(this)
                    }
                },
            )
        }
        if (requestFocus) {
            pagingAdapter.registerObserver(
                object : ObjectAdapter.DataObserver() {
                    override fun onChanged() {
                        requireView()
                            .findViewById<View>(androidx.leanback.R.id.grid_frame)
                            .requestFocus()
                        pagingAdapter.unregisterObserver(this)
                    }
                },
            )
        }

        val showFooter =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_show_grid_footer), true)
        val footerLayout = requireView().findViewById<View>(R.id.footer_layout)

        viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
            val count = pagingSource.getCount()
            if (count == 0) {
                positionTextView.text = getString(R.string.zero)
                noResultsTextView.animateToVisible()
            }
            totalCountTextView.text =
                formatNumber(count, server.serverPreferences.abbreviateCounters)
            if (showFooter) {
                footerLayout.animateToVisible()
            }
        }
        if (!showFooter) {
            footerLayout.visibility = View.GONE
        }

//        val pagerSize = mGridPresenter.numberOfColumns * 10
        val flow =
            Pager(
                PagingConfig(
                    pageSize = pageSize,
                    prefetchDistance = pageSize,
                    initialLoadSize = pageSize * 2,
                    maxSize = pageSize * 6,
                    jumpThreshold = pageSize * 6,
                    enablePlaceholders = true,
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
        _filterArgs = newFilterArgs
        _currentSortAndDirection = newFilterArgs.sortAndDirection
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
    }

    fun showTitle(show: Boolean) {
        gridHeaderTransitionHelper?.showTitle(show)

        // TODO: this animation would be nicer if both the grid header & title slide up together
        if (show) {
            titleView?.animateToVisible()
        } else {
            titleView?.animateToInvisible(View.GONE)
        }
    }

    /**
     * Use a [ImageGridClickedListener] for the [onItemViewClickedListener] which will supply the [ImageActivity] with the filter info to scroll images
     *
     * The [dataType] must be [DataType.IMAGE] or an exception will be thrown
     *
     * @return this for chaining
     */
    fun withImageGridClickListener(): StashGridFragment {
        if (dataType != DataType.IMAGE) {
            throw IllegalStateException("Cannot setup ${ImageGridClickedListener::class.java.simpleName} for dataType=$dataType")
        }
        onItemViewClickedListener = ImageGridClickedListener(this)
        return this
    }

    /**
     * Disable the built-in buttons by setting [sortButtonEnabled], [playAllButtonEnabled], [filterButtonEnabled], etc to false
     */
    fun disableButtons() {
        sortButtonEnabled = false
        playAllButtonEnabled = false
        filterButtonEnabled = false
    }

    companion object {
        private const val TAG = "StashGridFragment"
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
