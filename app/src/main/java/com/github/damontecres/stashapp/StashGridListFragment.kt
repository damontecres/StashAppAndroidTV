package com.github.damontecres.stashapp

import android.annotation.SuppressLint
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
import androidx.fragment.app.viewModels
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortOption
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.navigation.NavigationOnItemViewClickedListener
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashImageCardView
import com.github.damontecres.stashapp.suppliers.DataSupplierFactory
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.AlphabetSearchUtils
import com.github.damontecres.stashapp.util.DefaultKeyEventCallback
import com.github.damontecres.stashapp.util.PagingObjectAdapter
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.animateToInvisible
import com.github.damontecres.stashapp.util.animateToVisible
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.maybeStartPlayback
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.TitleTransitionHelper
import com.github.damontecres.stashapp.views.formatNumber
import com.github.damontecres.stashapp.views.models.ServerViewModel
import com.github.damontecres.stashapp.views.models.StashGridViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A [Fragment] that shows a grid of items of the same [DataType].
 *
 * The items are derived from a [FilterArgs] and queried via [DataSupplierFactory].
 */
class StashGridListFragment :
    Fragment(),
    DefaultKeyEventCallback {
    private val serverViewModel: ServerViewModel by activityViewModels()
    private val viewModel: StashGridViewModel by viewModels(ownerProducer = { requireParentFragment() })

    // Views
    private lateinit var footerLayout: View
    private lateinit var positionTextView: TextView
    private lateinit var totalCountTextView: TextView
    private lateinit var noResultsTextView: TextView
    private lateinit var mGridPresenter: VerticalGridPresenter
    private lateinit var mGridViewHolder: VerticalGridPresenter.ViewHolder
    private lateinit var mAdapter: PagingObjectAdapter
    private lateinit var alphabetFilterLayout: LinearLayout
    private lateinit var loadingProgressBar: ContentLoadingProgressBar
    private lateinit var jumpButtonLayout: LinearLayout

    private var remoteButtonPaging: Boolean = true

    private var adapter: PagingObjectAdapter? = null

    // Arguments
    var scrollToNextPage = false

    // State
    private var selectedPosition = -1
    private var gridHeaderTransitionHelper: TitleTransitionHelper? = null

    private var onBackPressedCallback: OnBackPressedCallback? = null

    // Modifiable properties

    /**
     * Request that the grid receive focus in [onStart]
     */
    var requestFocus: Boolean = false

    /**
     * Whether to enable scrolling to the top on a back press, defaults to true
     */
    var backPressScrollEnabled = true

    /**
     * The item clicked listener, will default to [NavigationOnItemViewClickedListener] in [onViewCreated] if not specified before
     */
    var onItemViewClickedListener: OnItemViewClickedListener? = null

    // Unmodifiable properties, current state

    /**
     * Type of items being displayed
     */
    lateinit var dataType: DataType

    val numberOfColumns: Int
        get() = mGridPresenter.numberOfColumns

    fun init(dataType: DataType) {
        this.dataType = dataType
    }

    private val mViewSelectedListener =
        OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            val position = mGridViewHolder.gridView.selectedPosition
            onSelectedOrJump(position)
        }

    private fun jumpTo(newPosition: Int) {
        if (mGridViewHolder.gridView.adapter != null) {
            if (abs(selectedPosition - newPosition) < mGridPresenter.numberOfColumns * 10) {
                // If new position is close to the current, smooth scroll
                mGridViewHolder.gridView.setSelectedPositionSmooth(newPosition)
                onSelectedOrJump(newPosition)
            } else {
                // If not, just jump without smooth scrolling

                viewLifecycleOwner.lifecycleScope.launch {
                    mAdapter.prefetch(newPosition).join()
                    mGridViewHolder.gridView.selectedPosition = newPosition
                    onSelectedOrJump(newPosition)
                }
            }
        }
    }

    private fun onSelectedOrJump(position: Int) {
        if (position != selectedPosition) {
            if (DEBUG) Log.v(TAG, "gridOnItemSelected=$position")
            selectedPosition = position
            viewModel.position = position
            positionTextView.text = formatNumber(position + 1, false)
            // If on the second row & the back callback exists, enable it
            onBackPressedCallback?.isEnabled = selectedPosition >= mGridPresenter.numberOfColumns
            mAdapter.maybePrefetch(position)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun gridOnItemSelected(position: Int) {
        if (position != selectedPosition) {
            if (DEBUG) Log.v(TAG, "gridOnItemSelected=$position")
            selectedPosition = position
            viewModel.position = position
            positionTextView.text = formatNumber(position + 1, false)
            // If on the second row & the back callback exists, enable it
            onBackPressedCallback?.isEnabled = selectedPosition >= mGridPresenter.numberOfColumns
            mAdapter.maybePrefetch(position)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val root =
            inflater.inflate(
                R.layout.stash_grid_list,
                container,
                false,
            ) as ViewGroup
        footerLayout = root.findViewById(R.id.footer_layout)

        val cardSize =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getInt("cardSize", requireContext().getString(R.string.card_size_default))
        val calculatedColumns = (cardSize * (ScenePresenter.CARD_WIDTH.toDouble() / dataType.defaultCardWidth)).toInt()

        mGridPresenter = StashGridPresenter()
        mGridPresenter.numberOfColumns = calculatedColumns
        mGridPresenter.onItemViewSelectedListener = mViewSelectedListener
        if (onItemViewClickedListener != null) {
            mGridPresenter.onItemViewClickedListener = onItemViewClickedListener
        }

        remoteButtonPaging =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_remote_page_buttons), true)

        val gridDock = root.findViewById<View>(androidx.leanback.R.id.browse_grid_dock) as ViewGroup
        mGridViewHolder = mGridPresenter.onCreateViewHolder(gridDock)
        mGridViewHolder.view.isFocusableInTouchMode = false
        gridDock.addView(mGridViewHolder.view)

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
                            viewModel.filterArgs.value!!,
                            QueryEngine(server),
                            factory,
                        )
                    Log.v(TAG, "Found position for $letter: $letterPosition")
                    val jumpPosition =
                        if (viewModel.filterArgs.value
                                ?.sortAndDirection
                                ?.direction == SortDirectionEnum.DESC
                        ) {
                            // Reverse if sorting descending
                            mAdapter.size() - letterPosition - 1
                        } else {
                            letterPosition
                        }

                    jumpTo(jumpPosition)
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
                    FilterAndPosition(viewModel.filterArgs.value!!, selectedPosition)
                }
        if (this::mAdapter.isInitialized) {
            mGridPresenter.onBindViewHolder(mGridViewHolder, mAdapter)
            if (selectedPosition != -1) {
                mGridViewHolder.gridView.selectedPosition = selectedPosition
            }
            showOrHideViews()
        }

        viewModel.loadingStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is StashGridViewModel.LoadingStatus.AdapterReady -> {
                    Log.v(
                        TAG,
                        "LoadingStatus.AdapterReady: same=${this::mAdapter.isInitialized && mAdapter == status.pagingAdapter} ${status.pagingAdapter}",
                    )
                    mAdapter = status.pagingAdapter
                    loadingProgressBar.hide()
                    mGridPresenter.onBindViewHolder(mGridViewHolder, status.pagingAdapter)
                    if (selectedPosition != -1) {
                        mGridViewHolder.gridView.selectedPosition = selectedPosition
                    }
                    showOrHideViews()
                }

                StashGridViewModel.LoadingStatus.FirstPageLoaded -> {
                    Log.v(TAG, "LoadingStatus.FirstPageLoaded")
                    if (requestFocus) {
                        mGridViewHolder.gridView.requestFocus()
                    }
//                    firstPageListener?.invoke()
                }

                StashGridViewModel.LoadingStatus.Start -> {
                    Log.v(TAG, "LoadingStatus.Start")
                    loadingProgressBar.show()
                }

                StashGridViewModel.LoadingStatus.NoOp -> {
                    Log.v(TAG, "LoadingStatus.NoOp")
//                    viewModel.loadingStatus.removeObservers(viewLifecycleOwner)
                }
            }
        }

        if (savedInstanceState == null) {
            Log.v(TAG, "onViewCreated first time")
            if (scrollToNextPage) {
                selectedPosition =
                    PreferenceManager
                        .getDefaultSharedPreferences(requireContext())
                        .getInt("maxSearchResults", 25)
            }
        } else {
            Log.v(TAG, "onViewCreated restoring")
            val previousPosition = savedInstanceState.getInt("mSelectedPosition")
            Log.v(TAG, "previousPosition=$previousPosition")
            selectedPosition = previousPosition
        }

        val prefBackPressScrollEnabled =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_back_button_scroll), true)

        if (prefBackPressScrollEnabled && backPressScrollEnabled) {
            onBackPressedCallback =
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, false) {
                    jumpTo(0)
                    isEnabled = false
                }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("mSelectedPosition", selectedPosition)
    }

    private fun showOrHideViews() {
        val count = mAdapter.size()
        if (count <= 0) {
            positionTextView.text = getString(R.string.zero)
            noResultsTextView.animateToVisible()
            loadingProgressBar.hide()
            jumpButtonLayout.animateToInvisible(View.GONE)
        } else {
            setupJumpButtons(count)
            jumpButtonLayout.animateToVisible()
        }
        if (count > 0 && selectedPosition >= 0) {
            positionTextView.text = formatNumber(selectedPosition + 1, false)
            positionTextView
        }
        totalCountTextView.text =
            formatNumber(
                count,
                serverViewModel.requireServer().serverPreferences.abbreviateCounters,
            )

        if (count > 0 &&
            SortOption.isJumpSupported(
                dataType,
                viewModel.filterArgs.value!!
                    .sortAndDirection.sort,
            )
        ) {
            alphabetFilterLayout.animateToVisible(400L)
        } else {
            alphabetFilterLayout.animateToInvisible(View.GONE, 400L)
        }

        val showFooter =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_show_grid_footer), true)

        if (showFooter) {
            footerLayout.animateToVisible()
        } else {
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
            jumpTo((selectedPosition - jump2).coerceIn(0, count - 1))
        }
        jumpButtonLayout[1].setOnClickListener {
            jumpTo((selectedPosition - jump1).coerceIn(0, count - 1))
        }
        jumpButtonLayout[2].setOnClickListener {
            jumpTo((selectedPosition + jump1).coerceIn(0, count - 1))
        }
        jumpButtonLayout[3].setOnClickListener {
            jumpTo((selectedPosition + jump2).coerceIn(0, count - 1))
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

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        // If play is pressed and the page contains scenes or markers and user is focused on a card (ie not a button)
        if ((keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) &&
            (dataType == DataType.SCENE || dataType == DataType.MARKER) &&
            requireActivity().currentFocus is StashImageCardView
        ) {
            val position = selectedPosition
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
        private const val TAG = "StashGridListFragment"

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
