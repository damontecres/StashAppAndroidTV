package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.chrynan.parcelable.core.getParcelableExtra
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.CreateFilterActivity
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.toFilterArgs
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getMaxMeasuredWidth
import com.github.damontecres.stashapp.util.parcelable
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

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

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

        val startingFilter =
            intent.getParcelableExtra(INTENT_FILTER_ARGS, FilterArgs::class, 0, parcelable)!!
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
        if (savedFilters.isEmpty()) {
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
                if (dataType == DataType.SCENE || dataType == DataType.PERFORMER) {
                    // TODO: add a separator
                    listOf(
                        "Create Filter",
                        "Create Filter from current",
                    ) + savedFilters.map { it.name.ifBlank { getString(dataType.pluralStringId) } }
                } else {
                    savedFilters.map { it.name.ifBlank { getString(dataType.pluralStringId) } }
                }
            val adapter =
                ArrayAdapter(
                    context,
                    R.layout.popup_item,
                    adapterItems,
                )
            listPopUp.setAdapter(adapter)
            listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
            listPopUp.anchorView = filterButton

            listPopUp.width = getMaxMeasuredWidth(context, adapter)
            listPopUp.isModal = true

            val filterParser = FilterParser(server.serverPreferences.serverVersion)
            listPopUp.setOnItemClickListener { parent: AdapterView<*>, view: View, position: Int, id: Long ->
                listPopUp.dismiss()
                val lookupPos =
                    if (dataType == DataType.SCENE || dataType == DataType.PERFORMER) {
                        position - 2
                    } else {
                        position
                    }
                if ((dataType == DataType.SCENE || dataType == DataType.PERFORMER) && (position == 0 || position == 1)) {
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
        fragment.sortButtonEnabled = false
        fragment.playAllButtonEnabled = false
        fragment.requestFocus = true
        supportFragmentManager.commit {
            if (!first) {
                addToBackStack(fragment.name)
            }
            replace(R.id.list_fragment, fragment)
        }
        sortButtonManager.setUpSortButton(sortButton, filter.dataType, filter.sortAndDirection)
    }

    companion object {
        private const val TAG = "FilterListActivity2"
        const val INTENT_FILTER_ARGS = "$TAG.filterArgs"
        const val INTENT_SCROLL_NEXT_PAGE = "$TAG.scrollToNextPage"
    }
}
