package com.github.damontecres.stashapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.PresenterSelector
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.navigation.NavigationOnItemViewClickedListener
import com.github.damontecres.stashapp.presenters.NullPresenter
import com.github.damontecres.stashapp.presenters.NullPresenterSelector
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.DefaultKeyEventCallback
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.animateToInvisible
import com.github.damontecres.stashapp.util.animateToVisible
import com.github.damontecres.stashapp.util.getFilterArgs
import com.github.damontecres.stashapp.util.putFilterArgs
import com.github.damontecres.stashapp.views.PlayAllOnClickListener
import com.github.damontecres.stashapp.views.SortButtonManager
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.TitleTransitionHelper
import com.github.damontecres.stashapp.views.models.ServerViewModel
import com.github.damontecres.stashapp.views.models.StashGridViewModel
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * A [Fragment] that shows a [StashDataGridFragment] along with controls for sorting, etc
 */
class StashGridControlsFragment() :
    Fragment(),
    DefaultKeyEventCallback {
    private val serverViewModel: ServerViewModel by activityViewModels()
    private val viewModel: StashGridViewModel by viewModels()

    var currentFilter: FilterArgs
        get() = viewModel.filterArgs.value!!
        set(newFilter) = viewModel.setFilter(newFilter)

    // Views
    private lateinit var sortButton: Button
    private lateinit var playAllButton: Button
    private lateinit var filterButton: Button
    private lateinit var subContentSwitch: SwitchMaterial
    private lateinit var searchButton: SearchView

    private lateinit var fragment: StashDataGridFragment

    var titleView: View? = null

    private var remoteButtonPaging: Boolean = true

    // Arguments
    private lateinit var initialFilter: FilterArgs

    // State
    private var gridHeaderTransitionHelper: TitleTransitionHelper? = null
    private var scrollToNextPage = false

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

    // Unmodifiable properties, current state

    /**
     * Type of items being displayed
     */
    val dataType: DataType
        get() = initialFilter.dataType

    constructor(
        filterArgs: FilterArgs,
        scrollToNextPage: Boolean = false,
    ) : this() {
        this.initialFilter = filterArgs
        this.scrollToNextPage = scrollToNextPage
    }

    constructor(
        dataType: DataType,
        findFilter: StashFindFilter? = null,
        objectFilter: StashDataFilter? = null,
        scrollToNextPage: Boolean = false,
    ) : this(FilterArgs(dataType, null, findFilter, objectFilter), scrollToNextPage)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v(TAG, "onCreate: dataType=$dataType")

        viewModel.init(
            NullPresenterSelector(presenterSelector, NullPresenter(dataType)),
        )

        if (savedInstanceState != null) {
            name = savedInstanceState.getString(STATE_NAME)
            initialFilter = savedInstanceState.getFilterArgs(STATE_FILTER) ?: initialFilter
            Log.v(TAG, "sortAndDirection=${initialFilter.sortAndDirection}")
        }

        currentFilter = initialFilter

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
                R.layout.stash_grid_controls,
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
        searchButton = root.findViewById(R.id.search_button_view)
        searchButton.onFocusChangeListener = onFocusChangeListener

        if (name == null) {
            name = getString(dataType.pluralStringId)
        }

        fragment =
            childFragmentManager.findFragmentById(R.id.grid_fragment) as StashDataGridFragment
        fragment.scrollToNextPage = scrollToNextPage
        fragment.init(dataType)
        if (onItemViewClickedListener != null) {
            fragment.onItemViewClickedListener = onItemViewClickedListener
        }

        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.currentPosition.observe(viewLifecycleOwner) { position ->
            val shouldShowTitle = position < fragment.numberOfColumns
            if (DEBUG) {
                Log.v(
                    TAG,
                    "showOrHideTitle: $shouldShowTitle, position=$position",
                )
            }
            showTitle(shouldShowTitle)
        }

        val filter =
            if (viewModel.filterArgs.isInitialized) {
                viewModel.filterArgs.value!!
            } else if (savedInstanceState != null) {
                initialFilter = savedInstanceState.getFilterArgs(STATE_FILTER)!!
                viewModel.setFilter(initialFilter)
                initialFilter
            } else {
                viewModel.setFilter(initialFilter)
                initialFilter
            }

        val gridHeader = view.findViewById<View>(R.id.grid_header)
        gridHeaderTransitionHelper = TitleTransitionHelper(view as ViewGroup, gridHeader)

        sortButton.nextFocusUpId = R.id.tab_layout
        SortButtonManager(StashServer.getCurrentServerVersion()) {
            viewModel.setFilter(currentFilter.with(it))
        }.setUpSortButton(sortButton, dataType, filter.sortAndDirection)

        val playAllListener =
            PlayAllOnClickListener(serverViewModel.navigationManager, dataType) {
                FilterAndPosition(viewModel.filterArgs.value!!, 0)
            }
        playAllButton.setOnClickListener(playAllListener)

        if (dataType.supportsPlaylists) {
            playAllButton.visibility = View.VISIBLE
            playAllButton.nextFocusUpId = R.id.tab_layout
        } else if (dataType == DataType.IMAGE) {
            playAllButton.visibility = View.VISIBLE
            playAllButton.nextFocusUpId = R.id.tab_layout
            playAllButton.text = getString(R.string.play_slideshow)
        }

        filterButton.nextFocusUpId = R.id.tab_layout
        filterButton.setOnClickListener {
            serverViewModel.navigationManager.navigate(
                Destination.CreateFilter(
                    dataType,
                    viewModel.filterArgs.value!!,
                ),
            )
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

        viewModel.setupSearchButton(searchButton)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (viewModel.filterArgs.isInitialized) {
            outState.putFilterArgs(STATE_FILTER, currentFilter)
        } else {
            outState.putFilterArgs(STATE_FILTER, initialFilter)
        }
        outState.putString(STATE_NAME, name)
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

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean = fragment.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event)

    companion object {
        private const val TAG = "StashGridControlsFragment"

        private const val STATE_FILTER = "currentFilter"
        private const val STATE_NAME = "name"

        private const val DEBUG = false
    }
}
