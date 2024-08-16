package com.github.damontecres.stashapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.view.get
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.AppFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.FilterType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.data.toStashFindFilter
import com.github.damontecres.stashapp.playback.PlaybackMarkersActivity
import com.github.damontecres.stashapp.playback.PlaybackMarkersFragment
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.getMaxMeasuredWidth
import com.github.damontecres.stashapp.util.getRandomSort
import com.github.damontecres.stashapp.util.toFindFilterType
import com.github.damontecres.stashapp.views.ImageGridClickedListener
import com.github.damontecres.stashapp.views.SortButtonManager
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import com.github.damontecres.stashapp.views.showSimpleListPopupWindow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilterListActivity : FragmentActivity() {
    private lateinit var titleTextView: TextView
    private lateinit var sortButton: Button
    private lateinit var playMarkersButton: Button
    private lateinit var queryEngine: QueryEngine
    private lateinit var dataType: DataType
    private lateinit var sortOptions: List<Pair<String, String>>
    private lateinit var sortButtonManager: SortButtonManager

    private var filter: StashFilter? = null
        set(newFilter) {
            field = newFilter
            if (dataType == DataType.MARKER) {
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
                        Log.v(TAG, "playMarkersButton clicked: newFilter=$newFilter")
                        val intent = Intent(this, PlaybackMarkersActivity::class.java)
                        intent.putExtra(PlaybackMarkersFragment.INTENT_FILTER_ID, newFilter)
                        intent.putExtra(PlaybackMarkersFragment.INTENT_DURATION_ID, duration)
                        startActivity(intent)
                    }
                }
            }
        }
    private var filterData: SavedFilterData? = null

    // Track the saved filter by name so when popping the back stack, we can restore the sort by
    // This is a bit hacky, but the DB enforces a unique mode+name for a saved filter, so it works
    private val filterDataByName = mutableMapOf<String, SavedFilterData?>()

    private lateinit var manager: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = PreferenceManager.getDefaultSharedPreferences(this)
        queryEngine = QueryEngine(this, true)

        setContentView(R.layout.filter_list)

        val dataTypeStr = intent.getStringExtra("dataType")
        dataType =
            if (dataTypeStr != null) {
                DataType.valueOf(dataTypeStr)
            } else {
                DataType.SCENE
            }

        // Resolve the strings, then sort
        sortOptions =
            dataType.sortOptions.map {
                Pair(
                    it.key,
                    this@FilterListActivity.getString(it.nameStringId),
                )
            }.sortedBy { it.second }

        val filterButton = findViewById<Button>(R.id.filter_button)
        filterButton.setOnClickListener {
            Toast.makeText(this, "Filters not loaded yet!", Toast.LENGTH_SHORT).show()
        }
        val onFocusChangeListener = StashOnFocusChangeListener(this)
        filterButton.onFocusChangeListener = onFocusChangeListener

        sortButton = findViewById(R.id.sort_button)
        playMarkersButton = findViewById(R.id.play_makers_button)

        val experimentalEnabled =
            manager.getBoolean(getString(R.string.pref_key_experimental_features), false)
        if (experimentalEnabled && dataType == DataType.MARKER) {
            playMarkersButton.visibility = View.VISIBLE
        }
        sortButtonManager =
            SortButtonManager { sortAndDirection ->
                val newFilter =
                    filterData!!.copy(
                        find_filter =
                            SavedFilterData.Find_filter(
                                q = null,
                                page = null,
                                per_page = null,
                                sort = sortAndDirection.sort,
                                direction = sortAndDirection.direction,
                                __typename = "",
                            ),
                    )
                this@FilterListActivity.filterData = newFilter
                filter =
                    copyFilter(
                        filter,
                        newFilter.find_filter?.sort,
                        newFilter.find_filter?.direction,
                    )
                setupFragment(newFilter, false, false)
                filterData = newFilter
            }

        titleTextView = findViewById(R.id.list_title)
        supportFragmentManager.addFragmentOnAttachListener { _, fragment ->
            val name = (fragment as StashGridFragment2).name
            titleTextView.text = name
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment2?
            titleTextView.text = fragment?.name
            filterData = filterDataByName[fragment?.name]

            sortButtonManager.setUpSortButton(sortButton, dataType, getSortAndDirection(filterData))
        }
        val exHandler =
            CoroutineExceptionHandler { _, ex: Throwable ->
                Log.e(TAG, "Error in filter coroutine", ex)
                Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_LONG).show()
            }

        lifecycleScope.launch(exHandler) {
            if (savedInstanceState == null) {
                val startingFilter = getStartingFilter()
                if (startingFilter.second != null) {
                    val filterData = startingFilter.second!!
                    this@FilterListActivity.filterData = filterData
                    filter =
                        when (startingFilter.first) {
                            FilterType.CUSTOM_FILTER -> {
                                StashCustomFilter(
                                    mode = filterData.mode,
                                    direction = filterData.find_filter?.direction?.toString(),
                                    sortBy = filterData.find_filter?.sort,
                                    description = "",
                                    query = filterData.find_filter?.q,
                                )
                            }

                            FilterType.SAVED_FILTER -> {
                                StashSavedFilter(
                                    savedFilterId = filterData.id,
                                    mode = filterData.mode,
                                    sortBy = filterData.find_filter?.sort,
                                    direction = filterData.find_filter?.direction?.toString(),
                                )
                            }

                            FilterType.APP_FILTER -> {
                                filter
                            }
                        }
                    setupFragment(filterData, true)
                } else {
                    Log.e(TAG, "No starting filter found for $dataType was null")
                    finish()
                }
            }
        }
        lifecycleScope.launch(exHandler) {
            val savedFilters = queryEngine.getSavedFilters(dataType)
            if (savedFilters.isEmpty()) {
                filterButton.setOnClickListener {
                    Toast.makeText(
                        this@FilterListActivity,
                        "No saved filters found",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            } else {
                val listPopUp =
                    ListPopupWindow(
                        this@FilterListActivity,
                        null,
                        android.R.attr.listPopupWindowStyle,
                    )
                val adapter =
                    ArrayAdapter(
                        this@FilterListActivity,
                        R.layout.popup_item,
                        savedFilters.map { it.name },
                    )
                listPopUp.setAdapter(adapter)
                listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
                listPopUp.anchorView = filterButton

                listPopUp.width = getMaxMeasuredWidth(this@FilterListActivity, adapter)
                listPopUp.isModal = true

                listPopUp.setOnItemClickListener { parent: AdapterView<*>, view: View, position: Int, id: Long ->
                    var savedFilter = savedFilters[position]
                    savedFilter =
                        if (savedFilter.find_filter != null &&
                            savedFilter.find_filter!!.sort?.startsWith("random_") == true
                        ) {
                            val newFindFilter =
                                savedFilter.find_filter!!.copy(sort = getRandomSort())
                            savedFilter.copy(find_filter = newFindFilter)
                        } else {
                            savedFilter
                        }
                    this@FilterListActivity.filterData = savedFilter
                    listPopUp.dismiss()
                    filter = StashSavedFilter(savedFilter)
                    setupFragment(savedFilter, false)
                }

                filterButton.setOnClickListener {
                    listPopUp.show()
                    listPopUp.listView?.requestFocus()
                }
            }
        }
    }

    private fun copyFilter(
        filter: StashFilter?,
        sortBy: String?,
        sortDirection: SortDirectionEnum?,
    ): StashFilter? {
        return when (filter) {
            is AppFilter -> filter
            is StashCustomFilter ->
                filter.copy(
                    sortBy = sortBy,
                    direction = sortDirection?.toString(),
                )

            is StashSavedFilter ->
                filter.copy(
                    sortBy = sortBy,
                    direction = sortDirection?.toString(),
                )

            else -> filter
        }
    }

    private suspend fun getStartingFilter(): Pair<FilterType, SavedFilterData?> =
        withContext(Dispatchers.IO) {
            val appFilter: StashFilter? = intent.getParcelableExtra("filter")
            if (appFilter != null && appFilter is AppFilter) {
                Log.v(TAG, "getStartingFilter: filter is AppFilter=$filter")
                return@withContext Pair(
                    FilterType.APP_FILTER,
                    appFilter.toSavedFilterData(this@FilterListActivity),
                )
            }
            val savedFilterId = intent.getStringExtra("savedFilterId")
            val direction = intent.getStringExtra("direction")
            val sortBy = intent.getStringExtra("sortBy")
            val dataTypeStr = intent.getStringExtra("dataType")
            val description = intent.getStringExtra("description")
            val dataType =
                if (dataTypeStr != null) {
                    DataType.valueOf(dataTypeStr)
                } else {
                    throw RuntimeException("dataType is required")
                }
            val query = intent.getStringExtra("query")
            if (savedFilterId != null) {
                Log.v(TAG, "getStartingFilter: filter is a saved filter id=$savedFilterId")
                // Load a saved filter
                return@withContext Pair(
                    FilterType.SAVED_FILTER,
                    queryEngine.getSavedFilter(savedFilterId.toString()),
                )
            } else if (direction != null || sortBy != null || query != null) {
                Log.v(
                    TAG,
                    "getStartingFilter: filter is generic direction=$direction, sortBy=$sortBy, query=$query",
                )
                // Generic filter
                return@withContext Pair(
                    FilterType.CUSTOM_FILTER,
                    SavedFilterData(
                        id = "-1",
                        mode = dataType.filterMode,
                        name = description ?: getString(dataType.pluralStringId),
                        find_filter =
                            SavedFilterData.Find_filter(
                                q = query,
                                page = null,
                                per_page = null,
                                sort = sortBy,
                                direction = if (direction != null) SortDirectionEnum.valueOf(direction) else null,
                                __typename = "",
                            ),
                        object_filter = null,
                        ui_options = null,
                        __typename = "",
                    ),
                )
            } else {
                // Default filter
                val filter = queryEngine.getDefaultFilter(dataType)
                if (filter == null) {
                    Log.v(
                        TAG,
                        "getStartingFilter: filter is default from app for $dataType",
                    )
                    return@withContext Pair(
                        FilterType.CUSTOM_FILTER,
                        SavedFilterData(
                            id = "-1",
                            mode = dataType.filterMode,
                            name = description ?: getString(dataType.pluralStringId),
                            find_filter =
                                SavedFilterData.Find_filter(
                                    q = null,
                                    page = null,
                                    per_page = null,
                                    sort = dataType.defaultSort.sort,
                                    direction = dataType.defaultSort.direction,
                                    __typename = "",
                                ),
                            object_filter = null,
                            ui_options = null,
                            __typename = "",
                        ),
                    )
                } else {
                    Log.v(
                        TAG,
                        "getStartingFilter: filter is default from server for $dataType",
                    )
                    return@withContext Pair(FilterType.SAVED_FILTER, filter)
                }
            }
        }

    private fun setupFragment(
        filter: SavedFilterData,
        first: Boolean,
        addToBackStack: Boolean = true,
    ) {
        val dataType = DataType.fromFilterMode(filter.mode)!!
        val name =
            filter.name.ifBlank {
                getString(dataType.pluralStringId)
            }
        val scrollToNextPage =
            first &&
                manager.getBoolean(
                    "scrollToNextResult",
                    true,
                ) && intent.getBooleanExtra("moveOnePage", false)

        val fragment =
            try {
                getFragment(
                    name,
                    dataType,
                    filter.find_filter?.toFindFilterType(),
                    filter.object_filter,
                    scrollToNextPage,
                )
            } catch (ex: Exception) {
                Log.e(
                    TAG,
                    "Error fetching fragment for saved filter id=${filter.id}",
                    ex,
                )
                Toast.makeText(
                    this@FilterListActivity,
                    "Error occurred trying to setup filter! This might a bug.",
                    Toast.LENGTH_LONG,
                ).show()
                return
            }
        fragment.requestFocus = true
        if (dataType == DataType.IMAGE) {
            fragment.onItemViewClickedListener = ImageGridClickedListener(fragment)
        }
        filterDataByName[name] = filter

        sortButtonManager.setUpSortButton(sortButton, dataType, getSortAndDirection(filter))

        var transaction =
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.list_fragment,
                    fragment,
                )
        if (!first && addToBackStack && transaction.isAddToBackStackAllowed) {
            transaction = transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun getFragment(
        name: String,
        dataType: DataType,
        findFilter: FindFilterType?,
        objectFilter: Any?,
        scrollToNextPage: Boolean,
    ): StashGridFragment2 {
        val cardSize =
            PreferenceManager.getDefaultSharedPreferences(this)
                .getInt("cardSize", getString(R.string.card_size_default))
        val calculatedCardSize =
            (cardSize * (ScenePresenter.CARD_WIDTH.toDouble() / dataType.defaultCardWidth)).toInt()

        val fragment =
            StashGridFragment2(
                dataType = dataType,
                findFilter = findFilter?.toStashFindFilter(),
                objectFilter = objectFilter,
                cardSize = calculatedCardSize,
                scrollToNextPage = scrollToNextPage,
            )
        fragment.name = name
        return fragment
    }

    companion object {
        const val TAG = "FilterListActivity"
    }

    class SortByArrayAdapter(
        context: Context,
        items: List<String>,
        var currentIndex: Int,
        var currentDirection: SortDirectionEnum?,
    ) :
        ArrayAdapter<String>(context, R.layout.sort_popup_item, R.id.popup_item_text, items) {
        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup,
        ): View {
            val view = super.getView(position, convertView, parent)
            view as LinearLayout
            (view.get(0) as TextView).text =
                if (position == currentIndex) {
                    when (currentDirection) {
                        SortDirectionEnum.ASC -> context.getString(R.string.fa_caret_up)
                        SortDirectionEnum.DESC -> context.getString(R.string.fa_caret_down)
                        SortDirectionEnum.UNKNOWN__ -> null
                        null -> null
                    }
                } else {
                    null
                }
            return view
        }
    }

    fun getSortAndDirection(filter: SavedFilterData?): SortAndDirection {
        val sort = filter?.find_filter?.sort
        val direction = filter?.find_filter?.direction
        return if (sort != null && direction != null) {
            SortAndDirection(sort, direction)
        } else if (sort != null) {
            SortAndDirection(sort, SortDirectionEnum.ASC)
        } else {
            dataType.defaultSort
        }
    }
}
