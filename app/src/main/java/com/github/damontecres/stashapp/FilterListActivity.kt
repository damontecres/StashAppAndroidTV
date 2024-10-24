package com.github.damontecres.stashapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.CreateFilterActivity
import com.github.damontecres.stashapp.filter.FilterOptions
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.toFilterArgs
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getFilterArgs
import com.github.damontecres.stashapp.util.getMaxMeasuredWidth
import com.github.damontecres.stashapp.util.putDataType
import com.github.damontecres.stashapp.util.putFilterArgs
import com.github.damontecres.stashapp.views.PlayAllOnClickListener
import com.github.damontecres.stashapp.views.SortButtonManager
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import kotlinx.coroutines.launch

/**
 * An activity that displays items of a single [DataType] in a [StashGridFragment].
 *
 * This activity manages the sort (delegating to the [StashGridFragment]) and can also retrieve saved filters.
 */
class FilterListActivity : FragmentActivity(R.layout.filter_list) {
    private lateinit var titleTextView: TextView
    private lateinit var filterButton: Button
    private lateinit var sortButton: Button
    private lateinit var playAllButton: Button

    private lateinit var sortButtonManager: SortButtonManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        filterButton = findViewById(R.id.filter_button)
        filterButton.setOnClickListener {
            Toast.makeText(this, "Filters not loaded yet!", Toast.LENGTH_SHORT).show()
        }
        val onFocusChangeListener = StashOnFocusChangeListener(this)
        filterButton.onFocusChangeListener = onFocusChangeListener

        sortButton = findViewById(R.id.sort_button)
        playAllButton = findViewById(R.id.play_all_button)
        titleTextView = findViewById(R.id.list_title)

        sortButtonManager =
            SortButtonManager(StashServer.getCurrentServerVersion()) { sortAndDirection ->
                val fragment =
                    supportFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment
                fragment.refresh(sortAndDirection)
            }

        supportFragmentManager.addFragmentOnAttachListener { _, fragment ->
            setTitleText((fragment as StashGridFragment).filterArgs)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment?
            if (fragment != null) {
                val fa = fragment.filterArgs
                setTitleText(fa)
                sortButtonManager.setUpSortButton(sortButton, fa.dataType, fa.sortAndDirection)
            }
        }

        val startingFilter = intent.getFilterArgs(INTENT_FILTER_ARGS)!!
        if (savedInstanceState == null) {
            setup(startingFilter, first = true)
        }

        if (startingFilter.dataType.supportsPlaylists) {
            playAllButton.visibility = View.VISIBLE
            playAllButton.setOnClickListener(
                PlayAllOnClickListener(
                    this,
                    startingFilter.dataType,
                ) {
                    val fragment =
                        supportFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment
                    fragment.filterArgs
                },
            )
        }

        lifecycleScope.launch(StashCoroutineExceptionHandler()) {
            populateSavedFilters(startingFilter.dataType)
        }
    }

    private fun setTitleText(filterArgs: FilterArgs) {
        titleTextView.text = filterArgs.name ?: getString(filterArgs.dataType.pluralStringId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val fragment =
            supportFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment?
        if (fragment != null) {
            setTitleText(fragment.filterArgs)
            sortButtonManager.setUpSortButton(
                sortButton,
                fragment.dataType,
                fragment.filterArgs.sortAndDirection,
            )
        }
    }

    private suspend fun populateSavedFilters(dataType: DataType) {
        val context = this@FilterListActivity
        val server = StashServer.requireCurrentServer()
        val savedFilters =
            QueryEngine(server).getSavedFilters(dataType)

        val createFilterSupported = dataType in FilterOptions.keys

        // Always show the list for data types supporting create filter
        if (savedFilters.isEmpty() && !createFilterSupported) {
            filterButton.setOnClickListener {
                Toast.makeText(
                    context,
                    "No saved filters found",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        } else {
            val listPopUp =
                ListPopupWindow(
                    context,
                    null,
                    android.R.attr.listPopupWindowStyle,
                )
            val adapterItems =
                savedFilters.map { it.name.ifBlank { getString(dataType.pluralStringId) } }
            val adapter = SavedFilterAdapter(context, createFilterSupported, adapterItems)

            listPopUp.setAdapter(adapter)
            listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
            listPopUp.anchorView = filterButton

            listPopUp.width = getMaxMeasuredWidth(context, adapter)
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
                    val intent =
                        Intent(this@FilterListActivity, CreateFilterActivity::class.java)
                            .putDataType(dataType)
                    if (position == 1) {
                        val fragment =
                            supportFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment
                        val filter = fragment.filterArgs
                        intent.putFilterArgs(CreateFilterActivity.INTENT_STARTING_FILTER, filter)
                    }
                    this@FilterListActivity.startActivity(intent)
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
                        Toast.makeText(
                            this@FilterListActivity,
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
        val scrollToNextPage = first && intent.getBooleanExtra(INTENT_SCROLL_NEXT_PAGE, false)
        val fragment = StashGridFragment(filter, null, scrollToNextPage)
        if (filter.dataType == DataType.IMAGE) {
            fragment.withImageGridClickListener()
        }
        fragment.name = filter.name
        fragment.disableButtons()
        fragment.requestFocus = true
        supportFragmentManager.commit {
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

        override fun getCount(): Int {
            return if (createEnabled) {
                filters.size + 3
            } else {
                filters.size
            }
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

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup?,
        ): View {
            return if (convertView != null) {
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
        }

        override fun areAllItemsEnabled(): Boolean {
            return !createEnabled
        }

        override fun isEnabled(position: Int): Boolean {
            return !(createEnabled && position == 2)
        }

        override fun getItemViewType(position: Int): Int {
            return if (isEnabled(position)) {
                0
            } else {
                1
            }
        }

        override fun getViewTypeCount(): Int {
            return if (createEnabled) {
                2
            } else {
                1
            }
        }
    }

    companion object {
        private const val TAG = "FilterListActivity2"
        const val INTENT_FILTER_ARGS = "$TAG.filterArgs"
        const val INTENT_SCROLL_NEXT_PAGE = "$TAG.scrollToNextPage"
    }
}
