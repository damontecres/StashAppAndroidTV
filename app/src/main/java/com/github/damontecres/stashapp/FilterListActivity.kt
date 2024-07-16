package com.github.damontecres.stashapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
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
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.ObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.AppFilter
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.FilterType
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.suppliers.GalleryDataSupplier
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.suppliers.MarkerDataSupplier
import com.github.damontecres.stashapp.suppliers.MovieDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.suppliers.StudioDataSupplier
import com.github.damontecres.stashapp.suppliers.TagDataSupplier
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.GalleryComparator
import com.github.damontecres.stashapp.util.ImageComparator
import com.github.damontecres.stashapp.util.MarkerComparator
import com.github.damontecres.stashapp.util.MovieComparator
import com.github.damontecres.stashapp.util.PerformerComparator
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.SceneComparator
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StudioComparator
import com.github.damontecres.stashapp.util.TagComparator
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.getMaxMeasuredWidth
import com.github.damontecres.stashapp.util.getRandomSort
import com.github.damontecres.stashapp.util.toFindFilterType
import com.github.damontecres.stashapp.views.FontSpan
import com.github.damontecres.stashapp.views.ImageGridClickedListener
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
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

    private var filter: StashFilter? = null
        set(newFilter) {
            field = newFilter
            Log.v(TAG, "newFilter=$newFilter")
            if (dataType == DataType.MARKER) {
                playMarkersButton.setOnClickListener {
                    val intent = Intent(this, PlaybackMarkersActivity::class.java)
                    intent.putExtra(PlaybackMarkersFragment.INTENT_FILTER_ID, newFilter)
                    // TODO duration?
                    intent.putExtra(PlaybackMarkersFragment.INTENT_DURATION_ID, 3_000L)
                    startActivity(intent)
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
        if (dataType == DataType.MARKER) {
            playMarkersButton.visibility = View.VISIBLE
        }

        titleTextView = findViewById(R.id.list_title)
        supportFragmentManager.addFragmentOnAttachListener { _, fragment ->
            val name = (fragment as StashGridFragment<*, *, *>).name
            titleTextView.text = name
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment<*, *, *>?
            titleTextView.text = fragment?.name
            filterData = filterDataByName[fragment?.name]
            setUpSortButton()
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
                                )
                            }

                            FilterType.APP_FILTER -> {
                                intent.getParcelableExtra("filter")
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
                    setupFragment(savedFilter, false)
                    filter =
                        StashSavedFilter(
                            savedFilter.id,
                            savedFilter.mode,
                            savedFilter.find_filter?.sort,
                        )
                }

                filterButton.setOnClickListener {
                    listPopUp.show()
                    listPopUp.listView?.requestFocus()
                }
            }
        }
    }

    private fun setUpSortButton() {
        val listPopUp =
            ListPopupWindow(
                this@FilterListActivity,
                null,
                android.R.attr.listPopupWindowStyle,
            )
        val serverVersion = ServerPreferences(this).serverVersion
        // Resolve the strings, then sort
        val sortOptions =
            dataType.sortOptions
                .filter { serverVersion.isAtLeast(it.requiresVersion) }
                .map {
                    Pair(
                        it.key,
                        this@FilterListActivity.getString(it.nameStringId),
                    )
                }.sortedBy { it.second }
        val resolvedNames = sortOptions.map { it.second }

        val currentDirection = filterData?.find_filter?.direction
        val currentKey = filterData?.find_filter?.sort
        val isRandom = currentKey?.startsWith("random") ?: false
        val index =
            if (isRandom) {
                sortOptions.map { it.first }.indexOf("random")
            } else {
                sortOptions.map { it.first }.indexOf(currentKey)
            }
        setSortButtonText(
            currentDirection,
            if (index >= 0) sortOptions[index].second else null,
            isRandom,
        )
        Log.v(TAG, "index=$index, currentKey=$currentKey, currentDirection=$currentDirection")
        val adapter =
            SortByArrayAdapter(
                this@FilterListActivity,
                resolvedNames,
                index,
                currentDirection,
            )
        listPopUp.setAdapter(adapter)
        listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
        listPopUp.anchorView = sortButton

        listPopUp.width = getMaxMeasuredWidth(this@FilterListActivity, adapter)
        listPopUp.isModal = true

        listPopUp.setOnItemClickListener { parent: AdapterView<*>, view: View, position: Int, id: Long ->
            val newSortBy = sortOptions[position].first
            listPopUp.dismiss()
            if (filterData == null) {
                return@setOnItemClickListener
            }

            val currentDirection = filterData?.find_filter?.direction
            val currentKey = filterData?.find_filter?.sort
            val newDirection =
                if (newSortBy == currentKey && currentDirection != null) {
                    if (currentDirection == SortDirectionEnum.ASC) SortDirectionEnum.DESC else SortDirectionEnum.ASC
                } else {
                    currentDirection ?: SortDirectionEnum.DESC
                }
            val resolvedNewSortBy =
                if (newSortBy.startsWith("random")) {
                    getRandomSort()
                } else {
                    newSortBy
                }
            Log.v(TAG, "New sort: resolvedNewSortBy=$resolvedNewSortBy, newDirection=$newDirection")
            val newFilter =
                filterData!!.copy(
                    find_filter =
                        SavedFilterData.Find_filter(
                            q = null,
                            page = null,
                            per_page = null,
                            sort = resolvedNewSortBy,
                            direction = newDirection,
                            __typename = "",
                        ),
                )
            this@FilterListActivity.filterData = newFilter
            setupFragment(newFilter, false, false)
            filter =
                StashSavedFilter(
                    newFilter.id,
                    newFilter.mode,
                    newFilter.find_filter?.sort,
                )
            filterData = newFilter
        }

        sortButton.setOnClickListener {
            val currentDirection = filterData?.find_filter?.direction
            val currentKey = filterData?.find_filter?.sort
            val index = sortOptions.map { it.first }.indexOf(currentKey)
            adapter.currentIndex = index
            adapter.currentDirection = currentDirection

            listPopUp.show()
            listPopUp.listView?.requestFocus()
        }
    }

    private fun setSortButtonText(
        currentDirection: SortDirectionEnum?,
        sortBy: CharSequence?,
        isRandom: Boolean,
    ) {
        val directionString =
            when (currentDirection) {
                SortDirectionEnum.ASC -> getString(R.string.fa_caret_up)
                SortDirectionEnum.DESC -> getString(R.string.fa_caret_down)
                SortDirectionEnum.UNKNOWN__ -> null
                null -> null
            }
        if (isRandom) {
            sortButton.text = getString(R.string.stashapp_random)
        } else if (directionString != null && sortBy != null) {
            SpannableString(directionString + " " + sortBy).apply {
                val start = 0
                val end = 1
                setSpan(
                    FontSpan(StashApplication.getFont(R.font.fa_solid_900)),
                    start,
                    end,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE,
                )
                sortButton.text = this
            }
        } else if (sortBy != null) {
            sortButton.text = sortBy
        } else {
            sortButton.text = "Sort By"
        }
    }

    private suspend fun getStartingFilter(): Pair<FilterType, SavedFilterData?> =
        withContext(Dispatchers.IO) {
            if (filter is AppFilter) {
                Log.v(TAG, "getStartingFilter: filter is AppFilter=$filter")
                return@withContext Pair(
                    FilterType.APP_FILTER,
                    (filter as AppFilter).toSavedFilterData(this@FilterListActivity),
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
        val fragment =
            try {
                getFragment(
                    name,
                    dataType,
                    filter.find_filter?.toFindFilterType(),
                    filter.object_filter,
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
        filterDataByName[name] = filter
        setUpSortButton()

        if (first) {
            // If the first page, maybe scroll
            val pageSize = manager.getInt("maxSearchResults", 50)
            val scrollToNextResult = manager.getBoolean("scrollToNextResult", true)
            val moveOnePage = intent.getBooleanExtra("moveOnePage", false)
            if (moveOnePage && scrollToNextResult) {
                // Caller wants to scroll and user has it enabled
                fragment.pagingAdapter.registerObserver(
                    object : ObjectAdapter.DataObserver() {
                        override fun onChanged() {
                            Log.v(TAG, "Skipping one page")
                            fragment.setSelectedPosition(pageSize)
                            fragment.pagingAdapter.unregisterObserver(this)
                        }
                    },
                )
            }
        }
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
    ): StashGridFragment<out Query.Data, out Any, out Query.Data> {
        val cardSize =
            PreferenceManager.getDefaultSharedPreferences(this)
                .getInt("cardSize", getString(R.string.card_size_default))
        val calculatedCardSize =
            (cardSize * (ScenePresenter.CARD_WIDTH.toDouble() / dataType.defaultCardWidth)).toInt()
        val filterParser = FilterParser(ServerPreferences(this).serverVersion)
        return when (dataType) {
            DataType.SCENE -> {
                val sceneFilter = filterParser.convertSceneObjectFilter(objectFilter)
                StashGridFragment(
                    SceneComparator,
                    SceneDataSupplier(findFilter, sceneFilter),
                    calculatedCardSize,
                    name,
                )
            }

            DataType.STUDIO -> {
                val studioFilter = filterParser.convertStudioObjectFilter(objectFilter)
                StashGridFragment(
                    StudioComparator,
                    StudioDataSupplier(findFilter, studioFilter),
                    calculatedCardSize,
                    name,
                )
            }

            DataType.PERFORMER -> {
                val performerFilter =
                    filterParser.convertPerformerObjectFilter(objectFilter)
                StashGridFragment(
                    PerformerComparator,
                    PerformerDataSupplier(findFilter, performerFilter),
                    calculatedCardSize,
                    name,
                )
            }

            DataType.TAG -> {
                val tagFilter = filterParser.convertTagObjectFilter(objectFilter)
                val selectorPresenter =
                    ClassPresenterSelector().addClassPresenter(
                        TagData::class.java,
                        TagPresenter(),
                    )
                StashGridFragment(
                    selectorPresenter,
                    TagComparator,
                    TagDataSupplier(findFilter, tagFilter),
                    calculatedCardSize,
                    name,
                )
            }

            DataType.MOVIE -> {
                val movieFilter = filterParser.convertMovieObjectFilter(objectFilter)
                StashGridFragment(
                    MovieComparator,
                    MovieDataSupplier(findFilter, movieFilter),
                    calculatedCardSize,
                    name,
                )
            }

            DataType.MARKER -> {
                val markerFilter =
                    if (objectFilter is SceneMarkerFilterType) {
                        objectFilter
                    } else {
                        filterParser.convertMarkerObjectFilter(objectFilter)
                    }
                StashGridFragment(
                    MarkerComparator,
                    MarkerDataSupplier(findFilter, markerFilter),
                    calculatedCardSize,
                    name,
                )
            }

            DataType.IMAGE -> {
                val imageFilter =
                    if (objectFilter is ImageFilterType) {
                        objectFilter
                    } else {
                        filterParser.convertImageObjectFilter(objectFilter)
                    }
                val fragment =
                    StashGridFragment(
                        ImageComparator,
                        ImageDataSupplier(findFilter, imageFilter),
                        calculatedCardSize,
                        name,
                    )
                fragment.onItemViewClickedListener =
                    ImageGridClickedListener(this, fragment) {
                        it.putExtra(ImageActivity.INTENT_FILTER, filter)
                        it.putExtra(ImageActivity.INTENT_FILTER_TYPE, filter?.filterType?.name)
                    }

                fragment
            }

            DataType.GALLERY -> {
                val galleryFilter =
                    if (objectFilter is GalleryFilterType) {
                        objectFilter
                    } else {
                        filterParser.convertGalleryObjectFilter(objectFilter)
                    }
                StashGridFragment(
                    GalleryComparator,
                    GalleryDataSupplier(findFilter, galleryFilter),
                    calculatedCardSize,
                    name,
                )
            }
        }
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
}
