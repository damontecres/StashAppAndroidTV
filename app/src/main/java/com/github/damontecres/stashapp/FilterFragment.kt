package com.github.damontecres.stashapp

import android.content.Context
import android.content.Intent
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
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.CreateFilterActivity
import com.github.damontecres.stashapp.filter.FilterOptions
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.toFilterArgs
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.getMaxMeasuredWidth
import com.github.damontecres.stashapp.util.putDataType
import com.github.damontecres.stashapp.util.putFilterArgs
import com.github.damontecres.stashapp.views.ImageAndFilter
import com.github.damontecres.stashapp.views.PlayAllOnClickListener
import com.github.damontecres.stashapp.views.SlideshowOnClickListener
import com.github.damontecres.stashapp.views.SortButtonManager
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import kotlinx.coroutines.launch

/**
 * An activity that displays items of a single [DataType] in a [StashGridFragment].
 *
 * This activity manages the sort (delegating to the [StashGridFragment]) and can also retrieve saved filters.
 */
class FilterFragment :
    Fragment(R.layout.filter_list),
    KeyEvent.Callback {
    private lateinit var titleTextView: TextView
    private lateinit var filterButton: Button
    private lateinit var sortButton: Button
    private lateinit var playAllButton: Button

    private lateinit var sortButtonManager: SortButtonManager

    private lateinit var dataType: DataType

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        filterButton = view.findViewById(R.id.filter_button)
        filterButton.setOnClickListener {
            Toast.makeText(requireContext(), "Filters not loaded yet!", Toast.LENGTH_SHORT).show()
        }
        val onFocusChangeListener = StashOnFocusChangeListener(requireContext())
        filterButton.onFocusChangeListener = onFocusChangeListener

        sortButton = view.findViewById(R.id.sort_button)
        sortButton.onFocusChangeListener = onFocusChangeListener
        playAllButton = view.findViewById(R.id.play_all_button)
        playAllButton.onFocusChangeListener = onFocusChangeListener
        titleTextView = view.findViewById(R.id.list_title)

        sortButtonManager =
            SortButtonManager(StashServer.getCurrentServerVersion()) { sortAndDirection ->
                val fragment =
                    childFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment
                fragment.refresh(sortAndDirection)
            }

        childFragmentManager.addFragmentOnAttachListener { _, fragment ->
            setTitleText((fragment as StashGridFragment).filterArgs)
        }
        childFragmentManager.addOnBackStackChangedListener {
            val fragment =
                childFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment?
            if (fragment != null) {
                val fa = fragment.filterArgs
                setTitleText(fa)
                sortButtonManager.setUpSortButton(sortButton, fa.dataType, fa.sortAndDirection)
            }
        }

        val dest = requireArguments().getDestination<Destination.Filter>()

        val startingFilter = dest.filterArgs
        dataType = startingFilter.dataType
        if (savedInstanceState == null) {
            setup(startingFilter, first = true)
        }

        if (startingFilter.dataType.supportsPlaylists) {
            playAllButton.visibility = View.VISIBLE
            playAllButton.setOnClickListener(
                PlayAllOnClickListener(
                    requireContext(),
                    startingFilter.dataType,
                ) {
                    val fragment =
                        childFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment
                    fragment.filterArgs
                },
            )
        } else if (startingFilter.dataType == DataType.IMAGE) {
            playAllButton.visibility = View.VISIBLE
            playAllButton.text = getString(R.string.play_slideshow)
            playAllButton.setOnClickListener(
                SlideshowOnClickListener(requireContext()) {
                    val fragment =
                        childFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment
                    val item = fragment.get(0) as ImageData?
                    ImageAndFilter(0, item, fragment.filterArgs)
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(StashCoroutineExceptionHandler()) {
            populateSavedFilters(dataType)
        }
    }

    private fun setTitleText(filterArgs: FilterArgs) {
        titleTextView.text = filterArgs.name ?: getString(filterArgs.dataType.pluralStringId)
    }

//    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
//        super.onRestoreInstanceState(savedInstanceState)
//        val fragment =
//            childFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment?
//        if (fragment != null) {
//            setTitleText(fragment.filterArgs)
//            sortButtonManager.setUpSortButton(
//                sortButton,
//                fragment.dataType,
//                fragment.filterArgs.sortAndDirection,
//            )
//        }
//    }

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
                    TODO()
                    val intent =
                        Intent(requireContext(), CreateFilterActivity::class.java)
                            .putDataType(dataType)
                    if (position == 1) {
                        val fragment =
                            childFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment
                        val filter = fragment.filterArgs
                        intent.putFilterArgs(CreateFilterActivity.INTENT_STARTING_FILTER, filter)
                    }
                    requireContext().startActivity(intent)
                } else {
                    val savedFilter =
                        savedFilters[lookupPos]
                    try {
                        val filterArgs =
                            savedFilter
                                .toFilterArgs(filterParser)
                                .withResolvedRandom()
                        setup(filterArgs)
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

    private fun setup(
        filter: FilterArgs,
        first: Boolean = false,
    ) {
        val scrollToNextPage = requireArguments().getDestination<Destination.Filter>().scrollToNextPage
        val fragment = StashGridFragment(filter, null, scrollToNextPage)
        fragment.name = filter.name
        fragment.disableButtons()
        fragment.requestFocus = true
        childFragmentManager.commit {
            if (!first) {
                addToBackStack(fragment.name)
            }
            replace(R.id.list_fragment, fragment)
        }
        sortButtonManager.setUpSortButton(sortButton, filter.dataType, filter.sortAndDirection)
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
            childFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment?
        return fragment?.onKeyUp(keyCode, event) ?: false
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        val fragment =
            childFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment?
        return fragment?.onKeyDown(keyCode, event) ?: false
    }

    override fun onKeyLongPress(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        val fragment =
            childFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment?
        return fragment?.onKeyLongPress(keyCode, event) ?: false
    }

    override fun onKeyMultiple(
        keyCode: Int,
        repeatCount: Int,
        event: KeyEvent,
    ): Boolean {
        val fragment =
            childFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment?
        return fragment?.onKeyMultiple(keyCode, repeatCount, event) ?: false
    }

    companion object {
        private const val TAG = "FilterListActivity2"
        const val INTENT_FILTER_ARGS = "$TAG.filterArgs"
        const val INTENT_SCROLL_NEXT_PAGE = "$TAG.scrollToNextPage"
    }
}
