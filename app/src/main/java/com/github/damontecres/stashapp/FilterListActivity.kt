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
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.playback.PlaybackMarkersActivity
import com.github.damontecres.stashapp.playback.PlaybackMarkersFragment
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.toFilterArgs
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.getMaxMeasuredWidth
import com.github.damontecres.stashapp.views.SortButtonManager
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.showSimpleListPopupWindow
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
    private lateinit var playMarkersButton: Button

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
        playMarkersButton = findViewById(R.id.play_makers_button)
        titleTextView = findViewById(R.id.list_title)

        sortButtonManager =
            SortButtonManager { sortAndDirection ->
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
                setTitleText(fragment.filterArgs)
            }
        }

        val startingFilter = intent.getParcelableExtra<FilterArgs>(INTENT_FILTER_ARGS)!!
        if (savedInstanceState == null) {
            setup(startingFilter, first = true)
        }

        val experimentalEnabled =
            preferences.getBoolean(getString(R.string.pref_key_experimental_features), false)
        if (experimentalEnabled && startingFilter.dataType == DataType.MARKER) {
            playMarkersButton.visibility = View.VISIBLE
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
        val filterParser = FilterParser(ServerPreferences(context).serverVersion)
        val savedFilters =
            QueryEngine(this).getSavedFilters(dataType)
                .map { it.toFilterArgs().ensureParsed(filterParser) }
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
            val adapter =
                ArrayAdapter(
                    context,
                    R.layout.popup_item,
                    savedFilters.map { it.name ?: getString(it.dataType.pluralStringId) },
                )
            listPopUp.setAdapter(adapter)
            listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
            listPopUp.anchorView = filterButton

            listPopUp.width = getMaxMeasuredWidth(context, adapter)
            listPopUp.isModal = true

            listPopUp.setOnItemClickListener { parent: AdapterView<*>, view: View, position: Int, id: Long ->
                listPopUp.dismiss()
                val savedFilter = savedFilters[position].withResolvedRandom()
                setup(savedFilter)
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
        fragment.requestFocus = true
        supportFragmentManager.commit {
            if (!first) {
                addToBackStack(fragment.name)
            }
            replace(R.id.list_fragment, fragment)
        }
        sortButtonManager.setUpSortButton(sortButton, filter.dataType, filter.sortAndDirection)
        if (filter.dataType == DataType.MARKER) {
            playMarkersButton.setOnClickListener {
                showSimpleListPopupWindow(
                    playMarkersButton,
                    listOf("3 seconds", "15 seconds", "20 seconds", "30 seconds"),
                ) {
                    val duration =
                        when (it) {
                            0 -> 3000L
                            1 -> 15_000L
                            2 -> 20_000L
                            3 -> 30_000L
                            else -> 30_000L
                        }
                    Log.v(TAG, "playMarkersButton clicked: newFilter=$filter")
                    val intent = Intent(this, PlaybackMarkersActivity::class.java)
                    intent.putExtra(PlaybackMarkersFragment.INTENT_FILTER_ID, filter)
                    intent.putExtra(PlaybackMarkersFragment.INTENT_DURATION_ID, duration)
                    startActivity(intent)
                }
            }
        }
    }

    companion object {
        private const val TAG = "FilterListActivity2"
        const val INTENT_FILTER_ARGS = "$TAG.filterArgs"
        const val INTENT_SCROLL_NEXT_PAGE = "$TAG.scrollToNextPage"
    }
}
