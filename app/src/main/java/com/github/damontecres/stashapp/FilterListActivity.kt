package com.github.damontecres.stashapp

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
import com.github.damontecres.stashapp.util.convertFilter
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.getMaxMeasuredWidth
import com.github.damontecres.stashapp.views.ImageGridClickedListener
import com.github.damontecres.stashapp.views.StashOnFocusChangeListener
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class FilterListActivity : FragmentActivity() {
    private lateinit var titleTextView: TextView
    private lateinit var queryEngine: QueryEngine
    private var filter: StashFilter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filter = intent.getParcelableExtra<StashFilter>("filter")
        queryEngine = QueryEngine(this, true)

        setContentView(R.layout.filter_list)

        val dataTypeStr = intent.getStringExtra("dataType")
        val dataType =
            if (dataTypeStr != null) {
                DataType.valueOf(dataTypeStr)
            } else {
                DataType.SCENE
            }

        val filterButton = findViewById<Button>(R.id.filter_button)
        filterButton.setOnClickListener {
            Toast.makeText(this, "Filters not loaded yet!", Toast.LENGTH_SHORT).show()
        }
        val onFocusChangeListener = StashOnFocusChangeListener(this)
        filterButton.onFocusChangeListener = onFocusChangeListener

        titleTextView = findViewById(R.id.list_title)
        supportFragmentManager.addFragmentOnAttachListener { _, fragment ->
            val name = (fragment as StashGridFragment<*, *>).name
            titleTextView.text = name
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.list_fragment) as StashGridFragment<*, *>?
            titleTextView.text = fragment?.name
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
                // TODO: Hide the filter button?
//                filterButton.visibility = View.INVISIBLE
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
                    val savedFilter = savedFilters[position]
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

    private suspend fun getStartingFilter(): Pair<FilterType, SavedFilterData?> {
        if (filter is AppFilter) {
            return Pair(
                FilterType.APP_FILTER,
                (filter as AppFilter).toSavedFilterData(this),
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
            // Load a saved filter
            return Pair(
                FilterType.SAVED_FILTER,
                queryEngine.getSavedFilter(savedFilterId.toString()),
            )
        } else if (direction != null || sortBy != null || query != null) {
            // Generic filter
            return Pair(
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
                return Pair(
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
                return Pair(FilterType.SAVED_FILTER, filter)
            }
        }
    }

    private fun setupFragment(
        filter: SavedFilterData,
        first: Boolean,
    ) {
        val dataType = DataType.fromFilterMode(filter.mode)!!
        val name =
            if (filter.name.isBlank()) {
                getString(dataType.pluralStringId)
            } else {
                filter.name
            }
        val fragment =
            getFragment(name, dataType, convertFilter(filter.find_filter), filter.object_filter)
        fragment.requestFocus = true

        if (first) {
            // If the first page, maybe scroll
            val pageSize =
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getInt("maxSearchResults", 50)
            val scrollToNextResult =
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean("scrollToNextResult", true)
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
        if (!first && transaction.isAddToBackStackAllowed) {
            transaction = transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun getFragment(
        name: String,
        dataType: DataType,
        findFilter: FindFilterType?,
        objectFilter: Any?,
    ): StashGridFragment<out Query.Data, out Any> {
        val cardSize =
            PreferenceManager.getDefaultSharedPreferences(this)
                .getInt("cardSize", getString(R.string.card_size_default))
        val calculatedCardSize =
            (cardSize * (ScenePresenter.CARD_WIDTH.toDouble() / dataType.defaultCardWidth)).toInt()
        // TODO other sizes
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
}
