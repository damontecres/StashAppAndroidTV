package com.github.damontecres.stashapp

import android.content.Context
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
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.ObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.suppliers.MarkerDataSupplier
import com.github.damontecres.stashapp.suppliers.MovieDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.suppliers.StudioDataSupplier
import com.github.damontecres.stashapp.suppliers.TagDataSupplier
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.ImageComparator
import com.github.damontecres.stashapp.util.MarkerComparator
import com.github.damontecres.stashapp.util.MovieComparator
import com.github.damontecres.stashapp.util.PerformerComparator
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.SceneComparator
import com.github.damontecres.stashapp.util.StudioComparator
import com.github.damontecres.stashapp.util.TagComparator
import com.github.damontecres.stashapp.util.convertFilter
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.toPx
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class FilterListActivity : FragmentActivity() {
    private lateinit var titleTextView: TextView
    private lateinit var queryEngine: QueryEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            val filter = getStartingFilter()
            if (savedInstanceState == null) {
                if (filter != null) {
                    setupFragment(filter, true)
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
                listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
                listPopUp.anchorView = filterButton
                // TODO: Better width calculation
                listPopUp.width = this@FilterListActivity.toPx(250).toInt()
                listPopUp.isModal = true

                val adapter =
                    ArrayAdapter(
                        this@FilterListActivity,
                        R.layout.popup_item,
                        savedFilters.map { it.name },
                    )
                listPopUp.setAdapter(adapter)

                listPopUp.setOnItemClickListener { parent: AdapterView<*>, view: View, position: Int, id: Long ->
                    val filter = savedFilters[position]
                    listPopUp.dismiss()
                    setupFragment(filter, false)
                }

                filterButton.setOnClickListener {
                    listPopUp.show()
                    listPopUp.listView?.requestFocus()
                }
            }
        }
    }

    private suspend fun getStartingFilter(): SavedFilterData? {
        val savedFilterId = intent.getStringExtra("savedFilterId")
        val direction = intent.getStringExtra("direction")
        val sortBy = intent.getStringExtra("sortBy")
        val dataTypeStr = intent.getStringExtra("dataType")
        val dataType =
            if (dataTypeStr != null) {
                DataType.valueOf(dataTypeStr)
            } else {
                throw RuntimeException("dataType is required")
            }
        val query = intent.getStringExtra("query")
        if (savedFilterId != null) {
            // Load a saved filter
            return queryEngine.getSavedFilter(savedFilterId.toString())
        } else if (direction != null || sortBy != null || query != null) {
            // Generic filter
            return SavedFilterData(
                id = "-1",
                mode = dataType.filterMode,
                name = getString(dataType.pluralStringId),
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
            )
        } else {
            // Default filter
            val filter = queryEngine.getDefaultFilter(dataType)
            if (filter == null) {
                return SavedFilterData(
                    id = "-1",
                    mode = dataType.filterMode,
                    name = getString(dataType.pluralStringId),
                    find_filter =
                        SavedFilterData.Find_filter(
                            q = null,
                            page = null,
                            per_page = null,
                            sort = null,
                            direction = null,
                            __typename = "",
                        ),
                    object_filter = null,
                    ui_options = null,
                    __typename = "",
                )
            } else {
                return filter
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
        val performerCardSize =
            (cardSize * (ScenePresenter.CARD_WIDTH.toDouble() / PerformerPresenter.CARD_WIDTH)).toInt()
        // TODO other sizes
        return when (dataType) {
            DataType.SCENE -> {
                val sceneFilter =
                    FilterParser.instance.convertSceneObjectFilter(objectFilter)
                StashGridFragment(
                    SceneComparator,
                    SceneDataSupplier(findFilter, sceneFilter),
                    null,
                    name,
                )
            }

            DataType.STUDIO -> {
                val studioFilter =
                    FilterParser.instance.convertStudioObjectFilter(objectFilter)
                StashGridFragment(
                    StudioComparator,
                    StudioDataSupplier(findFilter, studioFilter),
                    null,
                    name,
                )
            }

            DataType.PERFORMER -> {
                val performerFilter =
                    FilterParser.instance.convertPerformerObjectFilter(objectFilter)
                StashGridFragment(
                    PerformerComparator,
                    PerformerDataSupplier(findFilter, performerFilter),
                    performerCardSize,
                    name,
                )
            }

            DataType.TAG -> {
                val tagFilter =
                    FilterParser.instance.convertTagObjectFilter(objectFilter)
                val selectorPresenter =
                    ClassPresenterSelector().addClassPresenter(
                        TagData::class.java,
                        TagPresenter(TagLongClickCallBack()),
                    )
                StashGridFragment(
                    selectorPresenter,
                    TagComparator,
                    TagDataSupplier(findFilter, tagFilter),
                    null,
                    null,
                    name,
                )
            }

            DataType.MOVIE -> {
                val movieFilter = FilterParser.instance.convertMovieObjectFilter(objectFilter)
                StashGridFragment(
                    MovieComparator,
                    MovieDataSupplier(findFilter, movieFilter),
                    null,
                    name,
                )
            }

            DataType.MARKER -> {
                val markerFilter =
                    if (objectFilter is SceneMarkerFilterType) {
                        objectFilter
                    } else {
                        FilterParser.instance.convertMarkerObjectFilter(objectFilter)
                    }
                val selectorPresenter =
                    ClassPresenterSelector().addClassPresenter(
                        MarkerData::class.java,
                        MarkerPresenter(MarkerLongClickCallBack(this)),
                    )
                StashGridFragment(
                    selectorPresenter,
                    MarkerComparator,
                    MarkerDataSupplier(findFilter, markerFilter),
                    null,
                    null,
                    name,
                )
            }

            DataType.IMAGE -> {
                val imageFilter =
                    if (objectFilter is ImageFilterType) {
                        objectFilter
                    } else {
                        FilterParser.instance.convertImageObjectFilter(objectFilter)
                    }
                StashGridFragment(
                    ImageComparator,
                    ImageDataSupplier(findFilter, imageFilter),
                    null,
                    name,
                )
            }

            DataType.GALLERY -> {
                TODO()
            }
        }
    }

    class MarkerLongClickCallBack(private val context: Context) :
        StashPresenter.LongClickCallBack<MarkerData> {
        override val popUpItems: List<String>
            get() = listOf(context.getString(R.string.go_to_scene))

        override fun onItemLongClick(
            item: MarkerData,
            popUpItemPosition: Int,
        ) {
            val intent = Intent(context, VideoDetailsActivity::class.java)
            intent.putExtra(VideoDetailsActivity.MOVIE, item.scene.slimSceneData.id)
            context.startActivity(intent)
        }
    }

    inner class TagLongClickCallBack : StashPresenter.LongClickCallBack<TagData> {
        override val popUpItems: List<String>
            get() =
                listOf(
                    this@FilterListActivity.getString(R.string.view_scenes),
                    this@FilterListActivity.getString(R.string.view_markers),
                )

        override fun onItemLongClick(
            item: TagData,
            popUpItemPosition: Int,
        ) {
            if (popUpItemPosition == 0) {
                // scenes
                StashItemViewClickListener(this@FilterListActivity).onItemClicked(item)
            } else {
                // markers
                val filter =
                    SavedFilterData(
                        id = "-1",
                        mode = FilterMode.SCENE_MARKERS,
                        name = item.name,
                        find_filter =
                            SavedFilterData.Find_filter(
                                q = null,
                                page = null,
                                per_page = null,
                                sort = null,
                                direction = null,
                                __typename = "",
                            ),
                        object_filter =
                            SceneMarkerFilterType(
                                tags =
                                    Optional.present(
                                        HierarchicalMultiCriterionInput(
                                            value =
                                                Optional.present(
                                                    listOf(item.id),
                                                ),
                                            modifier = CriterionModifier.INCLUDES,
                                        ),
                                    ),
                            ),
                        ui_options = null,
                        __typename = "",
                    )
                setupFragment(filter, false)
            }
        }
    }

    companion object {
        const val TAG = "FilterListActivity"
    }
}
