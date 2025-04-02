package com.github.damontecres.stashapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.leanback.widget.SearchEditText
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.FilterOptions
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.presenters.NullPresenter
import com.github.damontecres.stashapp.presenters.NullPresenterSelector
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.suppliers.toFilterArgs
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.addExtraGridLongClicks
import com.github.damontecres.stashapp.util.calculatePageSize
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.getFilterArgs
import com.github.damontecres.stashapp.util.getMaxMeasuredWidth
import com.github.damontecres.stashapp.util.putFilterArgs
import com.github.damontecres.stashapp.views.PlayAllOnClickListener
import com.github.damontecres.stashapp.views.SortButtonManager
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.TitleTransitionHelper
import com.github.damontecres.stashapp.views.models.ServerViewModel
import com.github.damontecres.stashapp.views.models.StashGridViewModel
import kotlinx.coroutines.launch

/**
 * Displays items of a single [DataType] in a [StashDataGridFragment].
 *
 * This fragment manages the sort (delegating to the [StashDataGridFragment]) and can also retrieve saved filters.
 */
class FilterFragment :
    Fragment(R.layout.filter_list),
    KeyEvent.Callback {
    private val serverViewModel: ServerViewModel by activityViewModels()
    private val stashGridViewModel: StashGridViewModel by viewModels()

    private lateinit var buttonBar: View
    private lateinit var titleTextView: TextView
    private lateinit var filterButton: Button
    private lateinit var sortButton: Button
    private lateinit var playAllButton: Button
    private lateinit var searchEditText: SearchEditText

    private lateinit var sortButtonManager: SortButtonManager
    private lateinit var headerTransitionHelper: TitleTransitionHelper

    private lateinit var dataType: DataType

    private lateinit var fragment: StashDataGridFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dest = requireArguments().getDestination<Destination.Filter>()
        val startingFilter = dest.filterArgs
        dataType = startingFilter.dataType
        Log.d(TAG, "onCreate: dataType=$dataType")

        val presenterSelector = StashPresenter.defaultClassPresenterSelector()
        addExtraGridLongClicks(presenterSelector, dataType) {
            FilterAndPosition(
                stashGridViewModel.filterArgs.value!!,
                stashGridViewModel.currentPosition.value ?: -1,
            )
        }
        stashGridViewModel.init(
            NullPresenterSelector(
                presenterSelector,
                NullPresenter(dataType),
            ),
            calculatePageSize(requireContext(), dataType),
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val dest = requireArguments().getDestination<Destination.Filter>()

        fragment =
            childFragmentManager.findFragmentById(R.id.list_fragment) as StashDataGridFragment
        Log.v(
            TAG,
            "filterArgs.isInitialized=${stashGridViewModel.filterArgs.isInitialized}, " +
                "savedInstanceState.isNull=${savedInstanceState == null}",
        )
        if (!stashGridViewModel.scrollToNextPage.isInitialized || stashGridViewModel.scrollToNextPage.value == null) {
            stashGridViewModel.scrollToNextPage.value = dest.scrollToNextPage
        }
        fragment.requestFocus = true
        fragment.init(dataType)

        serverViewModel.currentServer.observe(viewLifecycleOwner) {
            sortButtonManager =
                SortButtonManager(StashServer.getCurrentServerVersion()) { sortAndDirection ->
                    stashGridViewModel.setFilter(sortAndDirection)
                }
            val filter =
                if (stashGridViewModel.filterArgs.isInitialized) {
                    stashGridViewModel.filterArgs.value!!
                } else if (savedInstanceState != null) {
                    val restoredFilter =
                        savedInstanceState.getFilterArgs(STATE_FILTER) ?: dest.filterArgs
                    stashGridViewModel.setFilter(restoredFilter)
                    restoredFilter
                } else {
                    stashGridViewModel.setFilter(dest.filterArgs)
                    dest.filterArgs
                }

            filterButton = view.findViewById(R.id.filter_button)
            filterButton.setOnClickListener {
                Toast
                    .makeText(requireContext(), "Filters not loaded yet!", Toast.LENGTH_SHORT)
                    .show()
            }
            val onFocusChangeListener = StashOnFocusChangeListener(requireContext())
            filterButton.onFocusChangeListener = onFocusChangeListener
            lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                populateSavedFilters(dataType)
            }

            buttonBar = view.findViewById(R.id.button_bar)

            sortButton = view.findViewById(R.id.sort_button)
            sortButton.onFocusChangeListener = onFocusChangeListener
            playAllButton = view.findViewById(R.id.play_all_button)
            playAllButton.onFocusChangeListener = onFocusChangeListener
            titleTextView = view.findViewById(R.id.list_title)
            titleTextView.text = filter.name ?: getString(dataType.pluralStringId)

            searchEditText = view.findViewById(R.id.search_edit_text)
            stashGridViewModel.setupSearch(searchEditText)

            headerTransitionHelper = TitleTransitionHelper(view as ViewGroup, buttonBar)
            stashGridViewModel.currentPosition.observe(viewLifecycleOwner) { position ->
                val shouldShowTitle = position < fragment.numberOfColumns
                headerTransitionHelper.showTitle(shouldShowTitle)
            }

            sortButtonManager.setUpSortButton(
                sortButton,
                filter.dataType,
                filter.sortAndDirection,
            )

            val playAllListener =
                PlayAllOnClickListener(serverViewModel.navigationManager, dataType) {
                    FilterAndPosition(stashGridViewModel.filterArgs.value!!, 0)
                }
            playAllButton.setOnClickListener(playAllListener)

            if (filter.dataType.supportsPlaylists) {
                playAllButton.visibility = View.VISIBLE
            } else if (filter.dataType == DataType.IMAGE) {
                playAllButton.visibility = View.VISIBLE
                playAllButton.text = getString(R.string.play_slideshow)
            }

            val initialRequestFocus = fragment.requestFocus
            if (initialRequestFocus) {
                stashGridViewModel.searchBarFocus.observe(viewLifecycleOwner) { hasFocus ->
                    // If the search text has focus, then the fragment shouldn't take it
                    fragment.requestFocus = !hasFocus
                }
            }
        }
    }

    private suspend fun populateSavedFilters(dataType: DataType) {
        val server = StashServer.requireCurrentServer()
        val savedFilters =
            QueryEngine(server).getSavedFilters(dataType)

        val createFilterSupported = dataType in FilterOptions.keys

        // Always show the list for data types supporting create filter
        if (savedFilters.isEmpty() && !createFilterSupported) {
            filterButton.setOnClickListener {
                Toast
                    .makeText(
                        requireContext(),
                        "No saved filters found",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        } else {
            val listPopUp =
                ListPopupWindow(
                    requireContext(),
                    null,
                    android.R.attr.listPopupWindowStyle,
                )
            val adapterItems =
                savedFilters.map { it.name.ifBlank { getString(dataType.pluralStringId) } }
            val adapter = SavedFilterAdapter(requireContext(), createFilterSupported, adapterItems)

            listPopUp.setAdapter(adapter)
            listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
            listPopUp.anchorView = filterButton

            listPopUp.width = getMaxMeasuredWidth(requireContext(), adapter)
            listPopUp.isModal = true

            val filterParser = FilterParser(server.serverPreferences.serverVersion)
            listPopUp.setOnItemClickListener { parent: AdapterView<*>, view: View, position: Int, id: Long ->
                Log.v(TAG, "filter list clicked position=$position")
                listPopUp.dismiss()
                val lookupPos =
                    if (adapter.createEnabled) {
                        position - 3
                    } else {
                        position
                    }
                if (adapter.createEnabled && (position == 0 || position == 1)) {
                    val destination =
                        if (position == 1) {
                            // Create from current
                            Destination.CreateFilter(
                                fragment.dataType,
                                stashGridViewModel.filterArgs.value!!,
                            )
                        } else {
                            Destination.CreateFilter(fragment.dataType, null)
                        }
                    serverViewModel.navigationManager.navigate(destination)
                } else {
                    val savedFilter =
                        savedFilters[lookupPos]
                    try {
                        val filterArgs =
                            savedFilter
                                .toFilterArgs(filterParser)
                                .withResolvedRandom()
                        fragment.cleanup()
                        serverViewModel.navigationManager.navigate(Destination.Filter(filterArgs))
                    } catch (ex: Exception) {
                        Log.e(TAG, "Exception parsing filter ${savedFilter.id}", ex)
                        Toast
                            .makeText(
                                requireContext(),
                                "Error with filter ${savedFilter.id}! Probably a bug: ${ex.message}",
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                }
            }

            filterButton.setOnClickListener {
                listPopUp.show()
                listPopUp.listView?.requestFocus()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (stashGridViewModel.filterArgs.isInitialized) {
            outState.putFilterArgs(STATE_FILTER, stashGridViewModel.filterArgs.value!!)
        }
    }

    private class SavedFilterAdapter(
        context: Context,
        val createEnabled: Boolean,
        val filters: List<String>,
    ) : BaseAdapter() {
        private val inflater = LayoutInflater.from(context)

        override fun getCount(): Int =
            if (createEnabled) {
                filters.size + 3
            } else {
                filters.size
            }

        override fun getItem(position: Int): Any {
            if (createEnabled) {
                return when (position) {
                    0 -> "Create filter"
                    1 -> "Create filter from current"
                    2 ->
                        if (filters.isEmpty()) {
                            "No saved filters"
                        } else {
                            "Saved filters"
                        }

                    else -> filters[position - 3]
                }
            }
            return filters[position]
        }

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup?,
        ): View =
            if (convertView != null) {
                (convertView as TextView).text = getItem(position).toString()
                convertView
            } else if (createEnabled && position == 2) {
                // header
                val view = inflater.inflate(R.layout.popup_header, parent, false) as TextView
                view.text = getItem(position).toString()
                view
            } else {
                // regular item
                val view = inflater.inflate(R.layout.popup_item, parent, false) as TextView
                view.text = getItem(position).toString()
                view
            }

        override fun areAllItemsEnabled(): Boolean = !createEnabled

        override fun isEnabled(position: Int): Boolean = !(createEnabled && position == 2)

        override fun getItemViewType(position: Int): Int =
            if (isEnabled(position)) {
                0
            } else {
                1
            }

        override fun getViewTypeCount(): Int =
            if (createEnabled) {
                2
            } else {
                1
            }
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        val fragment =
            childFragmentManager.findFragmentById(R.id.list_fragment) as StashDataGridFragment?
        return fragment?.onKeyUp(keyCode, event) ?: false
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        val fragment =
            childFragmentManager.findFragmentById(R.id.list_fragment) as StashDataGridFragment?
        return fragment?.onKeyDown(keyCode, event) ?: false
    }

    override fun onKeyLongPress(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        val fragment =
            childFragmentManager.findFragmentById(R.id.list_fragment) as StashDataGridFragment?
        return fragment?.onKeyLongPress(keyCode, event) ?: false
    }

    override fun onKeyMultiple(
        keyCode: Int,
        repeatCount: Int,
        event: KeyEvent,
    ): Boolean {
        val fragment =
            childFragmentManager.findFragmentById(R.id.list_fragment) as StashDataGridFragment?
        return fragment?.onKeyMultiple(keyCode, repeatCount, event) ?: false
    }

    companion object {
        private const val TAG = "FilterFragment"
        private const val STATE_FILTER = "currentFilter"
    }
}
