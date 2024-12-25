package com.github.damontecres.stashapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.view.contains
import androidx.core.view.get
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.PresenterSelector
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo.api.Query
import com.chrynan.parcelable.core.putParcelable
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.filter.CreateFilterActivity
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.navigation.NavigationOnItemViewClickedListener
import com.github.damontecres.stashapp.presenters.NullPresenter
import com.github.damontecres.stashapp.presenters.NullPresenterSelector
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashImageCardView
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.util.AlphabetSearchUtils
import com.github.damontecres.stashapp.util.DefaultKeyEventCallback
import com.github.damontecres.stashapp.util.PagingObjectAdapter
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashParcelable
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.animateToInvisible
import com.github.damontecres.stashapp.util.animateToVisible
import com.github.damontecres.stashapp.util.getFilterArgs
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.maybeStartPlayback
import com.github.damontecres.stashapp.util.putDataType
import com.github.damontecres.stashapp.util.putFilterArgs
import com.github.damontecres.stashapp.views.PlayAllOnClickListener
import com.github.damontecres.stashapp.views.SortButtonManager
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.TitleTransitionHelper
import com.github.damontecres.stashapp.views.formatNumber
import com.github.damontecres.stashapp.views.models.ServerViewModel
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.math.abs

/**
 * A [Fragment] that shows a grid of items of the same [DataType].
 *
 * The items are derived from a [FilterArgs] and queried via [DataSupplierFactory].
 */
class StashGridFragment() :
    Fragment(),
    DefaultKeyEventCallback {
    private val serverViewModel: ServerViewModel by activityViewModels()

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
    private lateinit var mAdapter: PagingObjectAdapter
    private lateinit var alphabetFilterLayout: LinearLayout
    private lateinit var loadingProgressBar: ContentLoadingProgressBar
    private lateinit var jumpButtonLayout: LinearLayout

    var titleView: View? = null

    private var remoteButtonPaging: Boolean = true

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
    var requestFocus: Boolean = true

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
     * The presenter for the items, defaults to [StashPresenter.defaultClassPresenterSelector]
     */
    var presenterSelector: PresenterSelector = StashPresenter.defaultClassPresenterSelector()

    /**
     * The item clicked listener, will default to [NavigationOnItemViewClickedListener] in [onViewCreated] if not specified before
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
                if (abs(mSelectedPosition - position) < mGridPresenter.numberOfColumns * 10) {
                    // If new position is close to the current, smooth scroll
                    mGridViewHolder.gridView.setSelectedPositionSmooth(position)
                    gridOnItemSelected(position)
                } else {
                    // If not, just jump without smooth scrolling

                    viewLifecycleOwner.lifecycleScope.launch {
                        mAdapter.prefetch(position).join()
                        mGridViewHolder.gridView.selectedPosition = position
                        gridOnItemSelected(position)
                    }
                }
            }
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
            if (DEBUG) Log.v(TAG, "gridOnItemSelected=$position")
            mSelectedPosition = position
            showOrHideTitle()
            positionTextView.text = formatNumber(position + 1, false)
            // If on the second row & the back callback exists, enable it
            onBackPressedCallback?.isEnabled = mSelectedPosition >= mGridPresenter.numberOfColumns
            mAdapter.maybePrefetch(position)
        }
    }

    private fun showOrHideTitle() {
        val shouldShowTitle = mSelectedPosition < mGridPresenter.numberOfColumns
        if (DEBUG) {
            Log.v(
                TAG,
                "showOrHideTitle: $shouldShowTitle, mSelectedPosition=$mSelectedPosition, " +
                    "mGridPresenter.numberOfColumns=${mGridPresenter.numberOfColumns}",
            )
        }
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
                    PreferenceManager
                        .getDefaultSharedPreferences(requireContext())
                        .getInt("cardSize", requireContext().getString(R.string.card_size_default))
                (cardSize * (ScenePresenter.CARD_WIDTH.toDouble() / dataType.defaultCardWidth)).toInt()
            }

        val gridPresenter = StashGridPresenter()
        gridPresenter.numberOfColumns = calculatedColumns
        setGridPresenter(gridPresenter)

        remoteButtonPaging =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_remote_page_buttons), true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root =
            inflater.inflate(
                R.layout.stash_grid_fragment,
                container,
                false,
            ) as ViewGroup
        val onFocusChangeListener = StashOnFocusChangeListener(requireContext())
        sortButton = root.findViewById(R.id.sort_button)
        sortButton.onFocusChangeListener = onFocusChangeListener
        playAllButton = root.findViewById(R.id.play_all_button)
        playAllButton.onFocusChangeListener = onFocusChangeListener
        filterButton = root.findViewById(R.id.filter_button)
        filterButton.onFocusChangeListener = onFocusChangeListener
        subContentSwitch = root.findViewById(R.id.sub_content_switch)
        subContentSwitch.onFocusChangeListener = onFocusChangeListener

        val gridDock = root.findViewById<View>(androidx.leanback.R.id.browse_grid_dock) as ViewGroup
        mGridViewHolder = mGridPresenter.onCreateViewHolder(gridDock)
        mGridViewHolder.view.isFocusableInTouchMode = false
        gridDock.addView(mGridViewHolder.view)
        if (name == null) {
            name = getString(dataType.pluralStringId)
        }

        jumpButtonLayout = root.findViewById(R.id.jump_layout)
        loadingProgressBar = root.findViewById(R.id.loading_progress_bar)

        alphabetFilterLayout = root.findViewById(R.id.alphabet_filter_layout)
        AlphabetSearchUtils.LETTERS.forEach { letter ->
            val button =
                inflater.inflate(R.layout.alphabet_button, alphabetFilterLayout, false) as Button
            button.text = letter.toString()
            button.onFocusChangeListener =
                StashOnFocusChangeListener(requireContext(), R.fraction.alphabet_zoom)
            button.setOnClickListener {
                loadingProgressBar.show()
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                    val server = StashServer.requireCurrentServer()
                    val factory = DataSupplierFactory(server.serverPreferences.serverVersion)
                    val letterPosition =
                        AlphabetSearchUtils.findPosition(
                            letter,
                            filterArgs,
                            QueryEngine(server),
                            factory,
                        )
                    Log.v(TAG, "Found position for $letter: $letterPosition")
                    val jumpPosition =
                        if (currentSortAndDirection.direction == SortDirectionEnum.DESC) {
                            // Reverse if sorting descending
                            mAdapter.size() - letterPosition - 1
                        } else {
                            letterPosition
                        }

                    currentSelectedPosition = jumpPosition
                    mGridViewHolder.gridView.requestFocus()
                    loadingProgressBar.hide()
                }
            }
            alphabetFilterLayout.addView(button)
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
            onItemViewClickedListener
                ?: NavigationOnItemViewClickedListener(serverViewModel.navigationManager) {
                    FilterAndPosition(
                        filterArgs,
                        currentSelectedPosition,
                    )
                }

        if (savedInstanceState == null) {
            Log.v(TAG, "onViewCreated first time")
            refresh(_filterArgs) {
                if (scrollToNextPage) {
                    Log.v(TAG, "scrolling to next page")
                    currentSelectedPosition =
                        PreferenceManager
                            .getDefaultSharedPreferences(requireContext())
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

        val playAllListener =
            PlayAllOnClickListener(requireContext(), serverViewModel.navigationManager, dataType) {
                FilterAndPosition(filterArgs, 0)
            }
        playAllButton.setOnClickListener(playAllListener)

        if (playAllButtonEnabled && dataType.supportsPlaylists) {
            playAllButton.visibility = View.VISIBLE
            playAllButton.nextFocusUpId = R.id.tab_layout
        } else if (playAllButtonEnabled && dataType == DataType.IMAGE) {
            playAllButton.visibility = View.VISIBLE
            playAllButton.nextFocusUpId = R.id.tab_layout
            playAllButton.text = getString(R.string.play_slideshow)
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
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
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
        loadingProgressBar.show()

        val pageSize = mGridPresenter.numberOfColumns * 10
        val server = StashServer.requireCurrentServer()
        val factory = DataSupplierFactory(server.serverPreferences.serverVersion)
        val dataSupplier =
            factory.create<Query.Data, StashData, Query.Data>(newFilterArgs)
        val pagingSource =
            StashPagingSource<Query.Data, StashData, StashData, Query.Data>(
                QueryEngine(server),
                dataSupplier = dataSupplier,
            )
        val pagingAdapter =
            PagingObjectAdapter(
                pagingSource,
                pageSize,
                viewLifecycleOwner.lifecycleScope,
                NullPresenterSelector(presenterSelector, NullPresenter(dataType)),
            )
        mAdapter = pagingAdapter
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
                        mGridViewHolder.gridView.requestFocus()
                        pagingAdapter.unregisterObserver(this)
                    }
                },
            )
        }

        pagingAdapter.registerObserver(
            object : ObjectAdapter.DataObserver() {
                override fun onChanged() {
                    loadingProgressBar.hide()
                    pagingAdapter.unregisterObserver(this)
                }
            },
        )

        val showFooter =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_show_grid_footer), true)
        val footerLayout = requireView().findViewById<View>(R.id.footer_layout)

        viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
            updateAdapter()
            mAdapter.init()
            val count = pagingSource.getCount()
            if (count == 0) {
                positionTextView.text = getString(R.string.zero)
                noResultsTextView.animateToVisible()
                loadingProgressBar.hide()
                jumpButtonLayout.animateToInvisible(View.GONE)
            } else {
                setupJumpButtons(count)
                jumpButtonLayout.animateToVisible()
            }
            totalCountTextView.text =
                formatNumber(count, server.serverPreferences.abbreviateCounters)
            if (showFooter) {
                footerLayout.animateToVisible()
            }
            _filterArgs = newFilterArgs
            _currentSortAndDirection = newFilterArgs.sortAndDirection
            if (count > 0 && SortOption.isJumpSupported(dataType, _currentSortAndDirection.sort)) {
                alphabetFilterLayout.animateToVisible(400L)
            } else {
                alphabetFilterLayout.animateToInvisible(View.GONE, 400L)
            }
        }
        if (!showFooter) {
            footerLayout.visibility = View.GONE
        }
    }

    private fun setupJumpButtons(count: Int) {
        val columns = mGridPresenter.numberOfColumns
        val jump2 =
            if (count >= 25_000) {
                columns * 2000
            } else if (count >= 7_000) {
                columns * 200
            } else if (count >= 2_000) {
                columns * 50
            } else {
                columns * 20
            }
        val jump1 =
            if (count >= 25_000) {
                columns * 500
            } else if (count >= 7_000) {
                columns * 50
            } else if (count >= 2_000) {
                columns * 15
            } else {
                columns * 6
            }
        jumpButtonLayout[0].setOnClickListener {
            currentSelectedPosition = (currentSelectedPosition - jump2).coerceIn(0, count - 1)
        }
        jumpButtonLayout[1].setOnClickListener {
            currentSelectedPosition = (currentSelectedPosition - jump1).coerceIn(0, count - 1)
        }
        jumpButtonLayout[2].setOnClickListener {
            currentSelectedPosition = (currentSelectedPosition + jump1).coerceIn(0, count - 1)
        }
        jumpButtonLayout[3].setOnClickListener {
            currentSelectedPosition = (currentSelectedPosition + jump2).coerceIn(0, count - 1)
        }
    }

    override fun onStart() {
        super.onStart()

        val browseFrameLayout =
            requireView().findViewById<BrowseFrameLayout>(androidx.leanback.R.id.grid_frame)
        browseFrameLayout.onFocusSearchListener =
            BrowseFrameLayout.OnFocusSearchListener { focused: View?, direction: Int ->
                if (focused != null && focused in alphabetFilterLayout) {
                    if (direction == View.FOCUS_LEFT) {
                        mGridViewHolder.gridView
                    } else {
                        null
                    }
                } else if (focused != null && focused in jumpButtonLayout) {
                    if (direction == View.FOCUS_RIGHT) {
                        mGridViewHolder.gridView
                    } else {
                        null
                    }
                } else if (direction == View.FOCUS_UP) {
                    val filterButton = requireActivity().findViewById<View>(R.id.filter_button)
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
     * Disable the built-in buttons by setting [sortButtonEnabled], [playAllButtonEnabled], [filterButtonEnabled], etc to false
     */
    fun disableButtons() {
        sortButtonEnabled = false
        playAllButtonEnabled = false
        filterButtonEnabled = false
    }

    fun get(index: Int): Any? = mAdapter.get(index)

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        // If play is pressed and the page contains scenes or markers and user is focused on a card (ie not a button)
        if ((keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) &&
            (dataType == DataType.SCENE || dataType == DataType.MARKER) &&
            requireActivity().currentFocus is StashImageCardView
        ) {
            val position = currentSelectedPosition
            val item = mAdapter.get(position)
            if (item != null) {
                maybeStartPlayback(requireContext(), item)
                return true
            } else {
                return false
            }
        } else if (remoteButtonPaging &&
            keyCode in
            setOf(
                KeyEvent.KEYCODE_PAGE_UP,
                KeyEvent.KEYCODE_CHANNEL_UP,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MEDIA_REWIND,
                KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD,
            ) &&
            requireActivity().currentFocus is StashImageCardView
        ) {
            jumpButtonLayout[1].callOnClick()
            return true
        } else if (remoteButtonPaging &&
            keyCode in
            setOf(
                KeyEvent.KEYCODE_PAGE_DOWN,
                KeyEvent.KEYCODE_CHANNEL_DOWN,
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD,
            ) &&
            requireActivity().currentFocus is StashImageCardView
        ) {
            jumpButtonLayout[2].callOnClick()
            return true
        } else {
            return super.onKeyUp(keyCode, event)
        }
    }

    companion object {
        private const val TAG = "StashGridFragment"

        private const val DEBUG = false
    }

    private class StashGridPresenter : VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false) {
        override fun initializeGridViewHolder(vh: ViewHolder?) {
            super.initializeGridViewHolder(vh)
            val gridView = vh!!.gridView
            val top = 10 // gridView.paddingTop
            val bottom = gridView.paddingBottom
            val right = 14
            val left = 14
            gridView.setPadding(left, top, right, bottom)
        }
    }
}
