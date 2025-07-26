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
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.leanback.system.Settings
import androidx.leanback.transition.TransitionHelper
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.FocusHighlightHelper
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.ItemBridgeAdapterShadowOverlayWrapper
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.ShadowOverlayHelper
import androidx.leanback.widget.VerticalGridView
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
import com.github.damontecres.stashapp.util.defaultCardWidth
import com.github.damontecres.stashapp.util.getDataType
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.maybeStartPlayback
import com.github.damontecres.stashapp.util.putDataType
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.formatNumber
import com.github.damontecres.stashapp.views.models.ServerViewModel
import com.github.damontecres.stashapp.views.models.StashGridViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.properties.Delegates

/**
 * A [Fragment] that shows a grid of [StashData] of the same [DataType].
 *
 * The items are derived from a [FilterArgs] and queried via [DataSupplierFactory].
 */
class StashDataGridFragment :
    Fragment(),
    DefaultKeyEventCallback {
    private lateinit var mShadowOverlayHelper: ShadowOverlayHelper
    private val serverViewModel: ServerViewModel by activityViewModels()
    private val viewModel: StashGridViewModel by viewModels(ownerProducer = { requireParentFragment() })

    // Views
    private lateinit var footerLayout: View
    private lateinit var positionTextView: TextView
    private lateinit var totalCountTextView: TextView
    private lateinit var noResultsTextView: TextView
    private lateinit var gridView: VerticalGridView
    private lateinit var itemBridgeAdapter: ItemBridgeAdapter
    private var pagingAdapter: PagingObjectAdapter? = null
    private lateinit var alphabetFilterLayout: VerticalGridView
    private lateinit var loadingProgressBar: ContentLoadingProgressBar
    private lateinit var jumpButtonLayout: LinearLayout

    private var remoteButtonPaging: Boolean = true
    private var prefBackPressScrollEnabled: Boolean = true

    // State
    private var previousPosition = -1
    private var selectedPosition = -1

    private var onBackPressedCallback: OnBackPressedCallback? = null

    // Modifiable properties

    /**
     * Request that the grid receive focus in [onStart]
     */
    var requestFocus: Boolean = false

    /**
     * The item clicked listener, will default to [NavigationOnItemViewClickedListener] in [onViewCreated] if not specified before
     */
    var onItemViewClickedListener: OnItemViewClickedListener? = null

    // Unmodifiable properties, current state

    /**
     * Type of items being displayed
     */
    lateinit var dataType: DataType

    var numberOfColumns by Delegates.notNull<Int>()
        private set

    fun init(dataType: DataType) {
        this.dataType = dataType
    }

    private val mViewSelectedListener =
        OnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            val position = gridView.selectedPosition
            onSelectedOrJump(position)
        }

    private val backPressFocusChangeListener: View.OnFocusChangeListener by lazy {
        object : StashOnFocusChangeListener(requireContext(), R.fraction.alphabet_zoom) {
            override fun onFocusChange(
                v: View,
                hasFocus: Boolean,
            ) {
                super.onFocusChange(v, hasFocus)
                if (hasFocus) {
                    onBackPressedCallback?.isEnabled = false
                }
            }
        }
    }

    private fun jumpTo(newPosition: Int) {
        Log.v(TAG, "jumpTo $newPosition")
        if (gridView.adapter != null) {
            if (abs(selectedPosition - newPosition) < numberOfColumns * 10) {
                // If new position is close to the current, smooth scroll
                gridView.setSelectedPositionSmooth(newPosition)
                onSelectedOrJump(newPosition)
            } else {
                // If not, just jump without smooth scrolling

                viewLifecycleOwner.lifecycleScope.launch {
                    pagingAdapter?.prefetch(newPosition)?.join()
                    gridView.selectedPosition = newPosition
                    onSelectedOrJump(newPosition)
                }
            }
        }
    }

    private fun onSelectedOrJump(position: Int) {
        if (position != selectedPosition) {
            if (DEBUG) Log.v(TAG, "newPosition=$position, previousPosition=$previousPosition")
            previousPosition = selectedPosition
            selectedPosition = position
            viewModel.position = position
            positionTextView.text = formatNumber(position + 1, false)
            // If on the second row & the back callback exists, enable it
            onBackPressedCallback?.isEnabled = selectedPosition >= numberOfColumns
            pagingAdapter?.maybePrefetch(position)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        if (savedInstanceState != null) {
            dataType = savedInstanceState.getDataType()
        }
        Log.v(TAG, "onCreateView: dataType=$dataType")
        val root =
            inflater.inflate(
                R.layout.stash_grid,
                container,
                false,
            ) as ViewGroup
        footerLayout = root.findViewById(R.id.footer_layout)

        val cardSize =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getInt("cardSize", requireContext().getString(R.string.card_size_default))
        numberOfColumns =
            (cardSize * (ScenePresenter.CARD_WIDTH.toDouble() / dataType.defaultCardWidth)).toInt()

        if (onItemViewClickedListener == null) {
            onItemViewClickedListener =
                NavigationOnItemViewClickedListener(serverViewModel.navigationManager) {
                    FilterAndPosition(viewModel.filterArgs.value!!, selectedPosition)
                }
        }

        remoteButtonPaging =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_remote_page_buttons), true)

        val mUseFocusDimmer = false
        mShadowOverlayHelper =
            ShadowOverlayHelper
                .Builder()
                .needsOverlay(mUseFocusDimmer)
                .needsShadow(true)
                .needsRoundedCorner(false)
                .preferZOrder(!Settings.getInstance(requireContext()).preferStaticShadows())
                .keepForegroundDrawable(true)
                .options(ShadowOverlayHelper.Options.DEFAULT)
                .build(requireContext())

        gridView = root.findViewById(R.id.browse_grid)
        gridView.setNumColumns(numberOfColumns)
        // gridView.setColumnWidth(0)
        if (gridView.adapter == null) {
            Log.v(TAG, "gridView.adapter == null")
            itemBridgeAdapter = VerticalGridItemBridgeAdapter()
            gridView.adapter = itemBridgeAdapter

            if (mShadowOverlayHelper.needsWrapper()) {
                val mShadowOverlayWrapper =
                    ItemBridgeAdapterShadowOverlayWrapper(
                        mShadowOverlayHelper,
                    )
                itemBridgeAdapter.setWrapper(mShadowOverlayWrapper)
            }
            mShadowOverlayHelper.prepareParentForShadow(gridView)
            gridView.setFocusDrawingOrderEnabled(
                mShadowOverlayHelper.getShadowType()
                    != ShadowOverlayHelper.SHADOW_DYNAMIC,
            )
            FocusHighlightHelper.setupBrowseItemFocusHighlight(
                itemBridgeAdapter,
                FocusHighlight.ZOOM_FACTOR_MEDIUM,
                mUseFocusDimmer,
            )
        } else {
            Log.v(TAG, "gridView.adapter != null")
            itemBridgeAdapter = gridView.adapter as ItemBridgeAdapter
        }
        gridView.setOnChildSelectedListener { parent, view, position, id ->
            val ibn =
                if (view == null) null else gridView.getChildViewHolder(view) as ItemBridgeAdapter.ViewHolder
            if (ibn != null) {
                mViewSelectedListener.onItemSelected(ibn.viewHolder, ibn.item, null, null)
            } else {
                mViewSelectedListener.onItemSelected(null, null, null, null)
            }
        }

        jumpButtonLayout = root.findViewById(R.id.jump_layout)
        loadingProgressBar = root.findViewById(R.id.loading_progress_bar)

        alphabetFilterLayout = root.findViewById(R.id.alphabet_scrollview)
        if (!SortOption.isJumpSupported(dataType)) {
            alphabetFilterLayout.visibility = View.GONE
        } else {
            val alphabetAdapter =
                ArrayObjectAdapter(
                    object : Presenter() {
                        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
                            val button =
                                inflater.inflate(
                                    R.layout.alphabet_button,
                                    alphabetFilterLayout,
                                    false,
                                ) as Button
                            button.onFocusChangeListener = backPressFocusChangeListener
                            return ViewHolder(button)
                        }

                        override fun onBindViewHolder(
                            viewHolder: ViewHolder,
                            item: Any?,
                        ) {
                            val letter = item as Char
                            (viewHolder.view as Button).text = letter.toString()
                            viewHolder.view.setOnClickListener {
                                loadingProgressBar.show()
                                viewLifecycleOwner.lifecycleScope.launch(
                                    StashCoroutineExceptionHandler(
                                        autoToast = true,
                                    ),
                                ) {
                                    val server = StashServer.requireCurrentServer()
                                    val factory =
                                        DataSupplierFactory(server.serverPreferences.serverVersion)
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
                                            pagingAdapter!!.size() - letterPosition - 1
                                        } else {
                                            letterPosition
                                        }

                                    jumpTo(jumpPosition)
                                    gridView.requestFocus()
                                    loadingProgressBar.hide()
                                }
                            }
                        }

                        override fun onUnbindViewHolder(viewHolder: ViewHolder) {
                            (viewHolder.view as Button).text = null
                            viewHolder.view.setOnClickListener(null)
                        }
                    },
                )
            alphabetAdapter.addAll(0, AlphabetSearchUtils.LETTERS.toList())
            alphabetFilterLayout.adapter = ItemBridgeAdapter(alphabetAdapter)
        }
        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        positionTextView = view.findViewById(R.id.position_text)
        totalCountTextView = view.findViewById(R.id.total_count_text)
        noResultsTextView = view.findViewById(R.id.no_results_text)

        val previousPosition =
            if (savedInstanceState == null) {
                Log.v(TAG, "onViewCreated savedInstanceState is null")
                -1
            } else {
                Log.v(TAG, "onViewCreated restoring")
                savedInstanceState.getInt("mSelectedPosition")
            }
        Log.v(TAG, "previousPosition=$previousPosition")

        if (pagingAdapter != null) {
            Log.v(TAG, "pagingAdapter.isInitialized")
            itemBridgeAdapter.setAdapter(pagingAdapter)
            if (selectedPosition != -1) {
                gridView.selectedPosition = selectedPosition
            }
            showOrHideViews()
        }

        viewModel.loadingStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is StashGridViewModel.LoadingStatus.AdapterReady -> {
                    val scrollToNextPage = viewModel.scrollToNextPage.value ?: false
                    Log.v(
                        TAG,
                        "LoadingStatus.AdapterReady: previousPosition=$previousPosition, scrollToNextPage=$scrollToNextPage",
                    )
                    pagingAdapter = status.pagingAdapter
                    itemBridgeAdapter.setAdapter(status.pagingAdapter)
                    if (previousPosition > 0) {
                        jumpTo(previousPosition)
                    } else if (previousPosition < 0 && scrollToNextPage) {
                        val page =
                            PreferenceManager
                                .getDefaultSharedPreferences(requireContext())
                                .getInt("maxSearchResults", 25)
                        jumpTo(page)
                        // Only scroll the first time
                        viewModel.scrollToNextPage.value = false
                    }
                    loadingProgressBar.hide()
                    if (requestFocus) {
                        gridView.requestFocus()
                    }
                    showOrHideViews()
                }

                StashGridViewModel.LoadingStatus.FirstPageLoaded -> {
                    Log.v(TAG, "LoadingStatus.FirstPageLoaded")
                    if (requestFocus) {
                        gridView.requestFocus()
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

        prefBackPressScrollEnabled =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_back_button_scroll), true)

        if (prefBackPressScrollEnabled) {
            onBackPressedCallback =
                requireActivity().onBackPressedDispatcher.addCallback(
                    viewLifecycleOwner,
                    selectedPosition >= numberOfColumns,
                ) {
                    jumpTo(0)
                    isEnabled = false
                }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.v(TAG, "onSaveInstanceState")
        super.onSaveInstanceState(outState)
        outState.putInt("mSelectedPosition", selectedPosition)
        outState.putDataType(dataType)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewStateRestored")
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onDestroyView() {
        Log.v(TAG, "onDestroyView")
        super.onDestroyView()
    }

    private fun showOrHideViews() {
        val count = pagingAdapter?.size() ?: -1
        if (count <= 0) {
            positionTextView.text = getString(R.string.zero)
            noResultsTextView.animateToVisible()
            loadingProgressBar.hide()
            jumpButtonLayout.animateToInvisible(View.GONE)
        } else {
            setupJumpButtons(count)
            jumpButtonLayout.animateToVisible()
            noResultsTextView.animateToInvisible(View.GONE)
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
        } else if (SortOption.isJumpSupported(dataType)) {
            // If the data type supports jump, let the buttons occupy space
            alphabetFilterLayout.animateToInvisible(View.INVISIBLE, 400L)
        } else {
            // If the data type doesn't support jumping, use the space for the grid
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
        val columns = numberOfColumns
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
        Log.v(TAG, "Setup jumps: $jump1 & $jump2, count=$count")
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
        jumpButtonLayout.forEach {
            it.onFocusChangeListener = backPressFocusChangeListener
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
                        gridView
                    } else {
                        null
                    }
                } else if (focused != null && focused in jumpButtonLayout) {
                    if (direction == View.FOCUS_RIGHT) {
                        gridView
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

    fun cleanup() {
        pagingAdapter?.clearCache()
    }

    override fun onKeyLongPress(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && prefBackPressScrollEnabled) {
            if (DEBUG) Log.d(TAG, "Long press back, maybe $selectedPosition=>$previousPosition")
            if (previousPosition >= 0 &&
                previousPosition != selectedPosition &&
                requireActivity().currentFocus is StashImageCardView
            ) {
                jumpTo(previousPosition)
                return true
            }
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_D && event.isCtrlPressed) {
            requireParentFragment().childFragmentManager.commit {
                add(requireView().id, FilterDebugFragment())
            }
            return true
        }
        // If play is pressed and the page contains scenes or markers and user is focused on a card (ie not a button)
        if ((keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) &&
            (dataType == DataType.SCENE || dataType == DataType.MARKER) &&
            requireActivity().currentFocus is StashImageCardView
        ) {
            val position = selectedPosition
            val item = pagingAdapter?.get(position)
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
        private const val TAG = "StashDataGridFragment"

        private const val DEBUG = false
    }

    inner class VerticalGridItemBridgeAdapter : ItemBridgeAdapter() {
        @SuppressLint("RestrictedApi")
        override fun onCreate(viewHolder: ViewHolder) {
            if (viewHolder.itemView is ViewGroup) {
                TransitionHelper.setTransitionGroup(
                    viewHolder.itemView as ViewGroup,
                    true,
                )
            }
            mShadowOverlayHelper.onViewCreated(viewHolder.itemView)
        }

        public override fun onBind(itemViewHolder: ViewHolder) {
            // Only when having an OnItemClickListener, we attach the OnClickListener.
            if (onItemViewClickedListener != null) {
                val itemView = itemViewHolder.viewHolder.view
                itemView.setOnClickListener {
                    if (onItemViewClickedListener != null) {
                        // Row is always null
                        onItemViewClickedListener!!.onItemClicked(
                            itemViewHolder.viewHolder,
                            itemViewHolder.item,
                            null,
                            null,
                        )
                    }
                }
            }
        }

        public override fun onUnbind(viewHolder: ViewHolder) {
            if (onItemViewClickedListener != null) {
                viewHolder.viewHolder.view.setOnClickListener(null)
            }
        }

        public override fun onAttachedToWindow(viewHolder: ViewHolder) {
            viewHolder.itemView.isActivated = true
        }
    }
}
